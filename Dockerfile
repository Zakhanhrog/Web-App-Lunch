# Stage 1: Build ứng dụng Java với Gradle
FROM gradle:8.5.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew war --no-daemon

# Stage 2: Tạo image Tomcat cuối cùng
FROM tomcat:9.0-jdk17-temurin

LABEL maintainer="your-email@example.com"
LABEL version="1.0"
LABEL description="WebAppLunch Application"

ENV APP_UPLOAD_DIR /usr/local/tomcat/lunch-data/images/food
ENV CATALINA_OPTS="-Dport=${PORT:-8080}"

# Chỉ tạo thư mục, user tomcat mặc định của image nên có quyền ghi vào /usr/local/tomcat/
RUN mkdir -p ${APP_UPLOAD_DIR}
RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=builder /app/build/libs/WebAppLunch.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
# CMD ["catalina.sh", "run"]