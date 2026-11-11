import os
from PIL import Image, ImageDraw, ImageFilter

base_image_path = r"C:\Users\VICTUS hp\.gemini\antigravity-ide\brain\ab24f7fb-77e2-4b36-afdb-21434be80137\rimay_cyber_icon_1792253963024.jpg"
res_dir = r"d:\septimo_semestre\robotica-vision-app\app\src\main\res"

if not os.path.exists(base_image_path):
    print("Base image not found:", base_image_path)
    exit(1)

base_img = Image.open(base_image_path).convert("RGBA")

# Mipmap dimensions (width = height)
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def create_rounded_icon(size, corner_radius_ratio=0.22):
    scale = 4
    canvas_size = size * scale
    radius = int(canvas_size * corner_radius_ratio)
    
    img_resized = base_img.resize((canvas_size, canvas_size), Image.Resampling.LANCZOS)
    
    mask = Image.new("L", (canvas_size, canvas_size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (canvas_size - 1, canvas_size - 1)], radius=radius, fill=255)
    
    outline = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    outline_draw = ImageDraw.Draw(outline)
    outline_draw.rounded_rectangle([(1, 1), (canvas_size - 2, canvas_size - 2)], radius=radius, outline=(0, 210, 180, 80), width=max(2, int(scale * 1.5)))
    
    result = Image.composite(img_resized, Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0)), mask)
    result = Image.alpha_composite(result, outline)
    
    return result.resize((size, size), Image.Resampling.LANCZOS)

def create_circle_icon(size):
    scale = 4
    canvas_size = size * scale
    
    img_resized = base_img.resize((canvas_size, canvas_size), Image.Resampling.LANCZOS)
    
    mask = Image.new("L", (canvas_size, canvas_size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([(0, 0), (canvas_size - 1, canvas_size - 1)], fill=255)
    
    outline = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    outline_draw = ImageDraw.Draw(outline)
    outline_draw.ellipse([(1, 1), (canvas_size - 2, canvas_size - 2)], outline=(0, 210, 180, 100), width=max(2, int(scale * 1.5)))
    
    result = Image.composite(img_resized, Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0)), mask)
    result = Image.alpha_composite(result, outline)
    
    return result.resize((size, size), Image.Resampling.LANCZOS)

# Generate mipmaps
for folder, size in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    # 1. Standard launcher
    sq_icon = create_rounded_icon(size)
    sq_icon.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
    
    # 2. Round launcher
    rd_icon = create_circle_icon(size)
    rd_icon.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
    print(f"Generated {folder}: {size}x{size}")

# Generate high-res in-app logo (512x512)
logo_512 = create_rounded_icon(512, corner_radius_ratio=0.20)
logo_path = os.path.join(res_dir, "drawable", "app_logo.png")
logo_512.save(logo_path, "PNG")
print("Saved 512x512 app_logo.png")

# Generate adaptive icon foreground (108dp base canvas, artwork centered in inner 72dp)
adaptive_size = 432  # 108dp * 4
inner_size = int(adaptive_size * 0.72)
offset = (adaptive_size - inner_size) // 2

adaptive_fg = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
inner_img = create_circle_icon(inner_size)
adaptive_fg.paste(inner_img, (offset, offset), inner_img)

adaptive_fg_path = os.path.join(res_dir, "drawable", "ic_launcher_foreground.png")
adaptive_fg.save(adaptive_fg_path, "PNG")
print("Saved adaptive ic_launcher_foreground.png")
