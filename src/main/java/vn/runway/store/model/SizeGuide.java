package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "size_guides")
public class SizeGuide {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 60)
  public String category;

  @Column(nullable = false)
  public int version = 1;

  @Column(nullable = false, length = 10)
  public String unit = "cm";
}
