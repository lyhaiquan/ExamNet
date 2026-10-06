# Dashboard giám thị (S7)

Chủ: Người 4. Trang web chạy trên trình duyệt: `index.html`, `app.js`, `style.css`.

- HTML + JavaScript thuần, **không cần Node hay bước build**. Trình duyệt có sẵn `new WebSocket(...)`.
- Thư mục này nằm trong `resources/` nên được đóng vào jar của server. Server tự phục vụ trang ở cổng 5001, rồi nâng cấp kết nối lên WebSocket (`examnet.server.ws`).
- Merge vào `main` là trang tự lên VPS cùng server: `http://<IP VPS>:5001/`.
- Làm ở phase 8 (spec §16).
