package vn.runway.store;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class StudioIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired VariantRepository variants;
  @Autowired ProductRepository products;
  @Autowired WardrobeItemRepository wardrobe;
  @Autowired EntityManagerFactory emf;
  static final String USER = "demo@runway.local", OTHER = "admin@runway.local";
  static final Long TOP = 10883670835505L, BOTTOM = 2026009L, SHOES = 10893259735345L;

  Long v(Long p) {
    return variants.findByProductIdOrderByIdAsc(p).get(0).id;
  }

  ObjectNode outfit() {
    var n = json.createObjectNode();
    n.put("name", "My everyday");
    var a = n.putArray("items");
    for (var slot : List.of("TOP", "BOTTOM", "SHOES")) {
      Long p = Map.of("TOP", TOP, "BOTTOM", BOTTOM, "SHOES", SHOES).get(slot);
      a.addObject().put("slot", slot).put("productId", p).put("variantId", v(p));
    }
    return n;
  }

  JsonNode createOutfit() throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/me/outfits")
                    .with(user(USER))
                    .with(csrf())
                    .contentType("application/json")
                    .content(outfit().toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  ObjectNode scene() {
    var n = json.createObjectNode();
    n.put("title", "Test scene")
        .put("slug", "test-scene")
        .put("coverImage", "/media/owned/scene-city.svg")
        .put("altText", "An illustrated outfit");
    n.putArray("hotspots")
        .addObject()
        .put("productId", TOP)
        .put("xPercent", 50)
        .put("yPercent", 40)
        .put("label", "Daily top");
    return n;
  }

  JsonNode createScene(ObjectNode n) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/admin/scenes")
                    .with(user(OTHER).roles("ADMIN"))
                    .with(csrf())
                    .contentType("application/json")
                    .content(n.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void saveReloadPreservesThreeSlotsAndSelectedVariant() throws Exception {
    var o = createOutfit();
    mvc.perform(get("/api/studio/outfits/" + o.path("id")).with(user(USER)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[1].variantId").value(v(BOTTOM)))
        .andExpect(jsonPath("$.visibility").value("PRIVATE"));
  }

  @Test
  void privateOutfitIsHiddenFromPublicAndOtherOwner() throws Exception {
    var o = createOutfit();
    for (String who : List.of(USER, OTHER)) {
      if (who.equals(USER))
        mvc.perform(get("/api/studio/outfits/" + o.path("id"))).andExpect(status().isNotFound());
      else
        mvc.perform(get("/api/studio/outfits/" + o.path("id")).with(user(who)))
            .andExpect(status().isNotFound());
    }
    mvc.perform(get("/api/studio/outfits")).andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void anotherOwnerCannotUpdateOrDeletePrivateOutfit() throws Exception {
    var o = createOutfit();
    var edit = outfit();
    edit.put("version", o.path("version").asLong());
    mvc.perform(
            put("/api/me/outfits/" + o.path("id"))
                .with(user(OTHER))
                .with(csrf())
                .contentType("application/json")
                .content(edit.toString()))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/me/outfits/" + o.path("id")).with(user(OTHER)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void itemOnlyUpdateIncrementsVersionAndStaleEditConflicts() throws Exception {
    var o = createOutfit();
    var edit = outfit();
    edit.put("version", o.path("version").asLong());
    ((ObjectNode) edit.withArray("items").get(0))
        .put("variantId", variants.findByProductIdOrderByIdAsc(TOP).get(1).id);
    mvc.perform(
            put("/api/me/outfits/" + o.path("id"))
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(edit.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(o.path("version").asInt() + 1));
    mvc.perform(
            put("/api/me/outfits/" + o.path("id"))
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(edit.toString()))
        .andExpect(status().isConflict());
  }

  @Test
  void duplicateSlotIsRejected() throws Exception {
    var n = outfit();
    ((ObjectNode) n.withArray("items").get(1)).put("slot", "TOP");
    mvc.perform(
            post("/api/me/outfits")
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(n.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void variantMustBelongToProduct() throws Exception {
    var n = outfit();
    ((ObjectNode) n.withArray("items").get(0)).put("variantId", v(SHOES));
    mvc.perform(
            post("/api/me/outfits")
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(n.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void wrongCategoryForSlotIsRejected() throws Exception {
    var n = outfit();
    ((ObjectNode) n.withArray("items").get(0)).put("productId", SHOES).put("variantId", v(SHOES));
    mvc.perform(
            post("/api/me/outfits")
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(n.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void successfulOutfitAddCreatesThreeLines() throws Exception {
    var n = json.createObjectNode();
    var a = n.putArray("items");
    for (Long p : List.of(TOP, BOTTOM, SHOES))
      a.addObject().put("variantId", v(p)).put("quantity", 1);
    mvc.perform(
            post("/api/cart/outfit")
                .with(csrf())
                .contentType("application/json")
                .content(n.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.item_count").value(3));
  }

  @Test
  void failedBatchDoesNotPartiallyChangeAccountCart() throws Exception {
    mvc.perform(
            post("/api/cart/items")
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content("{\"variantId\":" + v(TOP) + ",\"quantity\":2}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/cart/outfit")
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"items\":[{\"variantId\":"
                        + v(BOTTOM)
                        + ",\"quantity\":1},{\"variantId\":999,\"quantity\":1}]}"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/cart").with(user(USER)))
        .andExpect(jsonPath("$.item_count").value(2))
        .andExpect(jsonPath("$.items.length()").value(1));
  }

  @Test
  void accountCartSurvivesDifferentSessionAndIsIsolated() throws Exception {
    mvc.perform(
            post("/api/cart/items")
                .session(new MockHttpSession())
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content("{\"variantId\":" + v(TOP) + ",\"quantity\":2}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/cart").session(new MockHttpSession()).with(user(USER)))
        .andExpect(jsonPath("$.item_count").value(2));
    mvc.perform(get("/api/cart").with(user(OTHER))).andExpect(jsonPath("$.item_count").value(0));
  }

  @Test
  void guestMergeHappensOnce() throws Exception {
    var s = new MockHttpSession();
    mvc.perform(
            post("/api/cart/items")
                .session(s)
                .with(csrf())
                .contentType("application/json")
                .content("{\"variantId\":" + v(TOP) + ",\"quantity\":2}"))
        .andExpect(status().isOk());
    for (int n = 0; n < 2; n++)
      mvc.perform(get("/api/cart").session(s).with(user(USER)))
          .andExpect(jsonPath("$.item_count").value(2));
    assertThat(s.getAttribute("cart")).isNull();
  }

  @Test
  void wardrobeIdempotentWithNullAndSelectedVariant() throws Exception {
    for (int n = 0; n < 2; n++)
      mvc.perform(
              post("/api/me/wardrobe")
                  .with(user(USER))
                  .with(csrf())
                  .contentType("application/json")
                  .content("{\"productId\":" + TOP + "}"))
          .andExpect(status().isOk());
    for (int n = 0; n < 2; n++)
      mvc.perform(
              post("/api/me/wardrobe")
                  .with(user(USER))
                  .with(csrf())
                  .contentType("application/json")
                  .content("{\"productId\":" + TOP + ",\"variantId\":" + v(TOP) + "}"))
          .andExpect(status().isOk());
    assertThat(wardrobe.count()).isEqualTo(2);
  }

  @Test
  void wardrobeOwnerIsolation() throws Exception {
    var r =
        mvc.perform(
                post("/api/me/wardrobe")
                    .with(user(USER))
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"productId\":" + TOP + "}"))
            .andReturn();
    Long id = json.readTree(r.getResponse().getContentAsString()).path("id").asLong();
    mvc.perform(get("/api/me/wardrobe").with(user(OTHER)))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(delete("/api/me/wardrobe/" + id).with(user(OTHER)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void sceneDraftPublishUnpublishVisibility() throws Exception {
    var n = createScene(scene());
    mvc.perform(get("/api/scenes/test-scene")).andExpect(status().isNotFound());
    mvc.perform(
            post("/api/admin/scenes/" + n.path("id") + "/publish")
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/scenes/test-scene"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hotspots[0].xPercent").value(50));
    mvc.perform(
            post("/api/admin/scenes/" + n.path("id") + "/unpublish")
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/scenes/test-scene")).andExpect(status().isNotFound());
  }

  @Test
  void sceneOnlyHotspotChangeIncrementsVersion() throws Exception {
    var n = createScene(scene());
    var edit = scene();
    edit.put("version", n.path("version").asLong());
    ((ObjectNode) edit.withArray("hotspots").get(0)).put("xPercent", 75);
    mvc.perform(
            put("/api/admin/scenes/" + n.path("id"))
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(edit.toString()))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/admin/scenes/" + n.path("id"))
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(edit.toString()))
        .andExpect(status().isConflict());
  }

  @Test
  void invalidHotspotCoordinateIsRejected() throws Exception {
    var n = scene();
    ((ObjectNode) n.withArray("hotspots").get(0)).put("xPercent", 101);
    mvc.perform(
            post("/api/admin/scenes")
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(n.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unapprovedMediaIsRejected() throws Exception {
    var n = scene();
    n.put("coverImage", "https://example.org/image.jpg");
    mvc.perform(
            post("/api/admin/scenes")
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content(n.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void emptyDraftCannotPublish() throws Exception {
    var n = scene();
    n.putArray("hotspots");
    var s = createScene(n);
    mvc.perform(
            post("/api/admin/scenes/" + s.path("id") + "/publish")
                .with(user(OTHER).roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sceneAdminRequiresRoleAndCsrf() throws Exception {
    mvc.perform(
            post("/api/admin/scenes")
                .with(user(USER))
                .with(csrf())
                .contentType("application/json")
                .content(scene().toString()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/admin/scenes")
                .with(user(OTHER).roles("ADMIN"))
                .contentType("application/json")
                .content(scene().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void hiddenSceneProductStillExplainsButCannotBuy() throws Exception {
    var p = products.findById(TOP).orElseThrow();
    p.active = false;
    products.saveAndFlush(p);
    mvc.perform(get("/api/scenes/city"))
        .andExpect(jsonPath("$.hotspots[0].product.active").value(false));
    mvc.perform(
            post("/api/cart/items")
                .with(csrf())
                .contentType("application/json")
                .content("{\"variantId\":" + v(TOP) + ",\"quantity\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void catalogPaginatesSortsAndTreatsWildcardsLiterally() throws Exception {
    mvc.perform(get("/api/catalog?size=3&sort=price-asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(11))
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].price").value(490000));
    mvc.perform(get("/api/catalog").param("q", "%_"))
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void catalogUsesThreeQueriesForFullPage() throws Exception {
    products.flush();
    var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();
    mvc.perform(get("/api/catalog?size=60")).andExpect(status().isOk());
    assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    stats.setStatisticsEnabled(false);
  }

  @Test
  void invalidPaginationRejected() throws Exception {
    mvc.perform(get("/api/catalog?size=61")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/catalog?minPrice=20&maxPrice=10")).andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource({"84,S", "91.99,S", "92,M", "99.99,M", "100,L", "107.99,L", "108,XL", "115.99,XL"})
  void chestBoundary(double value, String size) throws Exception {
    mvc.perform(
            post("/api/size-recommendations")
                .with(csrf())
                .contentType("application/json")
                .content("{\"category\":\"Áo\",\"chest\":" + value + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(size));
  }

  @ParameterizedTest
  @CsvSource({"24.5,40", "25.49,40", "25.5,41", "26.5,42", "30.5,46"})
  void shoeUsesFootMeasurement(double value, String size) throws Exception {
    mvc.perform(
            post("/api/size-recommendations")
                .with(csrf())
                .contentType("application/json")
                .content("{\"category\":\"Giày\",\"footLength\":" + value + ",\"chest\":100}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(size));
  }

  @Test
  void optionalMeasurementsAndOutsideGuideExplain() throws Exception {
    mvc.perform(
            post("/api/size-recommendations")
                .with(csrf())
                .contentType("application/json")
                .content("{\"category\":\"Giày\",\"height\":175}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.confidence").value("INSUFFICIENT_DATA"));
    mvc.perform(
            post("/api/size-recommendations")
                .with(csrf())
                .contentType("application/json")
                .content("{\"category\":\"Áo\",\"chest\":120}"))
        .andExpect(jsonPath("$.confidence").value("OUTSIDE_GUIDE"))
        .andExpect(jsonPath("$.disclaimer").isNotEmpty());
  }
}
