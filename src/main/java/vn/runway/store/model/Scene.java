package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "scenes")
public class Scene {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 100)
  public String title;

  @Column(nullable = false, length = 100)
  public String slug;

  @Column(nullable = false, length = 700)
  public String coverImage;

  @Column(nullable = false, length = 250)
  public String altText;

  @Column(nullable = false, length = 20)
  public String status = "DRAFT";

  @Column(nullable = true)
  public java.time.Instant publishedAt;

  @Version public Long version;

  @Column(nullable = false)
  public java.time.Instant updatedAt = java.time.Instant.now();
}
