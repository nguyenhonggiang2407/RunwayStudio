package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_lines")
public class OrderLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long orderId;

  @Column(nullable = false)
  public Long variantId;

  @Column(nullable = false, length = 160)
  public String productName;

  @Column(nullable = false, length = 80)
  public String size;

  @Column(length = 700)
  public String image;

  @Column(nullable = false)
  public int quantity;

  @Column(nullable = false)
  public long unitPrice;
}
