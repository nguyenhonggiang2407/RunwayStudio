package vn.runway.store;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:runway-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@AutoConfigureMockMvc
@Transactional
class StoreIntegrationTest extends DemoAccountFixtures {
  @Autowired MockMvc mvc;
  @Autowired VariantRepository variants;
  @Autowired OrderRepository orders;
  @Autowired AccountRepository accounts;
  @Autowired ProductRepository products;

  private Long id() {
    return variants.findAll().get(0).id;
  }

  private MockHttpSession bag(int qty) throws Exception {
    MockHttpSession s = new MockHttpSession();
    mvc.perform(
            post("/cart/add.js")
                .session(s)
                .with(csrf())
                .contentType("application/json")
                .content("{\"items\":[{\"id\":" + id() + ",\"quantity\":" + qty + "}]}"))
        .andExpect(status().isOk());
    return s;
  }

  private String checkout(String key) {
    return "{\"recipient\":\"Nguyễn Văn An\",\"phone\":\"0912345678\",\"address\":\"12 Phố Huế, Hai"
               + " Bà Trưng, Hà Nội\",\"note\":\"Giao giờ hành chính\",\"requestId\":\""
        + key
        + "\"}";
  }

  @Test
  void anonymousCanReadSessionState() throws Exception {
    mvc.perform(get("/api/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void validationErrorsIncludeFieldsAndRequestId() throws Exception {
    mvc.perform(
            post("/api/register")
                .with(csrf())
                .contentType("application/json")
                .content("{\"name\":\"\",\"email\":\"invalid\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.email").exists())
        .andExpect(header().exists("X-Request-Id"));
  }

  @Test
  void publicCatalogHasProductsAndVariants() throws Exception {
    mvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(11))
        .andExpect(jsonPath("$[0].variants[0].stock").value(20));
  }

  @Test
  void csrfIsRequiredForWrites() throws Exception {
    mvc.perform(
            post("/api/cart/items")
                .contentType("application/json")
                .content("{\"variantId\":" + id() + ",\"quantity\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminRejectsCustomer() throws Exception {
    mvc.perform(get("/api/admin/products").with(user("demo@runway.local").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousCannotPlaceOrder() throws Exception {
    mvc.perform(
            post("/api/checkout")
                .with(csrf())
                .contentType("application/json")
                .content(checkout("anonymous-request-0001")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void oversizedCartQuantityRejected() throws Exception {
    mvc.perform(
            post("/api/cart/items")
                .with(csrf())
                .contentType("application/json")
                .content("{\"variantId\":" + id() + ",\"quantity\":100}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMoreThanStock() throws Exception {
    MockHttpSession s = new MockHttpSession();
    mvc.perform(
            post("/cart/add.js")
                .session(s)
                .with(csrf())
                .contentType("application/json")
                .content("{\"id\":" + id() + ",\"quantity\":21}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/cart").session(s)).andExpect(jsonPath("$.item_count").value(0));
  }

  @Test
  void batchAddIsAtomicOnFailure() throws Exception {
    MockHttpSession s = new MockHttpSession();
    mvc.perform(
            post("/cart/add.js")
                .session(s)
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"id\":"
                        + id()
                        + ",\"quantity\":1},{\"id\":999,\"quantity\":1}]}"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/cart").session(s)).andExpect(jsonPath("$.item_count").value(0));
  }

  @Test
  void checkoutSnapshotsPriceAndPreventsDuplicateOrder() throws Exception {
    Long v = id();
    MockHttpSession s = bag(2);
    String body = checkout("checkout-idempotency-0001");
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/checkout")
                  .session(s)
                  .with(user("demo@runway.local"))
                  .with(csrf())
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.order.status").value("PLACED"));
    assertThat(orders.count()).isEqualTo(1);
    assertThat(variants.findById(v).orElseThrow().stock).isEqualTo(18);
    mvc.perform(get("/api/cart").session(s)).andExpect(jsonPath("$.item_count").value(0));
  }

  @Test
  void failedCheckoutRollsBackAllStock() throws Exception {
    MockHttpSession s = bag(2);
    Variant v = variants.findById(id()).orElseThrow();
    v.stock = 1;
    variants.saveAndFlush(v);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user("demo@runway.local"))
                .with(csrf())
                .contentType("application/json")
                .content(checkout("stock-failure-request-0001")))
        .andExpect(status().isBadRequest());
    assertThat(orders.count()).isZero();
    assertThat(variants.findById(v.id).orElseThrow().stock).isEqualTo(1);
  }

  @Test
  void cancellationRestocksExactlyOnce() throws Exception {
    MockHttpSession s = bag(2);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user("demo@runway.local"))
                .with(csrf())
                .contentType("application/json")
                .content(checkout("cancel-order-request-0001")))
        .andExpect(status().isOk());
    Long orderId = orders.findAll().get(0).id;
    mvc.perform(
            post("/api/orders/" + orderId + "/cancel").with(user("demo@runway.local")).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/orders/" + orderId + "/cancel").with(user("demo@runway.local")).with(csrf()))
        .andExpect(status().isBadRequest());
    assertThat(variants.findById(id()).orElseThrow().stock).isEqualTo(20);
  }

  @Test
  void cannotCancelAnotherCustomersOrder() throws Exception {
    MockHttpSession s = bag(1);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user("demo@runway.local"))
                .with(csrf())
                .contentType("application/json")
                .content(checkout("private-order-request-0001")))
        .andExpect(status().isOk());
    Long orderId = orders.findAll().get(0).id;
    mvc.perform(
            post("/api/orders/" + orderId + "/cancel")
                .with(user("admin@runway.local").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsSkippingOrderStatus() throws Exception {
    MockHttpSession s = bag(1);
    mvc.perform(
            post("/api/checkout")
                .session(s)
                .with(user("demo@runway.local"))
                .with(csrf())
                .contentType("application/json")
                .content(checkout("status-order-request-0001")))
        .andExpect(status().isOk());
    Long orderId = orders.findAll().get(0).id;
    mvc.perform(
            put("/api/admin/orders/" + orderId + "/status")
                .with(user("admin@runway.local").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"DELIVERED\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void realLoginCreatesSessionAndRejectsWrongPassword() throws Exception {
    mvc.perform(
            post("/api/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"demo@runway.local\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    MvcResult r =
        mvc.perform(
                post("/api/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        "{\"email\":\"demo@runway.local\",\"password\":\""
                            + CUSTOMER_FIXTURE_PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
    mvc.perform(get("/api/me").session((MockHttpSession) r.getRequest().getSession()))
        .andExpect(jsonPath("$.user.email").value("demo@runway.local"));
  }

  @Test
  void duplicateEmailIsCaseInsensitive() throws Exception {
    mvc.perform(
            post("/api/register")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"name\":\"Test\",\"email\":\"DEMO@RUNWAY.LOCAL\",\"password\":\"Password123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminCanCreateAndUpdateStock() throws Exception {
    mvc.perform(
            put("/api/admin/variants/" + id())
                .with(user("admin@runway.local").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"size\":\"42\",\"price\":1200000,\"stock\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(5));
  }

  @Test
  void frontendRoutesResolve() throws Exception {
    mvc.perform(get("/shop")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    mvc.perform(get("/scenes")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  }
}
