# Chat AI — Android Kotlin

Ứng dụng chat AI Android viết bằng Kotlin, giao diện tối lấy cảm hứng từ kiểu bố cục ChatGPT. Đây là dự án độc lập, **không phải ứng dụng chính thức của OpenAI**.

## Tính năng

- Giao diện chat tối, bong bóng tin nhắn và nhập liệu nhiều dòng.
- Gọi Chat Completions API qua HTTPS; mặc định model `gpt-4o-mini`.
- Cấu hình API key, model, endpoint và system prompt trong màn hình ⚙.
- Lưu lịch sử hội thoại cục bộ; nút **Xóa lịch sử** trong cài đặt.
- GitHub Actions build APK debug và xuất artifact để tải về.
- Không nhúng API key vào source code.

## Build APK bằng GitHub Actions (khuyên dùng trên Termux)

1. Giải nén ZIP và tạo repository GitHub mới.
2. Trong thư mục `ChatAIKotlin`, chạy:

```bash
git init
git add .
git commit -m "Initial Chat AI Android app"
git branch -M main
git remote add origin https://github.com/USERNAME/REPOSITORY.git
git push -u origin main
```

3. Mở repository trên GitHub → **Actions** → workflow **Build Android APK**. Lần đầu, nếu GitHub hỏi, hãy bật Actions.
4. Chờ workflow thành công → mở run mới nhất → phần **Artifacts** → tải `ChatAI-debug-apk` và giải nén để lấy `app-debug.apk`.
5. Cài APK trên điện thoại. Android có thể yêu cầu cho phép cài ứng dụng từ nguồn đó.

Mỗi lần `git push` lên `main` hoặc `master`, workflow sẽ tự build lại. Có thể build thủ công bằng **Actions → Build Android APK → Run workflow**.

## Termux: giải nén và push

Cài Termux từ nguồn đáng tin cậy, sau đó:

```bash
pkg update && pkg upgrade -y
pkg install git unzip -y
termux-setup-storage
mkdir -p ~/tools
cp ~/storage/downloads/ChatAIKotlin.zip ~/tools/
cd ~/tools
unzip -o ChatAIKotlin.zip
cd ChatAIKotlin

git init
git add .
git commit -m "Initial Chat AI Android app"
git branch -M main
git remote add origin https://github.com/USERNAME/REPOSITORY.git
git push -u origin main
```

Thay `USERNAME` và `REPOSITORY` bằng thông tin repository thật. Nếu Git hỏi mật khẩu HTTPS, dùng GitHub Personal Access Token thay cho mật khẩu tài khoản; không lưu token vào file source.

## Dùng API

1. Cài và mở app → nhấn ⚙.
2. Nhập API key của nhà cung cấp tương thích với OpenAI Chat Completions API. Endpoint mặc định: `https://api.openai.com/v1/chat/completions`.
3. Giữ model mặc định hoặc nhập model mà tài khoản của bạn được phép dùng → **Lưu**.
4. Quay lại và gửi tin nhắn.

ChatGPT Plus/Free không tự động bao gồm API credits. API Platform được tính phí riêng tùy mức sử dụng và tài khoản. Tạo/quản lý key trong trang API Platform chính thức của OpenAI; không gửi key cho người khác.

## Build cục bộ (nếu đã cài Android SDK)

Dự án dùng JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3 và Android SDK 35. Nếu máy đã có Android SDK được cấu hình:

```bash
gradle --version
gradle assembleDebug
```

APK đầu ra: `app/build/outputs/apk/debug/app-debug.apk`.

Build Android trực tiếp trên Termux có thể phức tạp do SDK/build-tools và môi trường Android; GitHub Actions là đường build được cấu hình sẵn trong dự án này.

## Bảo mật và giới hạn

- API key lưu trong SharedPreferences riêng của app; đây là lưu trữ cục bộ cơ bản, không phải kho bí mật chống thiết bị đã root. Không commit API key vào Git.
- APK debug không phải bản phát hành ký release. Trước khi phân phối công khai, nên thêm Android Keystore/EncryptedSharedPreferences, streaming, Markdown/render code blocks, giới hạn sử dụng và kiểm thử kỹ.
- App gửi tối đa 30 tin nhắn gần nhất làm ngữ cảnh và lưu tối đa 100 mục lịch sử trên thiết bị.
- Chỉ sử dụng endpoint HTTPS. Endpoint tùy chỉnh cần là nhà cung cấp bạn tin cậy.
