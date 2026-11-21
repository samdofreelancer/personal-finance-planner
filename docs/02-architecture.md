# 02. Architecture

## Architecture summary (current)

Ứng dụng hiện là **modular monolith 3-tier**, triển khai local bằng Docker Compose:

| Tier | Technology | Responsibility |
|---|---|---|
| Presentation | Vue 3, Composition API, Vite | Form, validation phía client, metrics, chart, tables, CSV download |
| Web edge | Nginx | Serve static frontend; reverse-proxy `/api` tới backend |
| Application | Spring Boot 3.3.5, Java 21 | REST controllers, validation, assumptions persistence, forecast calculation, CSV generation |
| Persistence | PostgreSQL 16, Spring Data JPA/Hibernate + Flyway | Lưu assumptions, expense categories/amount changes, nhiều loans và repayment entries |

Không có microservice boundary. Không tạo service/domain boundaries chỉ để phản chiếu class; backend là một deployable unit.

## Component responsibilities

### Frontend (`frontend/src`)

- `App.vue`: application shell, điều hướng tới dashboard/assumptions/loans/annual/monthly và điều phối load, state, preview, explicit save, reset, export.
- `assets/style.css`: design tokens, sidebar/topbar layout và responsive presentation cho desktop/mobile.
- `components/AssumptionsForm.vue`: nhập thu nhập, tiết kiệm ban đầu và danh sách khoản chi tùy chỉnh với lịch thay đổi số tiền; phát sự kiện thay đổi/lưu/reset.
- `components/LoanScheduleForm.vue`: draft khoản vay và kỳ trả; thêm/sửa/xóa và lưu collection vào DB.
- `MetricCards.vue`, `FinancialPosition.vue`, `AnnualChart.vue`, `AnnualTable.vue`, `MonthlyTable.vue`: trình bày KPI, tiết kiệm/dư nợ mô phỏng và forecast; `FinancialPosition.vue` chỉ suy ra số dư từ assumptions và forecast response, không tạo dữ liệu tài sản thực tế.
- `services/api.js`: HTTP client cho assumptions, loan schedules, forecast, reset và CSV.
- `utils/validation.js`: client-side input validation.

### Backend (`backend/src/main/java/com/finance/planner`)

- `controller`: REST boundary (`AssumptionController`, `LoanController`, `ForecastController`) và API exception mapping.
- `service`: `AssumptionService` quản lý assumptions; `ExpenseScheduleService` lưu/đọc danh mục và mốc chi phí; `LoanScheduleService` quản lý aggregate khoản vay/lịch trả; `ValidationService` kiểm tra assumptions; `ForecastService` tính toán forecast.
- `dto`: request/response contracts.
- `entity`: JPA persistence model `Assumption`, `ExpenseCategory`, `ExpenseAmountChange`, `Loan`, `LoanRepayment`.
- `repository`: Spring Data JPA repositories cho assumptions, expense categories và loans.
- `resources/db/migration`: Flyway versioned migrations; Hibernate schema mode là `validate`.
- `config`: web/CORS configuration.

### Runtime

- `frontend`: Nginx container, public HTTP port 80 theo compose hiện tại.
- `backend`: Spring Boot container, port 8080 theo compose hiện tại.
- `db`: PostgreSQL 16, volume `finance_planner_db_data`.
- Backend chờ database healthcheck thành công trước khi khởi động.

### Browser E2E automation (`e2e/`)

- `tests/*.spec.js`: business journeys và assertions quan sát được.
- `pages/*.js`: Page Objects đóng gói locator, navigation và thao tác UI.
- `fixtures/test.js`: inject Page Objects và seed dữ liệu synthetic qua API của E2E stack.
- `playwright.config.js`: Chromium, chạy tuần tự và thu thập diagnostics.
- `scripts/run-e2e.mjs`: điều khiển Compose project riêng và gọi Playwright CLI.
- `package.json`/`package-lock.json`: dependency boundary độc lập với frontend.

