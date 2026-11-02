import { api, send } from "../core/api.js";
import { app, esc, title, head, toast, busy } from "../core/ui.js";
let list = [],
  products = [],
  media = [],
  draft,
  bindings;
function fresh() {
  draft = {
    id: null,
    version: null,
    title: "New Studio Scene",
    slug: "new-scene",
    coverImage: media[0],
    altText: "Một bộ phối thời trang minh họa.",
    hotspots: [],
  };
}
function read() {
  const f = document.querySelector("#scene-editor");
  for (const k of ["title", "slug", "coverImage", "altText"])
    draft[k] = f.elements.namedItem(k).value;
  draft.hotspots.forEach((h, n) => {
    h.productId = Number(f.elements.namedItem("product-" + n).value);
    h.xPercent = Number(f.elements.namedItem("x-" + n).value);
    h.yPercent = Number(f.elements.namedItem("y-" + n).value);
    h.label = f.elements.namedItem("label-" + n).value;
  });
}
function place() {
  draft.hotspots.forEach((h, n) => {
    const b = document.querySelector(`[data-edit-hotspot="${n}"]`);
    if (b) {
      b.style.left = h.xPercent + "%";
      b.style.top = h.yPercent + "%";
    }
    for (const [a, v] of [
      ["x", h.xPercent],
      ["y", h.yPercent],
    ]) {
      const f = document.querySelector(`[name="${a}-${n}"]`);
      if (f) f.value = v.toFixed(1);
    }
  });
}
function draw() {
  bindings?.abort();
  bindings = new AbortController();
  const signal = bindings.signal;
  app.innerHTML =
    head(
      "STUDIO BACK OFFICE",
      "Scenes & hotspots.",
      "Chọn hình minh họa đã duyệt, gắn sản phẩm, kiểm tra rồi xuất bản.",
    ) +
    `<section class="content-wrap"><div class="editor-toolbar"><a class="text-link" href="/admin">← Quản trị cửa hàng</a><label>Mở scene<select id="scene-select"><option value="">Scene mới</option>${list.map((s) => `<option value="${s.id}" ${s.id === draft.id ? "selected" : ""}>${esc(s.title)} · ${esc(s.status)}</option>`).join("")}</select></label><button class="btn light" id="new-scene">Tạo mới</button></div><form id="scene-editor"><div class="scene-editor-layout"><div><div class="scene-canvas editor-canvas" id="editor-canvas"><img src="${esc(draft.coverImage)}" alt="${esc(draft.altText)}" width="1200" height="1000">${draft.hotspots.map((h, n) => `<button type="button" class="hotspot" data-edit-hotspot="${n}" aria-label="Di chuyển hotspot ${n + 1}, dùng phím mũi tên">${n + 1}</button>`).join("")}</div><p class="small-text muted">Kéo điểm đánh dấu, dùng phím mũi tên hoặc nhập X/Y. Tọa độ theo %. Shift + mũi tên di chuyển 10%.</p><div class="hotspot-edit-list">${draft.hotspots.map((h, n) => `<fieldset><legend>Hotspot ${n + 1}</legend><label>Sản phẩm<select name="product-${n}">${products.map((p) => `<option value="${p.id}" ${h.productId === p.id ? "selected" : ""}>${esc(p.name)}</option>`).join("")}</select></label><div class="field-grid"><label>X (%)<input name="x-${n}" type="number" min="0" max="100" step="0.1" required value="${h.xPercent}"></label><label>Y (%)<input name="y-${n}" type="number" min="0" max="100" step="0.1" required value="${h.yPercent}"></label></div><label>Nhãn<input name="label-${n}" required maxlength="80" value="${esc(h.label)}"></label><button type="button" class="remove" data-remove-hotspot="${n}">Xóa hotspot</button></fieldset>`).join("")}</div><button type="button" class="btn light" id="add-hotspot" ${draft.hotspots.length >= 20 ? "disabled" : ""}>+ Thêm hotspot</button></div><aside class="panel form-stack"><span class="eyebrow">${esc(draft.status || "DRAFT")}</span><label>Tiêu đề<input name="title" required maxlength="100" value="${esc(draft.title)}"></label><label>Slug<input name="slug" required maxlength="100" pattern="[a-z0-9]+(-[a-z0-9]+)*" value="${esc(draft.slug)}"></label><label>Hình minh họa<select name="coverImage">${media.map((m) => `<option value="${m}" ${draft.coverImage === m ? "selected" : ""}>${esc(m.split("/").pop())}</option>`).join("")}</select></label><label>Mô tả hình cho người đọc màn hình<textarea name="altText" required maxlength="250">${esc(draft.altText)}</textarea></label><button class="btn" type="submit">Lưu scene</button>${draft.id ? `<button type="button" class="btn light" id="publish-scene">${draft.status === "PUBLISHED" ? "Chuyển về bản nháp" : "Xuất bản scene"}</button><p class="small-text">${draft.status === "PUBLISHED" ? `<a href="/scenes/${encodeURIComponent(draft.slug)}">Mở scene công khai ↗</a>` : "Scene nháp chỉ dành cho quản trị viên."}</p>` : ""}</aside></div></form></section>`;
  place();
  app.addEventListener(
    "change",
    (e) => {
      if (e.target.id === "scene-select")
        busy(null, async () => {
          if (e.target.value)
            draft = await api("/api/admin/scenes/" + e.target.value);
          else fresh();
          draw();
        });
      else if (e.target.closest("#scene-editor")) {
        read();
        if (e.target.name === "coverImage")
          document.querySelector("#editor-canvas img").src = draft.coverImage;
        place();
      }
    },
    { signal },
  );
  app.addEventListener(
    "click",
    (e) => {
      const b = e.target.closest("button");
      if (!b) return;
      if (b.id === "new-scene") {
        fresh();
        draw();
      } else if (b.id === "add-hotspot") {
        read();
        const p = products[0];
        draft.hotspots.push({
          productId: p.id,
          label: p.name,
          xPercent: 50,
          yPercent: 50,
        });
        draw();
      } else if (b.dataset.removeHotspot !== undefined) {
        read();
        draft.hotspots.splice(Number(b.dataset.removeHotspot), 1);
        draw();
      } else if (b.id === "publish-scene")
        busy(b, async () => {
          draft = await send(
            "/api/admin/scenes/" +
              draft.id +
              "/" +
              (draft.status === "PUBLISHED" ? "unpublish" : "publish"),
            {},
          );
          list = (await api("/api/admin/scenes?size=60")).items;
          draw();
          toast("Đã cập nhật trạng thái scene.");
        });
    },
    { signal },
  );
  app.addEventListener(
    "submit",
    (e) => {
      if (e.target.id !== "scene-editor") return;
      e.preventDefault();
      busy(e.submitter, async () => {
        read();
        draft = await send(
          "/api/admin/scenes" + (draft.id ? "/" + draft.id : ""),
          {
            title: draft.title,
            slug: draft.slug,
            coverImage: draft.coverImage,
            altText: draft.altText,
            version: draft.version,
            hotspots: draft.hotspots.map(
              ({ productId, xPercent, yPercent, label }) => ({
                productId,
                xPercent,
                yPercent,
                label,
              }),
            ),
          },
          draft.id ? "PUT" : "POST",
        );
        list = (await api("/api/admin/scenes?size=60")).items;
        draw();
        toast("Đã lưu scene.");
      });
    },
    { signal },
  );
  const canvas = document.querySelector("#editor-canvas");
  let dragging = null;
  canvas.addEventListener(
    "pointerdown",
    (e) => {
      const b = e.target.closest("[data-edit-hotspot]");
      if (b) {
        read();
        dragging = Number(b.dataset.editHotspot);
        b.setPointerCapture(e.pointerId);
      }
    },
    { signal },
  );
  canvas.addEventListener(
    "pointermove",
    (e) => {
      if (dragging === null) return;
      const r = canvas.getBoundingClientRect(),
        h = draft.hotspots[dragging];
      h.xPercent = Math.max(
        0,
        Math.min(100, ((e.clientX - r.left) / r.width) * 100),
      );
      h.yPercent = Math.max(
        0,
        Math.min(100, ((e.clientY - r.top) / r.height) * 100),
      );
      place();
    },
    { signal },
  );
  for (const event of ["pointerup", "pointercancel"])
    canvas.addEventListener(event, () => (dragging = null), { signal });
  canvas.addEventListener(
    "keydown",
    (e) => {
      const b = e.target.closest("[data-edit-hotspot]");
      if (
        !b ||
        !["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(e.key)
      )
        return;
      e.preventDefault();
      read();
      const h = draft.hotspots[Number(b.dataset.editHotspot)],
        delta = e.shiftKey ? 10 : 1;
      if (e.key === "ArrowLeft") h.xPercent -= delta;
      if (e.key === "ArrowRight") h.xPercent += delta;
      if (e.key === "ArrowUp") h.yPercent -= delta;
      if (e.key === "ArrowDown") h.yPercent += delta;
      h.xPercent = Math.max(0, Math.min(100, h.xPercent));
      h.yPercent = Math.max(0, Math.min(100, h.yPercent));
      place();
    },
    { signal },
  );
}
export async function adminStudio() {
  title("Scene editor");
  [list, products, media] = await Promise.all([
    api("/api/admin/scenes?size=60").then((r) => r.items),
    api("/api/admin/products"),
    api("/api/admin/media"),
  ]);
  fresh();
  draw();
}
