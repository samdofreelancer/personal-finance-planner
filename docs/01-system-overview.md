# 01. System Overview

## Product intent

Personal Finance Planner (UI brand: **Planwise**) là ứng dụng local-first giúp một hộ gia đình nhập các giả định tài chính, xem dự báo dòng tiền theo tháng/năm trong giai đoạn Nov 2026–Dec 2030, nhận biết kết quả tạm tính và xuất dữ liệu để tiếp tục phân tích.

Ứng dụng thay thế việc duy trì một bảng tính thủ công bằng một giao diện có thể chỉnh sửa giả định và tính toán lại nhất quán. Đây là công cụ mô phỏng theo input của người dùng, **không phải tư vấn tài chính**.

## Primary user journey

1. Mở ứng dụng trên máy local.
2. Frontend tải giả định đã lưu từ backend.
3. Frontend yêu cầu backend tính forecast và hiển thị metric, chart, bảng năm và bảng tháng.
4. Người dùng chỉnh giả định; UI validate và preview forecast.
5. Người dùng quản lý nhiều khoản vay, thêm/sửa/xóa các kỳ trả và bấm lưu lịch trả vào PostgreSQL.
6. Frontend chỉ lưu giả định khi người dùng bấm lưu; lịch trả có thao tác lưu riêng.
7. Người dùng có thể khôi phục bộ giả định demo (bao gồm lịch vay mẫu) hoặc xuất CSV.

## Users and operating model

- Một household / một bộ assumptions dùng chung.
- Local deployment qua Docker Compose là cách chạy được khuyến nghị.
- Chưa có authentication, user account, household tenancy hay cloud deployment.
- Dữ liệu giả định tài chính là dữ liệu nhạy cảm; mặc định vận hành cần hạn chế network exposure.

## In scope

- Chỉnh sửa giả định thu nhập, tăng lương, chi phí, tiền thuê và tiết kiệm ban đầu.
- Quản lý nhiều khoản vay, mỗi khoản có dư nợ gốc ban đầu và lịch trả theo ngày gồm gốc/lãi.
- Forecast 50 tháng, từ Nov 2026 đến Dec 2030; tổng hợp 5 năm (2026 là năm không đầy đủ).
- Reset dữ liệu về seed defaults và export CSV.

## Out of scope (current product baseline)

- Kết nối ngân hàng, thanh toán, đầu tư, thuế hoặc tư vấn tài chính.
- Nhiều người dùng/hộ gia đình, đăng nhập và phân quyền.
- Scenario management, so sánh nhiều kịch bản hoặc lưu lịch sử scenario.
- Cloud hosting hoặc public internet exposure.

## Terminology

| Term | Meaning |
|---|---|
| Assumptions | Các input do người dùng cấu hình để tính forecast |
| Forecast | Kết quả dự báo theo tháng và tổng hợp theo năm |
| Repayment entry | Một kỳ trả nợ có ngày đến hạn, tiền gốc và tiền lãi |
| Seed defaults | Bộ giả định và lịch khoản vay mẫu dùng để khởi tạo/khôi phục |
| Saved assumptions | Bộ giả định đã persist trong PostgreSQL |
| Simulation/preview | Forecast tính từ giả định thu nhập/chi phí đang chỉnh sửa và lịch khoản vay đã lưu |

## Product safety statement

Không đưa số liệu tài chính cá nhân thật vào source code hoặc repository. Seed values, automated test fixtures và ví dụ trong docs phải là dữ liệu tổng hợp/demo. Mọi thay đổi có thể ảnh hưởng đến phép tính phải có test đối chiếu baseline và mô tả rõ thay đổi.
