package vn.runway.store.web;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.service.*;

@RestController
public class OutfitController {
  private final OutfitService service;

  public OutfitController(OutfitService s) {
    service = s;
  }

  @GetMapping("/api/studio/outfits")
  public Responses.Page<Responses.Outfit> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
    return service.publicList(page, size);
  }

  @GetMapping("/api/studio/outfits/{id}")
  public Responses.Outfit get(@PathVariable Long id) {
    return service.visible(id);
  }

  @GetMapping("/api/me/outfits")
  public List<Responses.Outfit> mine() {
    return service.mine();
  }

  @PostMapping("/api/me/outfits")
  public Responses.Outfit create(@Valid @RequestBody StudioRequests.Outfit i) {
    return service.create(i, false);
  }

  @PutMapping("/api/me/outfits/{id}")
  public Responses.Outfit update(
      @PathVariable Long id, @Valid @RequestBody StudioRequests.Outfit i) {
    return service.update(id, i, false);
  }

  @DeleteMapping("/api/me/outfits/{id}")
  public Responses.Message delete(@PathVariable Long id) {
    service.delete(id, false);
    return new Responses.Message("Đã xóa outfit.");
  }

  @GetMapping("/api/admin/outfits")
  public Responses.Page<Responses.Outfit> curated(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
    return service.curated(page, size);
  }

  @PostMapping("/api/admin/outfits")
  public Responses.Outfit createCurated(@Valid @RequestBody StudioRequests.Outfit i) {
    return service.create(i, true);
  }

  @PutMapping("/api/admin/outfits/{id}")
  public Responses.Outfit updateCurated(
      @PathVariable Long id, @Valid @RequestBody StudioRequests.Outfit i) {
    return service.update(id, i, true);
  }

  @DeleteMapping("/api/admin/outfits/{id}")
  public Responses.Message deleteCurated(@PathVariable Long id) {
    service.delete(id, true);
    return new Responses.Message("Đã xóa outfit mẫu.");
  }

  @PostMapping("/api/admin/outfits/{id}/publish")
  public Responses.Outfit publish(@PathVariable Long id) {
    return service.publish(id, true);
  }

  @PostMapping("/api/admin/outfits/{id}/unpublish")
  public Responses.Outfit unpublish(@PathVariable Long id) {
    return service.publish(id, false);
  }
}
