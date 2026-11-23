# Truy cập từ xa bằng Tailscale

## Mục tiêu và kiến trúc

Tailscale tạo một private tailnet giữa máy Windows chạy Docker và các thiết bị được người dùng đăng nhập vào cùng tailnet. Tailscale Serve cung cấp HTTPS với hostname MagicDNS, nhưng chỉ các thiết bị/người dùng được phép trong tailnet mới truy cập được.

```text
Điện thoại / Laptop (Tailscale)
          |
          | encrypted tailnet + HTTPS
          v
Windows host (Tailscale Serve)
          |
          | http://127.0.0.1:80
          v
Docker Nginx -> Spring Boot -> PostgreSQL
                         |
                         +-> Redis (HTTP sessions)
```

- Không cần domain riêng, hosting, IP tĩnh hoặc port forwarding trên router.
- PostgreSQL, Redis và Spring Boot chỉ nằm trong Docker network; không publish port ra host.
- Nginx chỉ publish trên `127.0.0.1:80`, không mở trực tiếp cho thiết bị LAN.
- Không bật Tailscale Funnel. Funnel là chế độ public Internet, không phù hợp với ứng dụng tài chính cá nhân.
- Máy Windows phải bật, có Internet và Docker/Tailscale đang chạy thì mới truy cập được.

## 1. Cài Tailscale trên máy chạy ứng dụng

1. Tải Tailscale cho Windows từ [tailscale.com/download](https://tailscale.com/download).
2. Cài đặt và đăng nhập bằng tài khoản của bạn.
3. Đặt tên thiết bị dễ nhận biết, ví dụ `finance-planner-pc`, trong Tailscale admin console nếu cần.
4. Mở PowerShell và xác nhận:

```powershell
tailscale status
```

## 2. Giới hạn Docker chỉ nghe trên localhost

Production Compose đã bind cổng Nginx theo cấu hình:

```yaml
ports:
  - "127.0.0.1:80:80"
```

Áp dụng cấu hình:

```powershell
docker compose up -d
```

Kiểm tra ứng dụng vẫn chạy trên máy host:

```powershell
curl.exe -I http://127.0.0.1
```

Kết quả mong đợi: HTTP `200` hoặc redirect hợp lệ. Không dùng `docker compose down -v`; lệnh đó sẽ xóa volume database.

## 3. Bật HTTPS riêng tư bằng Tailscale Serve

Mở PowerShell trên Windows host. Chạy (nếu CLI yêu cầu quyền nâng cao, chấp thuận prompt của Windows):

```powershell
tailscale serve --bg 80
```

Nếu Tailscale yêu cầu bật HTTPS certificates/MagicDNS, mở URL được hiển thị và xác nhận trong Tailscale admin console. Sau đó chạy lại lệnh trên nếu cần.

Xem URL và trạng thái:

```powershell
tailscale serve status
```

Tailscale sẽ hiển thị URL dạng:

```text
https://finance-planner-pc.<tailnet-name>.ts.net
```

Đây là hostname do Tailscale cấp, không phải domain bạn phải mua. Giữ cấu hình Serve ở chế độ private; không chạy `tailscale funnel`.

## 4. Kết nối điện thoại/laptop

1. Cài Tailscale trên điện thoại/laptop cần truy cập.
2. Đăng nhập cùng tài khoản Tailscale (hoặc mời tài khoản khác vào tailnet nếu chủ động chia sẻ).
3. Bật kết nối Tailscale trên thiết bị.
4. Mở URL HTTPS được trả về bởi `tailscale serve status`.

Kiểm thử cả khi điện thoại dùng 4G/5G, đã tắt Wi-Fi. Nếu truy cập được, luồng remote access đã hoạt động qua Internet chứ không phụ thuộc LAN.

## 5. Vận hành và bảo mật

- Giữ Windows host bật và không để máy ngủ khi cần truy cập từ xa.
- Giới hạn thành viên/thiết bị trong tailnet; không chia sẻ URL như một website public.
- Ứng dụng có đăng nhập/đăng ký riêng theo từng household. Tailscale giới hạn lớp mạng, không thay thế authentication/authorization ở cấp ứng dụng. Chỉ thêm thiết bị/người dùng đáng tin cậy vào tailnet.
- Không publish cổng `8080` (backend) hoặc `5432` (PostgreSQL).
- Redis chỉ truy cập được từ Docker network; không publish cổng `6379`. Production session cookie mặc định bật `Secure`, `HttpOnly`, `SameSite=Lax`; chỉ development overlay mới tắt `Secure` cho HTTP localhost.
- Không commit Tailscale auth key, token hoặc secret vào Git.
- Sao lưu PostgreSQL định kỳ; Tailscale không phải giải pháp backup.

## 6. Tắt truy cập từ xa

Trên Windows host:

```powershell
tailscale serve reset
```

Muốn ngắt thiết bị khỏi tailnet, dùng Tailscale app hoặc admin console để disconnect/remove thiết bị.

## Troubleshooting

- `tailscale serve` báo chưa bật HTTPS: làm theo URL consent mà CLI in ra để bật MagicDNS và HTTPS certificates.
- Không truy cập được từ điện thoại: kiểm tra cả hai thiết bị đều `Connected` trong Tailscale, sau đó kiểm tra `tailscale serve status` trên host.
- URL không hoạt động: kiểm tra `curl.exe -I http://127.0.0.1` trên host và `docker compose ps`.
- Sau khi reboot: bảo đảm Docker Desktop/engine và Tailscale tự khởi động; Serve được cấu hình `--bg` sẽ tự tiếp tục khi Tailscale khởi động lại.
