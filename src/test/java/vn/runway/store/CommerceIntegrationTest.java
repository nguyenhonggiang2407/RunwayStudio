package vn.runway.store;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.repository.*;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:runway-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@AutoConfigureMockMvc
@Transactional
class CommerceIntegrationTest extends DemoAccountFixtures {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired VariantRepository variants;
  @Autowired OrderRepository orders;
  @Autowired InventoryMovementRepository movements;
  @Autowired PromotionCodeRepository promotions;
  @Autowired AuditEventRepository audit;
  static final String USER = "demo@runway.local", ADMIN = "admin@runway.local";

  Long v() {
    return variants.findByProductIdOrderByIdAsc(2026009L).get(0).id;
  }

  String input(String key, String code) {
    return "{\"recipient\":\"Demo Customer\",\"phone\":\"0912345678\",\"address\":\"12 Test Street,"
        + " Hanoi\",\"requestId\":\""
        + key
        + "\",\"voucherCode\":\""
        + code
        + "\"}";
  }

  void add(MockHttpSession s, int qty) throws Exception {
    mvc.perform(
            post("/api/cart/items")
                .session(s)
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content("{\"variantId\":" + v() + ",\"quantity\":" + qty + "}"))
        .andExpect(status().isOk());
  }

  JsonNode place(String key, String code) throws Exception {
    var s = new MockHttpSession();
    add(s, 1);
    return json.readTree(
        mvc.perform(
                post("/api/checkout")
                    .session(s)
                    .with(user(USER))
                    .with(csrf())
                    .contentType("application/json")
                    .content(input(key, code)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void serverCalculatesDiscountAndShipping() throws Exception {
    var o = place("server-pricing-test-001", "studio10").path("order");
    assertThat(o.path("subtotal").asLong()).isEqualTo(890000);
    assertThat(o.path("discount").asLong()).isEqualTo(89000);
    assertThat(o.path("shipping").asLong()).isEqualTo(30000);
    assertThat(o.path("total").asLong()).isEqualTo(831000);
  }

  @Test
  void capAndShippingThresholdAreServerRules() throws Exception {
    var s = new MockHttpSession();
    add(s, 3);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(input("discount-cap-test-001", "STUDIO10")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order.discount").value(150000))
        .andExpect(jsonPath("$.order.shipping").value(0));
  }

  @Test
  void expiredVoucherRollsBackAndKeepsCart() throws Exception {
    var p = promotions.findByCode("STUDIO10").orElseThrow();
    p.expiresAt = Instant.now().minusSeconds(1);
    promotions.saveAndFlush(p);
    var s = new MockHttpSession();
    add(s, 1);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(input("expired-promo-test-001", "STUDIO10")))
        .andExpect(status().isBadRequest());
    assertThat(orders.count()).isZero();
    assertThat(movements.count()).isZero();
    assertThat(variants.findById(v()).orElseThrow().stock).isEqualTo(20);
    mvc.perform(get("/api/cart").with(user(USER))).andExpect(jsonPath("$.item_count").value(1));
  }

  @Test
  void voucherCannotBeUsedTwiceBySameAccount() throws Exception {
    place("first-promo-test-001", "STUDIO10");
    var s = new MockHttpSession();
    add(s, 1);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(input("second-promo-test-001", "STUDIO10")))
        .andExpect(status().isBadRequest());
    assertThat(orders.count()).isEqualTo(1);
  }

  @Test
  void exhaustedVoucherRejected() throws Exception {
    var p = promotions.findByCode("STUDIO10").orElseThrow();
    p.used = p.usageLimit;
    promotions.saveAndFlush(p);
    var s = new MockHttpSession();
    add(s, 1);
    mvc.perform(
            post("/api/checkout/quote")
                .session(s)
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content("{\"voucherCode\":\"STUDIO10\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void quoteDoesNotConsumeInventoryOrVoucher() throws Exception {
    var s = new MockHttpSession();
    add(s, 1);
    for (int n = 0; n < 2; n++)
      mvc.perform(
              post("/api/checkout/quote")
                  .session(s)
                  .with(user(USER))
                  .with(csrf())
                  .contentType("application/json")
                  .content("{\"voucherCode\":\"STUDIO10\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.discount").value(89000));
    assertThat(promotions.findByCode("STUDIO10").orElseThrow().used).isZero();
    assertThat(orders.count()).isZero();
    assertThat(movements.count()).isZero();
  }

  @Test
  void oldRetryDoesNotEraseNewPersistentCartInAnotherSession() throws Exception {
    var o = place("persistent-retry-test-001", "");
    var s = new MockHttpSession();
    add(s, 3);
    mvc.perform(
            post("/api/checkout")
                .session(new MockHttpSession())
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(input("persistent-retry-test-001", "")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order.id").value(o.path("order").path("id").asLong()));
    mvc.perform(get("/api/cart").session(s).with(user(USER)))
        .andExpect(jsonPath("$.item_count").value(3));
    assertThat(orders.count()).isEqualTo(1);
    assertThat(movements.count()).isEqualTo(1);
  }

  @Test
  void cancellationProducesOneRestockEvent() throws Exception {
    var o = place("inventory-ledger-test-001", "");
    String id = o.path("order").path("id").asText();
    mvc.perform(post("/api/orders/" + id + "/cancel").with(user(USER)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.history.length()").value(2));
    mvc.perform(post("/api/orders/" + id + "/cancel").with(user(USER)).with(csrf()))
        .andExpect(status().isBadRequest());
    assertThat(movements.count()).isEqualTo(2);
    assertThat(movements.findAll().stream().mapToInt(m -> m.delta).sum()).isZero();
  }

  @Test
  void inventoryAdjustmentIsAudited() throws Exception {
    mvc.perform(
            put("/api/admin/variants/" + v())
                .with(user(ADMIN).roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"size\":\"S\",\"price\":900000,\"stock\":5}"))
        .andExpect(status().isOk());
    assertThat(audit.count()).isEqualTo(1);
    assertThat(movements.findAll().get(0).delta).isEqualTo(-15);
  }

  @Test
  void dashboardCountsDeliveredRevenueOnly() throws Exception {
    var o = place("delivery-revenue-test-001", "");
    String id = o.path("order").path("id").asText();
    mvc.perform(get("/api/admin/dashboard").with(user(ADMIN).roles("ADMIN")))
        .andExpect(jsonPath("$.revenue").value(0))
        .andExpect(jsonPath("$.customers").value(1));
    for (String s : new String[] {"CONFIRMED", "SHIPPED", "DELIVERED"})
      mvc.perform(
              put("/api/admin/orders/" + id + "/status")
                  .with(user(ADMIN).roles("ADMIN"))
                  .with(csrf())
                  .contentType("application/json")
                  .content("{\"status\":\"" + s + "\"}"))
          .andExpect(status().isOk());
    mvc.perform(get("/api/admin/dashboard").with(user(ADMIN).roles("ADMIN")))
        .andExpect(jsonPath("$.revenue").value(920000));
    String day = LocalDate.now(ZoneOffset.UTC).toString();
    mvc.perform(
            get("/api/admin/reports/sales?from=" + day + "&to=" + day)
                .with(user(ADMIN).roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days[0].revenue").value(920000));
  }

  @Test
  void reportsRejectInvalidRanges() throws Exception {
    for (String q : new String[] {"from=2026-11-02&to=2026-01-01", "from=2020-01-01&to=2026-11-02"})
      mvc.perform(get("/api/admin/reports/sales?" + q).with(user(ADMIN).roles("ADMIN")))
          .andExpect(status().isBadRequest());
  }

  @Test
  void addressesAreOwnerScoped() throws Exception {
    String b =
        "{\"recipient\":\"Demo Customer\",\"phone\":\"0912345678\",\"address\":\"12 Test Street,"
            + " Hanoi\"}";
    var r =
        mvc.perform(
                post("/api/me/addresses")
                    .with(user(USER))
                    .with(csrf())
                    .contentType("application/json")
                    .content(b))
            .andExpect(status().isOk())
            .andReturn();
    Long id = json.readTree(r.getResponse().getContentAsString()).path("id").asLong();
    mvc.perform(get("/api/me/addresses").with(user(ADMIN)))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            put("/api/me/addresses/" + id)
                .with(user(ADMIN))
                .with(csrf())
                .contentType("application/json")
                .content(b))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/me/addresses/" + id).with(user(ADMIN)).with(csrf()))
        .andExpect(status().isNotFound());
  }
}
