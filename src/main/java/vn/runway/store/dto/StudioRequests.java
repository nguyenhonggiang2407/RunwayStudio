package vn.runway.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public final class StudioRequests {
  private StudioRequests() {}

  public record OutfitItem(
      @NotNull Long productId,
      Long variantId,
      @NotBlank @Pattern(regexp = "TOP|BOTTOM|SHOES|ACCESSORY") String slot) {}

  public record Outfit(
      @NotBlank @Size(max = 100) String name,
      Long version,
      @NotNull @Size(min = 1, max = 4) List<@NotNull @Valid OutfitItem> items) {}

  public record CartItem(@NotNull Long variantId, @Min(1) @Max(99) int quantity) {}

  public record OutfitCart(@NotNull @Size(min = 1, max = 4) List<@NotNull @Valid CartItem> items) {}

  public record Wardrobe(@NotNull Long productId, Long variantId) {}

  public record Hotspot(
      @NotNull Long productId,
      @NotNull @DecimalMin("0") @DecimalMax("100") Double xPercent,
      @NotNull @DecimalMin("0") @DecimalMax("100") Double yPercent,
      @NotBlank @Size(max = 80) String label) {}

  public record Scene(
      @NotBlank @Size(max = 100) String title,
      @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
      @NotBlank @Size(max = 700) String coverImage,
      @NotBlank @Size(max = 250) String altText,
      Long version,
      @NotNull @Size(max = 20) List<@NotNull @Valid Hotspot> hotspots) {}

  public record Fit(
      @NotBlank @Size(max = 60) String category,
      @DecimalMin("100") @DecimalMax("230") Double height,
      @DecimalMin("25") @DecimalMax("250") Double weight,
      @DecimalMin("50") @DecimalMax("180") Double chest,
      @DecimalMin("45") @DecimalMax("180") Double waist,
      @DecimalMin("15") @DecimalMax("35") Double footLength) {}
}
