package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.runway.store.model.Outfit;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {
  org.springframework.data.domain.Page<Outfit> findByVisibilityAndStatusOrderByCreatedAtDesc(
      String visibility, String status, org.springframework.data.domain.Pageable page);

  java.util.List<Outfit> findByOwnerAccountIdOrderByCreatedAtDesc(Long owner);

  org.springframework.data.domain.Page<Outfit> findByOwnerAccountIdIsNullOrderByCreatedAtDesc(
      org.springframework.data.domain.Pageable page);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Outfit o where o.id=:id")
  java.util.Optional<Outfit> lockById(@Param("id") Long id);
}
