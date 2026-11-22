# Engineering Documentation

Tài liệu chuẩn của Personal Finance Planner. Đọc theo thứ tự dưới đây khi onboarding hoặc trước khi thay đổi kiến trúc/nghiệp vụ.

| Tài liệu | Mục đích |
|---|---|
| [01 — System overview](01-system-overview.md) | Product intent, người dùng, phạm vi và thuật ngữ |
| [02 — Architecture](02-architecture.md) | Kiến trúc as-is, runtime, module boundaries và luồng chính |
| [03 — Business rules](03-business-rules.md) | Công thức và quy tắc dự báo hiện hành |
| [04 — API contract](04-api-contract.md) | Endpoint, input source, side effects và response |
| [05 — Data model](05-data-model.md) | Persistence model và dữ liệu nhạy cảm |
| [06 — Development guide](06-development-guide.md) | Setup, test, workflow và checklist thay đổi |
| [07 — AI agent context](07-ai-agent-context.md) | Context bắt buộc để AI agent hiểu repo trước khi làm việc |
| [08 — Engineering constitution](08-engineering-constitution.md) | Nguyên tắc bất biến về thiết kế, chất lượng, bảo mật và quy trình |
| [10 — E2E agent workflow](10-e2e-agent-workflow.md) | Routing giữa QA Orchestrator, Playwright Planner, Generator và Healer |
| [11 — Remote access with Tailscale](11-remote-access-tailscale.md) | Truy cập riêng tư từ Internet qua Tailscale Serve, không cần domain hoặc mở port router |
| [12 — Tailscale network flow](12-tailscale-network-flow.md) | Sơ đồ và luồng request khi truy cập app qua tailnet |
| [13 — New machine deployment](13-new-machine-deployment.md) | Cài đặt PC Windows mới, backup/restore PostgreSQL, triển khai Docker và cấu hình Tailscale/auto-start |
| [Architecture decisions](decisions/README.md) | Quyết định đã chốt, đang đề xuất hoặc cần xem xét |
| [Diagrams](diagrams/) | Diagram source; dùng draw.io để render/chỉnh sửa trực quan |

Product feature specifications: `specs/006-self-service-registration/spec.md` defines always-open self-registration, independent household creation and blank first-use financial data.

Browser E2E automation có package độc lập tại [`../e2e/`](../e2e/): specs, Page Objects, fixtures, Playwright config và runner. Agent routing được mô tả tại [10 — E2E agent workflow](10-e2e-agent-workflow.md), diagram source tại [`diagrams/e2e-agent-routing.mmd`](diagrams/e2e-agent-routing.mmd).

## Quy tắc về độ tin cậy

- **As-is** mô tả code/config đang tồn tại, không phải kiến trúc mong muốn.
- **To-be** chỉ mô tả yêu cầu đã được đặc tả; không được coi là đã triển khai nếu chưa có code và verification evidence.
- Khi tài liệu khác code, ưu tiên code và cấu hình đang chạy; sau đó cập nhật tài liệu trong cùng thay đổi.
- Không đưa dữ liệu tài chính cá nhân thật, credential, token, connection string có mật khẩu hoặc dữ liệu định danh vào docs, fixtures, screenshots hay Git history.
- Các giá trị trong seed/default data là dữ liệu demo của project, không được hiểu là dữ liệu thật của người dùng.

## Nguồn đặc tả

- Product baseline: `specs/personal-finance-planner/spec.md`.
- Review remediation requirements: `specs/001-review-findings-remediation/spec.md`.
- Self-service registration and independent household requirements: `specs/006-self-service-registration/spec.md`.
- Forecast UI/UX refinement requirements: `specs/007-forecast-ui-ux/spec.md`.
- SpecKit artifacts là tài liệu đặc tả/lập kế hoạch; trạng thái implementation phải được xác minh từ source, tests và runtime.
