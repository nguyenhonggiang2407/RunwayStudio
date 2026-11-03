package vn.runway.store.config;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;

@Service
public class StudioDemoSeed {
  private final OutfitRepository outfits;
  private final OutfitItemRepository outfitItems;
  private final SceneRepository scenes;
  private final SceneHotspotRepository hotspots;
  private final SizeGuideRepository guides;
  private final SizeGuideRowRepository rows;
  private final PromotionCodeRepository promotions;
  private final ProductRepository products;
  private final VariantRepository variants;

  public StudioDemoSeed(
      OutfitRepository o,
      OutfitItemRepository i,
      SceneRepository s,
      SceneHotspotRepository h,
      SizeGuideRepository g,
      SizeGuideRowRepository r,
      PromotionCodeRepository p,
      ProductRepository products,
      VariantRepository v) {
    outfits = o;
    outfitItems = i;
    scenes = s;
    hotspots = h;
    guides = g;
    rows = r;
    promotions = p;
    this.products = products;
    variants = v;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void seed() {
    if (products.findById(2026009L).isEmpty()) return;
    var ps =
        List.of(
            products.findById(10883670835505L).orElseThrow(),
            products.findById(2026009L).orElseThrow(),
            products.findById(10893259735345L).orElseThrow());
    if (outfits.count() == 0) {
      var o = new Outfit();
      o.name = "The Everyday Edit";
      o.visibility = "PUBLIC";
      o.status = "PUBLISHED";
      outfits.saveAndFlush(o);
      int n = 0;
      for (var p : ps) {
        var i = new OutfitItem();
        i.outfitId = o.id;
        i.productId = p.id;
        i.variantId = variants.findByProductIdOrderByIdAsc(p.id).get(0).id;
        i.slot = List.of("TOP", "BOTTOM", "SHOES").get(n);
        i.sortOrder = n++;
        outfitItems.save(i);
      }
    }
    if (scenes.count() == 0)
      for (String slug : List.of("city", "weekend", "gallery")) {
        var s = new Scene();
        s.slug = slug;
        s.title =
            switch (slug) {
              case "city" -> "City, at your pace";
              case "weekend" -> "A slower kind of weekend";
              default -> "Everyday, on display";
            };
        s.coverImage =
            switch (slug) {
              case "city" -> "/media/original/50183a72cbfe3056fbabb0dc.webp";
              case "gallery" -> "/media/original/498c3e470b0739761850711d.webp";
              default -> "/media/original/0fec4316ef2291db71050c83.webp";
            };
        s.altText =
            switch (slug) {
              case "city" -> "Ảnh người mẫu mặc áo len xám và quần jeans rộng trên nền trắng.";
              case "gallery" -> "Ảnh người mẫu mặc áo thun trắng và quần jeans rộng trên nền trắng.";
              default -> "Ảnh người mẫu mặc áo màu olive và quần rộng màu nâu trên nền trắng.";
            };
        s.status = "PUBLISHED";
        s.publishedAt = Instant.now();
        scenes.saveAndFlush(s);
        var sceneProducts =
            slug.equals("gallery")
                ? List.of(products.findById(10883673653553L).orElseThrow(), ps.get(1), ps.get(2))
                : ps;
        int n = 0;
        for (var p : sceneProducts) {
          var h = new SceneHotspot();
          h.sceneId = s.id;
          h.productId = p.id;
          h.label = p.name;
          h.xPercent = List.of(50d, 50d, 60d).get(n);
          h.yPercent = List.of(31d, 69d, 96d).get(n++);
          hotspots.save(h);
        }
      }
    if (guides.count() == 0)
      for (String category : List.of("Áo", "Áo khoác", "Quần", "Giày")) {
        var g = new SizeGuide();
        g.category = category;
        guides.saveAndFlush(g);
        int count = category.equals("Giày") ? 7 : 4;
        for (int n = 0; n < count; n++) {
          var r = new SizeGuideRow();
          r.guideId = g.id;
          r.sortOrder = n;
          r.size =
              category.equals("Giày")
                  ? Integer.toString(40 + n)
                  : List.of("S", "M", "L", "XL").get(n);
          if (category.equals("Giày")) {
            r.minFoot = 24.5 + n;
            r.maxFoot = 25.5 + n;
          } else if (category.equals("Quần")) {
            r.minWaist = 68d + n * 8;
            r.maxWaist = 76d + n * 8;
          } else {
            r.minChest = 84d + n * 8;
            r.maxChest = 92d + n * 8;
          }
          rows.save(r);
        }
      }
    if (promotions.count() == 0) {
      var c = new PromotionCode();
      c.code = "STUDIO10";
      c.percentOff = 10;
      c.minSpend = 500000;
      c.maxDiscount = 150000;
      c.usageLimit = 1000;
      c.expiresAt = Instant.parse("2030-12-31T23:59:59Z");
      promotions.save(c);
    }
  }
}
