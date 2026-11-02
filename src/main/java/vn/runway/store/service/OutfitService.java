package vn.runway.store.service;

import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
@Transactional(readOnly = true)
public class OutfitService {
  private final OutfitRepository outfits;
  private final OutfitItemRepository items;
  private final CatalogService catalog;
  private final CurrentAccount current;
  private final AuditService audit;

  public OutfitService(
      OutfitRepository o,
      OutfitItemRepository i,
      CatalogService c,
      CurrentAccount u,
      AuditService a) {
    outfits = o;
    items = i;
    catalog = c;
    current = u;
    audit = a;
  }

  private void owner(Outfit o, Long id) {
    if (!Objects.equals(o.ownerAccountId, id)) throw new Missing("Không tìm thấy outfit.");
  }

  public Responses.Outfit detail(Outfit o) {
    var entries = items.findByOutfitIdOrderBySortOrderAsc(o.id);
    var ps = catalog.summaries(entries.stream().map(i -> i.productId).toList());
    long total = 0;
    boolean ready = !entries.isEmpty();
    var result = new ArrayList<Responses.OutfitItem>();
    for (var i : entries) {
      var p = ps.get(i.productId);
      var v =
          p == null
              ? null
              : p.variants().stream()
                  .filter(x -> x.id().equals(i.variantId))
                  .findFirst()
                  .orElse(null);
      if (v != null) total += v.price();
      ready &= p != null && p.active() && v != null && v.stock() > 0;
      result.add(new Responses.OutfitItem(i.productId, i.variantId, i.slot, i.sortOrder, p));
    }
    return new Responses.Outfit(
        o.id, o.name, o.visibility, o.status, o.version, o.createdAt, result, total, ready);
  }

  public Responses.Outfit visible(Long id) {
    var o = outfits.findById(id).orElseThrow(() -> new Missing("Không tìm thấy outfit."));
    if (!(o.ownerAccountId == null
        && o.visibility.equals("PUBLIC")
        && o.status.equals("PUBLISHED"))) owner(o, current.optional().map(a -> a.id).orElse(-1L));
    return detail(o);
  }

  public Responses.Page<Responses.Outfit> publicList(int p, int s) {
    PageBounds.check(p, s);
    return Responses.Page.of(
        outfits
            .findByVisibilityAndStatusOrderByCreatedAtDesc(
                "PUBLIC", "PUBLISHED", PageRequest.of(p, s))
            .map(this::detail));
  }

  public List<Responses.Outfit> mine() {
    return outfits.findByOwnerAccountIdOrderByCreatedAtDesc(current.require().id).stream()
        .map(this::detail)
        .toList();
  }

  public Responses.Page<Responses.Outfit> curated(int p, int s) {
    PageBounds.check(p, s);
    return Responses.Page.of(
        outfits
            .findByOwnerAccountIdIsNullOrderByCreatedAtDesc(PageRequest.of(p, s))
            .map(this::detail));
  }

  private void apply(Outfit o, StudioRequests.Outfit input) {
    if (o.status.equals("PUBLISHED") && input.items().size() < 3)
      throw new BadRequest("Outfit mẫu cần ít nhất ba món.");
    Set<String> slots = new HashSet<>();
    for (var i : input.items()) {
      if (!slots.add(i.slot())) throw new BadRequest("Mỗi vị trí chỉ có một món.");
      var p = catalog.get(i.productId());
      if (!p.active) throw new BadRequest("Sản phẩm đã ngừng bán.");
      if (i.variantId() != null) {
        var v =
            catalog
                .variants()
                .findById(i.variantId())
                .orElseThrow(() -> new Missing("Không tìm thấy phân loại."));
        if (!v.productId.equals(p.id)) throw new BadRequest("Phân loại không thuộc sản phẩm.");
      }
      boolean valid =
          switch (i.slot()) {
            case "SHOES" -> p.category.equals("Giày");
            case "BOTTOM" -> p.category.equals("Quần");
            case "TOP" -> Set.of("Áo", "Áo khoác", "Áo thun").contains(p.category);
            case "ACCESSORY" -> p.category.equals("Phụ kiện");
            default -> false;
          };
      if (!valid) throw new BadRequest("Sản phẩm không phù hợp với vị trí " + i.slot());
    }
    o.updatedAt = java.time.Instant.now();
    o.name = input.name().strip();
    if (o.id == null) outfits.saveAndFlush(o);
    else {
      items.deleteByOutfitId(o.id);
      items.flush();
    }
    int n = 0;
    for (var i : input.items()) {
      var e = new OutfitItem();
      e.outfitId = o.id;
      e.productId = i.productId();
      e.variantId = i.variantId();
      e.slot = i.slot();
      e.sortOrder = n++;
      items.save(e);
    }
  }

  @Transactional
  public Responses.Outfit create(StudioRequests.Outfit i, boolean curated) {
    var o = new Outfit();
    o.ownerAccountId = curated ? null : current.require().id;
    o.name = i.name();
    if (curated) o.visibility = "PUBLIC";
    apply(o, i);
    if (curated) audit.record(current.require().id, "CREATE", "OUTFIT", o.id);
    return detail(o);
  }

  @Transactional
  public Responses.Outfit update(Long id, StudioRequests.Outfit i, boolean curated) {
    var o = outfits.lockById(id).orElseThrow(() -> new Missing("Không tìm thấy outfit."));
    owner(o, curated ? null : current.require().id);
    if (i.version() == null || !i.version().equals(o.version))
      throw new Conflict("Outfit vừa được thay đổi. Vui lòng tải lại.");
    apply(o, i);
    outfits.flush();
    if (curated) audit.record(current.require().id, "UPDATE", "OUTFIT", id);
    return detail(o);
  }

  @Transactional
  public void delete(Long id, boolean curated) {
    var o = outfits.lockById(id).orElseThrow(() -> new Missing("Không tìm thấy outfit."));
    owner(o, curated ? null : current.require().id);
    items.deleteByOutfitId(id);
    items.flush();
    outfits.delete(o);
    if (curated) audit.record(current.require().id, "DELETE", "OUTFIT", id);
  }

  @Transactional
  public Responses.Outfit publish(Long id, boolean publish) {
    var o = outfits.lockById(id).orElseThrow(() -> new Missing("Không tìm thấy outfit."));
    owner(o, null);
    if (publish && items.findByOutfitIdOrderBySortOrderAsc(id).size() < 3)
      throw new BadRequest("Outfit mẫu cần ít nhất ba món.");
    o.status = publish ? "PUBLISHED" : "DRAFT";
    outfits.flush();
    audit.record(current.require().id, publish ? "PUBLISH" : "UNPUBLISH", "OUTFIT", id);
    return detail(o);
  }
}
