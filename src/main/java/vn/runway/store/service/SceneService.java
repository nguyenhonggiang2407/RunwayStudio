package vn.runway.store.service;

import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
@Transactional(readOnly = true)
public class SceneService {
  private final SceneRepository scenes;
  private final SceneHotspotRepository hotspots;
  private final CatalogService catalog;
  private final ApprovedMediaService media;
  private final AuditService audit;
  private final CurrentAccount current;

  public SceneService(
      SceneRepository s,
      SceneHotspotRepository h,
      CatalogService c,
      ApprovedMediaService m,
      AuditService a,
      CurrentAccount u) {
    scenes = s;
    hotspots = h;
    catalog = c;
    media = m;
    audit = a;
    current = u;
  }

  private Responses.Scene detail(Scene s) {
    var hs = hotspots.findBySceneIdOrderByIdAsc(s.id);
    var ps = catalog.summaries(hs.stream().map(h -> h.productId).toList());
    return new Responses.Scene(
        s.id,
        s.title,
        s.slug,
        s.coverImage,
        s.altText,
        s.status,
        s.publishedAt,
        s.version,
        hs.stream()
            .map(
                h ->
                    new Responses.Hotspot(
                        h.id, h.productId, h.xPercent, h.yPercent, h.label, ps.get(h.productId)))
            .toList());
  }

  public Responses.Page<Responses.Scene> list(int p, int size, boolean admin) {
    PageBounds.check(p, size);
    var page = PageRequest.of(p, size, Sort.by("id").descending());
    return Responses.Page.of(
        (admin
                ? scenes.findAll(page)
                : scenes.findByStatusOrderByPublishedAtDesc("PUBLISHED", page))
            .map(this::detail));
  }

  public Responses.Scene publicScene(String slug) {
    return detail(
        scenes
            .findBySlugAndStatus(slug, "PUBLISHED")
            .orElseThrow(() -> new Missing("Không tìm thấy scene.")));
  }

  public Responses.Scene adminScene(Long id) {
    return detail(scenes.findById(id).orElseThrow(() -> new Missing("Không tìm thấy scene.")));
  }

  private void apply(Scene s, StudioRequests.Scene i) {
    media.validate(i.coverImage());
    for (var h : i.hotspots()) {
      if (!Double.isFinite(h.xPercent())
          || !Double.isFinite(h.yPercent())
          || h.xPercent() < 0
          || h.xPercent() > 100
          || h.yPercent() < 0
          || h.yPercent() > 100) throw new BadRequest("Tọa độ hotspot không hợp lệ.");
      catalog.get(h.productId());
    }
    s.updatedAt = Instant.now();
    s.title = i.title().strip();
    s.slug = i.slug();
    s.coverImage = i.coverImage();
    s.altText = i.altText().strip();
    if (s.id == null) scenes.saveAndFlush(s);
    else {
      hotspots.deleteBySceneId(s.id);
      hotspots.flush();
    }
    for (var h : i.hotspots()) {
      var x = new SceneHotspot();
      x.sceneId = s.id;
      x.productId = h.productId();
      x.xPercent = h.xPercent();
      x.yPercent = h.yPercent();
      x.label = h.label().strip();
      hotspots.save(x);
    }
  }

  @Transactional
  public Responses.Scene create(StudioRequests.Scene i) {
    var s = new Scene();
    apply(s, i);
    audit.record(current.require().id, "CREATE", "SCENE", s.id);
    return detail(s);
  }

  @Transactional
  public Responses.Scene update(Long id, StudioRequests.Scene i) {
    var s = scenes.lockById(id).orElseThrow(() -> new Missing("Không tìm thấy scene."));
    if (i.version() == null || !i.version().equals(s.version))
      throw new Conflict("Scene vừa được thay đổi. Vui lòng tải lại.");
    if (s.status.equals("PUBLISHED") && i.hotspots().isEmpty())
      throw new BadRequest("Scene công khai cần ít nhất một hotspot.");
    apply(s, i);
    scenes.flush();
    audit.record(current.require().id, "UPDATE", "SCENE", id);
    return detail(s);
  }

  @Transactional
  public Responses.Scene publish(Long id, boolean publish) {
    var s = scenes.lockById(id).orElseThrow(() -> new Missing("Không tìm thấy scene."));
    media.validate(s.coverImage);
    if (publish && hotspots.findBySceneIdOrderByIdAsc(id).isEmpty())
      throw new BadRequest("Scene cần ít nhất một hotspot.");
    s.status = publish ? "PUBLISHED" : "DRAFT";
    if (publish) s.publishedAt = Instant.now();
    scenes.flush();
    audit.record(current.require().id, publish ? "PUBLISH" : "UNPUBLISH", "SCENE", id);
    return detail(s);
  }
}
