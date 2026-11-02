package vn.runway.store.model;

import jakarta.persistence.*;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = true)
  public Long actorId;

  @Column(nullable = false, length = 40)
  public String action;

  @Column(nullable = false, length = 40)
  public String targetType;

  @Column(nullable = false)
  public Long targetId;

  @Column(nullable = false)
  public java.time.Instant createdAt = java.time.Instant.now();
}
