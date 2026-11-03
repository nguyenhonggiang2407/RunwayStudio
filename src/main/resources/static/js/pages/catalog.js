import { runwayHome } from "./runway.js";
import { state } from "../core/state.js";
import { api } from "../core/api.js";
import { app, money, esc, title, head, empty, cards } from "../core/ui.js";
export async function product(slug) {
  const p = await api("/api/products/" + encodeURIComponent(slug));
  title(p.name);
  const photos = p.image.startsWith("/media/original/")
    ? await api("/product-photos.json")
    : {};
  const image = photos[p.slug]?.full || p.image;
  state.selectedVariant = p.variants.find((v) => v.stock > 0) || p.variants[0];
  app.innerHTML = `<section class="product-detail"><div class="detail-image"><img src="${esc(image)}" alt="${esc(p.name)}" fetchpriority="high"></div><div class="detail-info"><span class="eyebrow"><a href="/shop">CỬA HÀNG</a> / ${esc(p.category)}</span><h1>${esc(p.name)}</h1><div class="detail-price" id="detail-price">${money(state.selectedVariant?.price || 0)}</div><p class="muted">${esc(p.description)}</p><p class="small-text" style="margin-bottom:0">${p.category === "Giày" ? "Chọn cỡ giày" : "Chọn phân loại"}</p><div class="sizes">${p.variants.map((v) => `<button class="size ${state.selectedVariant?.id === v.id ? "selected" : ""}" data-variant='${esc(JSON.stringify(v))}' ${!v.stock ? "disabled" : ""} aria-pressed="${state.selectedVariant?.id === v.id}">${esc(v.size)}</button>`).join("")}</div><p class="small-text muted" id="stock-label">${state.selectedVariant?.stock ? `Còn ${state.selectedVariant.stock} sản phẩm` : "Tạm hết hàng"}</p><label>Số lượng<input id="product-qty" type="number" min="1" max="99" value="1" style="max-width:100px"></label><button class="btn" id="add-product" ${!state.selectedVariant?.stock ? "disabled" : ""}>Thêm vào giỏ hàng <span>↗</span></button><p class="small-text muted">Thanh toán khi nhận hàng · Miễn phí vận chuyển từ 2.000.000 ₫</p><details><summary>Thông tin sản phẩm</summary><p>Sản phẩm minh họa phục vụ đồ án. ${p.image.startsWith("/media/original/") ? "Ảnh được giữ từ bộ tư liệu Runway gốc." : "Hình minh họa cho mẫu bổ sung của Studio."} Giá và tồn kho do máy chủ quản lý.</p></details><p><a class="text-link" href="/fit-finder">Mở Fit Finder ↗</a></p><details><summary>Chọn size phù hợp</summary><p>${p.category === "Giày" ? "Chọn cỡ giày bạn thường sử dụng. Đo chiều dài bàn chân và đối chiếu bảng size của nhà sản xuất; hệ thống không dùng quy tắc size áo cho giày." : "Kiểm tra số đo và kiểu dáng trước khi chọn size. Phân loại có thể bao gồm màu hoặc kích cỡ theo dữ liệu mẫu."}</p></details><details><summary>Giao hàng</summary><p>Phí cố định 30.000 ₫ cho đơn dưới 2.000.000 ₫. Đơn đạt ngưỡng được miễn phí. Hình thức thanh toán: COD.</p></details></div></section><section class="section" style="padding-top:0"><div class="section-head"><h2>Phối thêm một chút.</h2><a class="text-link" href="/shop">Xem tất cả ↗</a></div><div class="products">${cards(state.products.filter((x) => x.id !== p.id).slice(0, 4))}</div></section>`;
}
export const home = runwayHome;
export async function shop() {
  title("Cửa hàng");
  const params = new URLSearchParams(location.search),
    cats = await api("/api/categories");
  app.innerHTML =
    head(
      "THE OBJECTS",
      "Bắt đầu từ một món đồ.",
      "Chọn món đồ hợp với nhịp sống của bạn.",
    ) +
    `<section class="catalog"><form id="filters" class="filters"><input aria-label="Tìm sản phẩm" name="q" type="search" placeholder="Tìm sản phẩm…" value="${esc(params.get("q") || "")}"><select name="category" aria-label="Danh mục"><option value="">Tất cả danh mục</option>${cats.map((c) => `<option ${params.get("category") === c ? "selected" : ""}>${esc(c)}</option>`).join("")}</select><select name="sort" aria-label="Sắp xếp"><option value="default">Khám phá</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option><option value="name">Tên sản phẩm</option></select><button class="btn">Tìm kiếm</button></form><div class="catalog-extra"><label>Giá từ<input name="minPrice" form="filters" type="number" min="0" step="1000" placeholder="VND"></label><label>Đến<input name="maxPrice" form="filters" type="number" min="0" step="1000" placeholder="VND"></label><label class="check"><input name="inStock" form="filters" type="checkbox"> Chỉ còn hàng</label></div><p id="result-count" aria-live="polite" class="small-text muted"></p><div id="product-grid" class="products"></div><nav id="catalog-pages" class="pagination" aria-label="Trang sản phẩm"></nav></section>`;
  if (params.get("sort"))
    document.querySelector("[name=sort]").value = params.get("sort");
  for (const k of ["minPrice", "maxPrice"])
    if (params.has(k))
      document.querySelector(`[name=${k}]`).value = params.get(k);
  if (params.get("inStock") === "true")
    document.querySelector("[name=inStock]").checked = true;
  await filterProducts(Number(params.get("page")) || 0);
}
let catalogRequest = 0;
export async function filterProducts(page = 0) {
  const request = ++catalogRequest,
    f = document.querySelector("#filters");
  if (!f) return;
  const q = new URLSearchParams(new FormData(f));
  q.set("page", String(page));
  q.set("size", "8");
  if (q.get("inStock")) q.set("inStock", "true");
  for (const k of [...q.keys()]) if (q.get(k) === "") q.delete(k);
  document.querySelector("#product-grid").setAttribute("aria-busy", "true");
  const r = await api("/api/catalog?" + q);
  if (request !== catalogRequest) return;
  document.querySelector("#result-count").textContent =
    `${r.totalElements} sản phẩm · Trang ${r.totalPages ? r.page + 1 : 0}/${r.totalPages}`;
  document.querySelector("#product-grid").innerHTML = r.items.length
    ? cards(r.items)
    : "<p>Chưa tìm thấy sản phẩm phù hợp. Thử thay đổi bộ lọc.</p>";
  document.querySelector("#product-grid").setAttribute("aria-busy", "false");
  document.querySelector("#catalog-pages").innerHTML =
    `<button class="btn light small" data-catalog-page="${page - 1}" ${page === 0 ? "disabled" : ""}>← Trước</button><button class="btn light small" data-catalog-page="${page + 1}" ${page + 1 >= r.totalPages ? "disabled" : ""}>Tiếp →</button>`;
  history.replaceState(null, "", "/shop?" + q);
}
