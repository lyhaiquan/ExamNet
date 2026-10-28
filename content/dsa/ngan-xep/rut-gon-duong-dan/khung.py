t = int(input())
for _ in range(t):
    duong_dan = input()
    st = []
    for ten in duong_dan.split("/"):
        pass  # bỏ "", "."; gặp ".." thì lấy ra; còn lại đẩy vào
    print("/" + "/".join(st))
