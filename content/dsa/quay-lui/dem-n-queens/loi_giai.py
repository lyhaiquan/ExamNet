n = int(input())
DAY = (1 << n) - 1


def dat(cot, cheo1, cheo2):
    if cot == DAY:
        return 1
    dem = 0
    trong = DAY & ~(cot | cheo1 | cheo2)
    while trong:
        bit = trong & -trong
        trong ^= bit
        dem += dat(cot | bit, ((cheo1 | bit) << 1) & DAY, (cheo2 | bit) >> 1)
    return dem


print(dat(0, 0, 0))
