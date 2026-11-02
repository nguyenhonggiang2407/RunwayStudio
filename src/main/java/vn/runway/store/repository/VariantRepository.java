package vn.runway.store.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.runway.store.model.Variant;

public interface VariantRepository extends JpaRepository<Variant, Long> {
  List<Variant> findByProductIdOrderByIdAsc(Long productId);

  List<Variant> findByProductIdInOrderByIdAsc(Collection<Long> ids);

  org.springframework.data.domain.Page<Variant> findByStockLessThanEqualOrderByStockAsc(
      int stock, org.springframework.data.domain.Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from Variant v where v.id=:id")
  Optional<Variant> lockById(@Param("id") Long id);
}
