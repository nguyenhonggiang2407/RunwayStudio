package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "outfit_items")
public class OutfitItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long outfitId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = true)
  public Long variantId;

  @Column(nullable = false, length = 20)
  public String slot;

  @Column(nullable = false)
  public int sortOrder;
}
