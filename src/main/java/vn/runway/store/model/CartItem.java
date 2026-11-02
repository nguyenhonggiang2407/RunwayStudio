package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false)
  public Long variantId;

  @Column(nullable = false)
  public int quantity;
}
