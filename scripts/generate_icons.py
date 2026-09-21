import os
import io
import resvg_py
from PIL import Image, ImageDraw

WORKSPACE = os.path.abspath(os.path.dirname(__file__) + "/..")
ASSETS_DIR = os.path.join(WORKSPACE, "assets")
RES_DIR = os.path.join(WORKSPACE, "app", "src", "main", "res")
DRAWABLE_DIR = os.path.join(RES_DIR, "drawable")

LIGHT_SVG = os.path.join(ASSETS_DIR, "logo-light.svg")
with open(LIGHT_SVG, "r", encoding="utf-8") as f:
    text = f.read()

# 1. Extract defs and full artwork (signal rings + glow + woven X + 5G badge)
defs_start = text.find("<defs>")
defs_end = text.find("</defs>") + 7
defs = text[defs_start:defs_end]

bg_rect_str = '<rect x="8" y="8" width="1008" height="1008" rx="232" fill="url(#bg)" stroke="#D5DEEB" stroke-width="6"/>'
content = text[defs_end:text.find("</svg>")].replace(bg_rect_str, "")

artwork_svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">\n{defs}\n{content}\n</svg>'
artwork_bytes = resvg_py.svg_to_bytes(svg_string=artwork_svg)
im_art_raw = Image.open(io.BytesIO(artwork_bytes)).convert("RGBA")

# Crop tightly to the artwork bounds (842x842)
bbox = im_art_raw.getbbox()
im_art_cropped = im_art_raw.crop(bbox)

# 2. Render Full-Bleed Background (1024x1024, no rounded corners, no borders)
bg_svg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">\n{defs}\n<rect width="1024" height="1024" fill="url(#bg)"/>\n</svg>'
bg_bytes = resvg_py.svg_to_bytes(svg_string=bg_svg)
im_bg_full = Image.open(io.BytesIO(bg_bytes)).convert("RGBA")

os.makedirs(DRAWABLE_DIR, exist_ok=True)

# 3. Generate high-res 432x432 adaptive icon layers in res/drawable/
# Scaled at 72% so dashed signal rings fit completely inside the squircle
ARTWORK_RATIO = 0.72
ICON_SIZE = 432

# Background: full bleed
bg_tile = im_bg_full.resize((ICON_SIZE, ICON_SIZE), Image.Resampling.LANCZOS)
bg_tile.save(os.path.join(DRAWABLE_DIR, "ic_launcher_background.png"), "PNG")

# Foreground: transparent canvas with full artwork (including rings) centered
fg_canvas = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
art_target_size = int(ICON_SIZE * ARTWORK_RATIO)
w, h = im_art_cropped.size
scale = art_target_size / max(w, h)
mw, mh = int(w * scale), int(h * scale)
art_resized = im_art_cropped.resize((mw, mh), Image.Resampling.LANCZOS)

pos_x = (ICON_SIZE - mw) // 2
pos_y = (ICON_SIZE - mh) // 2
fg_canvas.paste(art_resized, (pos_x, pos_y), art_resized)
fg_canvas.save(os.path.join(DRAWABLE_DIR, "ic_launcher_foreground.png"), "PNG")

# 4. In-App Drawables
def make_squircle_mask(size, radius):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size, size), radius=radius, fill=255)
    return mask

# High-res 512x512 app logo (full squircle with rings)
full_512_bg = im_bg_full.resize((512, 512), Image.Resampling.LANCZOS)
art_512_size = int(512 * ARTWORK_RATIO)
scale_512 = art_512_size / max(w, h)
mw_512, mh_512 = int(w * scale_512), int(h * scale_512)
art_512 = im_art_cropped.resize((mw_512, mh_512), Image.Resampling.LANCZOS)
full_512_bg.paste(art_512, ((512 - mw_512) // 2, (512 - mh_512) // 2), art_512)

mask_512 = make_squircle_mask(512, int(512 * 0.22))
logo_512 = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
logo_512.paste(full_512_bg, (0, 0), mask_512)
logo_512.save(os.path.join(DRAWABLE_DIR, "ic_logo.png"), "PNG")
logo_512.save(os.path.join(DRAWABLE_DIR, "ic_logo_light.png"), "PNG")

# Artwork on transparent (for app bar / compact usage)
art_cropped_256 = im_art_cropped.resize((256, 256), Image.Resampling.LANCZOS)
art_cropped_256.save(os.path.join(DRAWABLE_DIR, "ic_mark_light.png"), "PNG")

print("Cleaned up! Only res/drawable is used.")
