import { api, send, updateCount } from "../core/api.js";
import { app, esc, money, title, head, busy, toast } from "../core/ui.js";
export async function scenes(slug) {
  title("Shop The Scene");
  if (!slug) {
    const r = await api("/api/scenes");
    app.innerHTML =
      head(
        "SHOP THE SCENE",
        "Cảm hứng nằm trong chi tiết.",
        "Chạm vào món đồ trong khung hình để khám phá bộ phối.",
      ) +
      `<section class="section scene-gallery">${r.items.map((s, n) => `<a class="scene-tile" href="/scenes/${encodeURIComponent(s.slug)}"><div><img src="${esc(s.coverImage)}" alt="${esc(s.altText)}" loading="lazy" width="1200" height="1000"><span>${String(n + 1).padStart(2, "0")} / THE SCENE</span></div><h2>${esc(s.title)} ↗</h2><p>${s.hotspots.length} món đồ để khám phá</p></a>`).join("") || "<p>Studio đang chuẩn bị những khung hình mới.</p>"}</section>`;
    return;
  }
  const s = await api("/api/scenes/" + encodeURIComponent(slug));
  app.innerHTML =
    head(
      "THE SCENE / " + esc(s.slug).toUpperCase(),
      esc(s.title),
      "Chạm vào điểm đánh dấu. Tìm món đồ hợp với bạn.",
    ) +
    `<section class="scene-layout"><div class="scene-canvas" id="scene-canvas"><img src="${esc(s.coverImage)}" alt="${esc(s.altText)}" width="1200" height="1000">${s.hotspots.map((h, n) => `<button class="hotspot" data-hotspot="${h.id}" aria-label="Xem ${esc(h.label)}">${n + 1}</button>`).join("")}</div><aside class="scene-sidebar"><span class="eyebrow">LOOK CLOSER</span><h2>Cả một bộ phối.<br>Từng chi tiết nhỏ.</h2><p class="muted">Chọn điểm trên ảnh hoặc món đồ dưới đây để xem giá, phân loại và tình trạng còn hàng.</p><div class="scene-products">${s.hotspots.map((h) => `<button class="scene-product" data-open-hotspot="${h.id}"><img src="${esc(h.product?.image)}" alt=""><span><b>${esc(h.label)}</b><small>${money(h.product?.price || 0)} · ${h.product?.active ? "Xem phân loại ↗" : "Đã ngừng bán"}</small></span></button>`).join("")}</div><a class="text-link" href="/scenes">← Tất cả khung hình</a></aside></section><dialog id="scene-product-dialog" aria-labelledby="scene-dialog-title"><div id="scene-dialog-content"></div></dialog>`;
  for (const h of s.hotspots) {
    const b = app.querySelector(`[data-hotspot="${h.id}"]`);
    b.style.left = h.xPercent + "%";
    b.style.top = h.yPercent + "%";
  }
  const dialog = document.querySelector("#scene-product-dialog");
  function open(id) {
    const h = s.hotspots.find((h) => h.id === Number(id)),
      p = h.product,
      available = p?.active && p.variants.some((v) => v.stock > 0);
    document.querySelector("#scene-dialog-content").innerHTML =
      `<button class="dialog-close icon-btn" aria-label="Đóng" id="close-scene-dialog">×</button><img src="${esc(p?.image)}" alt="${esc(p?.name)}"><span class="eyebrow">FROM THE SCENE</span><h2 id="scene-dialog-title">${esc(h.label)}</h2><p>${money(p?.price || 0)}</p>${
        available
          ? `<label>Phân loại<select id="scene-variant">${p.variants
              .filter((v) => v.stock > 0)
              .map(
                (v) =>
                  `<option value="${v.id}">${esc(v.size)} · ${money(v.price)} · Còn ${v.stock}</option>`,
              )
              .join(
                "",
              )}</select></label><button class="btn" id="scene-add">Thêm vào giỏ ↗</button>`
          : '<p class="note">Món đồ hiện đã ngừng bán hoặc hết hàng. Bạn vẫn có thể tham khảo cách phối.</p>'
      }${p?.active ? `<a class="text-link" href="/product/${encodeURIComponent(p.slug)}">Xem sản phẩm ↗</a>` : ""}`;
    dialog.showModal();
    document.querySelector("#close-scene-dialog").onclick = () =>
      dialog.close();
    const add = document.querySelector("#scene-add");
    if (add)
      add.onclick = () =>
        busy(add, async () => {
          await send("/cart/add.js", {
            id: Number(document.querySelector("#scene-variant").value),
            quantity: 1,
          });
          await updateCount();
          toast("Đã thêm sản phẩm vào giỏ.");
          dialog.close();
        });
  }
  app.addEventListener("click", (e) => {
    const b = e.target.closest("[data-hotspot],[data-open-hotspot]");
    if (b) open(b.dataset.hotspot || b.dataset.openHotspot);
  });
}
