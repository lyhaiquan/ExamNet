# Image chạy bài làm Python của học viên (spec §8.1). Chủ: Người 3.
#
# Cờ an toàn (--network none, giới hạn RAM/CPU/số tiến trình, --read-only,
# --cap-drop ALL…) đặt lúc `docker run` trong server.run, không đặt ở đây.
#
# Build thử, đứng ở thư mục gốc repo:
#   docker build -t labcast-runner:py3.12 runner

FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY . /runner

# Không chạy bằng root; server.run còn ép thêm --user 65534:65534
USER 65534:65534
WORKDIR /tmp

ENTRYPOINT ["python", "/runner/run.py"]
