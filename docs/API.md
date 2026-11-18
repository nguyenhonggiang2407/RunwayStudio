# REST API

JSON; tiền là số nguyên VND, timestamp ISO-8601 UTC. Response DTO có kiểu; không serialize Account/password, entity hoặc requestKey nội bộ. ID là số nguyên an toàn cho JavaScript trong seed và ID tạo mới. Page: `{items,page,size,totalElements,totalPages}`, size 1–60, page từ 0.

Mọi ghi dùng CSRF từ `GET /api/csrf`, gửi header theo `headerName` trả về. Cookie session cùng origin. 401 chưa đăng nhập, 403 role/CSRF, 404 tài nguyên không tồn tại hoặc riêng tư người khác, 409 conflict/version/constraint, 429 rate limit có Retry-After. Validation/business input 400.

```json
{"code":"VALIDATION_FAILED","message":"Kiểm tra các trường thông tin.","fieldErrors":{"email":"must be a well-formed email address"},"requestId":"server-generated-id"}
```

## Account và catalog

| Method / route | Quyền | Hợp đồng |
|---|---|---|
| GET /api/csrf | Public | token, headerName |
| GET /api/me | Public | authenticated, user safe hoặc null |
| POST /api/register | Public + CSRF | name, email, password; role luôn CUSTOMER; UTF-8 password ≤72 byte |
| POST /api/login | Public + CSRF | email, password; đổi session ID |
| POST /api/logout | CSRF | 204 |
| GET /api/products | Public | List shape cũ, q/category, tối đa 60; dùng /api/catalog cho nhiều trang |
| GET /api/products/{slug} | Public | Product + variants, chỉ active |
| GET /api/catalog | Public | q, category, page, size, sort=default/name/price-asc/price-desc, minPrice/maxPrice, inStock |
| GET /api/categories | Public | Danh mục active |

Price filter chọn sản phẩm có ít nhất một variant trong khoảng; giá trên card là giá thấp nhất toàn sản phẩm. Variant thật được xác nhận khi chọn và checkout. Query có escape wildcard `%`/`_` do user nhập.

## Giỏ, checkout và order

| Method / route | Quyền | Hợp đồng |
|---|---|---|
| GET /api/cart hoặc /cart.js | Guest/session hoặc account | items, item_count, subtotal, shipping, total, total_price, currency, sections |
| POST /api/cart/items | CSRF | variantId, quantity=0–99; 0 xóa dòng |
| POST /cart/add.js | CSRF | `{id,quantity}` hoặc `{items:[{id,quantity}]}`; batch max 50, nguyên tử |
| POST /api/cart/outfit | CSRF | `{items:[{variantId,quantity}]}`, 1–4 dòng; không gửi price |
| DELETE /api/cart | CSRF | Xóa giỏ hiện tại |
| POST /api/checkout/quote | Login + CSRF | voucherCode optional; không consume stock/promotion |
| POST /api/checkout | Login + CSRF | recipient, phone 0+9 digits, address 10–500, note optional, requestId 16–80 [a-zA-Z0-9-], voucherCode optional |
| GET /api/orders | Owner | List shape cũ, tối đa 60 |
| GET /api/me/orders | Owner | Page order details |
| POST /api/orders/{id}/cancel | Owner + CSRF | Chỉ PLACED; trả order/items/history |

GET cart có thể ghép giỏ guest sẵn có vào giỏ account một lần sau đăng nhập; đây là side effect hiện tại được kiểm thử. Client không gửi accountId hoặc price để điều khiển việc ghép. Giỏ guest được giữ lại khi merge thất bại; có thể DELETE cart để chủ động xóa giỏ.

Checkout trả `{order,items,history}`. Với đơn mới, server lưu fingerprint của thông tin nhận hàng, ghi chú và mã ưu đãi theo `accountId:requestId`. Retry cùng ID và cùng nội dung trả đơn đã tạo, giữ nguyên giỏ mới. Tên và địa chỉ bỏ khoảng trắng hai đầu, mã ưu đãi bỏ khoảng trắng và chuyển sang chữ hoa, ghi chú rỗng tương đương bỏ trống. Số điện thoại và nội dung ghi chú được so sánh nguyên trạng. Cùng ID với thông tin khác trả `409 CHECKOUT_REQUEST_CONFLICT`. Đơn trước migration V4 chưa có fingerprint và vẫn trả đơn cũ theo ID.

