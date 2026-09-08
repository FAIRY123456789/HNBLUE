"""Generate the HNBLUE favicon raster and ICO variants from one geometry."""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
BRANDING = ROOT / "frontend" / "public" / "branding"


def icon(size: int) -> Image.Image:
    scale = size / 64
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    radius = round(14 * scale)
    draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill="#08364d")

    width = max(2, round(7 * scale))
    for x in (18, 46):
        draw.line((round(x * scale), round(15 * scale), round(x * scale), round(49 * scale)), fill="#3cc6b0", width=width)

    wave = [
        (18, 34), (21, 30), (24, 27), (28, 26), (32, 27),
        (36, 29), (40, 33), (43, 35), (46, 35),
    ]
    draw.line([(round(x * scale), round(y * scale)) for x, y in wave], fill="#d7fff6", width=max(2, round(6 * scale)), joint="curve")

    leaf = [(39, 18), (43, 13), (49, 11), (54, 13), (52, 18), (47, 22), (40, 23)]
    draw.polygon([(round(x * scale), round(y * scale)) for x, y in leaf], fill="#78d6a3")
    return image


def main() -> None:
    BRANDING.mkdir(parents=True, exist_ok=True)
    icon(16).save(BRANDING / "favicon-16x16.png")
    icon(32).save(BRANDING / "favicon-32x32.png")
    icon(180).save(BRANDING / "apple-touch-icon.png")
    ico_images = [icon(size) for size in (16, 32, 48)]
    ico_path = BRANDING / "favicon.ico"
    ico_images[-1].save(ico_path, format="ICO", sizes=[(16, 16), (32, 32), (48, 48)])
    (ROOT / "frontend" / "public" / "favicon.ico").write_bytes(ico_path.read_bytes())
    print(f"Generated HNBLUE favicon assets in {BRANDING}")


if __name__ == "__main__":
    main()
