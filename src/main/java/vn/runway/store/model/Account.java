package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 150)
  public String email;

  @Column(nullable = false, length = 100)
  public String name;

  @Column(nullable = false)
  public String password;

  @Column(nullable = false, length = 20)
  public String role = "CUSTOMER";
}
