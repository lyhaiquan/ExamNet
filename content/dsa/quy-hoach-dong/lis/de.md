# Dãy con tăng dài nhất (LIS), O(n²)

Cho dãy *a*₀, *a*₁, …, *a*ₙ₋₁. Một **dãy con** có được bằng cách xoá đi một số phần tử, giữ nguyên thứ tự các phần tử còn lại. Tìm độ dài dãy con **tăng ngặt** (phần tử sau lớn hơn hẳn phần tử trước) dài nhất.

Ví dụ `10 9 2 5 3 7 101 18` có dãy con tăng dài nhất là `2 3 7 18` (hay `2 5 7 101`), độ dài 4.

**Mảng `dp`:** `dp[i]` là độ dài dãy con tăng dài nhất **kết thúc tại** *a*ᵢ. Dãy đó hoặc chỉ có mỗi *a*ᵢ (độ dài 1), hoặc nối *a*ᵢ vào sau một dãy con tăng kết thúc tại *a*ⱼ nào đó với *j* < *i* và *a*ⱼ < *a*ᵢ:

`dp[i] = 1 + max(dp[j])` với mọi *j* < *i* mà *a*ⱼ < *a*ᵢ (không có *j* nào thì `dp[i] = 1`).

Đáp án là số lớn nhất trong `dp` (dãy dài nhất có thể kết thúc ở bất kỳ đâu). Hai vòng lặp lồng nhau: O(*n*²).

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 1000). Dòng 2 gồm *n* số nguyên *a*ᵢ (−10⁹ ≤ *a*ᵢ ≤ 10⁹).

## Output

Độ dài dãy con tăng dài nhất.

## Ví dụ

| Input | Output |
| --- | --- |
| `8`<br>`10 9 2 5 3 7 101 18` | `4` |
| `5`<br>`5 4 3 2 1` | `1` |
| `4`<br>`2 2 2 2` | `1` |
