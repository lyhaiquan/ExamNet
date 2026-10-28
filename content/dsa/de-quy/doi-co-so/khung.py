CHU_SO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"


def doi(n, b):
    # n < b: một chữ số; ngược lại doi(n // b, b) rồi thêm chữ số n % b
    return ""


t = int(input())
for _ in range(t):
    n, b = map(int, input().split())
    print(doi(n, b))