E2E chạy qua một Docker Compose project và PostgreSQL volume riêng; nó không kết nối database planner thông thường. Vitest vẫn thuộc `frontend/` và không cài Playwright.

## Request/data flows

### Initial load

1. Vue gọi `GET /api/assumptions` và `GET /api/loans`.
2. `AssumptionController`/`LoanController` đọc dữ liệu qua services và repositories tương ứng.
3. Vue gọi `POST /api/forecast` với snapshot JSON gồm thu nhập, lịch chi phí tùy chỉnh và tiết kiệm ban đầu.
4. `ForecastController` validate assumptions và tải lịch trả đã lưu qua `LoanScheduleService`.
5. `ForecastService` áp dụng mốc hiệu lực của từng khoản chi, tổng hợp lịch trả theo tháng, tính cash flow và cumulative savings.
6. Vue render forecast response.

### Edit and explicit save

1. Form assumptions phát input; Vue cập nhật DTO, validate và preview forecast từ snapshot hiện tại.
2. Người dùng bấm `Lưu`; frontend gọi `PUT /api/assumptions` để lưu scalar assumptions và toàn bộ expense schedules trong cùng transaction.
3. Với khoản vay, người dùng chỉnh draft và bấm `Lưu lịch trả vào DB`; frontend gọi `PUT /api/loans` với toàn bộ collection.
4. Khi lưu thành công, frontend tải lại forecast để hiển thị kết quả theo lịch trả đã lưu.

**Important:** request ID hiện ngăn response cũ ghi đè UI state; nó không tự đảm bảo thứ tự ghi ở database. Đây là một review finding đang được theo dõi, không được mô tả như đã giải quyết.

### Reset and export

- Reset: `POST /api/assumptions/seed` khôi phục assumptions và sample loan schedule, sau đó frontend tải lại loans và forecast.
- Export: frontend gửi assumptions snapshot cùng expense schedules qua `POST /api/export/csv`; backend tính forecast từ input đó cùng các loan schedules đã lưu rồi trả CSV.

## Architecture diagram

Diagram tương tác đã được tạo bằng draw.io trong conversation. Mermaid source được version control tại [`diagrams/system-architecture.mmd`](diagrams/system-architecture.mmd). Khi thay đổi component hoặc luồng request, cập nhật source diagram cùng tài liệu này.

## Known gaps / target-state work

Các điểm dưới đây là trạng thái quan sát được từ source/config hiện tại và cũng là các hạng mục remediation; không được hiểu là đã khắc phục:

| Area | Current observation | Target / required follow-up |
|---|---|---|
| Network exposure | Compose publish ports without loopback binding; PostgreSQL port is published | Bind app ports to loopback by default; do not publish DB port |
| CORS | Explicit origins are configured in properties; verify actual environment-specific allowlist | Keep strict allowlist; document local origins |
| Autosave consistency | UI request IDs suppress stale UI responses, but concurrent PUTs can still race at persistence | Serialize writes or implement server-side concurrency control |
| Money arithmetic | ForecastService uses `double` for salary/raise intermediate calculations | Use decimal-safe arithmetic end-to-end and explicit rounding |
| Schema lifecycle | Flyway V1–V4 migrations; Hibernate `ddl-auto=validate` | Continue evolving schema only through reviewed migrations |
| Forecast source | Income and expense schedules are supplied as a JSON preview snapshot; loan schedules are loaded from PostgreSQL | Keep preview input explicit and non-persisting |
| Automated tests | Backend Java is the only forecast calculation implementation. Meaningful backend unit/integration tests are required to verify its behavior. | Add and maintain backend tests for forecast, validation, persistence and API contracts |

Review finding source of truth: [`specs/001-review-findings-remediation/spec.md`](../specs/001-review-findings-remediation/spec.md).

## Design constraints

- Keep the system a local-first modular monolith unless a separately approved requirement changes the deployment model.
- Keep forecast policy independent from HTTP and database infrastructure as it is refactored.
- Treat API DTOs as contracts; update frontend and backend together when changing them.
- Do not treat frontend validation as a security or correctness boundary; backend must validate independently.
