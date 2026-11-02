# Deployment và migration

## Local H2

`./mvnw clean verify` rồi `java -jar target/runway-store-2.0.0.jar`. Windows dùng `mvnw.cmd`. Profile `demo` lắng nghe 127.0.0.1:8080, seed giả, dữ liệu H2 trong `data/`. Dừng ứng dụng trước khi sao lưu file H2; không copy một file DB đang ghi.

## MySQL 8.4 mới

Tạo database utf8mb4 và user riêng có quyền với schema này. Đặt biến môi trường ngoài repository:

```sh
export SPRING_PROFILES_ACTIVE=mysql,prod
export DB_URL='jdbc:mysql://localhost:3306/runway_store?serverTimezone=UTC'
export DB_USERNAME=runway
export DB_PASSWORD='your-own-database-password'
java -jar target/runway-store-2.0.0.jar
```

`prod` tắt seed. Khi schema trống, Flyway chạy V1→V2→V3 trước Hibernate validate. Không có fallback mật khẩu DB. Không đưa `.env` hay password thật vào Git. Lệnh ví dụ chứa placeholder, không phải credential được cấp sẵn.

Để provision admin đầu tiên, bật **một lần** `RUNWAY_BOOTSTRAP_ENABLED=true`, đặt `RUNWAY_BOOTSTRAP_EMAIL` và `RUNWAY_BOOTSTRAP_PASSWORD` riêng (16–72 byte UTF-8). Sau startup thành công, tắt flag và gỡ password khỏi môi trường chạy. Bootstrap không đổi mật khẩu admin đã có và không nâng customer sẵn có lên admin. Đăng ký public luôn tạo CUSTOMER.

## Database v1 đã có dữ liệu

**Không bật baseline-on-migrate và không chạy V1 vào schema hiện hữu.** V1 mô tả schema nguồn, không phải script chuyển đổi mọi database tùy biến.

1. Dừng ghi, backup đầy đủ và thử restore sang schema kiểm thử riêng.
2. Kiểm kê DDL so với `db/migration/mysql/V1__source_baseline.sql` (H2 dùng thư mục h2). Kiểm tra ID, kiểu/cỡ cột, unique email/requestKey/slug, orphan variant/product/order/line và stock âm. Sửa dữ liệu sai trước khi thêm FK; không bỏ constraint để ép migration qua.
3. Chỉ khi schema đúng baseline và đã restore thử, dùng **Flyway CLI chính thức** với `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD`, `FLYWAY_LOCATIONS=filesystem:src/main/resources/db/migration/mysql`, `FLYWAY_BASELINE_VERSION=1`; chạy `flyway baseline`, kiểm tra history, sau đó `flyway migrate` và `flyway validate` trên bản copy trước.
4. Xác nhận snapshot đơn cũ, giá/tồn, đăng nhập, CRUD, checkout/hủy, owner guard và counts. MySqlAcceptanceIT có case tái tạo schema chưa version, chèn dữ liệu cũ rồi baseline/migrate, nhưng không thay thế kiểm thử snapshot của database thật.
5. Lặp quy trình với backup production và cửa sổ bảo trì; chạy app với `ddl-auto=validate`.

Flyway CLI: https://documentation.red-gate.com/fd/command-line-277578359.html. Không đưa password vào command-line argument hoặc log. Không dùng `flyway clean` trên database có dữ liệu cần giữ. MySQL DDL có implicit commit; rollback schema bằng backup đã thử restore, không giả định transaction có thể undo mọi ALTER TABLE. Chỉ dùng `repair` khi đã phân tích rõ migration lỗi.

Giữ catalog thật khi migrate: seed không overwrite dữ liệu cũ. Các ảnh v1 chưa có quyền không được copy vào release; admin phải chọn lại media approved. Nếu chỉ cần demo mới, tạo một schema local riêng; không xóa dữ liệu cũ để lấy seed.

## Docker Compose

Cần Docker/Compose trên máy chạy. Bản bàn giao có Dockerfile/Compose; môi trường tạo gói không có Docker nên chưa tuyên bố đã build image hoặc chạy Compose.

1. Copy `.env.example` thành `.env`; điền `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` riêng. Để prod mặc định.
2. `docker compose up --build` dựng app + MySQL. DB không publish port; app map loopback 8080 để reverse proxy quản lý HTTPS. Kiểm tra `docker compose ps` và `docker compose logs app`.
3. Cấu hình HTTPS reverse proxy tới 127.0.0.1:8080; strip các header Forwarded/X-Forwarded-* từ client và set lại đúng tại proxy. `prod` dùng secure session cookie. Không chạy prod cookie HTTPS trên một URL HTTP rồi kết luận login lỗi.
4. `/actuator/health/readiness` gồm readiness state và DB; liveness tách khỏi DB. Chỉ health/info được expose; chi tiết health không public.

Demo MySQL riêng trên local:

```sh
docker compose -f compose.yaml -f compose.demo.yaml up --build
```

Điền hai password demo riêng trong `.env`; override chỉ dùng `mysql`, không `prod`. Không đưa mode này ra Internet. Thử xong giữ volume theo nhu cầu; không dùng lệnh xóa volume khi còn dữ liệu cần giữ.

## CI MySQL acceptance

Chuẩn bị **hai schema trống, có thể bỏ**: `runway_acceptance` và `runway_legacy`. Truyền DB_URL, LEGACY_DB_URL, DB_USERNAME, DB_PASSWORD. `./mvnw -Pmysql-verify verify` chạy unit/H2 rồi failsafe IT. Không trỏ vào schema dev/prod: case legacy tạo bảng và insert fixture.

CI tạo MySQL service riêng; cùng tài khoản test có quyền trên cả hai schema. Test không biến lỗi deadlock/timeout thành lỗi nghiệp vụ để đạt giả.

## Phát hành và rollback

Gắn version JAR/image và backup trước thay đổi schema. Verify DB, build, smoke và snapshot. Với thay đổi code không đổi schema, có thể quay về artifact tương thích trước đó. Với V2/V3 đã áp dụng, không chạy ứng dụng cũ dựa trên giả định tự động rollback DB; khôi phục backup và artifact khớp khi cần.

Bản Java chưa được deploy tới host public trong bàn giao này. Sites đã dùng cho demo JavaScript trước đó chạy Workers, không host JVM/Spring Boot trực tiếp. Không gọi bản demo đó là host của JAR mới. Chọn VPS/container/JVM host phù hợp trước bước triển khai thực tế; không cần tích hợp thanh toán thật để demo COD.
