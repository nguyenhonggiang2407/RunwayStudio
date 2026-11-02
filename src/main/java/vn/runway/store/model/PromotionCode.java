package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion_codes")
public class PromotionCode {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 30)
  public String code;

  @Column(nullable = false)
  public int percentOff;

  @Column(nullable = false)
  public long minSpend;

  @Column(nullable = false)
  public long maxDiscount;

  @Column(nullable = false)
  public int usageLimit;

  @Column(nullable = false)
  public int used;

  @Column(nullable = false)
  public boolean active = true;

  @Column(nullable = false)
  public java.time.Instant expiresAt;
}
