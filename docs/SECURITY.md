# Security

## Cơ chế hiện tại

- Session auth, BCrypt, đổi session ID khi login; CSRF bắt buộc cho mọi POST/PUT/DELETE, kể cả size-recommendations và API admin. Không dùng JWT cho frontend cùng domain.
- `/api/admin/**` yêu cầu ADMIN. `/api/me/**`, orders và checkout yêu cầu đăng nhập; GET `/api/me` public để UI đọc trạng thái phiên. Private resources luôn có owner check trong service và trả 404 cho người khác.
- Cookie HttpOnly/SameSite Lax; prod Secure khi HTTPS. Demo chỉ loopback. CSP cấm inline script, object, external connect và framing. `style-src` còn `unsafe-inline` do template storefront v1 dùng style attribute; giảm inline style là backlog, không tuyên bố CSP đã hoàn toàn strict.
- Login/register giới hạn 20 request/15 phút theo servlet peer; bounded map 4096 peer, expire cửa sổ. Không tin X-Forwarded-For tùy ý. Đây là limiter theo instance; public deployment nhiều instance cần limiter tại edge/shared store. Proxy phải strip/set header đúng.
- Error DTO gồm code, message, fieldErrors, requestId; header X-Request-Id mới do server sinh. Unexpected error không trả stack/message exception; log chỉ requestId và class lỗi. Không log password/token/số đo.
- Giá, phí, voucher và tồn do server tính. Khóa account, order, variant và unique constraint chống race/retry. Không chấp nhận client total làm giá phải trả.
- Admin audit lưu actor/action/target; inventory ledger trong transaction thay đổi tồn. Không ghi địa chỉ hay nội dung tài khoản vào audit event.
- Media admin chỉ chọn SVG có sẵn, path regex và tồn tại classpath; không hỗ trợ upload SVG hoặc URL tùy ý. Số đo Fit Finder không được lưu.

## Dependency review 02/11/2026

Giữ major Spring Boot 3.5.x theo roadmap; nâng patch từ 3.5.7 lên 3.5.16. Override tương thích: Tomcat 10.1.61, Jackson BOM 2.22.4, Log4j 2.26.1; build/test được chạy với những version này. Scan thực tế ở `evidence/dependency-audit.json`, không phải badge CI giả.

OSV còn liệt kê hai advisory trên Spring MVC 6.2.19 với severity CRITICAL. Tài liệu chính thức Spring mô tả severity/điều kiện khác. Báo cáo raw vẫn được giữ:

| Advisory | Điều kiện từ vendor | Đánh giá ứng dụng |
|---|---|---|
| CVE-2026-47890 / GHSA-j9f9-w8pj-32f8 | SSE với view fragments và attacker-controlled data; vendor LOW | Không có SSE/ResponseBodyEmitter hay view fragments |
| CVE-2026-47884 / GHSA-pc63-qcmh-9cmg | XsltView + implicit wildcard view rendering; vendor MEDIUM | Không có XsltViewResolver, XSLT template hoặc implicit wildcard view |

Nguồn: https://spring.io/security/cve-2026-47890/ và https://spring.io/security/cve-2026-47884/. ProductionConfigurationTest kiểm tra các tiền điều kiện chính không xuất hiện. Đây là đánh giá khả năng áp dụng cho kiến trúc hiện tại, không là bảo đảm tuyệt đối và không biến dependency dễ tổn thương thành bản đã vá.

`security-triage.json` chỉ khớp đúng ID/package/version và hết hạn 01/01/2027. CI chặn HIGH/CRITICAL/UNKNOWN mới hoặc chưa review; không bỏ findings ra khỏi report. Thêm SSE/XSLT phải review lại ngay, không đợi hết hạn. Boot 3.5 đã hết OSS support (https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/); fix Framework 6.2.20 cho hai advisory là enterprise. Nâng major để theo OSS là một thay đổi tiếp theo, không lén đổi nền tảng của core release.

OSV audit chỉ bao phủ dependency runtime Maven đã resolve, không thay pentest, scan image hoặc phát hiện lỗ hổng chưa công bố. Npm audit và Gitleaks có phạm vi riêng. Không công bố “0 lỗ hổng bảo mật” từ kết quả này.

## Trước host public

Dùng mysql,prod; secret riêng, seed off, admin provision một lần rồi gỡ password, TLS/proxy đúng, backup/restore đã thử. Giới hạn demo/customer theo mục đích. Không dùng tài khoản admin demo công khai. Chỉ public source/media đã rõ quyền. GitHub Actions phải chạy thật trên repository đích trước khi gắn badge xanh.
