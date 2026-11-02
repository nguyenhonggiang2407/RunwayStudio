package vn.runway.store.web;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.service.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final CatalogService catalog;
  private final OrderService orders;
  private final AdminService admin;
  private final CurrentAccount current;
  private final PromotionService promotions;

  public AdminController(
      CatalogService c, OrderService o, AdminService a, CurrentAccount u, PromotionService p) {
    catalog = c;
    orders = o;
    admin = a;
    current = u;
    promotions = p;
  }

  @GetMapping("/products")
  public List<Responses.Product> products() {
    return catalog.list(null, null, true);
  }

  @GetMapping("/catalog")
  public Responses.Page<Responses.Product> page(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(required = false) String q) {
    return catalog.page(q, null, page, size, "default", null, null, false, true);
  }

  @PostMapping("/products")
  public Responses.Product create(@Valid @RequestBody Requests.ProductInput i) {
    return admin.create(i);
  }

  @PutMapping("/products/{id}")
  public Responses.Product update(
      @PathVariable Long id, @Valid @RequestBody Requests.ProductInput i) {
    return admin.update(id, i);
  }

  @DeleteMapping("/products/{id}")
  public Responses.Message deactivate(@PathVariable Long id) {
    return admin.deactivate(id);
  }

  @PostMapping("/products/{id}/variants")
  public Responses.Variant add(@PathVariable Long id, @Valid @RequestBody Requests.VariantInput i) {
    return admin.addVariant(id, i);
  }

  @PutMapping("/variants/{id}")
  public Responses.Variant variant(
      @PathVariable Long id, @Valid @RequestBody Requests.VariantInput i) {
    return admin.variant(id, i);
  }

  @GetMapping("/orders")
  public List<Responses.OrderDetail> orders() {
    return orders.list(current.require(), true);
  }

  @GetMapping("/order-page")
  public Responses.Page<Responses.OrderDetail> orders(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
    return orders.page(current.require(), true, page, size);
  }

  @PutMapping("/orders/{id}/status")
  public Responses.OrderDetail status(
      @PathVariable Long id, @Valid @RequestBody Requests.StatusInput i) {
    return orders.detail(orders.change(id, i.status(), current.require(), true));
  }

  @GetMapping("/dashboard")
  public Responses.Dashboard dashboard() {
    return admin.dashboard();
  }

  @GetMapping("/reports/sales")
  public Responses.SalesReport sales(@RequestParam LocalDate from, @RequestParam LocalDate to) {
    return admin.sales(from, to);
  }

  @GetMapping("/audit")
  public Responses.Page<Responses.Audit> audit(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return admin.audit(page, size);
  }

  @GetMapping("/variants/{id}/movements")
  public Responses.Page<Responses.Movement> moves(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return admin.movements(id, page, size);
  }

  @GetMapping("/promotions")
  public Responses.Page<Responses.Promotion> promos(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return promotions.list(page, size);
  }

  @PostMapping("/promotions")
  public Responses.Promotion promo(@Valid @RequestBody Requests.Promotion i) {
    return promotions.create(i);
  }
}
