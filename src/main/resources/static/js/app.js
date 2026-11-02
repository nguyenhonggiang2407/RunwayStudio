import { state } from "./core/state.js";
import { api, send, resetCsrf, updateCount } from "./core/api.js";
import { app, money, esc, toast, empty, busy } from "./core/ui.js";
import { home, shop, product, filterProducts } from "./pages/catalog.js";
import {
  cart,
  checkout,
  account,
  orderList,
  summary,
} from "./pages/commerce.js";
import { admin } from "./pages/admin.js";
import { studio } from "./studio/outfit.js";
import { scenes } from "./studio/scene.js";
import { wardrobe } from "./studio/wardrobe.js";
import { fitFinder } from "./studio/fit-finder.js";
async function refreshProducts() {
  state.products = await api("/api/products");
}
function safeNext() {
  const n = new URLSearchParams(location.search).get("next");
  return [
    "/checkout",
    "/orders",
    "/admin",
    "/cart",
    "/studio",
    "/wardrobe",
    "/scenes",
    "/fit-finder",
  ].includes(n)
    ? n
    : "/account";
}
async function render() {
  const p = location.pathname.replace(/\/$/, "") || "/";
  const routes = {
    "/": home,
    "/index.html": home,
    "/shop": shop,
    "/cart": cart,
    "/checkout": checkout,
    "/account": account,
    "/orders": orderList,
    "/admin": admin,
    "/studio": studio,
    "/wardrobe": wardrobe,
    "/fit-finder": fitFinder,
  };
  if (routes[p]) return routes[p]();
  if (p.startsWith("/product/")) return product(decodeURIComponent(p.slice(9)));
  if (p === "/scenes") return scenes();
  if (p.startsWith("/scenes/")) return scenes(decodeURIComponent(p.slice(8)));
  app.innerHTML = empty(
    "Không tìm thấy trang.",
    "Trở về cửa hàng để tiếp tục.",
  );
}
document.addEventListener("click", (e) => {
  const v = e.target.closest("[data-variant]");
  if (v) {
    state.selectedVariant = JSON.parse(v.dataset.variant);
    document.querySelectorAll("[data-variant]").forEach((b) => {
      b.classList.toggle("selected", b === v);
      b.setAttribute("aria-pressed", String(b === v));
    });
    document.querySelector("#detail-price").textContent = money(
      state.selectedVariant.price,
    );
    document.querySelector("#stock-label").textContent =
      `Còn ${state.selectedVariant.stock} sản phẩm`;
    return;
  }
  const b = e.target.closest("button");
  if (!b) return;
  if (b.dataset.catalogPage !== undefined)
    return busy(b, () => filterProducts(Number(b.dataset.catalogPage)));
  if (b.dataset.saveProduct)
    return busy(b, async () => {
      if (!state.me) {
        location.href = "/account?next=/wardrobe";
        return;
      }
      await send("/api/me/wardrobe", {
        productId: Number(b.dataset.saveProduct),
      });
      b.textContent = "♥";
      toast("Đã lưu vào tủ đồ.");
    });
  if (b.id === "add-product")
    return busy(b, async () => {
      const quantity = Number(document.querySelector("#product-qty").value);
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > 99)
        throw new Error("Số lượng phải từ 1 đến 99.");
      await send("/cart/add.js", { id: state.selectedVariant.id, quantity });
      await updateCount();
      toast("Đã thêm sản phẩm vào giỏ hàng.");
    });
  if (b.dataset.qty !== undefined)
    return busy(b, async () => {
      await send("/api/cart/items", {
        variantId: Number(b.dataset.id),
        quantity: Number(b.dataset.qty),
      });
      await cart();
      await updateCount();
    });
  if (b.id === "logout")
    return busy(b, async () => {
      await api("/api/logout", { method: "POST" });
      resetCsrf();
      sessionStorage.removeItem("checkout-request");
      location.href = "/account";
    });
  if (b.dataset.cancel)
    return busy(b, async () => {
      if (!confirm("Hủy đơn hàng này?")) return;
      await send("/api/orders/" + b.dataset.cancel + "/cancel", {});
      await orderList();
      toast("Đã hủy đơn hàng.");
    });
  if (b.dataset.status)
    return busy(b, async () => {
      if (
        b.dataset.status === "CANCELLED" &&
        !confirm("Hủy đơn hàng và hoàn tồn kho?")
      )
        return;
      await send(
        "/api/admin/orders/" + b.dataset.order + "/status",
        { status: b.dataset.status },
        "PUT",
      );
      await admin();
      toast("Đã cập nhật trạng thái.");
    });
  if (b.dataset.tab) {
    state.activeTab = b.dataset.tab;
    return busy(b, admin);
  }
  if (b.dataset.deactivate)
    return busy(b, async () => {
      if (!confirm("Ngừng bán sản phẩm này?")) return;
      await api("/api/admin/products/" + b.dataset.deactivate, {
        method: "DELETE",
      });
      await refreshProducts();
      await admin();
      toast("Đã ngừng bán.");
    });
  if (b.dataset.deleteAddress)
    return busy(b, async () => {
      await api("/api/me/addresses/" + b.dataset.deleteAddress, {
        method: "DELETE",
      });
      await account();
      toast("Đã xóa địa chỉ.");
    });
  if (b.id === "quote-promo")
    return busy(b, async () => {
      const r = await send("/api/checkout/quote", {
        voucherCode: document.querySelector("[name=voucherCode]").value,
      });
      document.querySelector("#checkout-summary").outerHTML = summary(r, true);
      document.querySelector("#quote-result").textContent = r.discount
        ? "Ưu đãi được áp dụng: −" + money(r.discount)
        : "Giá trị đơn đã cập nhật.";
      document.querySelector("#checkout-form button[type=submit]").textContent =
        "Xác nhận đặt hàng · " + money(r.total);
    });
});
document.addEventListener("change", (e) => {
  if (
    e.target.closest("#filters") ||
    e.target.getAttribute("form") === "filters"
  )
    busy(null, () => filterProducts());
  if (e.target.id === "checkout-address" && e.target.value) {
    const a = JSON.parse(e.target.value),
      f = document.querySelector("#checkout-form");
    for (const k of ["recipient", "phone", "address"])
      f.elements.namedItem(k).value = a[k];
  }
});
document.addEventListener("submit", (e) => {
  const f = e.target;
  if (
    ![
      "filters",
      "login-form",
      "register-form",
      "checkout-form",
      "new-product",
      "address-form",
    ].includes(f.id) &&
    !f.dataset.product &&
    !f.dataset.editVariant &&
    !f.dataset.addVariant
  )
    return;
  e.preventDefault();
  if (f.id === "filters") return busy(null, () => filterProducts());
  const data = Object.fromEntries(new FormData(f));
  busy(
    e.submitter || f.querySelector("button[type=submit],button:not([type])"),
    async () => {
      if (f.id === "login-form") {
        await send("/api/login", data);
        resetCsrf();
        location.href = safeNext();
      } else if (f.id === "register-form") {
        await send("/api/register", data);
        f.reset();
        toast("Đăng ký thành công. Hãy đăng nhập ở ô bên cạnh.");
      } else if (f.id === "checkout-form") {
        let pending;
        try {
          pending = JSON.parse(sessionStorage.getItem("checkout-request"));
        } catch {}
        if (!pending || pending.fingerprint !== state.checkoutFingerprint)
          pending = {
            id: crypto.randomUUID(),
            fingerprint: state.checkoutFingerprint,
          };
        sessionStorage.setItem("checkout-request", JSON.stringify(pending));
        const r = await send("/api/checkout", {
          ...data,
          requestId: pending.id,
        });
        sessionStorage.removeItem("checkout-request");
        await updateCount();
        app.innerHTML = `<section class="content-wrap"><div class="empty"><div class="success-icon">✓</div><span class="eyebrow">THANK YOU</span><h1>Đặt hàng thành công.</h1><p>Đơn RW-${String(r.order.id).padStart(5, "0")} đã được lưu.<br>Thanh toán khi nhận hàng: ${money(r.order.total)}.</p><a class="btn" href="/orders">Theo dõi đơn hàng ↗</a></div></section>`;
      } else if (f.id === "address-form") {
        await send("/api/me/addresses", data);
        await account();
        toast("Đã lưu địa chỉ.");
      } else if (f.id === "new-product" || f.dataset.product) {
        data.active = data.active === "true";
        await send(
          "/api/admin/products" +
            (f.dataset.product ? "/" + f.dataset.product : ""),
          data,
          f.dataset.product ? "PUT" : "POST",
        );
        await refreshProducts();
        await admin();
        toast("Đã lưu sản phẩm.");
      } else if (f.dataset.editVariant || f.dataset.addVariant) {
        data.price = Number(data.price);
        data.stock = Number(data.stock);
        await send(
          f.dataset.editVariant
            ? "/api/admin/variants/" + f.dataset.editVariant
            : "/api/admin/products/" + f.dataset.addVariant + "/variants",
          data,
          f.dataset.editVariant ? "PUT" : "POST",
        );
        await refreshProducts();
        await admin();
        toast("Đã lưu phân loại và tồn kho.");
      }
    },
  );
});
(async () => {
  try {
    const [session, catalog] = await Promise.all([
      api("/api/me"),
      api("/api/products"),
    ]);
    state.me = session.authenticated ? session.user : null;
    state.products = catalog;
    if (state.me)
      document.querySelector("#account-link").textContent = state.me.name
        .split(" ")
        .at(-1);
    await render();
    await updateCount();
  } catch (e) {
    app.innerHTML = empty("Chưa thể mở trang.", esc(e.message));
  }
})();
