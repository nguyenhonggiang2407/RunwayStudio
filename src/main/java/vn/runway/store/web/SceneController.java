package vn.runway.store.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.service.*;

@RestController
public class SceneController {
  private final SceneService service;
  private final ApprovedMediaService media;

  public SceneController(SceneService s, ApprovedMediaService m) {
    service = s;
    media = m;
  }

  @GetMapping("/api/scenes")
  public Responses.Page<Responses.Scene> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
    return service.list(page, size, false);
  }

  @GetMapping("/api/scenes/{slug}")
  public Responses.Scene get(@PathVariable String slug) {
    return service.publicScene(slug);
  }

  @GetMapping("/api/admin/scenes")
  public Responses.Page<Responses.Scene> admin(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
    return service.list(page, size, true);
  }

  @GetMapping("/api/admin/scenes/{id}")
  public Responses.Scene getAdmin(@PathVariable Long id) {
    return service.adminScene(id);
  }

  @GetMapping("/api/admin/media")
  public List<String> media() {
    return media.available();
  }

  @PostMapping("/api/admin/scenes")
  public Responses.Scene create(@Valid @RequestBody StudioRequests.Scene i) {
    return service.create(i);
  }

  @PutMapping("/api/admin/scenes/{id}")
  public Responses.Scene update(@PathVariable Long id, @Valid @RequestBody StudioRequests.Scene i) {
    return service.update(id, i);
  }

  @PostMapping("/api/admin/scenes/{id}/publish")
  public Responses.Scene publish(@PathVariable Long id) {
    return service.publish(id, true);
  }

  @PostMapping("/api/admin/scenes/{id}/unpublish")
  public Responses.Scene unpublish(@PathVariable Long id) {
    return service.publish(id, false);
  }
}
