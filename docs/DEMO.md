# Chạy demo

Đọc README để chạy JAR hoặc Maven. Profile `demo` dùng H2 và credential local minh họa, cổng 8080 loopback. Profile `mysql,prod` dùng thông tin riêng và không seed.

Các route chính: `/shop`, `/studio`, `/scenes`, `/wardrobe`, `/fit-finder`, `/cart`, `/checkout`, `/orders`, `/admin`, `/admin?view=scenes`.

Kịch bản trình diễn: `demo-script.md`. Bản Java này tách với bản demo JavaScript đã host trước đó; không gọi bản Java là đã deploy online.
