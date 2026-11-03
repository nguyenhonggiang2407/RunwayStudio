# Third-party notices

Nguồn lịch sử: [IdeaToCodee/runway](https://github.com/IdeaToCodee/runway), commit `e0219d0082e31af42438ad787a73b988d5e2fa16`. ZIP v1 đã giữ RunwaySection, ShoeOrbit, SceneRail và 140 ảnh. Nguồn không có LICENSE tại commit đã kiểm tra; ghi nguồn không thay thế quyền sử dụng.

Release 2.0 trước đây đã loại implementation, adapter và ảnh upstream khỏi static classpath. Theo yêu cầu khôi phục ảnh gốc, bản hiện tại đưa lại đúng 140 ảnh (55.000.817 byte) vào `src/main/resources/static/media/original/`. Từng SHA-256 khớp inventory của ZIP gốc; nguồn khôi phục là repository và commit nêu trên. Manifest `src/main/resources/original-photo-manifest.json` lưu đường dẫn, checksum, URL nguồn và thông tin upstream.

Chỉ các file ảnh được khôi phục. Implementation RunwaySection/ShoeOrbit/SceneRail, HTML, font, tracker và payment script upstream vẫn không được phân phối. Giao diện dùng sáu ảnh người mẫu và ảnh của chín sản phẩm gốc; Straight Trousers và Weekend Tote là sản phẩm bổ sung của bản nâng cấp, không có ảnh gốc và tiếp tục dùng SVG minh họa. Studio, Scenes và các module giao diện mới được bổ sung trực tiếp trên ứng dụng Java v1. Xem [provenance và media](docs/ATTRIBUTION.md).

Đối chiếu nguồn và checksum không xác nhận quyền sở hữu hoặc giấy phép ảnh. Không có LICENSE bao trùm được xác nhận ở commit đã kiểm tra; sự vắng mặt của LICENSE không cấp quyền cấp phép lại. Dự án giữ attribution, không tuyên bố tự tạo ảnh upstream và không cấp LICENSE mới cho các tài nguyên này.

Spring Boot/Security/Data, H2, MySQL Connector/J, Flyway, Maven và Playwright giữ giấy phép và thông báo tương ứng trong các gói dependency. Không tự cấp LICENSE cho thành phần upstream chưa được phép. Dự án không tuyên bố là sản phẩm chính thức của Nike hoặc Shopify.
