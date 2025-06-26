# ----- Giai đoạn 1: Build dự án -----
# Sử dụng image chứa Gradle và JDK 17, khớp với build.gradle của bạn
FROM gradle:8.5-jdk17-alpine AS builder

# Đặt thư mục làm việc bên trong container
WORKDIR /app

# Sao chép toàn bộ mã nguồn vào thư mục làm việc
COPY . .

# Chạy lệnh build của Gradle để tạo file .war
# --no-daemon được khuyến nghị cho môi trường CI/CD/build
RUN ./gradlew build --no-daemon

# ----- Giai đoạn 2: Tạo image để chạy ứng dụng -----
# Sử dụng image Tomcat 9 chính thức với JDK 17
FROM tomcat:9.0-jdk17-temurin

# Xóa các ứng dụng mặc định của Tomcat để dọn dẹp
RUN rm -rf /usr/local/tomcat/webapps/*

# Sao chép file .war từ giai đoạn build vào thư mục webapps của Tomcat
# Đổi tên thành ROOT.war để ứng dụng chạy tại địa chỉ gốc
# Tên file WebAppLunch.war được lấy từ phân tích build.gradle
COPY --from=builder /app/build/libs/WebAppLunch.war /usr/local/tomcat/webapps/ROOT.war

# Port mặc định của Tomcat là 8080
EXPOSE 8080

# Lệnh để khởi động server Tomcat
CMD ["catalina.sh", "run"]