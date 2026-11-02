package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
  org.springframework.data.domain.Page<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(
      Long id, org.springframework.data.domain.Pageable page);
}
