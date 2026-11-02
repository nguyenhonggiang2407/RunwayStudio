package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.runway.store.model.Scene;

public interface SceneRepository extends JpaRepository<Scene, Long> {
  org.springframework.data.domain.Page<Scene> findByStatusOrderByPublishedAtDesc(
      String status, org.springframework.data.domain.Pageable page);

  java.util.Optional<Scene> findBySlugAndStatus(String slug, String status);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Scene s where s.id=:id")
  java.util.Optional<Scene> lockById(@Param("id") Long id);
}
