# Font đóng gói trong app

- `Inter-Regular.ttf`, `Inter-Bold.ttf`: tách từ `Inter[opsz,wght].ttf` của [google/fonts](https://github.com/google/fonts/tree/main/ofl/inter) (opsz 14, wght 400 và 700) bằng `fontTools.varLib.instancer`. JavaFX chỉ đọc được font tĩnh, không đọc trục độ đậm của font biến thiên. Giấy phép: `OFL-Inter.txt`.
- `JetBrainsMono-Regular.ttf`, `JetBrainsMono-Bold.ttf`: từ [JetBrains/JetBrainsMono](https://github.com/JetBrains/JetBrainsMono/tree/master/fonts/ttf). Giấy phép: `OFL-JetBrainsMono.txt`.

Cả hai dùng SIL Open Font License 1.1, cho phép đóng gói kèm phần mềm. `Theme` nạp các file này lúc app khởi động, nên máy học viên không cần cài font.
