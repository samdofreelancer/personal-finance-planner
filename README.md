# Kế hoạch tài chính cá nhân 2026–2030

## Cấu hình local

Sao chép `.env.example` thành `.env` và đặt mật khẩu PostgreSQL riêng trước khi chạy Docker Compose. File `.env` được Git bỏ qua.

Web app duyệt qua các giả định, xem dự báo dòng tiền từng tháng, so sánh dòng tiền ròng theo năm và xuất kết quả CSV.

## Kiến trúc

Vue 3 + Spring Boot là ứng dụng duy nhất được hỗ trợ. Backend Java là nguồn tính toán forecast duy nhất.

- **Backend:** Spring Boot 3 (Java 21), REST API, PostgreSQL
- **Database:** PostgreSQL (household-owned assumptions, expense schedules và loan schedules)
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

## Tài khoản và hộ gia đình

- Lần đầu mở ứng dụng trên database chưa có tài khoản: tạo tài khoản đầu tiên; tài khoản này trở thành `OWNER` và nhận household hiện có.
- Sau bootstrap, đăng ký mới chỉ được phép qua invitation link/code do chủ hộ tạo. Link dùng một lần, mặc định hết hạn sau 7 ngày; token thô chỉ hiển thị lúc tạo.
- Mỗi người có email/tài khoản và mật khẩu riêng. `OWNER` và `MEMBER` cùng xem/chỉnh sửa dữ liệu tài chính của household; chỉ `OWNER` quản lý lời mời.
- Đăng nhập sử dụng server-side session cookie `HttpOnly`; request thay đổi dữ liệu cần CSRF token. Không lưu access/session token trong `localStorage`.
- Nếu truy cập HTTP thuần và trình duyệt không gửi Secure cookie, đặt `SESSION_COOKIE_SECURE=false` trong `.env` chỉ cho môi trường local HTTP. Với truy cập từ xa, giữ `true` và dùng HTTPS qua Tailscale Serve.

## Yêu cầu hệ thống

- Docker & Docker Compose

## Chạy toàn bộ bằng Docker (khuyên dùng)

```powershell
Copy-Item .env.example .env
# Chỉnh POSTGRES_PASSWORD trong .env
docker compose up --build
```

## Truy cập an toàn từ bất cứ đâu

Sử dụng **Tailscale** để truy cập riêng tư từ điện thoại/laptop khi ở ngoài nhà. Không cần mua domain, mở port router hay public backend/PostgreSQL. Hướng dẫn cài Tailscale trên Windows, bật HTTPS bằng Tailscale Serve và kết nối thiết bị nằm tại [Remote access with Tailscale](docs/11-remote-access-tailscale.md). Docker chỉ bind Nginx vào `127.0.0.1`; các thiết bị khác không thể truy cập trực tiếp qua LAN.

## Development bằng Docker với hot reload

Chạy cấu hình development riêng (không thay đổi production Compose):

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

- Frontend: http://localhost:5173 — Vite HMR tự cập nhật khi sửa Vue/CSS/JS.
- Backend: http://localhost:8080 — Maven watcher biên dịch Java khi source thay đổi; Spring Boot DevTools tự restart ứng dụng.
- Thay đổi `backend/pom.xml` hoặc `frontend/package.json` cần recreate service tương ứng để cài dependency mới: `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build backend frontend`.
- PostgreSQL tiếp tục dùng volume `finance_planner_db_data` hiện tại.
- `node_modules` và Maven dependencies được giữ trong named volumes để tránh cài lại mỗi lần khởi động.

Theo dõi log: `docker compose -f docker-compose.yml -f docker-compose.dev.yml logs -f frontend backend`.

Tắt development stack: `docker compose -f docker-compose.yml -f docker-compose.dev.yml down` (không thêm `-v`, để giữ dữ liệu database). Trở về production: `docker compose up -d --build`.

Các service sẽ khởi động:

| Service | URL | Mô tả |
|---------|-----|-------|
| Frontend | http://localhost | Vue.js + Nginx |
| Backend API | `http://localhost/api` | Spring Boot REST API qua Nginx; backend không publish port riêng |
| PostgreSQL | Docker network only | Database không publish port ra host/LAN |

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
| GET | `/api/auth/bootstrap` | Kiểm tra có cho phép đăng ký chủ hộ đầu tiên không |
| POST | `/api/auth/register` | Đăng ký chủ hộ đầu tiên hoặc thành viên bằng invitation token |
| POST | `/api/auth/login` | Đăng nhập, tạo server-side session |
| POST | `/api/auth/logout` | Hủy session hiện tại |
| GET | `/api/auth/me` | Lấy user và role của session hiện tại |
| GET | `/api/csrf` | Lấy CSRF token cho SPA |
| GET/PUT | `/api/assumptions` | Đọc/lưu giả định của household hiện tại |
| POST | `/api/assumptions/seed` | Khôi phục dữ liệu mẫu của household hiện tại |
| GET/PUT | `/api/loans` | Đọc/lưu lịch trả nợ của household hiện tại |
| POST | `/api/forecast` | Tính toán dự báo với dữ liệu khoản vay của household hiện tại |
| POST | `/api/export/csv` | Xuất CSV của household hiện tại |
| GET/POST | `/api/household/invitations` | Chủ hộ liệt kê/tạo lời mời |
| DELETE | `/api/household/invitations/{id}` | Chủ hộ thu hồi lời mời chưa sử dụng |

Financial APIs require an authenticated session. Household identity is resolved from the server-side principal; clients cannot select another `householdId`. Unsafe methods require `X-XSRF-TOKEN` from `/api/csrf`.

## Kiểm thử backend

```powershell
cd backend
mvn test
```

## Kiểm thử E2E bằng Playwright

```powershell
cd e2e
npm ci
npx playwright install chromium  # chỉ cần cài một lần
npm run e2e
```

Playwright nằm trong npm package độc lập `e2e/`; các spec trong `e2e/tests/` gọi Page Objects ở `e2e/pages/` để thao tác UI. Runner tự dựng một Docker Compose project và PostgreSQL riêng, chạy các luồng qua Chromium, sau đó dừng container. Database E2E tách biệt hoàn toàn với dữ liệu planner thông thường; không dùng `docker compose down -v`. HTML report và diagnostics nằm trong `e2e/playwright-report/` và `e2e/test-results/`. Chi tiết xem [Development & verification](docs/06-development-guide.md#browser-end-to-end-tests-playwright).

## Lưu ý

- Dữ liệu lưu trong PostgreSQL (qua Docker volume `finance_planner_db_data`).
- Frontend production được build bởi Vite và serve bằng Nginx, proxy `/api` sang backend.
- Cấu hình kết nối DB dùng environment variables (xem `docker-compose.yml` và `backend/src/main/resources/application.properties`).
- Đây là ứng dụng local-first dùng tài khoản riêng và dữ liệu chia sẻ theo household; không có đồng bộ ngân hàng, password reset qua email, MFA, social login hoặc cloud hosting.
