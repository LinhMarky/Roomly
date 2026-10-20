# Hạ tầng ROOMLY

Hạ tầng phục vụ một backend ROOMLY dùng MySQL và thư mục ảnh local. Có thể chạy backend trực tiếp theo [README backend](../backend/README.md#chạy-local) hoặc dùng Docker Compose dưới đây.

## Nội dung

| Đường dẫn | Mục đích |
| --- | --- |
| [compose.yaml](compose.yaml) | MySQL và backend ROOMLY, volume database/ảnh riêng |
| [roomly/.env.example](roomly/.env.example) | Mẫu cấu hình local và Docker |
| [roomly/init/001_schema.sql](roomly/init/001_schema.sql) | Khởi tạo 14 bảng trong database trống |

Các thư mục `.local` và file cấu hình riêng đã được [.gitignore](../.gitignore) loại khỏi Git. Dữ liệu local cũ được giữ tại vị trí hiện có; cấu hình đường dẫn ảnh/database đúng khi sử dụng lại.

## Chạy ROOMLY bằng Docker

Cần Docker Engine đang chạy, Docker Compose v2 và mạng cho lần tải image/build đầu. Chạy các lệnh PowerShell từ thư mục `Infra`.

### 1. Chuẩn bị môi trường

Với môi trường mới, sao chép file mẫu nếu `roomly/.env` chưa tồn tại:

```powershell
if (Test-Path -LiteralPath roomly/.env) {
    Write-Output 'Giữ cấu hình roomly/.env hiện có.'
} else {
    Copy-Item -LiteralPath roomly/.env.example -Destination roomly/.env
}
```

Sửa placeholder trước khi chạy: `ROOMLY_DB_PASSWORD`, `ROOMLY_MYSQL_ROOT_PASSWORD`, `ROOMLY_JWT_SECRET` và thông tin SMTP nếu cần gửi mail. JWT secret là Base64 của ít nhất 32 byte ngẫu nhiên; xem [cách sinh secret](../backend/README.md#chạy-local).

Các lệnh Compose dùng rõ `--env-file roomly/.env`; cấu hình local ở đường dẫn khác được giữ riêng. Giá trị mặc định của mẫu là database/user `roomly`, API host `18092`, MySQL host `3307`.

### 2. Build và chạy

```powershell
docker compose --env-file roomly/.env config --quiet
docker compose --env-file roomly/.env up --build --wait --wait-timeout 180
docker compose --env-file roomly/.env ps
docker compose --env-file roomly/.env logs --tail 100 backend
Invoke-RestMethod http://127.0.0.1:18092/actuator/health
```

Nếu backend local đang dùng cổng `18092`, dừng bản local hoặc đổi `ROOMLY_HTTP_PORT` trong file môi trường và cập nhật URL kiểm tra/client tương ứng.

| Dịch vụ | Cổng host, bind `127.0.0.1` | Lưu trữ |
| --- | --- | --- |
| Backend ROOMLY | `18092` → container `18092` | Volume `media_data` tại `/app/media` |
| MySQL 8.4 | `3307` → container `3306` | Volume `mysql_data` |

Backend dùng `ROOMLY_DB_SSL_MODE=REQUIRED` để kết nối TLS tới MySQL 8.4 ngay ở lần đăng nhập đầu, kể cả sau khi MySQL restart và cache xác thực bị xóa. `REQUIRED` mã hóa kết nối nhưng không xác minh danh tính chứng chỉ; stack này dùng mạng Docker nội bộ. Kết nối tới database bên ngoài cần cấu hình chứng chỉ và `VERIFY_IDENTITY` theo [hướng dẫn Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).

Backend chờ MySQL healthy trước khi khởi động. Image backend có healthcheck `/actuator/health`, bao gồm kết nối database; `up --wait` chờ cả backend healthy thay vì chỉ chờ process chạy. Backend chạy bằng user `roomly`, kho ảnh nằm trong volume có quyền ghi phù hợp. MySQL chỉ chạy file init khi khởi tạo volume mới; file SQL có `DROP TABLE` và không chứa dữ liệu seed. Thay mật khẩu khởi tạo trong `.env` không tự đổi mật khẩu bên trong volume đã có.

App Android emulator dùng `http://10.0.2.2:18092/api/v1/` với cổng mặc định. Khi đổi cổng host, build lại app với `ROOMLY_API_URL` phù hợp theo [README Android](../Frontend-Mobile-App/README.md#kết-nối-backend).

## Mail và push

Compose chuyển các biến SMTP trong file môi trường cho backend; đặt `ROOMLY_MAIL_ENABLED=true` sau khi cấu hình SMTP hoạt động. Mail bị tắt hoặc gửi lỗi sẽ không làm API trả OTP cho client.

Push trong Compose mặc định tắt. Cấu hình Firebase cho backend local theo [README backend](../backend/README.md#smtp-và-firebase). Khi bổ sung Firebase cho container, mount service account chỉ đọc bên ngoài image và đặt `ROOMLY_FIREBASE_CREDENTIALS` theo đường dẫn bên trong container.

## Kiểm tra

`docker compose ... config --quiet` kiểm tra cấu hình; cần kiểm tra thêm trạng thái container, health endpoint và các luồng API khi stack thực chạy.

Backend có unit test, test khởi động và test MySQL riêng. Xem [hướng dẫn kiểm thử](../backend/README.md#kiểm-thử). Test MySQL dùng schema `roomly_test`/`roomly_ci`, tách khỏi database đang sử dụng.

OpenAPI hiện tại được sinh bởi backend tại `/v3/api-docs`; Swagger UI ở `/swagger-ui/index.html`.

## Dừng và xử lý lỗi

```powershell
docker compose --env-file roomly/.env stop
```

`docker compose --env-file roomly/.env down` xóa container/network và giữ named volume. Cờ `-v` xóa dữ liệu database và ảnh trong volume.

| Hiện tượng | Kiểm tra |
| --- | --- |
| Không kết nối Docker daemon | Mở Docker Engine/Desktop, kiểm tra `docker info` |
| Build chưa tải được dependency/image | Kiểm tra mạng/proxy tới Maven Central và registry, rồi chạy lại `up --build --wait` |
| Backend chưa sẵn sàng | Xem `ps` và `logs --tail 100 backend mysql` với cùng `--env-file` |
| Cổng host bị chiếm | Đổi `ROOMLY_HTTP_PORT` hoặc `ROOMLY_MYSQL_HOST_PORT`, chạy lại `up -d` |
| `Public Key Retrieval is not allowed` | Dùng cấu hình TLS của stack; không đổi `ROOMLY_DB_SSL_MODE` thành `DISABLED` với user `caching_sha2_password` |
| JWT không hợp lệ khi khởi động | Kiểm tra `ROOMLY_JWT_SECRET` là Base64 của ít nhất 32 byte |
| Không thấy ảnh/database cũ | Đối chiếu đúng database và đường dẫn media; stack Docker dùng volume riêng |
