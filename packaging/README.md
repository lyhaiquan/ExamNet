# packaging — bộ cài cho phòng máy

Chủ: **Người 2**. Thiết kế ở spec §16.

Máy học viên và máy giáo viên **không cần cài Java**: `jpackage` (có sẵn trong JDK) đóng app JavaFX kèm luôn Java và JavaFX.

| Sản phẩm | Dùng khi |
| --- | --- |
| Bộ cài `.msi` | Phòng máy cho phép cài phần mềm |
| Bản chạy thẳng (thư mục app-image) | Phòng máy có phần mềm đóng băng ổ cứng; chạy từ USB hoặc thư mục dùng chung |
| `mo-tuong-lua.bat` | Người quản lý phòng máy chạy một lần bằng quyền admin: mở TCP 7000, UDP 7001–7002 |

Việc còn lại của giai đoạn 0 (xem `docs/specs/labcast-ke-hoach.md`):

- Workflow `.github/workflows/release.yml`: build trên máy Windows của GitHub khi gắn tag `v*`, đưa bộ cài lên GitHub Releases.
- Cấu hình `jpackage` và script tường lửa trong thư mục này.
