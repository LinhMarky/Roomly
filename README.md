# ROOMLY

Ứng dụng Android tìm phòng trọ với backend Spring Boot và MySQL. Người thuê có thể tìm tin, đặt lịch xem phòng, gửi yêu cầu thuê, đặt cọc và nhắn tin. Chủ trọ quản lý phòng, tin đăng và yêu cầu thuê; quản trị viên duyệt và xử lý nội dung theo quyền tài khoản.

## Cấu trúc

| Thư mục | Nội dung |
| --- | --- |
| [Frontend-Mobile-App](Frontend-Mobile-App/README.md) | Android Java/XML, package `com.example.roomly` |
| [backend](backend/README.md) | API ROOMLY, package `com.roomly`, JDBC và kho ảnh local |
| [Infra](Infra/README.md) | Docker Compose ROOMLY và cấu hình hạ tầng |
| [Infra/roomly](Infra/roomly/.env.example) | Mẫu môi trường và [schema MySQL](Infra/roomly/init/001_schema.sql) |

## Công cụ

| Thành phần | Yêu cầu theo cấu hình dự án |
| --- | --- |
| Backend local | JDK 17, Maven Wrapper 3.9.9 có sẵn |
| Database | MySQL 8; file schema được xuất từ MySQL 8.0.45 |
| Android | Android Studio, SDK 37, thiết bị/emulator API 24 trở lên |
| Gradle Android | JDK 25 và Gradle Wrapper 9.5.0; Java source/target 11 |
| Docker, khi cần | Docker Engine và Docker Compose v2 |

Trên Windows, đặt project trong đường dẫn không dấu, ví dụ `D:/Projects/Roomly`, để các JVM worker nhận đúng classpath. Dùng JDK riêng cho backend và Gradle Android. Lần tải wrapper/dependency đầu cần kết nối mạng.

## Chạy local

### 1. Backend

Làm theo [README backend](backend/README.md#chạy-local) để tạo database trống, nhập schema và khai báo biến môi trường. Sau khi cấu hình, chạy từ `backend`:

```powershell
.\mvnw.cmd clean verify
java -jar target/roomly.jar
```

ROOMLY là ứng dụng mặc định, không cần chọn Maven hoặc Spring profile. API chạy tại `http://127.0.0.1:18092/api/v1/`; kiểm tra ở terminal khác:

```powershell
Invoke-RestMethod http://127.0.0.1:18092/actuator/health
```

### 2. Android

Mở `Frontend-Mobile-App` trong Android Studio, đồng bộ Gradle và chạy bản debug trên emulator. API mặc định là `http://10.0.2.2:18092/api/v1/`.

Xem [README Android](Frontend-Mobile-App/README.md#kết-nối-backend) để dùng thiết bị qua USB, đổi URL hoặc tạo gói release.

### Docker tùy chọn

Làm theo [README Infra](Infra/README.md#chạy-roomly-bằng-docker) để chạy cùng backend ROOMLY với MySQL và volume ảnh riêng.

## Kiểm tra trước khi commit

Từ `backend`, với JDK 17:

```powershell
.\mvnw.cmd clean verify
```

Từ `Frontend-Mobile-App`, với JDK 25:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Backend có unit test, kiểm tra ứng dụng khởi động mặc định và test MySQL có điều kiện. [Workflow CI](.github/workflows/backend.yml) dùng database `roomly_ci` riêng. Hướng dẫn bật test MySQL nằm trong [README backend](backend/README.md#kiểm-thử). Instrumentation Android cần thiết bị/emulator đang kết nối.

## Cấu hình

- Biến chuẩn có tiền tố `ROOMLY_*`; xem [file mẫu](Infra/roomly/.env.example). Backend vẫn nhận biến `STUDENT14_*` cũ khi biến `ROOMLY_*` tương ứng chưa được đặt.
- Khi dùng lại dữ liệu local, đặt database, user, JWT secret và đường dẫn ảnh theo môi trường đang có. Việc đổi package ứng dụng không đổi dữ liệu MySQL hoặc di chuyển ảnh.
- [.gitignore](.gitignore) giữ cấu hình riêng, `.local`, credential, khóa ký, cache và artifact build ngoài Git. Giữ wrapper, file mẫu và schema trong repository.
- Firebase Android dùng `app/google-services.json`; cấu hình theo [README Android](Frontend-Mobile-App/README.md#firebase). Khóa Firebase Admin chỉ nằm ở server.

## Quy ước bảo trì

Dùng UTF-8 và cấu hình thụt lề trong [.editorconfig](.editorconfig): 4 spaces cho Java/XML, 2 spaces cho YAML/TOML. Giữ nguyên JAR và giấy phép của Maven/Gradle Wrapper. Không áp dụng formatter đại trà lên schema SQL đã được đối chiếu.

Comment giải thích lý do ở những chỗ dễ sửa sai: khóa dữ liệu, snapshot giá, token dùng một lần, callback sau khi đổi tài khoản và vòng đời Fragment. Khi sửa logic này, đọc cả service/repository tương ứng và chạy các test liên quan.

Trước khi công khai repository, kiểm tra cả file đang stage lẫn lịch sử commit để loại cấu hình riêng, dữ liệu local và credential. `.gitignore` chỉ ngăn thêm file mới, không xóa dữ liệu trong commit cũ.

## API và phạm vi hiện tại

API có prefix `/api/v1`. Khi backend chạy, xem OpenAPI tại `/v3/api-docs` hoặc Swagger UI tại `/swagger-ui/index.html`. Controller và DTO nằm trong [backend/src/main/java/com/roomly](backend/src/main/java/com/roomly).

Thanh toán đang dùng sandbox, chưa thu tiền thật. Phiên cọc có tiền ở trạng thái `PENDING` cần kết quả thanh toán hợp lệ từ backend; việc gỡ nút mô phỏng trên Android không tự hoàn tất thanh toán.

Mail xác minh cần SMTP hoạt động; khi mail bị tắt hoặc gửi lỗi, API không trả OTP cho client. Push cần cấu hình Firebase ở cả Android và backend. Build thành công cần được bổ sung bằng kiểm tra luồng thực tế trên thiết bị trước khi phát hành.
