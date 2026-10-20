# ROOMLY backend

API dùng Spring Boot 3.3.2, Java 17, JDBC và MySQL. Entry point duy nhất là [RoomlyApplication](src/main/java/com/roomly/RoomlyApplication.java), package `com.roomly`. Ảnh được lưu tại thư mục cấu hình qua `ROOMLY_MEDIA_DIRECTORY`.

Các lệnh dưới đây chạy từ thư mục `backend`, với `JAVA_HOME` trỏ tới JDK 17. Kiểm tra bằng `java -version` và `.\mvnw.cmd -v`.

## Chạy local

### 1. Chuẩn bị database

Tạo database trống trong MySQL 8 và user có quyền trên database đó. [001_schema.sql](../Infra/roomly/init/001_schema.sql) chứa DDL cho 14 bảng, có `DROP TABLE` và không có dữ liệu seed; chỉ nhập vào database trống dành cho lần khởi tạo này.

Mở MySQL client từ thư mục `backend`:

```powershell
mysql -h 127.0.0.1 -P 3306 -u root -p
```

Trong MySQL client:

```sql
CREATE DATABASE roomly CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE roomly;
SOURCE ../Infra/roomly/init/001_schema.sql;

CREATE USER 'roomly'@'127.0.0.1' IDENTIFIED BY 'replace-with-local-database-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON roomly.* TO 'roomly'@'127.0.0.1';
```

Thay mật khẩu placeholder trong lệnh `CREATE USER` bằng mật khẩu riêng và dùng cùng giá trị ở `ROOMLY_DB_PASSWORD`. User ứng dụng chỉ cần quyền đọc/ghi; việc tạo schema do tài khoản quản trị thực hiện. Với host kết nối khác, tạo user có host tương ứng.

Nếu dùng database đã có, giữ dữ liệu và tên database hiện tại rồi khai báo đúng ở bước cấu hình. Schema không tạo tài khoản admin hay người dùng mặc định.

### 2. Cấu hình môi trường

Tham khảo [Infra/roomly/.env.example](../Infra/roomly/.env.example) và [application.properties](src/main/resources/application.properties):

| Biến | Nội dung |
| --- | --- |
| `ROOMLY_DB_HOST` / `ROOMLY_DB_PORT` | Máy MySQL và cổng; mặc định `127.0.0.1:3306` |
| `ROOMLY_DATABASE` / `ROOMLY_DB_USER` | Tên database và user đang dùng; mẫu mới là `roomly` |
| `ROOMLY_DB_SSL_MODE` | Mặc định `PREFERRED`; Compose dùng `REQUIRED` cho MySQL 8.4 |
| `ROOMLY_DB_PASSWORD` | Mật khẩu MySQL |
| `ROOMLY_JWT_SECRET` | Base64 của ít nhất 32 byte ngẫu nhiên |
| `ROOMLY_BIND_ADDRESS` / `ROOMLY_HTTP_PORT` | Mặc định `127.0.0.1` / `18092` |
| `ROOMLY_MEDIA_DIRECTORY` | Kho ảnh; nên dùng đường dẫn tuyệt đối tới thư mục đang có |
| `ROOMLY_MAIL_ENABLED` / `ROOMLY_PUSH_ENABLED` | Mặc định `false` / `false` |
| `ROOMLY_MOCK_PAYMENT_ENABLED` | Mặc định `true`; thanh toán sandbox |

Ví dụ cho database mới; sửa host, port, database và user trước khi chạy:

```powershell
$env:ROOMLY_DB_HOST = '127.0.0.1'
$env:ROOMLY_DB_PORT = '3306'
$env:ROOMLY_DB_SSL_MODE = 'PREFERRED'
$env:ROOMLY_DATABASE = 'roomly'
$env:ROOMLY_DB_USER = 'roomly'
$env:ROOMLY_DB_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Mật khẩu MySQL' -AsSecureString)).Password
$env:ROOMLY_MEDIA_DIRECTORY = [IO.Path]::GetFullPath((Join-Path (Get-Location).Path '../Infra/roomly/.local/media'))
```

Tạo secret cho lần thiết lập đầu:

