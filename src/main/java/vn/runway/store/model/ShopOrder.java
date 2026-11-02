package vn.runway.store.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shop_orders")
public class ShopOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false, unique = true, length = 150)
  public String requestKey;

  @Column(nullable = false, length = 100)
  public String recipient;

  @Column(nullable = false, length = 20)
  public String phone;

  @Column(nullable = false, length = 500)
  public String address;

  @Column(length = 500)
  public String note;

  @Column(nullable = false, length = 20)
  public String status = "PLACED";

  @Column(nullable = false)
  public long subtotal;

  @Column(nullable = false)
  public long shipping;

  @Column(nullable = false)
  public long total;

  @Column(nullable = false)
  public long discount;

  @Column(length = 30)
  public String promotionCode;

  @Column(nullable = false)
  public Instant createdAt = Instant.now();
}
