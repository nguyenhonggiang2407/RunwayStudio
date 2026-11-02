package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "size_guide_rows")
public class SizeGuideRow {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false)
  public Long guideId;

  @Column(nullable = false, length = 20)
  public String size;

  @Column(nullable = true)
  public Double minChest;

  @Column(nullable = true)
  public Double maxChest;

  @Column(nullable = true)
  public Double minWaist;

  @Column(nullable = true)
  public Double maxWaist;

  @Column(nullable = true)
  public Double minFoot;

  @Column(nullable = true)
  public Double maxFoot;

  @Column(nullable = false)
  public int sortOrder;
}
