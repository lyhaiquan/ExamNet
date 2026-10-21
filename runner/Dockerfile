# Image chạy bài làm của học viên: Python, C, C++, Java (spec §8.1, spec đa ngôn ngữ §5). Chủ: Người 3.
#
# Cờ an toàn (--network none, giới hạn RAM/CPU/số tiến trình, --read-only,
# --cap-drop ALL…) đặt lúc `docker run` trong server.run, không đặt ở đây.
#
# Build thử, đứng ở thư mục gốc repo:
#   docker build -t labcast-runner:py3.12 runner
#
# Tên image giữ "py3.12" cho khỏi đổi cấu hình server và CI, dù image đã có thêm C, C++, Java.

# Ubuntu 24.04: có sẵn Python 3.12 và OpenJDK 17 (đúng chuẩn Java của spec), gcc 13, gdb 15
FROM ubuntu:24.04

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8

# python-is-python3: lệnh `python` (server gọi `python /runner/<script>.py`).
# gcc/g++ (C17, C++17), gdb (chạy từng dòng C/C++), JDK 17 (javac, JDI cho chạy từng dòng Java).
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python-is-python3 gcc g++ libc6-dev gdb \
        openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

# Header biên dịch sẵn cho bits/stdc++.h, một bản cho mỗi bộ cờ runner dùng (-O2 khi chạy, -g -O0 khi chạy
# từng dòng): bỏ được 1–2 s biên dịch mỗi lượt. g++ tự chọn bản khớp cờ trong thư mục .gch.
RUN H="$(find /usr/include -path '*/bits/stdc++.h' | head -n 1)" \
    && mkdir -p "$H.gch" \
    && g++ -std=c++17 -O2 -x c++-header "$H" -o "$H.gch/O2.gch" \
    && g++ -std=c++17 -g -O0 -x c++-header "$H" -o "$H.gch/g-O0.gch"

COPY . /runner

# Debugger cho Java: biên dịch sẵn vì lúc chạy thư mục /runner chỉ đọc
RUN javac --release 17 -encoding UTF-8 -d /runner/buoc /runner/buoc/JdiBuoc.java

# Không chạy bằng root; server.run còn ép thêm --user 65534:65534
USER 65534:65534
WORKDIR /tmp

ENTRYPOINT ["python", "/runner/run.py"]
