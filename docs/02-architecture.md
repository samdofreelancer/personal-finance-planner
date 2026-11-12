# 02. Architecture

## Architecture summary (as-is)

Ứng dụng hiện là **modular monolith 3-tier**, triển khai local bằng Docker Compose:

| Tier | Technology | Responsibility |
|---|---|---|
| Presentation | Vue 3, Composition API, Vite | Form, validation phía client, metrics, chart, tables, CSV download |
| Web edge | Nginx | Serve static frontend; reverse-proxy `/api` tới backend |
| Application | Spring Boot 3.3.5, Java 21 | REST controllers, validation, assumptions persistence, forecast calculation, CSV generation |
| Persistence | PostgreSQL 16, Spring Data JPA/Hibernate | Lưu một record assumptions |

Không có microservice boundary. Không tạo service/domain boundaries chỉ để phản chiếu class; backend là một deployable unit.

## Component responsibilities

### Frontend (`frontend/src`)

- `App.vue`: điều phối load, state, preview, autosave, reset và export.
- `components/AssumptionsForm.vue`: nhập giả định và phát sự kiện thay đổi/lưu/reset.
- `MetricCards.vue`, `AnnualChart.vue`, `AnnualTable.vue`, `MonthlyTable.vue`: trình bày forecast.
- `services/api.js`: HTTP client cho assumptions, forecast, reset và CSV.
- `utils/validation.js`: client-side input validation.

### Backend (`backend/src/main/java/com/finance/planner`)

- `controller`: REST boundary (`AssumptionController`, `ForecastController`) và API exception mapping.
- `service`: `AssumptionService` quản lý assumptions; `ValidationService` kiểm tra input; `ForecastService` tính toán forecast.
- `dto`: request/response contracts.
- `entity`: JPA persistence model `Assumption`.
- `repository`: Spring Data JPA repository.
- `config`: web/CORS configuration.

### Runtime

- `frontend`: Nginx container, public HTTP port 80 theo compose hiện tại.
- `backend`: Spring Boot container, port 8080 theo compose hiện tại.
- `db`: PostgreSQL 16, volume `finance_planner_db_data`.
- Backend chờ database healthcheck thành công trước khi khởi động.

## Request/data flows

### Initial load

1. Vue gọi `GET /api/assumptions`.
2. `AssumptionController` gọi `AssumptionService`.
3. Service đọc record đầu tiên từ `AssumptionRepository`; nếu chưa có record thì tạo entity mặc định trong memory path.
4. Vue gọi `GET /api/forecast` kèm assumptions dưới dạng query parameters.
5. `ForecastController` bổ sung default raise còn thiếu, validate input và gọi `ForecastService`.
6. Vue render forecast response.

### Edit and autosave

1. Form phát input; Vue cập nhật DTO và validate.
2. Frontend gọi forecast preview từ snapshot hiện tại.
3. Frontend debounce 250 ms rồi gọi `PUT /api/assumptions`.
4. Khi PUT thành công và request còn là request mới nhất ở UI, frontend gọi forecast lần nữa rồi cập nhật trạng thái lưu.

**Important:** request ID hiện ngăn response cũ ghi đè UI state; nó không tự đảm bảo thứ tự ghi ở database. Đây là một review finding đang được theo dõi, không được mô tả như đã giải quyết.

### Reset and export

- Reset: `POST /api/assumptions/seed` ghi đè assumptions hiện có bằng seed defaults, sau đó frontend tải forecast.
- Export: frontend gửi assumptions dưới dạng query parameters tới `GET /api/export/csv`; backend tự tính forecast từ input đó và trả CSV.

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
| Schema lifecycle | Hibernate `ddl-auto` defaults to `update` | Adopt versioned migrations; disable automatic schema mutation |
| Forecast source | `/api/forecast` and CSV currently calculate from query-bound request assumptions | Clearly distinguish saved forecast from non-persisting simulation; see remediation spec |
| Automated tests | Legacy JavaScript forecast oracle is isolated under `test/fixtures/`; Node tests use it only as a historical baseline. It is not part of production runtime. | Keep meaningful backend unit/integration tests as the authoritative verification of Java forecast behavior |

Review finding source of truth: [`specs/001-review-findings-remediation/spec.md`](../specs/001-review-findings-remediation/spec.md).

## Design constraints

- Keep the system a local-first modular monolith unless a separately approved requirement changes the deployment model.
- Keep forecast policy independent from HTTP and database infrastructure as it is refactored.
- Treat API DTOs as contracts; update frontend and backend together when changing them.
- Do not treat frontend validation as a security or correctness boundary; backend must validate independently.
