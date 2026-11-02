package vn.runway.store.service;

import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import vn.runway.store.dto.*;
import vn.runway.store.model.*;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
public class CartService {
  private final VariantRepository variants;
  private final ProductRepository products;
  private final CartItemRepository items;
  private final AccountRepository accounts;
  private final CurrentAccount current;

  public CartService(
      VariantRepository v,
      ProductRepository p,
      CartItemRepository i,
      AccountRepository a,
      CurrentAccount c) {
    variants = v;
    products = p;
    items = i;
    accounts = a;
    current = c;
  }

  @SuppressWarnings("unchecked")
  private Map<Long, Integer> guest(HttpSession s) {
    var cart = (Map<Long, Integer>) s.getAttribute("cart");
    return cart == null ? new TreeMap<>() : new TreeMap<>(cart);
  }

  private Long actor() {
    return current.optional().map(a -> a.id).orElse(null);
  }

  private Map<Long, Integer> stored(Long id) {
    return items.findByAccountIdOrderByVariantIdAsc(id).stream()
        .collect(Collectors.toMap(i -> i.variantId, i -> i.quantity, (a, b) -> a, TreeMap::new));
  }

  private void save(Long id, Map<Long, Integer> cart) {
    items.deleteByAccountId(id);
    items.flush();
    for (var e : cart.entrySet()) {
      var i = new CartItem();
      i.accountId = id;
      i.variantId = e.getKey();
      i.quantity = e.getValue();
      items.save(i);
    }
    items.flush();
  }

  private void removeGuest(HttpSession s) {
    var before = guest(s);
    s.removeAttribute("cart");
    if (!before.isEmpty() && TransactionSynchronizationManager.isSynchronizationActive())
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            public void afterCompletion(int status) {
              if (status == STATUS_ROLLED_BACK)
                synchronized (s) {
                  if (s.getAttribute("cart") == null) s.setAttribute("cart", before);
                }
            }
          });
  }

  private Map<Long, Integer> load(HttpSession s, Long id) {
    if (id == null) return guest(s);
    accounts.lockById(id).orElseThrow(() -> new Missing("Tài khoản không tồn tại."));
    var cart = stored(id);
    var g = guest(s);
    if (!g.isEmpty()) {
      var merged = new TreeMap<>(cart);
      g.forEach((v, q) -> merged.merge(v, q, Integer::sum));
      validate(merged);
      save(id, merged);
      removeGuest(s);
      return merged;
    }
    return cart;
  }

  private void validate(Map<Long, Integer> cart) {
    if (cart.size() > 50) throw new BadRequest("Giỏ hàng tối đa 50 phân loại.");
    var vs =
        variants.findAllById(cart.keySet()).stream().collect(Collectors.toMap(v -> v.id, v -> v));
    var ps =
        products.findAllById(vs.values().stream().map(v -> v.productId).toList()).stream()
            .collect(Collectors.toMap(p -> p.id, p -> p));
    for (var e : cart.entrySet()) {
      if (e.getValue() < 1 || e.getValue() > 99) throw new BadRequest("Số lượng phải từ 1 đến 99.");
      var v = vs.get(e.getKey());
      if (v == null) throw new Missing("Không tìm thấy phân loại.");
      var p = ps.get(v.productId);
      if (p == null || !p.active) throw new BadRequest("Sản phẩm đã ngừng bán.");
      if (e.getValue() > v.stock)
        throw new BadRequest(p.name + " / " + v.size + ": không đủ hàng.");
    }
  }

  @Transactional
  public Map<Long, Integer> values(HttpSession s) {
    synchronized (s) {
      return load(s, actor());
    }
  }

  @Transactional
  public void change(HttpSession s, Long id, int qty, boolean add) {
    synchronized (s) {
      Long a = actor();
      var cart = load(s, a);
      if (id == null || qty < 0 || qty > 99) throw new BadRequest("Số lượng không hợp lệ.");
      int next = (add ? cart.getOrDefault(id, 0) : 0) + qty;
      if (next == 0) cart.remove(id);
      else cart.put(id, next);
      if (next != 0) validate(cart);
      if (a == null) s.setAttribute("cart", cart);
      else save(a, cart);
    }
  }

  @Transactional
  public void addAll(HttpSession s, List<StudioRequests.CartItem> input) {
    synchronized (s) {
      if (input == null || input.isEmpty() || input.size() > 50)
        throw new BadRequest("Danh sách sản phẩm không hợp lệ.");
      Long a = actor();
      var cart = load(s, a);
      for (var i : input) {
        if (i == null || i.variantId() == null || i.quantity() < 1 || i.quantity() > 99)
          throw new BadRequest("Số lượng không hợp lệ.");
        cart.merge(i.variantId(), i.quantity(), Integer::sum);
      }
      validate(cart);
      if (a == null) s.setAttribute("cart", cart);
      else save(a, cart);
    }
  }

  @Transactional
  public void clear(HttpSession s) {
    synchronized (s) {
      Long a = actor();
      if (a != null) {
        accounts.lockById(a).orElseThrow();
        items.deleteByAccountId(a);
        items.flush();
      }
      removeGuest(s);
    }
  }

  @Transactional
  public Responses.Cart view(HttpSession s) {
    synchronized (s) {
      var cart = load(s, actor());
      var vs =
          variants.findAllById(cart.keySet()).stream().collect(Collectors.toMap(v -> v.id, v -> v));
      var ps =
          products.findAllById(vs.values().stream().map(v -> v.productId).toList()).stream()
              .collect(Collectors.toMap(p -> p.id, p -> p));
      var lines = new ArrayList<Responses.CartLine>();
      long subtotal = 0;
      int count = 0;
      for (var e : cart.entrySet()) {
        var v = vs.get(e.getKey());
        if (v == null) continue;
        var p = ps.get(v.productId);
        if (p == null) continue;
        long amount = v.price * e.getValue();
        subtotal += amount;
        count += e.getValue();
        lines.add(
            new Responses.CartLine(
                v.id,
                v.id,
                p.id,
                p.slug,
                p.name,
                p.name,
                v.size,
                e.getValue(),
                v.price,
                v.price,
                amount,
                p.image,
                v.stock,
                p.active && v.stock >= e.getValue()));
      }
      long shipping = PromotionService.shipping(subtotal);
      return new Responses.Cart(
          lines, count, subtotal, shipping, subtotal + shipping, subtotal, "VND", Map.of());
    }
  }
}
