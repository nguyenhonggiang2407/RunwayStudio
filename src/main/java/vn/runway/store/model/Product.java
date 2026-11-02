package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
  @Id public Long id;

  @Column(nullable = false, unique = true, length = 160)
  public String slug;

  @Column(nullable = false, length = 160)
  public String name;

  @Column(nullable = false, length = 60)
  public String category;

  @Column(length = 2000)
  public String description;

  @Column(length = 700)
  public String image;

  @Column(nullable = false)
  public boolean active = true;
}
