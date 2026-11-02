import { state } from "../core/state.js";
import { api } from "../core/api.js";
import { app, money, esc, title, head, empty, statusName } from "../core/ui.js";
export function summary(cart, checkout = false) {
  return `<aside class="summary" id="checkout-summary"><h2>Tóm tắt đơn hàng</h2><div class="summary-row"><span>Tạm tính</span><b>${money(cart.subtotal)}</b></div><div class="summary-row"><span>Vận chuyển</span><b>${cart.shipping ? money(cart.shipping) : "Miễn phí"}</b></div>${cart.discount ? `<div class="summary-row"><span>Ưu đãi</span><b>−${money(cart.discount)}</b></div>` : ""}<div class="summary-row total"><span>Tổng cộng</span><span>${money(cart.total)}</span></div>${checkout ? '<p class="small-text">Thanh toán khi nhận hàng (COD).</p>' : '<a class="btn" href="/checkout">Tiến hành đặt hàng ↗</a>'}<p class="small-text muted">Miễn phí giao hàng từ 2.000.000 ₫. Giá được kiểm tra lại khi đặt hàng.</p></aside>`;
}
export async function cart() {
  title("Giỏ hàng");
  const c = await api("/api/cart");
  if (!c.items.length) {
    app.innerHTML = empty(
      "Giỏ hàng đang chờ bạn.",
      "Thêm một món đồ để bắt đầu bộ phối của riêng mình.",
    );
    return;
  }
  app.innerHTML =
    head("YOUR BAG", "Giỏ hàng.", `${c.item_count} món đồ đã chọn.`) +
    `<section class="content-wrap two-col"><div>${c.items.map((i) => `<article class="cart-line"><a href="/product/${esc(i.slug)}"><img src="${esc(i.image)}" alt="${esc(i.name)}"></a><div><a href="/product/${esc(i.slug)}"><h3>${esc(i.name)}</h3></a><div class="small-text muted">${esc(i.size)} · ${money(i.price)}</div>${!i.available ? '<div class="small-text error">Không đủ tồn kho hoặc đã ngừng bán. Vui lòng sửa giỏ hàng.</div>' : ""}<div class="qty"><button data-qty="${i.quantity - 1}" data-id="${i.variantId}" aria-label="Giảm số lượng">−</button><span>${i.quantity}</span><button data-qty="${i.quantity + 1}" data-id="${i.variantId}" aria-label="Tăng số lượng">+</button></div><button class="remove" data-qty="0" data-id="${i.variantId}">Xóa</button></div><strong>${money(i.lineTotal)}</strong></article>`).join("")}<p style="margin-top:24px"><a class="text-link" href="/shop">← Tiếp tục khám phá</a></p></div>${summary(c)}</section>`;
}
export async function checkout() {
  title("Đặt hàng");
  if (!state.me) {
    app.innerHTML = empty(
      "Đăng nhập để đặt hàng.",
      "Giỏ hàng của bạn được giữ trong phiên hiện tại.",
      "/account?next=/checkout",
      "Đăng nhập",
    );
    return;
  }
  const c = await api("/api/cart");
  const addresses = await api("/api/me/addresses");
  state.checkoutFingerprint = JSON.stringify(
    c.items.map((i) => [i.variantId, i.quantity]),
  );
  if (!c.items.length) {
    app.innerHTML = empty(
      "Chưa có sản phẩm trong giỏ.",
      "Chọn sản phẩm trước khi đặt hàng.",
    );
    return;
  }
  if (c.items.some((i) => !i.available)) {
    app.innerHTML = empty(
      "Giỏ hàng cần được cập nhật.",
      "Một số sản phẩm không còn đủ tồn kho.",
      "/cart",
      "Kiểm tra giỏ hàng",
    );
    return;
  }
  app.innerHTML =
    head("CHECKOUT", "Hoàn tất bộ phối.") +
    `<section class="content-wrap two-col"><form id="checkout-form" class="panel form-stack"><h2>Thông tin nhận hàng</h2>${addresses.length ? `<label>Địa chỉ đã lưu<select id="checkout-address"><option value="">Nhập mới</option>${addresses.map((a) => `<option value="${esc(JSON.stringify(a))}">${esc(a.recipient)} · ${esc(a.address)}</option>`).join("")}</select></label>` : ""}<div class="field-grid"><label>Họ tên người nhận<input name="recipient" required maxlength="100" autocomplete="name" value="${esc(state.me.name)}"></label><label>Số điện thoại<input name="phone" required pattern="0[0-9]{9}" maxlength="10" inputmode="tel" autocomplete="tel" placeholder="09xxxxxxxx"></label><label class="full">Địa chỉ nhận hàng<textarea name="address" required minlength="10" maxlength="500" autocomplete="street-address" placeholder="Số nhà, đường, phường/xã, tỉnh/thành phố"></textarea></label><label class="full">Ghi chú (không bắt buộc)<textarea name="note" maxlength="500" placeholder="Ghi chú cho đơn hàng"></textarea></label></div><label>Mã ưu đãi<input name="voucherCode" maxlength="30" placeholder="STUDIO10 (demo)"></label><button type="button" class="btn light" id="quote-promo">Kiểm tra ưu đãi</button><p id="quote-result" class="small-text" aria-live="polite"></p><div class="note">Thanh toán khi nhận hàng (COD). Đây là hệ thống đồ án; chưa kết nối vận chuyển hay thu tiền trực tuyến.</div><button class="btn" type="submit">Xác nhận đặt hàng · ${money(c.total)}</button></form>${summary(c, true)}</section>`;
}
export async function account() {
  title("Tài khoản");
  if (state.me) {
    const addresses = await api("/api/me/addresses");
    app.innerHTML =
      head("YOUR ACCOUNT", `Xin chào, ${esc(state.me.name)}.`) +
      `<section class="content-wrap"><div class="panel form-stack"><p>${esc(state.me.email)}</p><div><a class="btn light" href="/wardrobe">Tủ đồ ↗</a> <a class="btn light" href="/studio">Outfit Studio ↗</a> <a class="btn" href="/orders">Đơn hàng của tôi ↗</a> ${state.me.role === "ADMIN" ? '<a class="btn light" href="/admin">Trang quản trị ↗</a>' : ""}</div><div><button class="btn light" id="logout">Đăng xuất</button></div></div><div class="panel address-panel"><h2>Sổ địa chỉ</h2>${addresses.map((a) => `<div class="address-row"><p><b>${esc(a.recipient)}</b> · ${esc(a.phone)}<br>${esc(a.address)}</p><button class="remove" data-delete-address="${a.id}">Xóa địa chỉ</button></div>`).join("")}<form id="address-form" class="form-stack"><label>Người nhận<input name="recipient" required maxlength="100"></label><label>Điện thoại<input name="phone" required pattern="0[0-9]{9}" maxlength="10"></label><label>Địa chỉ<textarea name="address" required minlength="10" maxlength="500"></textarea></label><button class="btn light">Lưu địa chỉ mới</button></form></div></section>`;
    return;
  }
  app.innerHTML = `<section class="auth-wrap"><form id="login-form" class="panel form-stack"><span class="eyebrow">WELCOME BACK</span><h1>Chào bạn trở lại.</h1><label>Email<input type="email" name="email" required autocomplete="username" maxlength="150"></label><label>Mật khẩu<input type="password" name="password" required autocomplete="current-password"></label><button type="submit" class="btn">Đăng nhập ↗</button></form><form id="register-form" class="panel form-stack"><span class="eyebrow">JOIN RUNWAY</span><h2>Tạo tài khoản.</h2><label>Họ tên<input name="name" required maxlength="100" autocomplete="name"></label><label>Email<input type="email" name="email" required maxlength="150" autocomplete="email"></label><label>Mật khẩu<input type="password" name="password" required minlength="8" maxlength="72" autocomplete="new-password" placeholder="Tối thiểu 8 ký tự"></label><button type="submit" class="btn light">Đăng ký tài khoản ↗</button></form></section>`;
}
export function orderCard(detail, admin = false) {
  const o = detail.order;
  const next =
    {
      PLACED: ["CONFIRMED", "CANCELLED"],
      CONFIRMED: ["SHIPPED", "CANCELLED"],
      SHIPPED: ["DELIVERED"],
      DELIVERED: [],
      CANCELLED: [],
    }[o.status] || [];
  return `<article class="order-card"><div class="order-top"><div><h3 style="margin-bottom:7px">RW-${String(o.id).padStart(5, "0")}</h3><span class="small-text muted">${new Date(o.createdAt).toLocaleString("vi-VN")}</span></div><span class="badge ${o.status === "CANCELLED" ? "cancel" : ""}">${statusName[o.status] || esc(o.status)}</span></div><div class="order-lines">${detail.items.map((i) => `<p><span>${esc(i.productName)} / ${esc(i.size)} × ${i.quantity}</span><b>${money(i.unitPrice * i.quantity)}</b></p>`).join("")}</div><p class="small-text">${esc(o.recipient)} · ${esc(o.phone)}<br>${esc(o.address)}${o.note ? "<br>Ghi chú: " + esc(o.note) : ""}</p><div class="summary-row"><span>Vận chuyển: ${o.shipping ? money(o.shipping) : "Miễn phí"}</span><b>Tổng: ${money(o.total)}</b></div><ol class="order-timeline" aria-label="Lịch sử đơn hàng">${(detail.history || []).map((h) => `<li><b>${esc(statusName[h.status] || h.status)}</b><small>${new Date(h.createdAt).toLocaleString("vi-VN")}</small></li>`).join("")}</ol>${o.discount ? `<p class="small-text">Ưu đãi ${esc(o.promotionCode)}: −${money(o.discount)}</p>` : ""}${admin ? next.map((s) => `<button class="btn small ${s === "CANCELLED" ? "danger" : ""}" data-status="${s}" data-order="${o.id}">${statusName[s]}</button>`).join(" ") : o.status === "PLACED" ? `<button class="btn small danger" data-cancel="${o.id}">Hủy đơn hàng</button>` : ""}</article>`;
}
export async function orderList() {
  title("Đơn hàng");
  if (!state.me) {
    app.innerHTML = empty(
      "Đăng nhập để xem đơn hàng.",
      "Theo dõi những món đồ bạn đã đặt.",
      "/account?next=/orders",
      "Đăng nhập",
    );
    return;
  }
  const list = await api("/api/orders");
  app.innerHTML =
    head("YOUR ORDERS", "Đơn hàng của bạn.") +
    `<section class="content-wrap">${list.length ? list.map((o) => orderCard(o)).join("") : empty("Bạn chưa có đơn hàng.", "Khám phá bộ sưu tập và tìm món đồ yêu thích.")}</section>`;
}
