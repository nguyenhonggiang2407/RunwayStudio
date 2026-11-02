package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "scene_hotspots")
public class SceneHotspot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long sceneId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public double xPercent;

  @Column(nullable = false)
  public double yPercent;

  @Column(nullable = false, length = 80)
  public String label;
}
