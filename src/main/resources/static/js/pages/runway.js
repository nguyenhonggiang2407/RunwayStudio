import { state } from "../core/state.js";
import { api } from "../core/api.js";
import { app, cards, esc, money, title } from "../core/ui.js";
import { editorial, everydayEdit } from "./editorial.js";

export async function runwayHome() {
  const looks = await api("/runway-looks.json");
  title("Discover, Style, Save & Shop");
  app.innerHTML = editorial() + everydayEdit() + `<section class="runway-showroom" aria-label="Bộ sưu tập Runway"><div class="runway-main"><div class="runway-intro"><div><span class="eyebrow">RUNWAY / THE ORIGINAL EDIT</span><h2>YOUR STYLE.<br>YOUR STUDIO.</h2></div><p>Sáu góc nhìn. Một phong cách riêng.<br>Chọn bộ phối để khám phá từng món.</p></div><div class="runway-models" id="runway-models">${looks.map((look, i) => `<button class="runway-person" data-look="${i}" aria-label="Chọn bộ phối ${i + 1}: ${esc(look.name)}" aria-pressed="false"><img src="${esc(i === 0 ? look.image : look.preview)}" data-full-image="${esc(look.image)}" alt="${esc(look.name)} — ảnh người mẫu từ bộ Runway gốc" width="1100" height="1920" ${i === 0 ? 'fetchpriority="high"' : 'loading="lazy"'}></button>`).join("")}</div><div class="runway-controls"><button class="look-arrow" data-look-step="-1" aria-label="Bộ phối trước">←</button><div class="look-pagination" aria-label="Chọn bộ phối">${looks.map((look, i) => `<button data-look="${i}" aria-label="Bộ phối ${i + 1}: ${esc(look.name)}" aria-pressed="false"><span>${String(i + 1).padStart(2, "0")}</span><b>${esc(look.name)}</b></button>`).join("")}</div><button class="look-arrow" data-look-step="1" aria-label="Bộ phối tiếp theo">→</button></div></div><aside class="look-rail" aria-labelledby="look-rail-heading"><div class="look-rail-heading"><h2 id="look-rail-heading">Trong bộ phối</h2><span>03 GỢI Ý</span></div><div id="look-products" class="look-products"></div><div class="look-summary"><p id="look-caption" aria-live="polite"></p><div><span>Tổng giá từ</span><strong id="look-total"></strong></div><small>Chọn kích cỡ và xem giá hiện tại ở từng sản phẩm.</small><a class="btn" href="/studio">Tự phối trong Studio <span>→</span></a><a class="text-link" href="/shop">Khám phá cửa hàng</a></div></aside></section><section class="section photo-collection"><div class="section-head"><div><span class="eyebrow">THE COLLECTION</span><h2>Những món đồ nổi bật.</h2></div><a class="text-link" href="/shop">Xem bộ sưu tập ↗</a></div><div class="products">${cards(state.products.filter((p) => p.image.startsWith("/media/original/")).slice(0, 8))}</div></section><section class="explore-strip" aria-label="Khám phá Runway"><a href="/studio"><span>01 / OUTFIT STUDIO</span><h2>Phối theo cách của bạn.</h2><b>Chọn áo, quần, giày và phụ kiện ↗</b></a><a href="/scenes"><span>02 / SHOP THE SCENE</span><h2>Nhìn gần từng chi tiết.</h2><b>Khám phá sản phẩm trong khung hình ↗</b></a><a href="/wardrobe"><span>03 / MY WARDROBE</span><h2>Giữ lại món đồ bạn thích.</h2><b>Tạo bộ sưu tập riêng của bạn ↗</b></a></section>`;
  let selected = 0;
  function select(index) {
    selected = (index + looks.length) % looks.length;
    const look = looks[selected];
    for (const button of app.querySelectorAll("[data-look]")) {
      const active = Number(button.dataset.look) === selected;
      button.setAttribute("aria-pressed", String(active));
      if (button.classList.contains("runway-person")) {
        button.dataset.position =
          (Number(button.dataset.look) - selected + looks.length) %
          looks.length;
        if (active) {
          const image = button.querySelector("img");
          image.src = image.dataset.fullImage;
        }
      }
    }
    const products = look.products
      .map((slug) => state.products.find((p) => p.slug === slug))
      .filter(Boolean);
    app.querySelector("#look-products").innerHTML = products
      .map(
        (p, i) =>
          `<a class="look-product" href="/product/${encodeURIComponent(p.slug)}"><div class="look-product-image"><img src="${esc(p.image)}" alt="${esc(p.name)}" width="160" height="160"><span>${i + 1}</span></div><div><span class="look-category">${esc(p.category)}</span><h3>${esc(p.name)}</h3><p>${money(p.price)}</p></div><span class="look-product-arrow" aria-hidden="true">↗</span></a>`,
      )
      .join("");
    app.querySelector("#look-caption").textContent =
      `${String(selected + 1).padStart(2, "0")} / ${look.name}`;
    app.querySelector("#look-total").textContent = money(
      products.reduce((sum, p) => sum + p.price, 0),
    );
  }
  app.querySelector(".runway-showroom").addEventListener("click", (event) => {
    const button = event.target.closest(
      "button[data-look],button[data-look-step]",
    );
    if (!button) return;
    select(
      button.dataset.look !== undefined
        ? Number(button.dataset.look)
        : selected + Number(button.dataset.lookStep),
    );
  });
  select(0);
}
