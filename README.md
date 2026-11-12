# Kế hoạch tài chính cá nhân 2026–2030

Web app duyệt qua các giả định, xem dự báo dòng tiền từng tháng, so sánh dòng tiền ròng theo năm và xuất kết quả CSV.

## Kiến trúc (mới)

- **Backend:** Spring Boot 3 (Java 21), REST API, PostgreSQL
- **Database:** PostgreSQL (bảng `assumptions`, 1 dòng)
- **Frontend:** Vue.js 3 + Vite, gọi backend qua REST API

## Cấu trúc thư mục

```
backend/          # Spring Boot + PostgreSQL
frontend/         # Vue.js 3 + Vite
specs/            # Spec Kit (spec.md, plan.md, tasks.md)
.specify/         # Spec Kit scripts & templates
.github/          # Copilot integration
```

## Yêu cầu hệ thống

- Java 21+
- Apache Maven 3.9+
- PostgreSQL (cài đặt riêng, tạo database `personal_finance_planner`)
- Node.js 18+, npm

## Cài đặt & chạy

### 1. Cài PostgreSQL

Tạo database và user:

```sql
CREATE DATABASE personal_finance_planner;
CREATE USER planner WITH PASSWORD 'planner';
GRANT ALL PRIVILEGES ON DATABASE personal_finance_planner TO planner;
```

Cập nhật `backend/src/main/resources/application.properties` với thông tin kết nối thực tế.

### 2. Chạy Backend (Spring Boot)

```powershell
cd backend
mvn spring-boot:run
```

Backend chạy tại: `http://localhost:8080`

### 3. Chạy Frontend (Vue.js)

```powershell
cd frontend
npm install
npm run dev
```

Frontend chạy tại: `http://localhost:5173`

### 4. Sử dụng

Mở trình duyệt tại `http://localhost:5173`, chỉnh sửa giả định, dữ liệu sẽ lưu vào PostgreSQL qua REST API.

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/assumptions` | Lấy giả định từ DB |
| PUT | `/api/assumptions` | Lưu giả định vào DB |
| POST | `/api/assumptions/seed` | Seed dữ liệu mặc định |
| GET | `/api/forecast` | Tính toán dự báo |
| GET | `/api/export/csv` | Xuất CSV |

## Kiểm thử backend

```powershell
cd backend
mvn test
```

## Lưu ý

- Dữ liệu không được lưu trong localStorage nữa, mà lưu trong PostgreSQL.
- Cần cài PostgreSQL trước khi chạy backend.
- Frontend proxy `/api` qua Vite tới `http://localhost:8080` (xem `vite.config.js`).