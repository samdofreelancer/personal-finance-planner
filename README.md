# Kế hoạch tài chính cá nhân 2026–2030

## Cấu hình local

Sao chép `.env.example` thành `.env` và đặt mật khẩu PostgreSQL riêng trước khi chạy Docker Compose. File `.env` được Git bỏ qua.

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
docs/             # Tài liệu sản phẩm, kiến trúc, API, nghiệp vụ và vận hành
.specify/         # Spec Kit scripts & templates
.github/          # Copilot integration
docker-compose.yml
```

## Tài liệu kỹ thuật

Điểm bắt đầu: [`docs/README.md`](docs/README.md). Bộ tài liệu phân biệt rõ kiến trúc đang chạy (as-is), các finding/cải tiến đang chờ triển khai (to-be) và các quyết định đã được ghi nhận.

- [Product & system overview](docs/01-system-overview.md)
- [Architecture](docs/02-architecture.md)
- [Business rules](docs/03-business-rules.md)
- [API contract](docs/04-api-contract.md)
- [Data model](docs/05-data-model.md)
- [Development & verification](docs/06-development-guide.md)
- [Architecture decisions](docs/decisions/README.md)
- [System architecture diagram source](docs/diagrams/system-architecture.mmd)

## Yêu cầu hệ thống

- Docker & Docker Compose

## Chạy toàn bộ bằng Docker (khuyên dùng)

```powershell
Copy-Item .env.example .env
# Chỉnh POSTGRES_PASSWORD trong .env
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
- Đây là ứng dụng local-first cho một hộ gia đình; không có đăng nhập, multi-tenant hay đồng bộ ngân hàng.
