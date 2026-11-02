# Runway Studio 2.0

**Discover, Style, Save & Shop** — một ứng dụng thương mại thời trang với Outfit Studio và Shop The Scene. Nâng cấp tăng dần từ project Java trong `RunwayStore_Fullstack_Source(1).zip`; giữ package `vn.runway.store`, session/CSRF và các API mua hàng cũ.

![Runway Studio](docs/screenshots/home-desktop-1440.png)

## Chạy nhanh

Cần **JDK 17**. Maven Wrapper tải Maven tự động trong lần đầu; không cần Node để chạy website.

Unix/macOS:

```sh
./mvnw clean verify
java -jar target/runway-store-2.0.0.jar
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
java -jar target\runway-store-2.0.0.jar
```

Mở **http://localhost:8080**. Có thể chạy `RUN_DEMO.sh` hoặc `RUN_DEMO.bat`. Với JAR tải riêng: `java -jar runway-store-2.0.0.jar`.

Profile mặc định `demo` dùng H2 lưu tại `data/runway`, chỉ lắng nghe loopback. Tài khoản **chỉ dành cho local demo**:

| Quyền | Email | Mật khẩu local |
|---|---|---|
| Khách | demo@runway.local | Demo@12345 |
| Admin | admin@runway.local | Admin@12345 |

Không dùng profile `demo` trên host public. Profile `mysql,prod` tắt seed, bắt buộc cấu hình DB và cookie HTTPS; không có mật khẩu admin mặc định. Đọc [hướng dẫn triển khai](docs/DEPLOYMENT.md) để tạo admin lần đầu bằng thông tin riêng.

## Chức năng hiện có

| Nhóm | Hành vi |
|---|---|
| Cửa hàng | Tìm kiếm, lọc danh mục/giá/tồn, phân trang và sắp xếp tại DB; truy vấn variant theo batch |
| Giỏ hàng | Giỏ khách trong session; giỏ đăng nhập lưu DB, ghép guest một lần; thêm nhiều dòng nguyên tử |
| Checkout | COD, snapshot giá, khóa tồn kho, idempotency theo tài khoản; retry đơn cũ giữ giỏ mới |
| Đơn hàng | Lịch sử trạng thái; chuyển trạng thái hợp lệ; hủy và hoàn tồn đúng một lần |
| Outfit Studio | Áo/quần/giày/phụ kiện, chọn size, đổi thứ tự; lưu/tải outfit riêng tư; admin curated/public; add-all |
| Shop The Scene | Ảnh minh họa gắn hotspot theo %, xem giá/tồn thật; admin kéo hoặc dùng phím/nhập tọa độ, draft/publish |
| My Wardrobe | Lưu sản phẩm có hoặc không chọn variant; không trùng; cách ly dữ liệu theo tài khoản |
| Fit Finder | Rule theo vòng ngực, vòng eo hoặc chiều dài bàn chân; giải thích, biên rõ; không lưu số đo |
| Commerce | Sổ địa chỉ; voucher hạn dùng/ngưỡng/cap/lượt dùng; quote không tiêu thụ ưu đãi |
| Admin | CRUD sản phẩm/variant; audit thay đổi; ledger tồn; doanh thu chỉ đơn đã giao; báo cáo ngày UTC qua API |

Seed mới có 11 sản phẩm, 50 variant, 1 outfit curated, 3 scenes và 4 bảng size. Giữ ID và slug của 9 sản phẩm cũ để tương thích; thêm quần và túi để phối đủ bộ. Tất cả sản phẩm, giá và bảng size là dữ liệu minh họa.

## Kiểm thử

Minh chứng mới ngày **02/11/2026** nằm trong [VALIDATION](docs/VALIDATION.md). Phân biệt kết quả chạy thực tế và cấu hình chưa chạy trên GitHub/Docker.

```sh
./mvnw clean verify
npm ci
npm run check
npx playwright install chromium
npm run test:e2e
```

Playwright mở ứng dụng Java thật bằng H2 tạm tại cổng 4187; chạy desktop 1440 và mobile 390/360, không truy cập database người dùng. Các mật khẩu trong test là fixture dùng cho tiến trình test riêng.

MySQL acceptance chạy trong schema rỗng dùng riêng cho test:

```sh
# Export DB_URL, LEGACY_DB_URL, DB_USERNAME, DB_PASSWORD tới hai schema tạm riêng biệt.
./mvnw -Pmysql-verify verify
```

Xem [TEST_CASES](docs/TEST_CASES.md) và [DEPLOYMENT](docs/DEPLOYMENT.md) trước khi chạy migration trên dữ liệu cũ.

## Kiến trúc và quyết định

Java 17 · Spring Boot 3.5.16 · Spring Security · JPA · Flyway · MySQL 8.4/H2 · HTML/CSS/ES modules. Modular monolith, giữ session+CSRF cùng domain, không thêm JWT hoặc microservices cho dữ liệu demo.

- [Architecture](docs/ARCHITECTURE.md) · [ERD](docs/ERD.md) · [API](docs/API.md)
- [Security](docs/SECURITY.md) · [Baseline](docs/BASELINE.md) · [Roadmap](docs/ROADMAP.md)
- [Nguồn và tài nguyên](docs/ATTRIBUTION.md) · [Third-party notices](THIRD_PARTY_NOTICES.md)
- [Kịch bản demo 3–5 phút](docs/demo-script.md)

## Phạm vi chưa hoàn tất

Chưa tích hợp thanh toán sandbox, email, quy trình trả hàng, upload ảnh người dùng, tài khoản nhân viên nhiều cấp hoặc ML/AR. Fit Finder dùng bảng minh họa. UI Studio hiện tải tối đa 60 sản phẩm; API catalog có phân trang nhưng bộ chọn Studio cho catalog lớn còn là backlog. Admin UI dùng danh sách tối đa 60 sản phẩm/đơn; API phân trang riêng đã có. Chưa đo Lighthouse/coverage, chưa chạy Docker tại môi trường bàn giao, chưa công bố GitHub Actions đã xanh và chưa triển khai Java lên host public.

Quét dependency còn hiển thị hai advisory trên Spring MVC 6.2.19; đánh giá điều kiện không áp dụng cho ứng dụng được lưu cùng test và ngày hết hạn, **không xóa cảnh báo khỏi báo cáo**. Spring Boot 3.5 đã hết OSS support; kế hoạch nâng major cần một thay đổi riêng sau release core, theo ràng buộc giữ 3.5.x của roadmap.

Bản ZIP sạch không chứa 140 ảnh và ba trang upstream chưa rõ quyền, cũng không chứa lịch sử Git gốc có những tài nguyên đó. Giữ ZIP gốc riêng. Khi đưa bản sạch lên GitHub, khởi tạo repository từ nội dung ZIP sạch; không push branch baseline riêng tư có tài nguyên chưa được duyệt. Không tự gán LICENSE cho thành phần upstream.
