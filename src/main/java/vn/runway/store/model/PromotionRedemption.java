package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion_redemptions")
public class PromotionRedemption {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long promotionId;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false)
  public Long orderId;
}
