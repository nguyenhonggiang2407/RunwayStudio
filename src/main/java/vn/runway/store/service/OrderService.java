package vn.runway.store.service;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
public class OrderService {
  private static final Map<String, Set<String>> TRANSITIONS =
      Map.of(
          "PLACED",
          Set.of("CONFIRMED", "CANCELLED"),
          "CONFIRMED",
          Set.of("SHIPPED", "CANCELLED"),
          "SHIPPED",
          Set.of("DELIVERED"),
          "DELIVERED",
          Set.of(),
          "CANCELLED",
          Set.of());
  private final AccountRepository accounts;
  private final ProductRepository products;
  private final VariantRepository variants;
  private final OrderRepository orders;
  private final OrderLineRepository lines;
  private final OrderStatusHistoryRepository history;
  private final InventoryService inventory;
  private final PromotionService promotions;
  private final CartService carts;

  public OrderService(
      AccountRepository a,
      ProductRepository p,
      VariantRepository v,
      OrderRepository o,
      OrderLineRepository l,
      OrderStatusHistoryRepository h,
      InventoryService i,
      PromotionService promo,
      CartService c) {
    accounts = a;
    products = p;
    variants = v;
    orders = o;
    lines = l;
    history = h;
    inventory = i;
    promotions = promo;
    carts = c;
  }

  public record CheckoutResult(ShopOrder order, boolean created) {}

  @Transactional
  public CheckoutResult checkoutCart(Account buyer, HttpSession s, Requests.Checkout input) {
    accounts.lockById(buyer.id).orElseThrow(() -> new Missing("Tài khoản không tồn tại."));
    var old = orders.findByRequestKey(buyer.id + ":" + input.requestId());
    if (old.isPresent()) return new CheckoutResult(old.get(), false);
    var result = checkout(buyer, carts.values(s), input);
    if (result.created()) carts.clear(s);
    return result;
  }

  @Transactional
  public CheckoutResult checkout(Account buyer, Map<Long, Integer> cart, Requests.Checkout input) {
    accounts.lockById(buyer.id).orElseThrow(() -> new Missing("Tài khoản không tồn tại."));
    String key = buyer.id + ":" + input.requestId();
    var old = orders.findByRequestKey(key);
    if (old.isPresent()) return new CheckoutResult(old.get(), false);
    if (cart.isEmpty() || cart.size() > 50)
      throw new BadRequest("Giỏ hàng trống hoặc vượt giới hạn.");
    var snapshots = new ArrayList<OrderLine>();
    var locked = new TreeMap<Long, Variant>();
    long subtotal = 0;
    for (var e : new TreeMap<>(cart).entrySet()) {
      int qty = e.getValue();
      if (qty < 1 || qty > 99) throw new BadRequest("Số lượng không hợp lệ.");
      var v =
          variants.lockById(e.getKey()).orElseThrow(() -> new Missing("Phân loại không tồn tại."));
      var p =
          products.findById(v.productId).orElseThrow(() -> new Missing("Sản phẩm không tồn tại."));
      if (!p.active || v.stock < qty)
        throw new BadRequest(p.name + " / " + v.size + ": không đủ hàng.");
      var l = new OrderLine();
      l.variantId = v.id;
      l.productName = p.name;
      l.size = v.size;
      l.image = p.image;
      l.quantity = qty;
      l.unitPrice = v.price;
      snapshots.add(l);
      subtotal = Math.addExact(subtotal, Math.multiplyExact(v.price, qty));
      locked.put(v.id, v);
    }
    var discount = promotions.check(input.voucherCode(), subtotal, buyer.id, true);
    var o = new ShopOrder();
    o.accountId = buyer.id;
    o.requestKey = key;
    o.recipient = input.recipient().strip();
    o.phone = input.phone();
    o.address = input.address().strip();
    o.note = input.note();
    o.subtotal = subtotal;
    o.shipping = PromotionService.shipping(subtotal);
    o.discount = discount.amount();
    o.promotionCode = discount.promotion() == null ? null : discount.promotion().code;
    o.total = subtotal + o.shipping - o.discount;
    orders.saveAndFlush(o);
    for (var l : snapshots) {
      l.orderId = o.id;
      lines.save(l);
      inventory.adjust(locked.get(l.variantId), -l.quantity, "CHECKOUT", "ORDER", o.id, buyer.id);
    }
    promotions.redeem(discount, buyer.id, o.id);
    event(o, buyer.id);
    orders.flush();
    return new CheckoutResult(o, true);
  }

