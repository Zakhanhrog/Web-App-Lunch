# Stage 1: Build ứng dụng Java với Gradle
FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /app
# Copy toàn bộ source code vào thư mục /app trong image builder
COPY . .
# Chạy lệnh Gradle để build file WAR
# Đảm bảo bạn có gradlew (Gradle Wrapper) trong project, nếu không dùng 'gradle war'
RUN ./gradlew war --no-daemon

# Stage 2: Tạo image Tomcat cuối cùng
FROM tomcat:9.0-jdk17-temurin

LABEL maintainer="your-email@example.com"
LABEL version="1.0"
LABEL description="WebAppLunch Application"

ENV APP_UPLOAD_DIR /usr/local/tomcat/lunch-data/images/food
ENV CATALINA_OPTS="-Dport=${PORT:-8080}"

RUN mkdir -p ${APP_UPLOAD_DIR} && chown -R tomcat:tomcat ${APP_UPLOAD_DIR}
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy file WAR từ stage 'builder'
# Đường dẫn trong stage builder sẽ là /app/build/libs/WebAppLunch.war
COPY --from=builder /app/build/libs/WebAppLunch.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
# CMD ["catalina.sh", "run"]