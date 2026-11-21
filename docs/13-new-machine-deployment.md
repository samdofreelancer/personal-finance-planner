# Triển khai Personal Finance Planner trên máy Windows mới

Tài liệu này hướng dẫn chuyển ứng dụng từ laptop hiện tại sang một PC Windows khác, giữ nguyên dữ liệu PostgreSQL và thiết lập truy cập riêng tư qua Tailscale. Quy trình áp dụng cho production Compose, không phải môi trường development/E2E.

> **Nguyên tắc an toàn:** không xóa volume database trên máy nguồn. Backup phải được tạo và kiểm tra trước khi dừng/chuyển dữ liệu. Chỉ restore vào database đích mới hoặc database đã được xác nhận có thể ghi đè.

## 1. Mô hình triển khai

```text
Phone / Laptop (Tailscale client)
          | private HTTPS tailnet
          v
Windows PC (Tailscale Serve)
          | http://127.0.0.1:80
          v
Docker Compose
  Nginx frontend -> Spring Boot backend -> PostgreSQL volume
```

- Frontend chỉ bind `127.0.0.1:80` trên host.
- Backend `8080` và PostgreSQL `5432` chỉ truy cập trong Docker network.
- Tailscale Serve là lối truy cập từ xa; không bật Funnel, không port-forward router.
- PostgreSQL volume trên laptop và PC là hai bản độc lập. Clone Git không chuyển dữ liệu.

## 2. Chuẩn bị PC mới

Trên PC Windows:

1. Cài Windows Update và bật virtualization trong BIOS/UEFI nếu WSL2 yêu cầu.
2. Cài Git for Windows.
3. Cài Docker Engine theo một trong hai lựa chọn:
   - **Docker Desktop (đơn giản hơn):** cài Docker Desktop, bật WSL2 backend và cấu hình tự khởi động khi đăng nhập Windows.
   - **Docker Engine tự quản lý trong WSL2 (nâng cao):** cài một Linux distro, Docker Engine và Docker Compose plugin; bảo đảm lệnh `docker` từ PowerShell kết nối đúng daemon. Không giả định script startup của laptop hiện tại dùng được nguyên trạng trên PC.
