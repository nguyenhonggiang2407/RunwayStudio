import { state } from "../core/state.js";
import { api } from "../core/api.js";
import { app, money, esc, title, head, empty } from "../core/ui.js";
import { orderCard } from "./commerce.js";
import { adminStudio } from "../studio/admin.js";
export function productFields(p = {}) {
  return `<div class="field-grid"><label>Tên sản phẩm<input name="name" required maxlength="160" value="${esc(p.name)}"></label><label>Đường dẫn<input name="slug" required maxlength="160" pattern="[a-z0-9]+(-[a-z0-9]+)*" value="${esc(p.slug)}" placeholder="ten-san-pham"></label><label>Danh mục<input name="category" required maxlength="60" value="${esc(p.category)}"></label><label>Ảnh (đường dẫn /media/...)<input name="image" required value="${esc(p.image || state.products[0]?.image)}"></label><label class="full">Mô tả<textarea name="description" maxlength="2000">${esc(p.description)}</textarea></label><label>Hiển thị<select name="active"><option value="true" ${p.active !== false ? "selected" : ""}>Đang bán</option><option value="false" ${p.active === false ? "selected" : ""}>Ngừng bán</option></select></label></div>`;
}
export async function admin() {
  title("Quản trị");
  if (
    new URLSearchParams(location.search).get("view") === "scenes" &&
    state.me?.role === "ADMIN"
  )
    return adminStudio();
  if (!state.me || state.me.role !== "ADMIN") {
    app.innerHTML = empty(
      "Khu vực dành cho quản trị viên.",
      "Đăng nhập bằng tài khoản có quyền quản trị.",
      "/account?next=/admin",
      "Đăng nhập",
    );
    return;
  }
  const [stats, all, list] = await Promise.all([
    api("/api/admin/dashboard"),
    api("/api/admin/products"),
    api("/api/admin/orders"),
  ]);
  app.innerHTML =
    head("RUNWAY BACK OFFICE", "Quản lý cửa hàng.") +
    `<section class="content-wrap"><div class="stats"><div class="stat"><b>${stats.orders}</b><span>Đơn hàng</span></div><div class="stat"><b>${money(stats.revenue)}</b><span>Doanh thu đã giao</span></div><div class="stat"><b>${stats.pending}</b><span>Chờ xác nhận</span></div><div class="stat"><b>${stats.products}</b><span>Sản phẩm</span></div></div><p><a class="text-link" href="/admin?view=scenes">Scenes & hotspot editor ↗</a> · <a class="text-link" href="/studio?curated=1">Outfit mẫu ↗</a></p><div class="admin-tabs"><button class="btn ${state.activeTab === "products" ? "" : "light"}" data-tab="products">Sản phẩm & tồn kho</button><button class="btn ${state.activeTab === "orders" ? "" : "light"}" data-tab="orders">Đơn hàng (${list.length})</button></div>${state.activeTab === "orders" ? (list.length ? list.map((o) => orderCard(o, true)).join("") : empty("Chưa có đơn hàng.", "Đơn mới sẽ xuất hiện tại đây.")) : `<div class="admin-layout"><div class="panel"><h2>Danh mục sản phẩm</h2>${all.map((p) => `<details class="admin-product"><summary style="grid-column:1/-1;display:flex;align-items:center;gap:15px;cursor:pointer"><img src="${esc(p.image)}" alt=""><div><h3>${esc(p.name)}</h3><span class="small-text muted">${esc(p.category)} · ${p.active ? "Đang bán" : "Ngừng bán"} · ${p.variants.length} phân loại</span></div></summary><div style="grid-column:1/-1"><form class="form-stack admin-edit" data-product="${p.id}">${productFields(p)}<button class="btn small">Lưu thông tin</button></form>${p.variants.map((v) => `<form class="variant-row" data-edit-variant="${v.id}"><label>Phân loại<input name="size" required maxlength="80" value="${esc(v.size)}"></label><label>Giá (VND)<input name="price" type="number" required min="1000" max="1000000000" value="${v.price}"></label><label>Tồn kho<input name="stock" type="number" required min="0" max="1000000" value="${v.stock}"></label><button class="btn small">Lưu</button></form>`).join("")}<form class="variant-row" data-add-variant="${p.id}"><label>Phân loại mới<input name="size" required maxlength="80" placeholder="M / Đen"></label><label>Giá (VND)<input name="price" type="number" required min="1000" max="1000000000"></label><label>Tồn kho<input name="stock" type="number" required min="0" max="1000000" value="20"></label><button class="btn small light">Thêm</button></form><button class="remove" data-deactivate="${p.id}" style="margin-top:20px">Ngừng bán sản phẩm</button></div></details>`).join("")}</div><form id="new-product" class="panel form-stack"><h2>Thêm sản phẩm</h2>${productFields()}<p class="small-text muted">Chỉ dùng hình SVG đã duyệt trong /media/owned/. Sau khi tạo, mở sản phẩm trong danh sách để thêm giá, size và tồn kho.</p><button class="btn">Tạo sản phẩm ↗</button></form></div>`}</section>`;
}