  private void event(ShopOrder o, Long actor) {
    var e = new OrderStatusHistory();
    e.orderId = o.id;
    e.actorId = actor;
    e.status = o.status;
    history.save(e);
  }

  @Transactional
  public Responses.Quote quote(Account buyer, HttpSession s, String code) {
    var c = carts.view(s);
    if (c.itemCount() == 0) throw new BadRequest("Giỏ hàng đang trống.");
    var d = promotions.check(code, c.subtotal(), buyer.id, false);
    return new Responses.Quote(
        c.subtotal(),
        c.shipping(),
        d.amount(),
        c.total() - d.amount(),
        d.promotion() == null ? null : d.promotion().code);
  }

  @Transactional(readOnly = true)
  public Responses.OrderDetail detail(ShopOrder o) {
    return new Responses.OrderDetail(
        new Responses.Order(
            o.id,
            o.recipient,
            o.phone,
            o.address,
            o.note,
            o.status,
            o.subtotal,
            o.shipping,
            o.discount,
            o.total,
            o.promotionCode,
            o.createdAt),
        lines.findByOrderIdOrderByIdAsc(o.id).stream()
            .map(
                l ->
                    new Responses.OrderLine(
                        l.id, l.variantId, l.productName, l.size, l.image, l.quantity, l.unitPrice))
            .toList(),
        history.findByOrderIdOrderByIdAsc(o.id).stream()
            .map(e -> new Responses.OrderEvent(e.status, e.createdAt))
            .toList());
  }

  @Transactional(readOnly = true)
  public List<Responses.OrderDetail> list(Account a, boolean admin) {
    return page(a, admin, 0, 60).items();
  }

  @Transactional(readOnly = true)
  public Responses.Page<Responses.OrderDetail> page(Account a, boolean admin, int page, int size) {
    PageBounds.check(page, size);
    var p =
        PageRequest.of(
            page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
    return Responses.Page.of(
        (admin ? orders.findAll(p) : orders.findByAccountId(a.id, p)).map(this::detail));
  }

  @Transactional
  public ShopOrder change(Long id, String next, Account actor, boolean admin) {
    var o = orders.lockById(id).orElseThrow(() -> new Missing("Không tìm thấy đơn hàng."));
    if (!admin && !o.accountId.equals(actor.id)) throw new Missing("Không tìm thấy đơn hàng.");
    if (!admin && (!next.equals("CANCELLED") || !o.status.equals("PLACED")))
      throw new BadRequest("Chỉ có thể hủy đơn chờ xác nhận.");
    if (!TRANSITIONS.getOrDefault(o.status, Set.of()).contains(next))
      throw new BadRequest("Không thể chuyển trạng thái như yêu cầu.");
    if (next.equals("CANCELLED")) {
      var ls = new ArrayList<>(lines.findByOrderIdOrderByIdAsc(id));
      ls.sort(Comparator.comparing(l -> l.variantId));
      for (var l : ls) {
        var v =
            variants
                .lockById(l.variantId)
                .orElseThrow(() -> new Missing("Phân loại không tồn tại."));
        inventory.adjust(
            v, l.quantity, "CANCEL_ORDER", "ORDER", id, actor == null ? null : actor.id);
      }
    }
    o.status = next;
    event(o, actor == null ? null : actor.id);
    return orders.save(o);
  }
}
