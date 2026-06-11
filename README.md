# TechStore

Website bán công nghệ full-stack: Spring Boot + React + MySQL.

## Tính năng chính

- Đăng ký / đăng nhập JWT, phân quyền Admin – Staff – Customer
- Danh mục sản phẩm, giỏ hàng, wishlist, đặt hàng
- Thanh toán COD và MoMo (sandbox)
- Upload ảnh qua Cloudinary
- Chat realtime (STOMP / SockJS)
- Trang admin: quản lý user, sản phẩm, đơn hàng, coupon, review
- Trang chủ: sản phẩm bán chạy (theo `soldCount`) và sản phẩm theo thương hiệu

## Yêu cầu

- Java 17+, Maven 3.9+
- Node.js 22+, npm
- MySQL 8.0+ (chạy local) hoặc Docker Desktop (chạy Docker)

## Cài đặt

```powershell
Copy-Item .env.example .env
Copy-Item frontend\.env.example frontend\.env
```

Sửa `.env`: JWT secret, MySQL, Cloudinary (upload ảnh), MoMo (nếu test thanh toán).

Tạo JWT secret:

```powershell
openssl rand -base64 32
```

## Chạy bằng Docker (khuyến nghị)

```powershell
docker compose up --build -d
```

| Dịch vụ  | Địa chỉ                |
|----------|------------------------|
| Frontend | http://localhost:3000  |
| Backend  | http://localhost:8081  |
| MySQL    | localhost:3306         |

Swagger: http://localhost:8081/swagger-ui/index.html

## Chạy local

**Backend** (thư mục gốc):

```powershell
mvn spring-boot:run
```

**Frontend**:

```powershell
cd frontend
npm ci
npm run dev
```

Frontend: http://localhost:5173 — proxy `/api` và `/ws` sang backend.

## Cấu trúc project

```
src/           Backend Spring Boot
frontend/      Frontend React + Vite + Tailwind
docs/          Tài liệu module admin
postman/       Collection test API
```

## Lưu ý

- Không commit file `.env` (chứa mật khẩu và API key).
- Upload ảnh cần cấu hình Cloudinary trong `.env`.
- `soldCount` được cộng khi admin chuyển đơn sang trạng thái **Completed**.
