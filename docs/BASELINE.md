# Baseline nguồn

Ngày đối chiếu: 02/11/2026. Nguồn `RunwayStore_Fullstack_Source(1).zip`, SHA-256 `e5fb095f716d9aea0eed47115616c3c279d40463a6a583d10bfc7af1eb359b41`.

Import nguyên trạng vào commit `a27fa836d6cb9223ef49bfc1e3e3d1bd613d1d7d`, tag `v1-baseline`, branch `baseline/runway-store-v1`. Công việc nâng cấp ở `upgrade/runway-studio-2`. Baseline được chạy lại: 16 test pass, 0 lỗi, 0 skipped; log `evidence/baseline-maven-test.txt`.

Trong phiên làm việc có khởi tạo lại môi trường. Bản nâng cấp được khôi phục từ thay đổi đã ghi lại và kiểm thử lại; kết quả trong VALIDATION là kết quả của gói khôi phục, không lấy số cũ làm bằng chứng thay thế.

Giữ ID/slug nguồn, route sản phẩm, cấu trúc cart/order cũ, session/CSRF, COD, snapshot giá, khóa variant, quy tắc chuyển trạng thái. Tách controller/service; thêm các bảng phiên bản mới bằng Flyway. Không đổi toàn bộ package hoặc viết lại nền tảng.

Bản baseline riêng tư và ZIP gốc giữ tài nguyên tham khảo chưa rõ quyền. Bản phát hành sạch loại các tài nguyên đó cùng script reimport, dùng SVG mới. Không công khai lịch sử Git chứa các blob chưa được duyệt.

Danh sách file nguồn và checksum: `baseline-inventory.json`.
