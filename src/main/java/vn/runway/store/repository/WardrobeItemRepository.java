package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.WardrobeItem;

public interface WardrobeItemRepository extends JpaRepository<WardrobeItem, Long> {
  java.util.List<WardrobeItem> findByAccountIdOrderByCreatedAtDesc(Long id);

  java.util.Optional<WardrobeItem> findByAccountIdAndProductIdAndVariantKey(Long a, Long p, long v);

  long countByAccountId(Long id);
}
