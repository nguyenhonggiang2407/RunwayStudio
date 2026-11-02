package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long variantId;

  @Column(nullable = false)
  public int delta;

  @Column(nullable = false)
  public int resultingStock;

  @Column(nullable = false, length = 30)
  public String reason;

  @Column(nullable = false, length = 30)
  public String refType;

  @Column(nullable = true)
  public Long refId;

  @Column(nullable = true)
  public Long actorId;

  @Column(nullable = false)
  public java.time.Instant createdAt = java.time.Instant.now();
}
