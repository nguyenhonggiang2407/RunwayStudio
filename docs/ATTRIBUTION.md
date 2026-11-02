# Provenance và media

| Thành phần | Nguồn / xử lý trong 2.0 |
|---|---|
| Java storefront và backend nền | ZIP người dùng cung cấp; được giữ và sửa tăng dần |
| Ý tưởng RunwaySection, ShoeOrbit, SceneRail | IdeaToCodee/runway tại commit e0219d0082e31af42438ad787a73b988d5e2fa16; không thấy LICENSE ở nguồn đã kiểm tra |
| 140 ảnh và 3 trang trải nghiệm upstream | Không nằm trong source release sạch hoặc static classpath 2.0; giữ ở ZIP baseline riêng tư |
| SVG trong static/media/owned | Hình vector mới tạo cho bản nâng cấp này, không nhúng ảnh/font/script từ ngoài |
| UI Studio/Scenes và adapter fetch | ES module mới; không phân phối implementation upstream hoặc override fetch toàn cục |
| Spring, Maven, MySQL connector, H2, Flyway, Playwright | Dependency chính thức; giấy phép/thông báo nằm trong từng package |

Giữ attribution lịch sử không có nghĩa được phép cấp phép lại upstream. Không có LICENSE bao trùm mã chưa được xác minh. Không nhận các phần upstream là tự viết. Các slug cũ được giữ làm route tương thích, không phải tuyên bố liên kết thương hiệu.

Hình và thông tin demo chỉ để trình diễn kỹ thuật, không là hàng hóa/chính sách bán hàng thật. Không reimport ảnh từ upstream vào public release nếu chưa có quyền rõ ràng.
