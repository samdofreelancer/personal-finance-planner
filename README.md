# Kế hoạch tài chính cá nhân 2026–2030

Web app chạy cục bộ để thay đổi giả định, xem dự báo dòng tiền từng tháng, so sánh dòng tiền ròng theo năm và xuất kết quả CSV.

## Chạy ứng dụng

Yêu cầu Python 3. Chạy trong thư mục này:

```powershell
python serve.py
```

Mở `http://127.0.0.1:8765` trong trình duyệt. Dừng server bằng `Ctrl+C`.

Ứng dụng không dùng backend lưu dữ liệu, không tải thư viện từ mạng và chỉ bind vào loopback `127.0.0.1`. Các giả định được lưu trong local storage của trình duyệt hiện tại. Nút **Xuất CSV** tải bảng tổng hợp và dự báo tháng; **Khôi phục mặc định** xóa các thay đổi sau khi xác nhận.

## Kiểm thử công thức

Yêu cầu Node.js 18 trở lên:

```powershell
node --test test/forecast.test.mjs
```

## Giả định ban đầu

Giá trị khởi tạo khớp workbook hiện tại: lương cơ bản 50 triệu, hệ số tháng 1 là 2,5×, tăng lương 10% vào mỗi tháng 4, tiết kiệm ban đầu 220 triệu, lãi kỳ tháng 11/12 theo số ngân hàng và lãi tất toán 3.683.193đ. Đây là giá trị đang lưu trong workbook, không khẳng định rằng lãi tất toán đã được ngân hàng xác nhận. App mặc định để bỏ chọn xác nhận, nên kết quả tháng 01/2027 và các số dư tiết kiệm từ đó là tạm tính; chỉ chọn xác nhận sau khi đối chiếu với ngân hàng. Hệ số và lãi tất toán có thể sửa trực tiếp trong form. Lãi suất danh nghĩa 13,2%/năm không được dùng để ước tính lãi ngân hàng.
