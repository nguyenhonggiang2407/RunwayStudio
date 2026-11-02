package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.SizeGuide;

public interface SizeGuideRepository extends JpaRepository<SizeGuide, Long> {
  java.util.Optional<SizeGuide> findByCategory(String category);
}
