# Kế hoạch tài chính cá nhân 2026–2030

Web app duyệt qua các giả định, xem dự báo dòng tiền từng tháng, so sánh dòng tiền ròng theo năm và xuất kết quả CSV.

## Kiến trúc (mới)

- **Backend:** Spring Boot 3 (Java 21), REST API, PostgreSQL
- **Database:** PostgreSQL (bảng `assumptions`, 1 dòng)
- **Frontend:** Vue.js 3 + Vite (build static), Nginx serve
- **Orchestration:** Docker Compose

## Cấu trúc thư mục

```
backend/          # Spring Boot + PostgreSQL
frontend/         # Vue.js 3 + Vite + Nginx
specs/            # Spec Kit (spec.md, plan.md, tasks.md)
.specify/         # Spec Kit scripts & templates
.github/          # Copilot integration
docker-compose.yml
```

## Yêu cầu hệ thống

- Docker & Docker Compose

## Chạy toàn bộ bằng Docker (khuyên dùng)

```powershell
docker compose up --build
```

Các service sẽ khởi động:

| Service | URL | Mô tả |
|---------|-----|-------|
| Frontend | http://localhost | Vue.js + Nginx |
| Backend API | http://localhost:8080 | Spring Boot REST API |
| PostgreSQL | localhost:5432 | Database (internal network) |

Dừng và xóa volume:

```powershell
docker compose down -v
```

## Chạy riêng lẻ (không dùng Docker)

### Backend (cần PostgreSQL đã cài)

```powershell
cd backend
mvn spring-boot:run
```

### Frontend (cần Node.js 18+)

```powershell
cd frontend
npm install
npm run dev
```

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

- Dữ liệu lưu trong PostgreSQL (qua Docker volume `finance_planner_db_data`).
- Frontend production được build bởi Vite và serve bằng Nginx, proxy `/api` sang backend.
- Cấu hình kết nối DB dùng environment variables (xem `docker-compose.yml` và `backend/src/main/resources/application.properties`).