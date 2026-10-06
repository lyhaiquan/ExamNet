# Image của máy chủ ExamNet.
#
# Không build Java trong Docker: CI đã chạy `./mvnw verify` và có sẵn jar, ở đây
# chỉ chép jar vào. Không có bước RUN nào nên build cho cả hai kiến trúc
# (amd64 cho DigitalOcean/Azure, arm64 cho Oracle Always Free) gần như tức thì.
#
# Build thử trên máy mình (đứng ở thư mục gốc repo, sau khi đã build jar):
#   docker build -f deploy/Dockerfile --build-arg GIT_SHA=$(git rev-parse HEAD) -t examnet-server .

FROM eclipse-temurin:17-jre

# Mã commit được gài vào biến môi trường để cổng :8080/version trả về đúng bản đang chạy
ARG GIT_SHA=dev
ENV EXAMNET_COMMIT=${GIT_SHA}

WORKDIR /app
COPY source/server/target/examnet-server.jar /app/examnet-server.jar

# Database SQLite nằm trong volume để deploy lại không mất dữ liệu
ENV EXAMNET_DB=/data/examnet.db
VOLUME /data

#      EXP/1.0  WebSocket  trạng thái
EXPOSE 5000     5001       8080

ENTRYPOINT ["java", "-jar", "/app/examnet-server.jar"]
