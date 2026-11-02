export const app = document.querySelector("#app");
export const money = (n) =>
  new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(n);
export const esc = (s) =>
  String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
export const statusName = {
  PLACED: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  SHIPPED: "Đang giao",
  DELIVERED: "Đã giao",
  CANCELLED: "Đã hủy",
};
let toastTimer;
export function toast(message) {
  const t = document.querySelector("#toast");
  t.textContent = message;
  t.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove("show"), 4000);
}
export function title(text) {
  document.title = text + " | Runway Studio";
}
export function head(eyebrow, name, desc = "") {
  return `<div class="page-head"><span class="eyebrow">${eyebrow}</span><h1>${name}</h1>${desc ? `<p>${desc}</p>` : ""}</div>`;
}
export function empty(
  name,
  description,
  link = "/shop",
  label = "Khám phá cửa hàng",
) {
  return `<div class="empty"><h2>${name}</h2><p>${description}</p><a class="btn" href="${link}">${label} ↗</a></div>`;
}
export async function busy(button, task) {
  if (button?.disabled) return;
  const old = button?.textContent;
  if (button) {
    button.disabled = true;
    button.textContent = "Đang xử lý…";
  }
  try {
    await task();
  } catch (e) {
    toast(e.message);
  } finally {
    if (button?.isConnected) {
      button.disabled = false;
      button.textContent = old;
    }
  }
}
export function cards(items) {
  return items
    .map(
      (p) =>
        `<article class="product-card"><a href="/product/${encodeURIComponent(p.slug)}"><div class="product-image"><span class="product-tag">RUNWAY OBJECTS</span><img src="${esc(p.image)}" alt="${esc(p.name)}" loading="lazy" width="620" height="620"></div><div class="category">${esc(p.category)}</div><h3>${esc(p.name)}</h3><div class="price">${money(p.price)}</div></a><button class="save-product" data-save-product="${p.id}" aria-label="Lưu ${esc(p.name)} vào tủ đồ">♡</button></article>`,
    )
    .join("");
}
