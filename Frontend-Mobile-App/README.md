# ROOMLY Android

Ứng dụng Android dùng Java, XML, Fragment, ViewModel, LiveData và ViewBinding. App giao tiếp với backend ROOMLY qua HTTP; namespace và application ID là `com.example.roomly`.

## Công cụ

| Thành phần | Cấu hình |
| --- | --- |
| Android SDK | Compile/target `37`; min SDK `24` |
| Gradle Wrapper | `9.5.0` |
| Android Gradle Plugin | `9.3.3` |
| JVM chạy Gradle | JDK `25`, theo [daemon toolchain](gradle/gradle-daemon-jvm.properties) |
| Java source/target | `11` |

Trên Windows, đặt project trong đường dẫn không dấu, ví dụ `D:/Projects/Roomly`. AGP chặn đường dẫn có ký tự ngoài ASCII; bỏ chặn có thể làm JVM worker không tìm thấy test class.

Mở chính thư mục `Frontend-Mobile-App` trong Android Studio. Cài SDK 37 và chọn JDK 25 cho Gradle, sau đó đồng bộ project. Dùng wrapper có sẵn; lần tải dependency đầu cần kết nối mạng.

Các cấu hình build nằm trong [app/build.gradle.kts](app/build.gradle.kts), [version catalog](gradle/libs.versions.toml) và [Gradle Wrapper](gradle/wrapper/gradle-wrapper.properties).

## Kết nối backend

Khởi động API theo [README backend](../backend/README.md#chạy-local). Backend mặc định bind `127.0.0.1:18092`.

### Android emulator

App debug mặc định gọi `http://10.0.2.2:18092/api/v1/`. `10.0.2.2` là địa chỉ emulator dùng để truy cập máy tính chạy backend.

Chọn emulator và bấm Run trong Android Studio, hoặc build từ thư mục này:

```powershell
.\gradlew.bat :app:assembleDebug
```

### Điện thoại qua USB

Bật USB debugging, kết nối thiết bị và kiểm tra bằng `adb devices`. Chuyển tiếp cổng rồi build với URL phù hợp:

```powershell
adb reverse tcp:18092 tcp:18092
.\gradlew.bat -PROOMLY_API_URL=http://127.0.0.1:18092/api/v1/ :app:installDebug
```

`installDebug` cần thiết bị được ADB nhận. Chạy lại `adb reverse` khi kết nối thiết bị được tạo lại.

### Đổi máy chủ API

Truyền thuộc tính Gradle `ROOMLY_API_URL` khi chạy tác vụ build hoặc install:

```powershell
.\gradlew.bat -PROOMLY_API_URL=http://10.0.2.2:18092/api/v1/ :app:assembleDebug
```

Giá trị được đưa vào `BuildConfig.API_BASE_URL`, nên cần build và cài lại app để áp dụng URL mới. Nếu dùng LAN, đặt URL theo IP máy tính và cấu hình `ROOMLY_BIND_ADDRESS` cùng quyền truy cập cổng của backend tương ứng.

## Firebase

App build được khi chưa có `app/google-services.json`; Google Services plugin chỉ được áp dụng khi file này tồn tại.

Để dùng push:

1. Đăng ký Android app có package `com.example.roomly` trong project Firebase của backend.
2. Tải cấu hình Android vào `app/google-services.json`, rồi đồng bộ và build lại.
3. Bật Firebase phía server theo [README backend](../backend/README.md#smtp-và-firebase).
4. Trên Android 13 trở lên, cấp quyền thông báo khi app yêu cầu.

`google-services.json` là cấu hình client local đã được ignore. File service account và khóa Firebase Admin chỉ dùng ở backend.

## Build và kiểm tra

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

macOS/Linux dùng `sh ./gradlew` thay cho `.\gradlew.bat`.

| Kết quả | Đường dẫn |
| --- | --- |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Báo cáo unit test | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Báo cáo lint | `app/build/reports/lint-results-debug.html` |

Để biên dịch hoặc chạy instrumentation test:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` cần thiết bị/emulator đang kết nối và điều kiện dữ liệu phù hợp với kịch bản test. Unit test và lint không thay thế kiểm tra luồng API, mail, push hoặc thanh toán trên thiết bị.

### Gói release

URL release phải dùng HTTPS và trỏ tới máy chủ triển khai. Ví dụ sau cần thay bằng máy chủ của bạn:

```powershell
.\gradlew.bat -PROOMLY_API_URL=https://api.example.com/api/v1/ :app:assembleRelease
```

Build hiện chưa khai báo khóa ký phát hành. Cấu hình signing riêng trước khi phân phối; khóa ký và mật khẩu được giữ ngoài Git theo [.gitignore gốc](../.gitignore).

## Tổ chức source

| Thư mục trong `app/src/main` | Trách nhiệm |
| --- | --- |
| `java/com/example/roomly/data/api` | HTTP client và đọc/ghi JSON |
| `java/com/example/roomly/data/model` | Model dữ liệu |
| `java/com/example/roomly/data/repository` | Gọi API và quản lý trạng thái dùng chung |
| `java/com/example/roomly/ui` | Fragment, ViewModel và adapter |
| `java/com/example/roomly/push` | Khởi tạo ứng dụng, Firebase và thông báo |
| `res/layout`, `res/values` | Layout, chuỗi và giao diện |

Các lớp repository còn tiền tố `Demo` đang gọi API ROOMLY qua `ApiClient`; tiền tố này là tên cũ trong source. Không suy ra rằng dữ liệu của các lớp đó được tạo mô phỏng trên thiết bị.

Thanh toán hiện dùng sandbox. Với cọc có tiền, app cần kết quả thanh toán hợp lệ từ backend để hoàn tất phiên `PENDING`.
