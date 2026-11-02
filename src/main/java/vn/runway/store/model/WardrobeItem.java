package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "wardrobe_items")
public class WardrobeItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = true)
  public Long variantId;

  @Column(nullable = false)
  public long variantKey;

  @Column(nullable = false)
  public java.time.Instant createdAt = java.time.Instant.now();
}
