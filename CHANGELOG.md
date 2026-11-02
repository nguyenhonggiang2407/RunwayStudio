# Changelog

## 2.0.0 core — 02/11/2026

- Nâng cấp từ source ZIP Java v1, giữ package và contract storefront cũ, bổ sung typed DTO/service boundaries.
- Flyway H2/MySQL, FK/index/check constraints; prod seed off, secure cookie, limiter/error request ID, explicit admin bootstrap.
- Checkout retry không consume giỏ mới; giỏ account/merge guest, batch atomic, ledger/audit và order timeline.
- Outfit owner CRUD/version, curated API, Studio UI; Scenes draft/publish/hotspot editor có keyboard; private wardrobe và size rules giải thích.
- Voucher, quote, address book; delivered-only reports; customer count loại ADMIN.
- ES modules và hình SVG mới; bỏ unverified upstream media/experience implementation khỏi release sạch, giữ provenance.
- Maven/MySQL/Playwright/security evidence, Docker/Compose và CI source; không công bố Java hosting, Docker run hay hosted CI chưa thực hiện.

## 1.0.0 baseline

Ứng dụng Java trong ZIP gốc. Baseline riêng tư được giữ để đối chiếu. Xem docs/BASELINE.md.
