package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "outfits")
public class Outfit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = true)
  public Long ownerAccountId;

  @Column(nullable = false, length = 100)
  public String name;

  @Column(nullable = false, length = 20)
  public String visibility = "PRIVATE";

  @Column(nullable = false, length = 20)
  public String status = "DRAFT";

  @Version public Long version;

  @Column(nullable = false)
  public java.time.Instant createdAt = java.time.Instant.now();

  @Column(nullable = false)
  public java.time.Instant updatedAt = java.time.Instant.now();
}
