# Image của server LabCast — chỉ để chạy trên VPS cho nhóm thử phần TCP từ xa.
# Trong phòng máy thật, server chạy thẳng trên máy giáo viên (spec §5.1).
#
# Không build Java trong Docker: CI đã chạy `./mvnw verify` và có sẵn jar, ở đây
# chỉ chép jar vào. Không có bước RUN nào nên build cho cả hai kiến trúc
# (amd64 cho DigitalOcean/Azure, arm64 cho Oracle Always Free) gần như tức thì.
#
# Build thử trên máy mình (đứng ở thư mục gốc repo, sau khi đã build jar):
#   docker build -f deploy/Dockerfile --build-arg GIT_SHA=$(git rev-parse HEAD) -t labcast-server .

FROM eclipse-temurin:17-jre

# Mã commit được gài vào biến môi trường để cổng :8080/version trả về đúng bản đang chạy
ARG GIT_SHA=dev
ENV LABCAST_COMMIT=${GIT_SHA}

WORKDIR /app
COPY source/server/target/labcast-server.jar /app/labcast-server.jar

# Database SQLite nằm trong volume để deploy lại không mất dữ liệu
ENV LABCAST_DB=/data/labcast.db
VOLUME /data

#      LCP/1.0  trạng thái
EXPOSE 7000     8080

ENTRYPOINT ["java", "-jar", "/app/labcast-server.jar"]
