export function editorial() {
  return `
    <section class="editorial-feature" aria-labelledby="editorial-title">
      <div class="editorial-copy">
        <h1 id="editorial-title">Phối đồ cho ngày thường.</h1>
        <p>Chọn một món dễ mặc rồi thử phối cho buổi học hoặc cuối tuần.</p>
        <div class="editorial-actions">
          <a class="btn" href="/shop">Xem cửa hàng <span aria-hidden="true">↗</span></a>
          <a class="text-link" href="/studio">Tạo bộ phối <span aria-hidden="true">↗</span></a>
        </div>
      </div>
      <figure class="editorial-image">
        <img src="/media/licensed/city-editorial-1200.webp"
          srcset="/media/licensed/city-editorial-600.webp 600w, /media/licensed/city-editorial-1200.webp 1200w"
          sizes="(max-width: 760px) 100vw, 50vw" width="1200" height="1800" fetchpriority="high" decoding="async"
          alt="Người mẫu mặc áo thun nâu và quần tối màu, ngồi bên bức tường vẽ tại Olinda">
        <figcaption>
          <span class="editorial-disclosure">Ảnh cảm hứng, không phải sản phẩm của cửa hàng.</span>
          <a href="https://www.pexels.com/photo/man-in-brown-crew-neck-t-shirt-and-black-denim-jeans-3116018/" target="_blank" rel="noopener noreferrer">Jonnas Duarte / Pexels</a>
        </figcaption>
      </figure>
    </section>`;
}

export function everydayEdit() {
  return `
    <section class="everyday-edit" aria-labelledby="everyday-title">
      <div class="everyday-heading">
        <h2 id="everyday-title">Mặc gì, mang gì?</h2>
        <p>Hai gợi ý cho tủ đồ hằng ngày. Ảnh chụp dùng để tham khảo cách phối.</p>
      </div>
      <div class="everyday-grid">
        <article class="everyday-card">
          <figure>
            <img src="/media/licensed/wardrobe-rail-960.webp"
              srcset="/media/licensed/wardrobe-rail-480.webp 480w, /media/licensed/wardrobe-rail-960.webp 960w"
              sizes="(max-width: 760px) 90vw, (max-width: 1320px) 44vw, 560px"
              width="960" height="1440" loading="lazy" decoding="async"
              alt="Giá treo áo khoác và trang phục tông kem, be bên cửa sổ">
            <figcaption>Ảnh: <a href="https://www.pexels.com/photo/close-up-of-a-clothing-rack-8989864/" target="_blank" rel="noopener noreferrer">Alena Darmel / Pexels</a></figcaption>
          </figure>
          <div class="everyday-card-copy">
            <h3>Một lớp khoác nhẹ</h3>
            <p>Thử áo thun trơn với một lớp khoác tông trung tính để dễ đổi bộ phối khi ra ngoài.</p>
            <a class="text-link" href="/shop?category=%C3%81o+kho%C3%A1c">Xem áo khoác <span aria-hidden="true">↗</span></a>
          </div>
        </article>
        <article class="everyday-card">
          <figure>
            <img src="/media/licensed/everyday-tote-960.webp"
              srcset="/media/licensed/everyday-tote-480.webp 480w, /media/licensed/everyday-tote-960.webp 960w"
              sizes="(max-width: 760px) 90vw, (max-width: 1320px) 44vw, 560px"
              width="960" height="1440" loading="lazy" decoding="async"
              alt="Túi tote vải màu nâu nhạt đặt trên bàn trắng cạnh chậu cây">
            <figcaption>Ảnh: <a href="https://www.pexels.com/photo/brown-tote-bag-on-white-table-8148587/" target="_blank" rel="noopener noreferrer">Vie Studio / Pexels</a></figcaption>
          </figure>
          <div class="everyday-card-copy">
            <h3>Một chiếc túi cho ngày dài</h3>
            <p>Nếu thường mang sổ, bình nước hoặc áo mỏng, hãy chọn túi theo những món bạn thực sự cần mang.</p>
            <a class="text-link" href="/shop?category=Ph%E1%BB%A5+ki%E1%BB%87n">Xem phụ kiện <span aria-hidden="true">↗</span></a>
          </div>
        </article>
      </div>
    </section>`;
}
