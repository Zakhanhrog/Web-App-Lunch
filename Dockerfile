# Sử dụng base image Tomcat 9 với OpenJDK 17
FROM tomcat:9.0-jdk17-temurin

# Label (tùy chọn)
LABEL maintainer="your-email@example.com"
LABEL version="1.0"
LABEL description="WebAppLunch Application"

# Biến môi trường cho đường dẫn upload ảnh bên trong container
# Giá trị này có thể được ghi đè bởi biến môi trường APP_UPLOAD_FOOD_IMAGE_DIR trên Railway
ENV APP_UPLOAD_DIR /usr/local/tomcat/lunch-data/images/food
# Biến môi trường cho cổng Tomcat (Railway sẽ dùng biến PORT)
ENV CATALINA_OPTS="-Dport=${PORT:-8080}"

# Tạo thư mục upload ảnh và cấp quyền (nếu cần)
RUN mkdir -p ${APP_UPLOAD_DIR} && chown -R tomcat:tomcat ${APP_UPLOAD_DIR}

# Xóa các ứng dụng web mặc định của Tomcat để tiết kiệm dung lượng (tùy chọn)
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file WAR đã được build vào thư mục webapps của Tomcat
# Tên file WAR phải khớp với output từ build.gradle (WebAppLunch.war)
COPY build/libs/WebAppLunch.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat sẽ tự động giải nén ROOT.war vào thư mục ROOT khi khởi động

# Expose cổng mà Tomcat sẽ lắng nghe (Railway sẽ tự động map port)
# Giá trị này nên khớp với cổng bạn mong muốn Tomcat chạy trên đó bên trong container
EXPOSE 8080

# Lệnh để khởi động Tomcat khi container chạy
# CMD ["catalina.sh", "run"] được kế thừa từ base image tomcat