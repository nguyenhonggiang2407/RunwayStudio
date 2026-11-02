package vn.runway.store.web;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import vn.runway.store.dto.*;
import vn.runway.store.service.*;

@RestController
public class StoreController {
  private final CatalogService catalog;
  private final CartService carts;
  private final OrderService orders;
  private final CurrentAccount current;
  private final AuthService auth;

  public StoreController(
      CatalogService c, CartService cart, OrderService o, CurrentAccount u, AuthService a) {
    catalog = c;
    carts = cart;
    orders = o;
    current = u;
    auth = a;
  }

  public record Csrf(String token, String headerName) {}

  public record Me(boolean authenticated, Responses.User user) {}

  @GetMapping("/api/csrf")
  public Csrf csrf(CsrfToken t) {
    return new Csrf(t.getToken(), t.getHeaderName());
  }

  @GetMapping("/api/me")
  public Me me() {
    return current.optional().map(a -> new Me(true, current.safe(a))).orElse(new Me(false, null));
  }

  @PostMapping("/api/register")
  public Responses.Message register(@Valid @RequestBody Requests.Register i) {
    return auth.register(i);
  }

  @PostMapping("/api/login")
  public Responses.User login(
      @Valid @RequestBody Requests.Login i, HttpServletRequest r, HttpServletResponse s) {
    return auth.login(i, r, s);
  }

  @GetMapping("/api/products")
  public List<Responses.Product> products(
      @RequestParam(required = false) String q, @RequestParam(required = false) String category) {
    return catalog.list(q, category, false);
  }

  @GetMapping("/api/products/{slug}")
  public Responses.Product product(@PathVariable String slug) {
    return catalog.bySlug(slug);
  }

  @GetMapping("/api/categories")
  public List<String> categories() {
    return catalog.categories();
  }

  @GetMapping("/api/catalog")
  public Responses.Page<Responses.Product> catalog(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(defaultValue = "default") String sort,
      @RequestParam(required = false) Long minPrice,
      @RequestParam(required = false) Long maxPrice,
      @RequestParam(defaultValue = "false") boolean inStock) {
    return catalog.page(q, category, page, size, sort, minPrice, maxPrice, inStock, false);
  }

  @GetMapping({"/api/cart", "/cart.js"})
  public Responses.Cart cart(HttpSession s) {
    return carts.view(s);
  }

  @PostMapping("/api/cart/items")
  public Responses.Cart setCart(@Valid @RequestBody Requests.CartChange i, HttpSession s) {
    carts.change(s, i.variantId(), i.quantity(), false);
    return carts.view(s);
  }

  @DeleteMapping("/api/cart")
  public Responses.Cart clear(HttpSession s) {
    carts.clear(s);
    return carts.view(s);
  }

  public record LegacyItem(@NotNull Long id, @Min(1) @Max(99) Integer quantity) {}

  public record LegacyAdd(
      Long id, Integer quantity, @Size(max = 50) List<@NotNull @Valid LegacyItem> items) {}

  @PostMapping("/cart/add.js")
  public Responses.Cart legacyAdd(@Valid @RequestBody LegacyAdd i, HttpSession s) {
    var entries = i.items() != null ? i.items() : List.of(new LegacyItem(i.id(), i.quantity()));
    carts.addAll(
        s,
        entries.stream()
            .map(x -> new StudioRequests.CartItem(x.id(), x.quantity() == null ? 0 : x.quantity()))
            .toList());
    return carts.view(s);
  }

  @PostMapping("/api/cart/outfit")
  public Responses.Cart outfit(@Valid @RequestBody StudioRequests.OutfitCart i, HttpSession s) {
    carts.addAll(s, i.items());
    return carts.view(s);
  }

  @PostMapping("/api/checkout")
  public Responses.OrderDetail checkout(@Valid @RequestBody Requests.Checkout i, HttpSession s) {
    synchronized (s) {
      return orders.detail(orders.checkoutCart(current.require(), s, i).order());
    }
  }

  public record Quote(@Size(max = 30) String voucherCode) {}

  @PostMapping("/api/checkout/quote")
  public Responses.Quote quote(@Valid @RequestBody Quote i, HttpSession s) {
    return orders.quote(current.require(), s, i.voucherCode());
  }

  @GetMapping("/api/orders")
  public List<Responses.OrderDetail> orders() {
    return orders.list(current.require(), false);
  }

  @GetMapping("/api/me/orders")
  public Responses.Page<Responses.OrderDetail> orders(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
    return orders.page(current.require(), false, page, size);
  }

  @PostMapping("/api/orders/{id}/cancel")
  public Responses.OrderDetail cancel(@PathVariable Long id) {
    return orders.detail(orders.change(id, "CANCELLED", current.require(), false));
  }
}
