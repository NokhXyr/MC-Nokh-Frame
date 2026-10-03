"""Contact sheet of recorded stills: python contact.py out.png file1 file2 ... (paths or globs)."""
import glob, os, sys
from PIL import Image, ImageDraw
out, pats = sys.argv[1], sys.argv[2:]
fs = [f for p in pats for f in sorted(glob.glob(p))]
W, H, cols = 400, 225, 3
rows = (len(fs) + cols - 1) // cols
s = Image.new("RGB", (cols * W, rows * (H + 14)), "black"); d = ImageDraw.Draw(s)
for i, f in enumerate(fs):
    x, y = (i % cols) * W, (i // cols) * (H + 14)
    s.paste(Image.open(f).convert("RGB").resize((W, H)), (x, y)); d.text((x + 2, y + H), os.path.basename(f), fill="white")
s.save(out)
