# ADR 0001 — Giữ modular monolith

Status: accepted, 02/11/2026.

Giữ Java 17, Spring Boot 3.5.x, session/CSRF, JPA và frontend không framework để có thể kiểm tra hồi quy từng thay đổi từ source ZIP. Tách service/DTO và frontend module, không chuyển toàn bộ package cùng checkout. JWT/microservices không giải quyết một nhu cầu hiện tại của website cùng domain.

Tradeoff: layer package chưa tối ưu cho feature lớn; list detail còn query theo record ở một số API. Đưa refactor feature package và query tuning tiếp vào PR riêng. Boot 4.x là nâng cấp sau core vì ràng buộc không đổi major; không bỏ qua các advisory của 3.5.x.
