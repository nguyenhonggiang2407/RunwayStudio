package vn.runway.store.repository;

import org.springframework.data.jpa.repository.*;
import vn.runway.store.model.SceneHotspot;

public interface SceneHotspotRepository extends JpaRepository<SceneHotspot, Long> {
  java.util.List<SceneHotspot> findBySceneIdOrderByIdAsc(Long id);

  void deleteBySceneId(Long id);
}
