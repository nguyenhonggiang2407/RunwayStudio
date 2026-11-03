# Validation

## 04/11/2026 — licensed editorial and portfolio review

The new homepage City Notes uses one real photograph under the Pexels License. The original catalog, six-look controls, Java services, seed data and schemas are unchanged. [Media license record](MEDIA_LICENSES.md).

| Check actually run | Result | Evidence |
| --- | --- | --- |
| Java 17 / Maven `verify` | 94 tests, 0 failure/error/skipped; packaged JAR built successfully | `editorial-validation.json` summary extracted from Surefire XML; full local Maven log reviewed |
| Full Java-app browser suite | 18/18 passed at 1440×1000, 390×844 and 360×800; no retry | [Recorded run](evidence/editorial-e2e-run.txt) |
| Screenshot readiness follow-up | 3/3 guest-flow tests passed after making the screenshot helper await visible image decoding | [Actual report](evidence/editorial-screenshot-results.json) |
| JavaScript and assets | 14 module syntax/import checks; 140 original SHA-256 hashes and 2 licensed WebP variants checked | `npm run check`; [summary](evidence/editorial-validation.json) |
| Visual inspection | Homepage desktop/mobile screenshots viewed after image decoding; real live-before captures retained | `docs/screenshots/home-*.png` and `home-before-*.png` |

The full suite covers the licensed editorial/local image/CTA, original six looks, guest cart/auth boundary, saved outfit/reload/add-all, voucher/COD order/timeline, scene hotspot/cart, Fit Finder and admin scene keyboard editing/publishing. No public customer data was used; Playwright runs a separate temporary H2 process on port 4187.

An initial desktop screenshot was captured before its photo decoded. That incomplete capture was replaced by an actual browser capture after decoding, and the screenshot helper now waits for visible images. No screenshot was constructed or retouched.

The historical `playwright-results.json` below remains the 15-test photo-restoration report. The new full-run log and three-test screenshot report above are separate evidence; their counts are not added together as independent coverage. Docker and MySQL were not run locally in this change.

### CI and public demo follow-up

[GitHub Actions run 41720242244](https://github.com/nguyenhonggiang2407/RunwayStudio/actions/runs/41720242244) completed successfully on application commit `4f212cd3f2481ef11a42ba87cfc62409d7c8869c`: `java-browser`, `mysql` and `secrets` jobs all succeeded. The MySQL job is hosted CI evidence, separate from local H2 tests.

On **4 November 2026**, the public demo initially timed out in HTTP checks. A later real Chromium visit loaded the new City Notes interface. Five changed/new resources (the editorial module, homepage module, photography CSS and both WebP variants) returned 200 and matched the exact SHA-256 of their Git blobs at the tested application commit. Readiness returned HTTP 200 with `UP`; six original look controls remained present; desktop 1440 and mobile 390 had no horizontal overflow or JavaScript page errors. [Read-only verification](evidence/editorial-live.json) and `live-editorial-desktop.png` / `live-editorial-mobile.png` record the result.

This verifies the changed public UI resources at that time. No live account, cart, order or database write was performed; the full customer/admin flows above were tested locally. Render dashboard deploy IDs and workload metrics were not inspected because no workspace was selected. No workspace action is needed for this completed public verification.

## 03/11/2026 — khôi phục ảnh gốc, kiểm thử local

Kết quả hiện tại được đối chiếu trực tiếp từ báo cáo Maven/Playwright và các tài nguyên trong working tree. [Tóm tắt JSON](evidence/photo-restoration.json) không chứa secrets hoặc đường dẫn người dùng local.

| Kiểm tra | Kết quả thực tế | Nguồn bằng chứng |
|---|---|---|
| Maven `clean verify` | **94/94 pass**, 7 suite; 0 failure/error/skipped | `target/surefire-reports/TEST-*.xml`; tổng đã trích vào JSON |
| Playwright trên ứng dụng Java local | **15/15 pass**, 0 retry/flaky/skipped; Chromium, desktop 1440×1000 và mobile 390×844 / 360×800 | `docs/evidence/playwright-results.json`, `playwright.config.js` |
| JavaScript | Syntax và import paths **13 module** pass | `npm run check` → `tools/check-ui.mjs` |
| Media gốc | **140 file, 55.000.817 byte**, 0 SHA-256 sai khác so với manifest và inventory ZIP gốc; 9 cặp ảnh card/full và 6 ảnh người mẫu/looks; không import ảnh ngoài | `tools/check-media.mjs`, `src/main/resources/original-photo-manifest.json`, `docs/baseline-inventory.json` |

Các luồng browser bao gồm ảnh gốc/sáu lựa chọn runway, catalog/variant/giỏ khách, lưu và tải outfit, add-all/voucher/checkout/timeline, hotspot/size và admin draft/publish trên cả ba viewport. Đây là kiểm thử hồi quy local, không phải audit a11y hoặc benchmark hiệu năng.

[URL HTTPS hiện có](https://runwaystudio-portfolio-demo.onrender.com) đã được triển khai và xác minh ở phiên bản trước. **Phiên bản khôi phục ảnh trong working tree này còn chờ CI và xác minh triển khai trên Render**; các kết quả local ở trên không chứng minh phiên bản mới đã live. Những kết quả MySQL, dependency audit và HTTP smoke bên dưới là bằng chứng lịch sử, chưa được chạy lại trong đợt khôi phục ảnh này.

## Lịch sử — 02/11/2026

Phần dưới giữ lại chi tiết bàn giao ngày 02/11. Các con số và trạng thái chưa triển khai ở đây mô tả thời điểm đó; báo cáo `playwright-results.json` tại cùng đường dẫn nay đã được cập nhật bằng lần chạy 15 test ngày 03/11 nêu trên.

Kết quả dưới đây được chạy thật trên bản source khôi phục. Không dùng báo cáo 16/16 trong ZIP v1 để chứng minh tính năng 2.0.

| Kiểm tra | Kết quả / môi trường | Bằng chứng |
|---|---|---|
| GitHub Actions hosted | 3/3 jobs success: java-browser, mysql, secrets; source commit `bc73b05c7ea1ab9d5a966ce53e1f74adfe444221` | [Run #1](https://github.com/nguyenhonggiang2407/RunwayStudio/actions/runs/41466543082) |
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

**Chưa xác minh / chưa làm tại ngày 02/11:** Docker image và Compose chưa được chạy vì môi trường không có Docker; chưa deploy Java HTTPS, chưa video demo, chưa benchmark Lighthouse/coverage, chưa payment sandbox. Không công bố điểm số hoặc badge xanh cho các mục này. Backlog ở ROADMAP.md. Trạng thái triển khai hiện tại được tách riêng trong phần 03/11 ở đầu tài liệu.

Các log nguồn có timezone của executor khác +07; ngày hiển thị cho bàn giao là 02/11/2026. `baseline/` chứa bằng chứng lịch sử v1 được đánh dấu riêng, không trộn vào kết quả release.

Repository đã public ngày 02/11/2026. Lần chạy GitHub ở trên xác minh commit import mã nguồn sạch; commit tài liệu kế tiếp bổ sung liên kết này và cập nhật trạng thái công bố, không thay đổi mã ứng dụng.
