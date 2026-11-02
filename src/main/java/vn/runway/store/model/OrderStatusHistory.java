package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long orderId;

  @Column(nullable = true)
  public Long actorId;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(nullable = false)
  public java.time.Instant createdAt = java.time.Instant.now();
}
