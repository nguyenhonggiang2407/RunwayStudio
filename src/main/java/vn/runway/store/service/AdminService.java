package vn.runway.store.service;

import java.time.*;
import java.time.temporal.ChronoUnit;
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
public class AdminService {
  private final CatalogService catalog;
  private final OrderRepository orders;
  private final AccountRepository accounts;
  private final AuditService audit;
  private final InventoryService inventory;
  private final CurrentAccount current;
  private final ApprovedMediaService media;
  private final AuditEventRepository events;
  private final InventoryMovementRepository movements;

  public AdminService(
      CatalogService c,
      OrderRepository o,
      AccountRepository a,
      AuditService audit,
      InventoryService i,
      CurrentAccount u,
      ApprovedMediaService m,
      AuditEventRepository e,
      InventoryMovementRepository v) {
    catalog = c;
    orders = o;
    accounts = a;
    this.audit = audit;
    inventory = i;
    current = u;
    media = m;
    events = e;
    movements = v;
  }

  private long nextId() {
    return UUID.randomUUID().getMostSignificantBits() & 0x001fffffffffffffL;
  }

  private void apply(Product p, Requests.ProductInput i) {
    media.validate(i.image());
    p.name = i.name().strip();
    p.slug = i.slug();
    p.category = i.category().strip();
    p.description = i.description();
    p.image = i.image();
    p.active = i.active();
  }

  @Transactional
  public Responses.Product create(Requests.ProductInput i) {
    var p = new Product();
    p.id = nextId();
    apply(p, i);
    catalog.products().saveAndFlush(p);
    audit.record(current.require().id, "CREATE", "PRODUCT", p.id);
    return catalog.product(p);
  }

  @Transactional
  public Responses.Product update(Long id, Requests.ProductInput i) {
    var p = catalog.get(id);
    apply(p, i);
    catalog.products().save(p);
    audit.record(current.require().id, "UPDATE", "PRODUCT", id);
    return catalog.product(p);
  }

  @Transactional
  public Responses.Message deactivate(Long id) {
    var p = catalog.get(id);
    p.active = false;
    catalog.products().save(p);
    audit.record(current.require().id, "DEACTIVATE", "PRODUCT", id);
    return new Responses.Message("Đã ngừng bán; lịch sử đơn vẫn được giữ.");
  }

  @Transactional
  public Responses.Variant addVariant(Long id, Requests.VariantInput i) {
    catalog.get(id);
    var v = new Variant();
    v.id = nextId();
    v.productId = id;
    v.size = i.size().strip();
    v.price = i.price();
    catalog.variants().saveAndFlush(v);
    inventory.adjust(v, i.stock(), "INITIAL_STOCK", "VARIANT", v.id, current.require().id);
    catalog.variants().flush();
    audit.record(current.require().id, "CREATE", "VARIANT", v.id);
    return catalog.variant(v);
  }

  @Transactional
  public Responses.Variant variant(Long id, Requests.VariantInput i) {
    var v =
        catalog.variants().lockById(id).orElseThrow(() -> new Missing("Không tìm thấy phân loại."));
    inventory.adjust(v, i.stock() - v.stock, "ADMIN_ADJUST", "VARIANT", id, current.require().id);
    v.size = i.size().strip();
    v.price = i.price();
    catalog.variants().flush();
    audit.record(current.require().id, "UPDATE", "VARIANT", id);
    return catalog.variant(v);
  }

  public Responses.Dashboard dashboard() {
    return new Responses.Dashboard(
        orders.count(),
        orders.deliveredRevenue(),
        orders.countByStatus("PLACED"),
        catalog.products().count(),
        accounts.countByRole("CUSTOMER"),
        catalog
            .variants()
            .findByStockLessThanEqualOrderByStockAsc(5, PageRequest.of(0, 20))
            .map(catalog::variant)
            .getContent());
  }

  public Responses.SalesReport sales(LocalDate from, LocalDate to) {
    if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365)
      throw new BadRequest("Khoảng báo cáo tối đa 366 ngày.");
    return new Responses.SalesReport(
        from.toString(),
        to.toString(),
        "UTC",
        orders
            .sales(
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
            .stream()
            .map(
                r ->
                    new Responses.SalesDay(
                        Objects.toString(r[0]),
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue()))
            .toList());
  }

  public Responses.Page<Responses.Audit> audit(int p, int s) {
    PageBounds.check(p, s);
    return Responses.Page.of(
        events
            .findAllByOrderByCreatedAtDesc(PageRequest.of(p, s))
            .map(
                e ->
                    new Responses.Audit(
                        e.id, e.actorId, e.action, e.targetType, e.targetId, e.createdAt)));
  }

  public Responses.Page<Responses.Movement> movements(Long id, int p, int s) {
    PageBounds.check(p, s);
    return Responses.Page.of(
        movements
            .findByVariantIdOrderByCreatedAtDesc(id, PageRequest.of(p, s))
            .map(
                m ->
                    new Responses.Movement(
                        m.id,
                        m.variantId,
                        m.delta,
                        m.resultingStock,
                        m.reason,
                        m.refType,
                        m.refId,
                        m.createdAt)));
  }
}
