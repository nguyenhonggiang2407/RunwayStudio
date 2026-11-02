package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
  org.springframework.data.domain.Page<AuditEvent> findAllByOrderByCreatedAtDesc(
      org.springframework.data.domain.Pageable page);
}
