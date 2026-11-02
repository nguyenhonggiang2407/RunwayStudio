package vn.runway.store.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.model.WardrobeItem;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
@Transactional(readOnly = true)
public class WardrobeService {
  private final WardrobeItemRepository wardrobe;
  private final AccountRepository accounts;
  private final CatalogService catalog;
  private final CurrentAccount current;

  public WardrobeService(
      WardrobeItemRepository w, AccountRepository a, CatalogService c, CurrentAccount u) {
    wardrobe = w;
    accounts = a;
    catalog = c;
    current = u;
  }

  public List<Responses.Wardrobe> list() {
    var rows = wardrobe.findByAccountIdOrderByCreatedAtDesc(current.require().id);
    var ps = catalog.summaries(rows.stream().map(i -> i.productId).toList());
    return rows.stream()
        .map(
            i ->
                new Responses.Wardrobe(
                    i.id, i.productId, i.variantId, i.createdAt, ps.get(i.productId)))
        .toList();
  }

  @Transactional
  public Responses.Wardrobe save(StudioRequests.Wardrobe input) {
    Long a = current.require().id;
    accounts.lockById(a).orElseThrow();
    var p = catalog.get(input.productId());
    if (!p.active) throw new BadRequest("Sản phẩm đã ngừng bán.");
    if (input.variantId() != null) {
      var v =
          catalog
              .variants()
              .findById(input.variantId())
              .orElseThrow(() -> new Missing("Không tìm thấy phân loại."));
      if (!v.productId.equals(p.id)) throw new BadRequest("Phân loại không thuộc sản phẩm.");
    }
    long key = input.variantId() == null ? 0 : input.variantId();
    var found = wardrobe.findByAccountIdAndProductIdAndVariantKey(a, p.id, key);
    WardrobeItem i;
    if (found.isPresent()) i = found.get();
    else {
      if (wardrobe.countByAccountId(a) >= 200) throw new BadRequest("Tủ đồ tối đa 200 món.");
      i = new WardrobeItem();
      i.accountId = a;
      i.productId = p.id;
      i.variantId = input.variantId();
      i.variantKey = key;
      wardrobe.saveAndFlush(i);
    }
    return new Responses.Wardrobe(i.id, i.productId, i.variantId, i.createdAt, catalog.product(p));
  }

  @Transactional
  public void delete(Long id) {
    Long a = current.require().id;
    accounts.lockById(a).orElseThrow();
    wardrobe.delete(
        wardrobe
            .findById(id)
            .filter(i -> i.accountId.equals(a))
            .orElseThrow(() -> new Missing("Không tìm thấy món đã lưu.")));
  }
}