```powershell
$jwtBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($jwtBytes) } finally { $rng.Dispose() }
$env:ROOMLY_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

Lưu secret trong cấu hình local và dùng lại khi khởi động lại; thay secret sẽ làm token đã cấp không còn hợp lệ. Khi dùng dữ liệu cũ, giữ secret và đường dẫn ảnh đang dùng.

Các biến `STUDENT14_*` trước đây vẫn được nhận khi biến `ROOMLY_*` tương ứng chưa có; giá trị mặc định cũ cho database/user/media được giữ để tương thích. Đặt các biến `ROOMLY_*` rõ ràng khi thiết lập môi trường mới.

Dependency `spring-dotenv` hỗ trợ `.env` theo thư mục làm việc. File mẫu trong `Infra/roomly` không tự được nạp từ đó; ví dụ này dùng môi trường terminal/IDE.

### 3. Build và chạy

```powershell
.\mvnw.cmd clean verify
java -jar target/roomly.jar
```

`clean` loại bỏ class cũ trước khi đóng gói. Không cần chọn profile. Kiểm tra từ terminal khác:

```powershell
Invoke-RestMethod http://127.0.0.1:18092/actuator/health
```

## SMTP và Firebase

Để gửi mã xác minh/khôi phục tài khoản, đặt `ROOMLY_MAIL_ENABLED=true`, rồi cấu hình `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_SSL` và `MAIL_FROM` theo nhà cung cấp SMTP. File mẫu dùng port `587`, STARTTLS và xác thực.

Mail bị tắt hoặc SMTP gửi thất bại sẽ làm API yêu cầu mã trả lỗi; OTP không được trả cho client.

Để bật push, đặt `ROOMLY_PUSH_ENABLED=true`, `ROOMLY_FIREBASE_PROJECT_ID` và `ROOMLY_FIREBASE_CREDENTIALS`. Credential phải trỏ tới file service account đọc được bên ngoài repository. Android cần dùng cùng project Firebase; xem [README Android](../Frontend-Mobile-App/README.md#firebase).

## Tài khoản và phiên đăng nhập

Schema không seed tài khoản hoặc mật khẩu. Đăng ký tài khoản qua app/API và xác minh email bằng SMTP đã cấu hình. Để cấp quyền quản trị cho đúng tài khoản trong database của bạn, người quản trị database thực hiện:

```sql
UPDATE users SET is_admin = 1 WHERE email = 'admin@example.invalid';
SELECT id, email, email_verified, is_admin FROM users WHERE email = 'admin@example.invalid';
```

Thay email mẫu bằng tài khoản đã đăng ký; không đưa tài khoản hoặc mật khẩu thật vào source. Vai trò được kiểm tra từ database trong mỗi yêu cầu nghiệp vụ.

Access JWT có hiệu lực 15 phút. Refresh token có hiệu lực 30 ngày và được đổi sau mỗi lần sử dụng. Đăng xuất/đổi mật khẩu thu hồi refresh token; access JWT đã cấp còn hiệu lực đến hạn của nó nếu tài khoản vẫn hoạt động.

## Docker

Xem [README Infra](../Infra/README.md#chạy-roomly-bằng-docker). Kết nối MySQL trong Compose bắt buộc TLS để hoạt động ngay ở lần đăng nhập đầu với `caching_sha2_password`. Image dùng JDK/JRE 21, biên dịch Java target 17 và chạy cùng `roomly.jar`. MySQL và kho ảnh của stack được lưu trong named volume riêng.

## Kiểm thử

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

macOS/Linux dùng `sh ./mvnw`. Báo cáo nằm tại `target/surefire-reports`.

[RoomlyApplicationTest](src/test/java/com/roomly/RoomlyApplicationTest.java) kiểm tra ứng dụng khởi động mặc định, resource tiện ích, bảo vệ API và OpenAPI với H2. Test này không thay thế kiểm chứng SQL MySQL.

[RoomlyMysqlIntegrationTest](src/test/java/com/roomly/RoomlyMysqlIntegrationTest.java) chỉ chạy khi có `ROOMLY_TEST_MYSQL_URL`. Nó nhập lại schema và kiểm tra API với MySQL thật; chỉ chấp nhận database có tên `roomly_test`, `roomly_ci` hoặc tên có hậu tố tương ứng như `roomly_test_local`.

Chuẩn bị database kiểm thử trống, rồi đặt `ROOMLY_TEST_MYSQL_URL`, `ROOMLY_TEST_MYSQL_USER` và `ROOMLY_TEST_MYSQL_PASSWORD` trước khi chạy `verify`. Database đó có thể bị xóa/tạo lại bảng; dùng môi trường kiểm thử riêng. [Workflow CI](../.github/workflows/backend.yml) tạo MySQL service với database `roomly_ci`.

## Source và API

- [controller](src/main/java/com/roomly/controller): HTTP endpoint dưới `/api/v1`.
- [dto](src/main/java/com/roomly/dto): request/response và validation.
- [service](src/main/java/com/roomly/service): nghiệp vụ, transaction, email và push.
- [repository](src/main/java/com/roomly/repository): truy vấn JDBC và khóa dữ liệu.
- [config](src/main/java/com/roomly/config): JWT/security và Firebase.

OpenAPI được sinh từ backend tại `/v3/api-docs`; Swagger UI ở `/swagger-ui/index.html`. Thanh toán hiện là sandbox; gateway thật cần tích hợp và kiểm chứng riêng.
