package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.OutfitItem;

public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {
  java.util.List<OutfitItem> findByOutfitIdOrderBySortOrderAsc(Long id);

  void deleteByOutfitId(Long id);
}
