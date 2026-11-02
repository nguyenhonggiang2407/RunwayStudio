package vn.runway.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.*;

public final class Responses {
  private Responses() {}

  public record Message(String message) {}

  public record User(Long id, String name, String email, String role) {}

  public record Variant(
      Long id, Long productId, String size, long price, int stock, Long version) {}

  public record Product(
      Long id,
      String slug,
      String name,
      String category,
      String description,
      String image,
      boolean active,
      List<Variant> variants,
      long price) {}

  public record Page<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <T> Page<T> of(org.springframework.data.domain.Page<T> p) {
      return new Page<>(
          p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
  }

  public record CartLine(
      Long id,
      Long variantId,
      Long productId,
      String slug,
      String name,
      String title,
      String size,
      int quantity,
      long price,
      long unitPrice,
      long lineTotal,
      String image,
      int stock,
      boolean available) {}

  public record Cart(
      List<CartLine> items,
      @JsonProperty("item_count") int itemCount,
      long subtotal,
      long shipping,
      long total,
      @JsonProperty("total_price") long totalPrice,
      String currency,
      Map<String, String> sections) {}

  public record OutfitItem(
      Long productId, Long variantId, String slot, int sortOrder, Product product) {}

  public record Outfit(
      Long id,
      String name,
      String visibility,
      String status,
      Long version,
      Instant createdAt,
      List<OutfitItem> items,
      long subtotal,
      boolean purchasable) {}

  public record Hotspot(
      Long id, Long productId, double xPercent, double yPercent, String label, Product product) {}

  public record Scene(
      Long id,
      String title,
      String slug,
      String coverImage,
      String altText,
      String status,
      Instant publishedAt,
      Long version,
      List<Hotspot> hotspots) {}

  public record Wardrobe(
      Long id, Long productId, Long variantId, Instant createdAt, Product product) {}

  public record GuideRow(
      String size,
      Double minChest,
      Double maxChest,
      Double minWaist,
      Double maxWaist,
      Double minFoot,
      Double maxFoot) {}

  public record SizeGuide(
      Long id, String category, int version, String unit, List<GuideRow> rows) {}

  public record Fit(
      String category,
      String size,
      String confidence,
      String reason,
      int guideVersion,
      String disclaimer) {}

  public record Address(Long id, String recipient, String phone, String address) {}

  public record Order(
      Long id,
      String recipient,
      String phone,
      String address,
      String note,
      String status,
      long subtotal,
      long shipping,
      long discount,
      long total,
      String promotionCode,
      Instant createdAt) {}

  public record OrderLine(
      Long id,
      Long variantId,
      String productName,
      String size,
      String image,
      int quantity,
      long unitPrice) {}

  public record OrderEvent(String status, Instant createdAt) {}

  public record OrderDetail(Order order, List<OrderLine> items, List<OrderEvent> history) {}

  public record Promotion(
      Long id,
      String code,
      int percentOff,
      long minSpend,
      long maxDiscount,
      int usageLimit,
      int used,
      Instant expiresAt,
      boolean active) {}

  public record Quote(
      long subtotal, long shipping, long discount, long total, String promotionCode) {}

  public record Dashboard(
      long orders,
      long revenue,
      long pending,
      long products,
      long customers,
      List<Variant> lowStock) {}

  public record SalesDay(String date, long orders, long revenue) {}

  public record SalesReport(String from, String to, String timezone, List<SalesDay> days) {}

  public record Audit(
      Long id, Long actorId, String action, String targetType, Long targetId, Instant createdAt) {}

  public record Movement(
      Long id,
      Long variantId,
      int delta,
      int resultingStock,
      String reason,
      String refType,
      Long refId,
      Instant createdAt) {}
}
