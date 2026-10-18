"""Vẽ icon LabCast: chữ "LC" trắng trên ô vuông bo góc màu chính của app (#4f46e5, như logo ở thanh trên).

    python packaging/icon/tao_icon.py

Ghi packaging/icon/labcast.ico (lối tắt Windows, jpackage --icon) và các PNG cho cửa sổ app trong
source/client/src/main/resources/labcast/client/ui/icon/. Vẽ ở 1024 px rồi thu nhỏ để cạnh mịn.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

GOC = Path(__file__).resolve().parents[2]
FONT = GOC / "source/client/src/main/resources/labcast/client/fonts/Inter-Bold.ttf"
MAU = (79, 70, 229, 255)
CO = 1024


def ve():
    img = Image.new("RGBA", (CO, CO), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    le = CO // 32
    d.rounded_rectangle([le, le, CO - le, CO - le], radius=CO * 22 // 100, fill=MAU)
    font = ImageFont.truetype(str(FONT), CO * 46 // 100)
    x0, y0, x1, y1 = d.textbbox((0, 0), "LC", font=font)
    d.text(((CO - (x1 - x0)) / 2 - x0, (CO - (y1 - y0)) / 2 - y0), "LC", font=font, fill="white")
    return img


img = ve()
ico = GOC / "packaging/icon/labcast.ico"
img.save(ico, sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
png = GOC / "source/client/src/main/resources/labcast/client/ui/icon"
png.mkdir(parents=True, exist_ok=True)
for s in (16, 32, 48, 64, 128, 256):
    img.resize((s, s), Image.LANCZOS).save(png / f"labcast-{s}.png")
print("Đã ghi", ico, "và", png)
