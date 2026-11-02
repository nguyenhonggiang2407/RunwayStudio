import { state } from "../core/state.js";
import { api, send, updateCount } from "../core/api.js";
import { app, esc, money, title, head, toast, busy } from "../core/ui.js";
const slots = ["TOP", "BOTTOM", "SHOES", "ACCESSORY"],
  labels = {
    TOP: "Áo / áo khoác",
    BOTTOM: "Quần",
    SHOES: "Giày",
    ACCESSORY: "Phụ kiện",
  };
let catalog = [],
  saved = [],
  curated = [],
  draft,
  order = [...slots],
  bindings;
const adminMode = () =>
  state.me?.role === "ADMIN" &&
  new URLSearchParams(location.search).get("curated") === "1";
const accepts = (s, p) =>
  s === "TOP"
    ? ["Áo", "Áo khoác", "Áo thun"].includes(p.category)
    : p.category ===
      { BOTTOM: "Quần", SHOES: "Giày", ACCESSORY: "Phụ kiện" }[s];
const selection = (s) => draft.items.find((i) => i.slot === s),
  product = (i) => catalog.find((p) => p.id === i?.productId) || i?.product,
  variant = (i) => product(i)?.variants.find((v) => v.id === i?.variantId);
function fresh() {
  draft = { id: null, version: null, name: "My everyday edit", items: [] };
  order = [...slots];
}
function read() {
  const f = document.querySelector("#outfit-form");
  draft.name = f.elements.namedItem("name").value;
  draft.items = order.flatMap((slot) => {
    const p = Number(f.elements.namedItem("product-" + slot).value),
      v = Number(f.elements.namedItem("variant-" + slot).value);
    return p ? [{ slot, productId: p, variantId: v || null }] : [];
  });
}
async function refresh() {
  saved = state.me
    ? adminMode()
      ? (await api("/api/admin/outfits")).items
      : await api("/api/me/outfits")
    : [];
}
function load(o, own) {
  draft = {
    ...o,
    id: own ? o.id : null,
    items: o.items.map((i) => ({ ...i })),
  };
  order = [
    ...o.items.map((i) => i.slot),
    ...slots.filter((s) => !o.items.some((i) => i.slot === s)),
  ];
}
function card(o, own) {
  return `<article class="saved-outfit"><div class="outfit-thumbs">${o.items
    .slice(0, 3)
    .map(
      (i) =>
        `<img src="${esc(i.product?.image)}" alt="${esc(i.product?.name)}" loading="lazy">`,
    )
    .join(
      "",
    )}</div><div><span class="eyebrow">${own ? (adminMode() ? esc(o.status) : "CHỈ MÌNH BẠN") : "CURATED EDIT"}</span><h3>${esc(o.name)}</h3><p>${money(o.subtotal)}</p><button class="btn small light" data-load-outfit="${o.id}" data-own="${own}">${own ? "Mở bộ phối" : "Phối theo ý bạn"} ↗</button> ${own ? `<button class="remove" data-delete-outfit="${o.id}">Xóa</button>` : `<button class="remove" data-share-outfit="${o.id}">Chia sẻ</button>`}</div></article>`;
}
function draw(focus) {
  bindings?.abort();
  bindings = new AbortController();
  const signal = bindings.signal;
  app.innerHTML =
    head(
      "YOUR CREATIVE SPACE",
      adminMode() ? "Curated Outfit Studio." : "Outfit Studio.",
      "Chọn từng món. Nhìn cả bộ phối. Tạo một phong cách của riêng bạn.",
    ) +
    `<section class="studio-workspace"><div class="outfit-board" aria-label="Bản xem trước bộ phối"><div class="board-top"><span>THE PERSONAL EDIT</span><span>${String(draft.items.length).padStart(2, "0")} OBJECTS</span></div><div class="board-grid">${order
      .map((s, n) => {
        const i = selection(s),
          p = product(i);
        return `<div class="board-slot"><span class="slot-index">0${n + 1} / ${labels[s]}</span>${p ? `<img src="${esc(p.image)}" alt="${esc(p.name)}"><span class="board-label">${esc(p.name)} · ${esc(variant(i)?.size || "Chưa chọn size")}</span>` : `<div class="slot-placeholder"><span>+</span><p>Thêm ${labels[s].toLowerCase()}</p></div>`}</div>`;
      })
      .join(
        "",
      )}</div><p class="board-footnote">Một bản phối để bắt đầu câu chuyện của bạn.</p></div><form id="outfit-form" class="studio-controls panel"><span class="eyebrow">MAKE IT YOURS</span><label>Tên bộ phối<input name="name" maxlength="100" required value="${esc(draft.name)}"></label>${order
      .map((s, n) => {
        const i = selection(s),
          p = product(i);
        return `<fieldset class="slot-control"><legend>0${n + 1} / ${labels[s]}</legend><div class="slot-order"><button type="button" class="icon-btn" data-move-slot="${s}" data-direction="-1" ${n === 0 ? "disabled" : ""} aria-label="Đưa ${labels[s]} lên trước">↑</button><button type="button" class="icon-btn" data-move-slot="${s}" data-direction="1" ${n === 3 ? "disabled" : ""} aria-label="Đưa ${labels[s]} xuống sau">↓</button></div><label>Sản phẩm<select id="product-${s}" name="product-${s}" data-slot-select="${s}"><option value="">Chưa chọn</option>${catalog
          .filter((p) => accepts(s, p))
          .map(
            (p) =>
              `<option value="${p.id}" ${i?.productId === p.id ? "selected" : ""}>${esc(p.name)}</option>`,
          )
          .join(
            "",
          )}</select></label><label>Phân loại<select id="variant-${s}" name="variant-${s}" data-variant-select="${s}" ${!p ? "disabled" : ""}><option value="">Chọn size</option>${(p?.variants || []).map((v) => `<option value="${v.id}" ${i?.variantId === v.id ? "selected" : ""} ${!v.stock ? "disabled" : ""}>${esc(v.size)} · ${money(v.price)}${!v.stock ? " · Hết hàng" : ""}</option>`).join("")}</select></label></fieldset>`;
      })
      .join(
        "",
      )}<div class="studio-total"><span>Tạm tính</span><b>${money(draft.items.reduce((n, i) => n + (variant(i)?.price || 0), 0))}</b></div><p class="small-text muted">Chọn phân loại cho từng món. Giá và tồn kho được kiểm tra lại khi đặt hàng.</p><div class="studio-buttons"><button class="btn" type="button" id="outfit-add">Thêm cả bộ vào giỏ ↗</button><button class="btn light" type="submit">${state.me ? "Lưu " + (adminMode() ? "outfit mẫu" : "bộ phối") : "Đăng nhập để lưu"}</button>${adminMode() && draft.id ? '<button type="button" class="btn light" id="publish-outfit">Xuất bản outfit mẫu</button>' : ""}<button class="text-link" type="button" id="new-outfit">Bắt đầu bộ mới</button></div></form></section><section class="section"><div class="section-head"><div><span class="eyebrow">YOUR IDEAS, KEPT SAFE</span><h2>${adminMode() ? "Outfit mẫu" : "Bộ phối đã lưu"}.</h2></div></div>${state.me ? (saved.length ? `<div class="outfit-list">${saved.map((o) => card(o, true)).join("")}</div>` : '<p class="muted">Chưa có bộ phối. Hãy lưu ý tưởng đầu tiên của bạn.</p>') : '<p><a class="text-link" href="/account?next=/studio">Đăng nhập để giữ lại bộ phối ↗</a></p>'}</section>${adminMode() ? "" : `<section class="section"><div class="section-head"><div><span class="eyebrow">A LITTLE INSPIRATION</span><h2>Từ Studio.</h2></div></div><div class="outfit-list">${curated.map((o) => card(o, false)).join("")}</div></section>`}`;
  app.addEventListener(
    "change",
    (e) => {
      if (e.target.dataset.slotSelect) {
        read();
        const i = selection(e.target.dataset.slotSelect);
        if (i)
          i.variantId =
            product(i)?.variants.find((v) => v.stock > 0)?.id || null;
        draw(e.target.id);
      } else if (e.target.dataset.variantSelect) {
        read();
        draw(e.target.id);
      }
    },
    { signal },
  );
  app.addEventListener(
    "click",
    (e) => {
      const b = e.target.closest("button");
      if (!b) return;
      if (b.dataset.moveSlot) {
        read();
        const n = order.indexOf(b.dataset.moveSlot),
          m = n + Number(b.dataset.direction);
        [order[n], order[m]] = [order[m], order[n]];
        draw("product-" + b.dataset.moveSlot);
      } else if (b.id === "new-outfit") {
        fresh();
        draw();
      } else if (b.dataset.loadOutfit)
        busy(b, async () => {
          const o =
            b.dataset.own === "true"
              ? saved.find((o) => o.id === Number(b.dataset.loadOutfit))
              : await api("/api/studio/outfits/" + b.dataset.loadOutfit);
          load(o, b.dataset.own === "true");
          draw();
          document
            .querySelector(".studio-workspace")
            .scrollIntoView({ behavior: "smooth" });
        });
      else if (b.dataset.deleteOutfit)
        busy(b, async () => {
          await api(
            (adminMode() ? "/api/admin/outfits/" : "/api/me/outfits/") +
              b.dataset.deleteOutfit,
            { method: "DELETE" },
          );
          await refresh();
          if (draft.id === Number(b.dataset.deleteOutfit)) fresh();
          draw();
          toast("Đã xóa bộ phối.");
        });
      else if (b.dataset.shareOutfit)
        busy(b, async () => {
          await navigator.clipboard.writeText(
            location.origin + "/studio?outfit=" + b.dataset.shareOutfit,
          );
          toast("Đã sao chép liên kết outfit công khai.");
        });
      else if (b.id === "outfit-add")
        busy(b, async () => {
          read();
          if (!draft.items.length || draft.items.some((i) => !i.variantId))
            throw new Error("Chọn sản phẩm và phân loại cho từng món.");
          await send("/api/cart/outfit", {
            items: draft.items.map((i) => ({
              variantId: i.variantId,
              quantity: 1,
            })),
          });
          await updateCount();
          toast("Đã thêm cả bộ vào giỏ hàng.");
        });
      else if (b.id === "publish-outfit")
        busy(b, async () => {
          await send("/api/admin/outfits/" + draft.id + "/publish", {});
          await refresh();
          draw();
          toast("Đã xuất bản outfit mẫu.");
        });
    },
    { signal },
  );
  app.addEventListener(
    "submit",
    (e) => {
      if (e.target.id !== "outfit-form") return;
      e.preventDefault();
      busy(e.submitter, async () => {
        if (!state.me) {
          location.href = "/account?next=/studio";
          return;
        }
        read();
        if (!draft.items.length) throw new Error("Chọn ít nhất một món đồ.");
        const root = adminMode() ? "/api/admin/outfits" : "/api/me/outfits";
        const r = await send(
          root + (draft.id ? "/" + draft.id : ""),
          {
            name: draft.name,
            version: draft.version,
            items: draft.items.map(({ slot, productId, variantId }) => ({
              slot,
              productId,
              variantId,
            })),
          },
          draft.id ? "PUT" : "POST",
        );
        load(r, true);
        await refresh();
        draw();
        toast("Đã lưu bộ phối. Bạn có thể mở lại ở đây.");
      });
    },
    { signal },
  );
  if (focus) document.getElementById(focus)?.focus();
}
export async function studio() {
  title("Outfit Studio");
  fresh();
  catalog = (await api("/api/catalog?size=60")).items;
  curated = (await api("/api/studio/outfits")).items;
  await refresh();
  const id = new URLSearchParams(location.search).get("outfit");
  if (id) {
    const o = await api("/api/studio/outfits/" + encodeURIComponent(id));
    load(o, o.visibility !== "PUBLIC");
  }
  draw();
}
