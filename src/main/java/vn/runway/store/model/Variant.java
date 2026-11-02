package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "variants")
public class Variant {
  @Id public Long id;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false, length = 80)
  public String size;

  @Column(nullable = false)
  public long price;

  @Column(nullable = false)
  public int stock;

  @Version public Long version;
}
