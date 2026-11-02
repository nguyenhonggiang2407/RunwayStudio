# Test cases và cách chạy

## Java/H2

`./mvnw clean verify` chạy test không cần DB ngoài. Mỗi test ghi dùng transaction rollback; H2 được tạo từ Flyway V1–V3 và Hibernate validate.

| Suite | Phạm vi |
|---|---|
| StoreIntegrationTest | Catalog/routes, CSRF, auth thật tạo session, admin deny, quantity/stock, batch atomic, snapshot/idempotency, cancellation/owner/state, duplicate email, validation/error ID, anonymous session state |
| StudioIntegrationTest | Outfit 3 slot save/reload, private read/write/delete, version item-only, slot/variant/category validation, batch add, cart persistence/merge, wardrobe uniqueness/owner, scene draft/publish/unpublish, hotspot version/coords/media/availability, catalog query count/filter, rule size boundary |
| CommerceIntegrationTest | Voucher calculation/cap/expiry/limit/one-per-account, quote non-consuming, rollback, retry keeps new cross-session cart, inventory/audit, delivered-only revenue/report, addresses owner guard |
| SecurityRegressionTest | Limiter 429/Retry-After, peer isolation and window expiry with controlled clock |
| ProductionConfigurationTest | No demo seed, secure cookie/schema validate, CSP/no sensitive actuator, SSE/XSLT advisory preconditions |
| AdminBootstrapTest | Disabled no-op, explicit strong credential requirement, no customer promotion, hash/admin creation, no overwrite |

Các test boundary size là parameterized với giá trị ngay dưới/đúng ngưỡng; không gộp số case với số method. Không @Disabled để che failure. Không công bố coverage khi chưa đo.

## MySQL

`./mvnw -Pmysql-verify verify` với DB_URL và LEGACY_DB_URL tới hai schema test rỗng. MySqlAcceptanceIT không nằm trong default surefire, không giả là đã test MySQL nếu chỉ chạy H2.

1. Fresh schema + seed + validate.
2. Price-sort/filter Criteria query trên MySQL.
3. Ba lần lặp, mỗi lần 10 buyer tranh một item cuối: đúng 1 order, stock 0 và một ledger row mới; mọi failure còn lại phải là BadRequest. Timeout/DB exception làm test fail.
4. 10 retry song song cùng buyer/requestId: cùng order ID, một decrement/ledger.
5. Hai cancellation đồng thời: một transition thành công, một restock.
6. Schema v1 chưa version có dữ liệu → baseline 1 tường minh → V2/V3; giữ snapshot/stock, backfill history, FK từ chối orphan.

## Playwright

`npm ci`, `npx playwright install chromium`, `npm run test:e2e`. JAR phải build trước. Tiến trình Java test dùng H2 mem, cổng 4187, readiness; nâng limiter chỉ trong command test vì dùng nhiều fixture. Ba project Chromium 1440×1000, 390×844, 360×800; một worker, không retry che flaky.

- Browse/filter → chọn variant → cart → login boundary.
- UI login → outfit mẫu → đổi variant → save → reload → add-all → quote/voucher → COD → orders/timeline.
- Scene hotspot → dialog variant → add cart → Fit Finder Giày 25.5 cm → size 41 và disclaimer.
- UI admin login → draft editor → tọa độ keyboard → save/draft 404 public → publish → scene public.

Mỗi luồng kiểm tra horizontal overflow và lỗi JavaScript `pageerror`. Screenshot ở docs/screenshots là screenshot Chromium thật, không mockup. Không coi 12 luồng này là audit a11y đầy đủ hoặc kiểm thử mọi trình duyệt. Không tuyên bố Lighthouse score từ ảnh chụp.

## Security / config

Maven dependency list → `python3 tools/dependency_audit.py --fail-on-high`. Báo raw findings và triage đúng version/ngày hết hạn. `npm audit --audit-level=high`; Gitleaks dir scan working tree có redact. CI lưu report trên GitHub khi được push, nhưng workflow trong ZIP không tự chứng minh một CI run đã thành công.
