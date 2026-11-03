# Provenance và media

| Thành phần | Nguồn / xử lý trong 2.0 |
|---|---|
| Java storefront và backend nền | ZIP người dùng cung cấp; được giữ và sửa tăng dần |
| Ý tưởng RunwaySection, ShoeOrbit, SceneRail | IdeaToCodee/runway tại commit e0219d0082e31af42438ad787a73b988d5e2fa16; không thấy LICENSE ở nguồn đã kiểm tra |
| 140 ảnh upstream | Đã khôi phục nguyên byte vào `src/main/resources/static/media/original/` từ nguồn khớp inventory ZIP gốc; tổng 55.000.817 byte, đối chiếu đủ 140 SHA-256 |
| RunwaySection, ShoeOrbit, SceneRail và HTML/font/script upstream | Không phân phối lại implementation, HTML, font, tracker hoặc payment script; chỉ khôi phục các file ảnh |
| Manifest ảnh gốc | `src/main/resources/original-photo-manifest.json` ghi đường dẫn, SHA-256, URL nguồn và repository/commit upstream cho từng ảnh |
| SVG trong static/media/owned | Hình vector mới tạo cho bản nâng cấp; Straight Trousers và Weekend Tote là hai sản phẩm bổ sung không có ảnh gốc, tiếp tục dùng SVG minh họa |
| UI Studio/Scenes và adapter fetch | ES module mới; không phân phối implementation upstream hoặc override fetch toàn cục |
| City Notes editorial | Ảnh chụp thật của Jonnas Duarte, Pexels 3116018, được cấp phép theo Pexels License; 2 bản WebP lưu local. Xem [MEDIA_LICENSES](MEDIA_LICENSES.md). Không ánh xạ trang phục trong ảnh thành sản phẩm catalog. |
| Spring, Maven, MySQL connector, H2, Flyway, Playwright | Dependency chính thức; giấy phép/thông báo nằm trong từng package |

Giao diện dùng lại sáu ảnh người mẫu và ảnh của chín sản phẩm gốc; các kích thước ảnh còn lại được giữ để bảo toàn bộ media nguồn. Tên, giá, tồn kho và liên kết phối đồ của demo không xác nhận sản phẩm thật hoặc quan hệ thương mại với nguồn ảnh.

Giữ attribution và khớp checksum chỉ xác nhận nguồn gốc file, không chứng minh quyền sở hữu hay giấy phép sử dụng ảnh. Không thấy LICENSE bao trùm tại commit upstream đã kiểm tra; việc không có LICENSE không cấp quyền cấp phép lại. Dự án không nhận ảnh hoặc implementation upstream là tự tạo và không gán LICENSE mới cho chúng. Các slug cũ được giữ làm route tương thích, không phải tuyên bố liên kết thương hiệu.

Hình và thông tin demo chỉ để trình diễn kỹ thuật, không là hàng hóa/chính sách bán hàng thật. Xem [Third-party notices](../THIRD_PARTY_NOTICES.md) và [baseline](BASELINE.md) để phân biệt lịch sử release sạch với trạng thái media hiện tại.