4. Cài Tailscale cho Windows từ [tailscale.com/download](https://tailscale.com/download), đăng nhập đúng tailnet.
5. Mở PowerShell mới và xác minh:

```powershell
git --version
docker version
docker compose version
tailscale status
```

`docker version` phải hiển thị cả Client và Server. Nếu chỉ có Client hoặc báo không kết nối được daemon, xử lý Docker trước khi tiếp tục.

## 3. Lấy source code và cấu hình môi trường

Chọn branch/commit đã được xác nhận là phiên bản muốn triển khai. Không deploy từ working tree có thay đổi chưa commit.

```powershell
git clone https://github.com/samdofreelancer/personal-finance-planner.git
Set-Location personal-finance-planner
git switch develop
git pull --ff-only
Copy-Item .env.example .env
notepad .env
```

Trong `.env`, đặt `POSTGRES_PASSWORD` thành mật khẩu mạnh, riêng cho deployment này. Không dùng giá trị placeholder trong `.env.example`; không commit `.env`, gửi mật khẩu qua chat, hoặc ghi secret vào tài liệu. Các biến `SONAR_*` chỉ cần nếu cài SonarQube riêng.

Kiểm tra `.env` tồn tại và Compose đọc được cấu hình:

```powershell
docker compose config --quiet
```

### Tự động hóa setup bằng PowerShell

Sau khi đã cài Git, Docker Engine/Compose và (nếu dùng) Tailscale, có thể chạy script từ thư mục repository với PowerShell:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\setup-new-machine.ps1 -ConfigureTailscale -ConfigureAutoStart
```

Script sẽ kiểm tra Docker/Git, tạo `.env` nếu chưa có (tự sinh mật khẩu PostgreSQL ngẫu nhiên), kiểm tra Compose, build và chạy stack, đợi API trả HTTP 200, cấu hình Tailscale Serve và đăng ký Scheduled Task khi có các switch tương ứng. Nếu `.env` đã tồn tại, script giữ nguyên và từ chối tiếp tục nếu vẫn dùng mật khẩu mẫu.

Các chế độ hữu ích:

```powershell
# Chỉ kiểm tra/chuẩn bị và deploy; không cấu hình Tailscale hoặc Scheduled Task
.\scripts\setup-new-machine.ps1

# Bỏ build image, chỉ tạo/chạy containers
.\scripts\setup-new-machine.ps1 -SkipBuild
```

Script không tự cài Windows features, Git, Docker hoặc Tailscale; không backup/restore database; không xóa volume; không bật Tailscale Funnel. `-ConfigureAutoStart` đăng ký task chạy `scripts/start-compose-stack.ps1` sau khi user đăng nhập. Docker Engine vẫn phải được cấu hình tự khởi động riêng (ví dụ bật Docker Desktop startup hoặc cấu hình daemon WSL2). Với Docker Engine WSL2 tự quản lý, không dùng `start-stack.ps1` cũ trừ khi distro và hook của máy mới đã được cấu hình tương ứng.

## 4. Backup database trên laptop nguồn

Thực hiện trên laptop đang giữ dữ liệu mới nhất. Dùng cùng phiên bản source/schema đã được kiểm thử với bản triển khai đích.

### 4.1. Tạm dừng ghi dữ liệu

Thông báo cho người dùng ngừng thao tác trên app, sau đó dừng frontend và backend nhưng giữ database chạy:

```powershell
docker compose stop frontend backend
docker compose ps
```

Không chạy `docker compose down -v` và không xóa volume `finance_planner_db_data`.

### 4.2. Tạo custom-format backup

Chạy từ thư mục repository trên laptop nguồn:

```powershell
$backup = Join-Path $PWD 'planner-transfer.dump'
docker exec finance-planner-db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/planner-transfer.dump'
if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed' }
docker cp finance-planner-db:/tmp/planner-transfer.dump $backup
docker exec finance-planner-db rm -f /tmp/planner-transfer.dump
if (-not (Test-Path $backup) -or (Get-Item $backup).Length -eq 0) { throw 'Backup file missing or empty' }
Get-FileHash $backup -Algorithm SHA256
```

Lưu file dump và checksum ở nơi an toàn, mã hóa khi chuyển qua USB/cloud. File backup chứa dữ liệu tài chính hộ gia đình; không commit vào Git. Sau khi xác nhận backup đã an toàn, có thể khởi động lại app nguồn nếu vẫn cần dùng laptop:

```powershell
docker compose up -d
```

### 4.3. Chuyển backup sang PC

Chép `planner-transfer.dump` sang thư mục tạm riêng trên PC (ví dụ `C:\SecureTransfer\`). Xác minh checksum trên PC khớp checksum laptop:

```powershell
Get-FileHash C:\SecureTransfer\planner-transfer.dump -Algorithm SHA256
```

Không tiếp tục nếu checksum khác nhau.

## 5. Khởi tạo database và restore trên PC

Thực hiện trong repository trên PC mới. Bước này giả định PC mới chưa có dữ liệu cần giữ trong volume của ứng dụng.

### 5.1. Khởi động riêng PostgreSQL

```powershell
docker compose up -d db
docker compose ps db
```

Chờ trạng thái `healthy`:

```powershell
$deadline = (Get-Date).AddMinutes(2)
do {
    $state = docker inspect --format '{{.State.Health.Status}}' finance-planner-db 2>$null
    if ($state -eq 'healthy') { break }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)
if ($state -ne 'healthy') { throw 'PostgreSQL did not become healthy' }
```

### 5.2. Restore backup

Copy dump vào container rồi restore. Thông tin database/user được lấy từ biến môi trường của container, không hardcode mật khẩu vào lệnh:

```powershell
docker cp C:\SecureTransfer\planner-transfer.dump finance-planner-db:/tmp/planner-transfer.dump
docker exec finance-planner-db sh -c 'pg_restore --no-owner --role="$POSTGRES_USER" -U "$POSTGRES_USER" -d "$POSTGRES_DB" /tmp/planner-transfer.dump'
if ($LASTEXITCODE -ne 0) { throw 'pg_restore failed; inspect the restore output before proceeding' }
docker exec finance-planner-db rm -f /tmp/planner-transfer.dump
```

Lệnh trên dành cho database đích mới, rỗng. **Không chạy lặp lại trên database đã có dữ liệu**: restore lần hai có thể báo object đã tồn tại hoặc tạo trạng thái không mong muốn. Nếu restore thất bại, dừng tại đây và điều tra; không xóa volume theo phản xạ.

Kiểm tra database có dữ liệu và schema hợp lệ trước khi bật backend:

```powershell
docker exec finance-planner-db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\\dt"'
```

Đối chiếu các bảng/migration với phiên bản ứng dụng nguồn. Chỉ triển khai backend từ commit tương thích với schema được backup; không dùng code cũ hơn để khởi động trên schema mới hơn.

## 6. Build và chạy ứng dụng

```powershell
docker compose up -d --build
docker compose ps
```

Kiểm tra local:

```powershell
curl.exe -I http://127.0.0.1/
curl.exe -i http://127.0.0.1/api/assumptions
```

Frontend và API phải trả HTTP thành công. Mở giao diện, kiểm tra các giả định/dữ liệu đã restore và thực hiện một thao tác đọc trước khi cho phép ghi dữ liệu mới. Không gọi endpoint seed/reset để “kiểm tra” vì thao tác đó có thể thay đổi dữ liệu.

## 7. Cấu hình Tailscale Serve trên PC

1. Xác nhận PC đang online trong đúng tailnet:

```powershell
tailscale status
```

2. Cấu hình Serve proxy tới frontend loopback:

```powershell
tailscale serve --bg 80
tailscale serve status
```

3. Nếu Tailscale yêu cầu bật MagicDNS/HTTPS certificates, làm theo hướng dẫn được CLI hiển thị trong admin console.
4. Ghi lại hostname HTTPS mới do `tailscale serve status` cung cấp. Hostname của PC có thể khác laptop; không sao chép hostname laptop sang PC.
5. Không chạy `tailscale funnel` và không chạy `tailscale serve reset` trong quá trình setup thông thường.
6. Trên điện thoại, bật Tailscale cùng tailnet và mở hostname HTTPS mới. Thử bằng mạng di động để xác minh không phụ thuộc Wi-Fi/LAN.

Hướng dẫn chi tiết: [11 — Remote access with Tailscale](11-remote-access-tailscale.md).

## 8. Thiết lập tự khởi động sau reboot

Mục tiêu là sau khi Windows đăng nhập, Docker Engine sẵn sàng, Compose stack chạy, và Tailscale Serve tiếp tục phục vụ HTTPS.

### Docker Desktop

- Bật tùy chọn khởi động Docker Desktop khi đăng nhập Windows.
- Giữ restart policy `unless-stopped` của ba service trong `docker-compose.yml`.
- Đăng nhập Windows và xác minh `docker info`, `docker compose ps`.

### Docker Engine tự quản lý trong WSL2

- Cấu hình distro và Docker daemon tự khởi động theo quy trình quản trị Linux của PC đó.
- Script `scripts/start-stack.ps1` hiện tại được viết cho máy đã có distro `Ubuntu`, hook `/usr/local/sbin/docker-boot`, và lệnh WSL tương ứng. Đây không phải installer tổng quát; chỉ đăng ký Scheduled Task chạy script sau khi đã cài đặt/kiểm thử các prerequisite này trên PC mới.
- Có thể dùng Task Scheduler chạy `powershell.exe -NoProfile -ExecutionPolicy Bypass -File "<repo-path>\scripts\start-stack.ps1"` khi user đăng nhập. Cấu hình delay phù hợp để WSL/Docker sẵn sàng.
- Log của script nằm tại `%LOCALAPPDATA%\personal-finance-planner\logs\start-stack.log`.

### Tailscale

- Xác nhận Windows service Tailscale có Startup Type `Automatic` và đang `Running`.
- Serve được cấu hình bằng `--bg`; xác nhận cấu hình còn tồn tại sau khi restart Tailscale/Windows bằng `tailscale serve status`.

## 9. Checklist nghiệm thu

- [ ] `docker version` hiển thị Client và Server.
- [ ] `docker compose config --quiet` thành công.
- [ ] Database container `healthy`.
- [ ] Frontend và backend container `Up`.
- [ ] `http://127.0.0.1/` trả HTTP 200.
- [ ] `http://127.0.0.1/api/assumptions` trả HTTP 200 và dữ liệu khớp nguồn.
- [ ] Backend `8080` và PostgreSQL `5432` không publish ra host/LAN.
- [ ] `tailscale status` cho thấy PC connected.
- [ ] `tailscale serve status` hiển thị HTTPS proxy tới `127.0.0.1:80`.
- [ ] Điện thoại truy cập được qua HTTPS khi dùng 4G/5G và bật Tailscale.
- [ ] Reboot PC, đăng nhập Windows, chờ startup hoàn tất rồi lặp lại các kiểm tra local/remote.
- [ ] Nếu dùng Scheduled Task, xác nhận `PersonalFinancePlanner-Compose-Stack` đã đăng ký và `start-compose-stack.log` không có lỗi.
- [ ] Backup gốc vẫn được lưu an toàn; đã xác minh checksum.

## 10. Chuyển quyền vận hành từ laptop sang PC

Chỉ chuyển hẳn sau khi PC vượt qua checklist và dữ liệu đã được đối chiếu. Cập nhật bookmark/shortcut sang hostname Tailscale mới. Giữ laptop và backup ở trạng thái an toàn trong giai đoạn xác nhận; không xóa volume nguồn. Khi PC đã là host chính, lập lịch backup PostgreSQL định kỳ và thử restore định kỳ.

## Troubleshooting

| Triệu chứng | Kiểm tra |
|---|---|
| `docker info` không kết nối được | Docker Desktop/WSL distro/daemon chưa chạy hoặc CLI đang trỏ nhầm context. |
| `docker compose config` báo thiếu password | Kiểm tra `.env` ở đúng thư mục và `POSTGRES_PASSWORD` không còn placeholder. |
| DB không healthy | `docker compose logs db`; kiểm tra cấu hình DB và quyền truy cập volume. |
| Restore báo role/database không tồn tại | `.env`/container DB không khớp user/database trong cấu hình; kiểm tra lại trước khi restore. |
| API chưa sẵn sàng | `docker compose logs backend`; kiểm tra migration/schema và `docker compose ps`. |
| Local chạy nhưng Tailscale không vào được | Kiểm tra `tailscale status`, `tailscale serve status`, và `curl.exe -I http://127.0.0.1/`. |
| Sau reboot app chưa chạy | Kiểm tra Docker startup, Scheduled Task (nếu dùng WSL tự quản lý), log `start-stack.log`, và Tailscale Windows service. |

## Tài liệu liên quan

- [README](../README.md)
- [Development & verification](06-development-guide.md)
- [Remote access with Tailscale](11-remote-access-tailscale.md)
- [Tailscale network flow](12-tailscale-network-flow.md)
