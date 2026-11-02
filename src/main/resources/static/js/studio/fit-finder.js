import { api, send } from "../core/api.js";
import { app, esc, title, head, busy } from "../core/ui.js";
export async function fitFinder() {
  title("Fit Finder");
  const guides = await api("/api/size-guides");
  app.innerHTML =
    head(
      "A LITTLE GUIDANCE",
      "Tìm size. Bớt đắn đo.",
      "Đối chiếu bảng size minh họa. Bạn có thể mua hàng mà không cần cung cấp số đo.",
    ) +
    `<section class="fit-layout content-wrap"><form id="fit-form" class="panel form-stack"><span class="eyebrow">YOUR MEASUREMENTS / CM</span><label for="fit-category">Loại sản phẩm</label><select name="category" id="fit-category">${guides.map((g) => `<option>${esc(g.category)}</option>`).join("")}</select><label id="fit-chest">Vòng ngực (cm)<input name="chest" type="number" min="50" max="180" step="0.1" placeholder="Ví dụ: 94"></label><label id="fit-waist">Vòng eo (cm)<input name="waist" type="number" min="45" max="180" step="0.1" placeholder="Ví dụ: 78"></label><label id="fit-foot">Chiều dài bàn chân (cm)<input name="footLength" type="number" min="15" max="35" step="0.1" placeholder="Ví dụ: 25.5"></label><p class="small-text muted">Các trường số đo đều tùy chọn. Số đo chỉ dùng cho lần đối chiếu này.</p><button class="btn">Đối chiếu bảng size ↗</button></form><div><div id="fit-result" class="fit-result" role="status" aria-live="polite"><span class="eyebrow">FIND YOUR STARTING POINT</span><h2>Một gợi ý để bắt đầu.</h2><p>Giày dùng chiều dài bàn chân; áo dùng vòng ngực; quần dùng vòng eo.</p></div><details class="panel"><summary>Xem bảng size minh họa</summary><div id="fit-table"></div></details><p class="small-text muted">Dữ liệu minh họa, không bảo đảm vừa tuyệt đối. Đối chiếu bảng size thực tế. Số đo không được lưu.</p></div></section>`;
  const form = document.querySelector("#fit-form");
  function category() {
    const c = form.elements.category.value;
    for (const [id, on] of [
      ["fit-chest", !["Giày", "Quần"].includes(c)],
      ["fit-waist", c === "Quần"],
      ["fit-foot", c === "Giày"],
    ]) {
      const label = document.getElementById(id);
      label.hidden = !on;
      label.querySelector("input").disabled = !on;
    }
    const g = guides.find((g) => g.category === c);
    document.querySelector("#fit-table").innerHTML = g
      ? `<table><caption>${esc(c)} · v${g.version} · cm, đầu khoảng gồm và cuối khoảng không gồm</caption><thead><tr><th>Size</th><th>Từ</th><th>Đến</th></tr></thead><tbody>${g.rows.map((r) => `<tr><td>${esc(r.size)}</td><td>${r.minChest ?? r.minWaist ?? r.minFoot}</td><td>${r.maxChest ?? r.maxWaist ?? r.maxFoot}</td></tr>`).join("")}</tbody></table>`
      : "Chưa có bảng size.";
  }
  form.elements.category.onchange = category;
  category();
  form.onsubmit = (e) => {
    e.preventDefault();
    busy(e.submitter, async () => {
      const b = Object.fromEntries(new FormData(form));
      for (const k of Object.keys(b))
        if (k !== "category") {
          if (b[k] === "") delete b[k];
          else b[k] = Number(b[k]);
        }
      const r = await send("/api/size-recommendations", b);
      document.querySelector("#fit-result").innerHTML =
        `<span class="eyebrow">${r.size ? "SIZE THAM KHẢO" : "CẦN THÊM ĐỐI CHIẾU"}</span><h2>${esc(r.size || "Chưa đủ dữ liệu")}</h2><p>${esc(r.reason)}</p><small>${esc(r.disclaimer)}</small>`;
    });
  };
}
