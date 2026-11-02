package vn.runway.store.dto;

import jakarta.validation.constraints.*;

public final class Requests {
  private Requests() {}

  public record Register(
      @NotBlank @Size(max = 100) String name,
      @NotBlank @Email @Size(max = 150) String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record Login(
      @NotBlank @Email @Size(max = 150) String email, @NotBlank @Size(max = 72) String password) {}

  public record CartChange(@NotNull Long variantId, @Min(0) @Max(99) int quantity) {}

  public record Checkout(
      @NotBlank @Size(max = 100) String recipient,
      @NotBlank @Pattern(regexp = "0[0-9]{9}") String phone,
      @NotBlank @Size(min = 10, max = 500) String address,
      @Size(max = 500) String note,
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,80}") String requestId,
      @Size(max = 30) String voucherCode) {
    public Checkout(String r, String p, String a, String n, String key) {
      this(r, p, a, n, key, null);
    }
  }

  public record ProductInput(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 160) String slug,
      @NotBlank @Size(max = 60) String category,
      @Size(max = 2000) String description,
      @NotBlank @Pattern(regexp = "/media/owned/[a-z0-9-]+\\.svg") String image,
      boolean active) {}

  public record VariantInput(
      @NotBlank @Size(max = 80) String size,
      @Min(1000) @Max(1000000000) long price,
      @Min(0) @Max(1000000) int stock) {}

  public record Address(
      @NotBlank @Size(max = 100) String recipient,
      @NotBlank @Pattern(regexp = "0[0-9]{9}") String phone,
      @NotBlank @Size(min = 10, max = 500) String address) {}

  public record Promotion(
      @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,30}") String code,
      @Min(1) @Max(100) int percentOff,
      @Min(0) long minSpend,
      @Min(0) long maxDiscount,
      @Min(1) @Max(1000000) int usageLimit,
      @NotNull java.time.Instant expiresAt,
      boolean active) {}

  public record StatusInput(@NotBlank String status) {}
}
