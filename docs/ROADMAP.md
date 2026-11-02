# Roadmap — trạng thái bản core

Ngày cập nhật: 02/11/2026. Không coi toàn bộ PR 00–13 trong kế hoạch là đã hoàn tất chỉ vì có source cho một phần.

| Nhóm kế hoạch | Kết quả hiện tại | Phần tiếp theo |
|---|---|---|
| 00 baseline | ZIP giữ riêng, tag/branch private, test mới và inventory | Không public blob upstream |
| 01 hardening | Prod seed off, cookie, CSP script, login limiter, error schema, CI config | Chạy CI trên repository thật; rate limit phân tán nếu scale |
| 02 MySQL/Flyway | Fresh và populated baseline được test trên MySQL 8.4; FK/index; validate | Thử snapshot DB triển khai thực tế trước mỗi migration |
| 03 DTO/catalog | Typed responses, DB filter/sort/paging, query count test | Paging UI admin và query detail collections lớn |
| 04 checkout | Idempotency, account/variant lock, inventory ledger, rollback, concurrency tests | Order returns và reservation nếu có payment async |
| 05 frontend | ES modules, responsive 360/390/1440, giữ route cũ qua redirect | Audit a11y toàn diện / Lighthouse |
| 06–07 Outfit | Owner CRUD/version, curated riêng, add-all và UI lưu/tải | Catalog selector paging/search khi >60 product |
| 08 Scenes | Draft/publish/unpublish; ảnh approved; drag/keyboard/numeric editor | Upload ảnh an toàn và object storage |
| 09 Wardrobe/Fit | Idempotent favorites, owner guard, size rules có giải thích | Rule data theo sản phẩm thật, không thêm ML khi thiếu dữ liệu |
| 10 commerce | Voucher, địa chỉ, timeline; aggregate delivered-sales API | UI biểu đồ 7/30/90, top-selling, email |
| 11 payment | Chưa làm, không có provider/account sandbox | Xác thực callback + webhook idempotent trong PR riêng |
| 12 operation | Docker/Compose, readiness, CI workflows và Playwright source; test trực tiếp có evidence | Docker build thực tế, HTTPS host, coverage/perf measurements |
| 13 publication | Source sạch và tài liệu/screenshots có thể review | Tạo repository, chạy Actions, video thật, chọn license phần có quyền và release |

Không triển khai payment thật/AR/microservices. Release core COD có thể được đánh giá độc lập với backlog 2.1. Không công bố số Lighthouse, coverage hay CI xanh nếu chưa có báo cáo chạy thật.
