package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customer_addresses")
public class CustomerAddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long accountId;

  @Column(nullable = false, length = 100)
  public String recipient;

  @Column(nullable = false, length = 20)
  public String phone;

  @Column(nullable = false, length = 500)
  public String address;
}
