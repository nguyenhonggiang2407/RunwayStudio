package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.PromotionRedemption;

public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Long> {
  boolean existsByPromotionIdAndAccountId(Long p, Long a);
}
