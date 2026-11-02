import { state } from "../core/state.js";
import { api } from "../core/api.js";
import {
  app,
  esc,
  money,
  title,
  head,
  empty,
  busy,
  toast,
} from "../core/ui.js";
export async function wardrobe() {
  title("My Wardrobe");
  if (!state.me) {
    app.innerHTML = empty(
      "Một góc nhỏ, rất riêng.",
      "Đăng nhập để lưu món đồ yêu thích và bộ phối của bạn.",
      "/account?next=/wardrobe",
      "Đăng nhập",
    );
    return;
  }
  const [items, outfits] = await Promise.all([
    api("/api/me/wardrobe"),
    api("/api/me/outfits"),
  ]);
  app.innerHTML =
    head(
      "MY WARDROBE",
      "Những điều bạn muốn giữ.",
      "Sản phẩm yêu thích và ý tưởng phối đồ, dành riêng cho bạn.",
    ) +
    `<section class="section"><div class="section-head"><h2>${items.length} món đồ đã lưu.</h2><a class="text-link" href="/shop">Thêm cảm hứng ↗</a></div>${items.length ? `<div class="products">${items.map((i) => `<article class="product-card"><a href="/product/${encodeURIComponent(i.product.slug)}"><div class="product-image"><img src="${esc(i.product.image)}" alt="${esc(i.product.name)}" loading="lazy"></div><div class="category">${esc(i.product.category)}</div><h3>${esc(i.product.name)}</h3><p>${money(i.product.price)}</p></a><button class="remove" data-remove-wardrobe="${i.id}">Bỏ lưu</button></article>`).join("")}</div>` : '<div class="panel"><p>Tủ đồ đang chờ ý tưởng đầu tiên. Chạm ♡ ở sản phẩm bạn thích.</p><a class="text-link" href="/shop">Khám phá cửa hàng ↗</a></div>'}<div class="section-head wardrobe-heading"><h2>Bộ phối của bạn.</h2><a class="text-link" href="/studio">Mở Studio ↗</a></div>${
      outfits.length
        ? `<div class="outfit-list">${outfits
            .map(
              (o) =>
                `<a class="saved-outfit" href="/studio?outfit=${o.id}"><div class="outfit-thumbs">${o.items
                  .slice(0, 3)
                  .map(
                    (i) =>
                      `<img src="${esc(i.product.image)}" alt="${esc(i.product.name)}" loading="lazy">`,
                  )
                  .join(
                    "",
                  )}</div><div><span class="eyebrow">PRIVATE EDIT</span><h3>${esc(o.name)}</h3><p>${money(o.subtotal)}</p><span class="text-link">Mở bộ phối ↗</span></div></a>`,
            )
            .join("")}</div>`
        : '<p class="muted">Chưa lưu bộ phối. Hãy bắt đầu trong Outfit Studio.</p>'
    }</section>`;
  app.onclick = (e) => {
    const b = e.target.closest("[data-remove-wardrobe]");
    if (b)
      busy(b, async () => {
        await api("/api/me/wardrobe/" + b.dataset.removeWardrobe, {
          method: "DELETE",
        });
        await wardrobe();
        toast("Đã bỏ lưu sản phẩm.");
      });
  };
}
