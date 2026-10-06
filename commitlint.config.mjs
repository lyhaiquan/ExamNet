// Luật commit của nhóm — chạy ở hai nơi:
//   1. Hook commit-msg của husky, chặn ngay trên máy mình khi `git commit`.
//   2. GitHub Actions (.github/workflows/pr-title.yml) kiểm tiêu đề PR. Repo chỉ cho
//      squash merge nên tiêu đề PR chính là commit nằm lại trên main — kể cả khi
//      ai đó bỏ qua hook bằng `git commit --no-verify`.
//
// Mẫu:  <type>(<scope>): <mô tả>
//       feat(server): thêm vòng accept và thread pool
//       fix(common): đọc lặp cho đủ LEN byte payload
//       docs(report): viết mục 8.9 thiết kế truyền thông
//
// Chi tiết và ví dụ: CONTRIBUTING.md

export default {
  extends: ["@commitlint/config-conventional"],
  rules: {
    // Bắt buộc ghi scope để lịch sử commit cho thấy ai làm phần nào
    "scope-empty": [2, "never"],
    "scope-enum": [
      2,
      "always",
      [
        // năm module Maven
        "common",
        "server",
        "service",
        "client",
        "bench",
        // ngoài code
        "docs",
        "report",
        "ci",
        "deploy",
        "build",
        "deps",
        "repo",
      ],
    ],
    // Cho phép mô tả tiếng Việt viết hoa chữ đầu ("Thêm …") hay viết thường đều được
    "subject-case": [0],
    "header-max-length": [2, "always", 100],
    // Thân commit tiếng Việt hay có dòng dài; không bắt ngắt dòng
    "body-max-line-length": [0],
    "footer-max-line-length": [0],
  },
};
