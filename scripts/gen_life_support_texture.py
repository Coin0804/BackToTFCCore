"""Generate 16x16 block texture for Life Support Device."""
from PIL import Image
import os

IMG = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
P = IMG.load()

# ── Color palette ──
DARK_METAL  = (40, 42, 46, 255)
MID_METAL   = (55, 58, 62, 255)
LIGHT_METAL = (70, 74, 78, 255)
BORDER_DARK = (25, 27, 30, 255)
GLOW_BLUE   = (80, 220, 255, 255)
GLOW_GREEN  = (80, 255, 150, 255)
CROSS_RED   = (220, 60, 60, 255)
DIM_BLUE    = (30, 100, 130, 255)
DIM_GREEN   = (30, 110, 60, 255)
SCREEN_BG   = (15, 20, 25, 255)

def rect(x1, y1, x2, y2, color):
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                P[x, y] = color

def hline(x1, x2, y, color):
    for x in range(x1, x2 + 1):
        if 0 <= x < 16 and 0 <= y < 16:
            P[x, y] = color

def vline(x, y1, y2, color):
    for y in range(y1, y2 + 1):
        if 0 <= x < 16 and 0 <= y < 16:
            P[x, y] = color

def dot(x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        P[x, y] = color

# ── Base metal face ──
rect(0, 0, 15, 15, MID_METAL)
rect(0, 0, 15, 0, BORDER_DARK)    # top
rect(0, 15, 15, 15, BORDER_DARK)  # bottom
rect(0, 0, 0, 15, BORDER_DARK)    # left
rect(15, 0, 15, 15, BORDER_DARK)  # right

# Inner highlight
rect(1, 1, 14, 1, LIGHT_METAL)
rect(1, 1, 1, 14, LIGHT_METAL)

# ── Screen panel ──
rect(3, 3, 12, 9, SCREEN_BG)

# ── ECG heart-rate line (green glow) ──
hline(4, 5, 8, DIM_GREEN)          # baseline
dot(6, 7, GLOW_GREEN)
dot(6, 6, GLOW_GREEN)
# R-wave spike
dot(7, 6, GLOW_GREEN)
dot(7, 5, GLOW_GREEN)
dot(7, 4, GLOW_GREEN)
dot(7, 3, GLOW_GREEN)             # peak
# S-wave dip
dot(8, 4, GLOW_GREEN)
dot(8, 5, GLOW_GREEN)
dot(8, 6, GLOW_GREEN)
dot(8, 7, GLOW_GREEN)
dot(8, 8, GLOW_GREEN)             # dip
# Return
dot(9, 7, GLOW_GREEN)
hline(10, 11, 8, DIM_GREEN)

# ── Blue indicator dots (top-right of screen) ──
dot(10, 4, GLOW_BLUE)
dot(12, 4, GLOW_BLUE)

# ── Bottom screen line ──
hline(4, 11, 10, DIM_BLUE)

# ── Medical cross (bottom-left of screen) ──
hline(4, 6, 11, CROSS_RED)
vline(5, 10, 12, CROSS_RED)

# ── Rivets ──
dot(2, 2, DARK_METAL)
dot(13, 2, DARK_METAL)
dot(2, 13, DARK_METAL)
dot(13, 13, DARK_METAL)

# ── Bottom vents ──
for vy in range(12, 15):
    hline(4, 12, vy, DARK_METAL)

# ── Save ──
out_dir = "src/main/resources/assets/backtotfccore/textures/block"
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "life_support_device.png")
IMG.save(out)
print(f"Saved: {out} ({os.path.getsize(out)} bytes)")