Giỏ hiện tại không tham gia so sánh vì checkout thành công đã làm trống giỏ cũ. Client dùng ID mới cho lần mua mới. Khi giao diện nhận xung đột, nó nhắc người dùng kiểm tra Đơn hàng. Lần bấm xác nhận tiếp theo tạo ID mới. Phí 30.000 cho subtotal <2.000.000, miễn từ ngưỡng và tính theo subtotal trước giảm giá. Voucher là % có cap/min/expiry/limit, một redemption/account. Khi hủy, lượt voucher đã dùng được giữ nguyên và tồn hoàn đúng một lần.

Transitions: PLACED→CONFIRMED/CANCELLED; CONFIRMED→SHIPPED/CANCELLED; SHIPPED→DELIVERED. Customer chỉ hủy PLACED. Snapshot giữ tên/giá/ảnh/size lúc đặt.

## Studio, Scenes, Wardrobe và Fit

| Method / route | Quyền | Hợp đồng |
|---|---|---|
| GET /api/studio/outfits | Public | Page curated PUBLIC/PUBLISHED |
| GET /api/studio/outfits/{id} | Public hoặc owner | 404 với private/draft không thuộc người gọi |
| GET/POST /api/me/outfits | Owner + CSRF khi ghi | name, items [{productId,variantId nullable,slot}] |
| PUT/DELETE /api/me/outfits/{id} | Owner + CSRF | PUT cần version; PRIVATE luôn thuộc account hiện tại |
| GET /api/scenes | Public | Page published |
| GET /api/scenes/{slug} | Public | Published scene/hotspots/product summary; product off-sale vẫn được giải thích |
| GET/POST /api/me/wardrobe | Owner + CSRF khi ghi | productId, variantId nullable; idempotent |
| DELETE /api/me/wardrobe/{id} | Owner + CSRF | 404 khi không thuộc owner |
| GET /api/size-guides | Public | Guide version và rows |
| POST /api/size-recommendations | Public + CSRF | category và chest/waist/footLength optional; height/weight optional chưa đủ riêng lẻ |
| GET/POST /api/me/addresses | Owner + CSRF khi ghi | recipient, phone, address; tối đa 10 |
| PUT/DELETE /api/me/addresses/{id} | Owner + CSRF | Không nhận accountId từ client |

Slot là TOP/BOTTOM/SHOES/ACCESSORY; mỗi slot một món, category phải phù hợp, variant phải thuộc product. Private outfit 1–4 món để có thể lưu nháp; curated publish cần ≥3. Body không cho customer đặt visibility/status/owner. Chia sẻ chỉ public curated.

Fit trả category,size nullable,confidence,reason,guideVersion,disclaimer; confidence GUIDE_MATCH/INSUFFICIENT_DATA/OUTSIDE_GUIDE. Khoảng min gồm và max không gồm. Không lưu số đo.

## Admin

Tất cả `/api/admin/**` yêu cầu ADMIN; mọi ghi có CSRF.

| Route | Method / hành vi |
|---|---|
| /products, /products/{id} | GET list cũ tối đa 60; POST create, PUT update, DELETE soft deactivate |
| /catalog | GET page catalog admin, q/page/size |
| /products/{id}/variants, /variants/{id} | POST add, PUT update size/price/stock; lock + ledger/audit |
| /orders, /order-page | GET list cũ hoặc Page |
| /orders/{id}/status | PUT `{status}`; state machine |
| /dashboard | GET counts, delivered revenue, pending, low stock; customer count chỉ CUSTOMER |
| /reports/sales?from=&to= | GET ngày YYYY-MM-DD, inclusive range ≤366 ngày; UTC, dựa trên event DELIVERED |
| /audit, /variants/{id}/movements | GET page audit / inventory |
| /promotions | GET page; POST code,percentOff,minSpend,maxDiscount,usageLimit,expiresAt,active |
| /outfits | GET page curated; POST create curated |
| /outfits/{id} | PUT version/items; DELETE curated |
| /outfits/{id}/publish hoặc /unpublish | POST, curated riêng |
| /scenes, /scenes/{id} | GET list/detail admin; POST create, PUT update (version) |
| /scenes/{id}/publish hoặc /unpublish | POST khi scene hợp lệ, hotspot ≥1 để publish |
| /media | GET approved scene image paths |

Scene input: title,slug,coverImage,altText,version (update),hotspots [{productId,xPercent,yPercent,label}], tối đa 20. X/Y cần có và nằm [0,100]. Chỉ ảnh tồn tại trong `/media/owned/*.svg`; API không nhận URL/upload. ProductInput cùng policy media. Không có API payment/card hoặc reset-password trong release core.

Báo cáo `revenue` là tổng `order.total` của đơn đã DELIVERED (đã trừ discount và gồm shipping), không phải lợi nhuận ròng.
