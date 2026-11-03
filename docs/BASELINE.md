# Baseline nguồn

Ngày đối chiếu: 02/11/2026. Nguồn `RunwayStore_Fullstack_Source(1).zip`, SHA-256 `e5fb095f716d9aea0eed47115616c3c279d40463a6a583d10bfc7af1eb359b41`.

Import nguyên trạng vào commit `a27fa836d6cb9223ef49bfc1e3e3d1bd613d1d7d`, tag `v1-baseline`, branch `baseline/runway-store-v1`. Công việc nâng cấp ở `upgrade/runway-studio-2`. Baseline được chạy lại: 16 test pass, 0 lỗi, 0 skipped; log `evidence/baseline-maven-test.txt`.

Trong phiên làm việc có khởi tạo lại môi trường. Bản nâng cấp được khôi phục từ thay đổi đã ghi lại và kiểm thử lại; kết quả trong VALIDATION là kết quả của gói khôi phục, không lấy số cũ làm bằng chứng thay thế.

Giữ ID/slug nguồn, route sản phẩm, cấu trúc cart/order cũ, session/CSRF, COD, snapshot giá, khóa variant, quy tắc chuyển trạng thái. Tách controller/service; thêm các bảng phiên bản mới bằng Flyway. Không đổi toàn bộ package hoặc viết lại nền tảng.

Bản baseline riêng tư và ZIP gốc giữ tài nguyên tham khảo chưa rõ giấy phép. Release sạch trước đây đã loại ảnh và implementation upstream, dùng SVG mới. Trong bản cập nhật hiện tại theo yêu cầu khôi phục ảnh gốc, 140 file ảnh (55.000.817 byte) đã được đưa lại nguyên byte vào `src/main/resources/static/media/original/`; cả 140 SHA-256 khớp inventory ZIP gốc. Nguồn khôi phục là [IdeaToCodee/runway](https://github.com/IdeaToCodee/runway) tại commit `e0219d0082e31af42438ad787a73b988d5e2fa16`.

Manifest `src/main/resources/original-photo-manifest.json` ghi đường dẫn phục vụ, SHA-256, URL nguồn và commit upstream. Chỉ các file ảnh được khôi phục; HTML, font, tracker, payment script và implementation trải nghiệm upstream vẫn không được phân phối. Lịch sử Git baseline riêng tư không được nhập lại vào lịch sử repository hiện tại.

Checksum xác nhận tính nguyên vẹn và nguồn gốc, không xác nhận quyền sở hữu hay giấy phép ảnh. Không thấy LICENSE bao trùm tại commit upstream đã kiểm tra; dự án không tự nhận quyền hoặc cấp phép lại các ảnh này. Xem [ATTRIBUTION](ATTRIBUTION.md) và [Third-party notices](../THIRD_PARTY_NOTICES.md).

Danh sách file nguồn và checksum: `baseline-inventory.json`.
