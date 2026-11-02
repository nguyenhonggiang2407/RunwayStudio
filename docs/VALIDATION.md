# Validation — 02/11/2026

Kết quả dưới đây được chạy thật trên bản source khôi phục. Không dùng báo cáo 16/16 trong ZIP v1 để chứng minh tính năng 2.0.

| Kiểm tra | Kết quả / môi trường | Bằng chứng |
|---|---|---|
| Baseline ZIP v1 | 16/16, 0 fail/error/skipped | evidence/baseline-maven-test.txt |
| Java/H2 upgrade | 78/78, 0 fail/error/skipped; JDK 17.0.20, Maven 3.9.11, Boot 3.5.16; Flyway + validate | evidence/java-verify.txt |
| Customer count regression sau full suite | 12/12 CommerceIntegrationTest; customer count không tính ADMIN | evidence/dashboard-regression.txt |
| MySQL | 8/8 IT; standalone MySQL Community 8.4.11, không phải H2 mode | evidence/mysql-verify.txt |
| HTTP packaged JAR | 12/12: auth+CSRF, route/media, last-item race, tính tiền, retry giữ giỏ mới, cancel/admin | evidence/http-smoke.txt và http-smoke-results.json |
| Browser E2E trên JAR cuối | 12/12, 0 retry; Chromium 141 / Playwright 1.56.1; 1440×1000, 390×844, 360×800 | evidence/playwright-results.json, evidence/playwright-run.txt |
| JavaScript | Syntax và import paths 12 module | tools/check-ui.mjs; CI command npm run check |
| Maven runtime dependency | 81 package; raw 2 advisory Spring MVC, assessment không áp dụng theo điều kiện đã review; 0 HIGH/CRITICAL/UNKNOWN chưa review | evidence/dependency-audit.json, SECURITY.md, security-triage.json |
| Node dependency | npm audit: 0 vulnerabilities tại lúc chạy | evidence/npm-audit.txt |
| Secret scan | Phạm vi source release sạch, không bao gồm runtime data/network traces/generated test artifacts | evidence/secret-scan.json và secret-scan.txt |

MySQL: last-item race lặp ba vòng x10 buyer, đúng 1 success/vòng, stock không âm; 10 retry một order; hai cancel một restock. Case legacy tạo schema v1 chưa version, chèn dữ liệu, baseline 1 tường minh, migrate V2/V3 và giữ snapshot/stock, FK chặn orphan. Không trỏ các test này vào DB có dữ liệu người dùng.

JAR cuối được kiểm tra ZIP integrity và đối chiếu resource/config/module với source. HTTP smoke và Playwright chạy trên đúng JAR đó sau thay đổi customer count và readiness DB. Full 78 suite và 8 MySQL IT có log riêng; không cộng chúng rồi gọi là một lần chạy duy nhất. Source formatter chỉ đổi trình bày. Các test không có @Disabled để che lỗi.

Browser chụp ảnh thật của storefront/Outfit/Scenes/admin trên ba viewport; no-horizontal-overflow và no-pageerror được assert trong cả 12 luồng. Đây không là audit a11y toàn diện. Ảnh không chứa credential hoặc dữ liệu thật.

**Chưa xác minh / chưa làm:** Docker image và Compose chưa được chạy vì môi trường không có Docker; GitHub Actions mới là workflow trong source, chưa có hosted run; chưa deploy Java HTTPS, chưa video demo, chưa benchmark Lighthouse/coverage, chưa payment sandbox. Không công bố điểm số hoặc badge xanh cho các mục này. Backlog ở ROADMAP.md.

Các log nguồn có timezone của executor khác +07; ngày hiển thị cho bàn giao là 02/11/2026. `baseline/` chứa bằng chứng lịch sử v1 được đánh dấu riêng, không trộn vào kết quả release.
