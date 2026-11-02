package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.SizeGuideRow;

public interface SizeGuideRowRepository extends JpaRepository<SizeGuideRow, Long> {
  java.util.List<SizeGuideRow> findByGuideIdOrderBySortOrderAsc(Long id);
}
