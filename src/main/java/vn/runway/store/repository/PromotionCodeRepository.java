package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.runway.store.model.PromotionCode;

public interface PromotionCodeRepository extends JpaRepository<PromotionCode, Long> {
  java.util.Optional<PromotionCode> findByCode(String code);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PromotionCode p where p.code=:code")
  java.util.Optional<PromotionCode> lockByCode(@Param("code") String code);
}
