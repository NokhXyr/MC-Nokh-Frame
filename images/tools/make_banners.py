"""Builds the animated banners of the Nokh Frame page (images/*.gif) from footage recorded in game.

    python images/tools/make_banners.py [names...]

The footage was recorded by the scripts of images/tools/capture/ (takes.txt, takes2.txt) into
run-capture/screenshots, at 1600 x 900 with the GUI at scale 2. The studio's panel is the right 360
pixels of every frame; the stage (what a photo shows) is the left 1240.

Look at every banner with gifsheet.py before it ships, and at heat.py when one is too heavy.
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)

import kit  # noqa: E402
from theme import Theme  # noqa: E402

SRC = os.path.join(ROOT, "run-capture", "screenshots")
OUT = os.path.dirname(HERE)             # images/, the folder this toolkit sits in
THEME = Theme.load(os.path.join(HERE, "theme.json"))
LOGO = os.path.join(ROOT, "media", "logo.png")
STAGE = (0, 0, 1240, 860)               # the studio's stage, without the panel and the hint line


def src(name):
    return os.path.join(SRC, name)


def out(name):
    return os.path.join(OUT, name)


# ------------------------------------------------------------------ the banner on top of the page

def hero():
    # The player turns on itself (one full turn in 80 frames, so the loop is seamless) on the library set.
    kit.hero_banner(out("hero.gif"), THEME, src("hero_plate.png"), "NOKH FRAME", logo=LOGO,
                    frames=kit.clip(SRC, "hero", 1, 80), region=(440, 60, 610, 340), crop=(0, 160, 1240, 625), length=80,
                    floaters=5, drift_box=(300, 190, 420, 290))


# ------------------------------------------------------------------ one studio, many photos: prints land on a stack

PRINTS = [
    ("var_color.png", None, "COLOR", "brush"),
    ("var_png.png", None, "PNG IMAGE", "upload"),
    ("var_rift.png", None, "3D SCENE", "cube"),
    ("var_halo.png", None, "BLOCKBENCH", "hammer"),
    ("var_item.png", None, "ANY ITEM", "sword"),
    ("../../extras/arcadia/preview-world-shaders-rift.png", (121, 0, 1159, 720), "SHADERS", "sun"),
]
CARD = (272, 189)                       # the photo inside a print, in the proportions of the stage
BORDER = 8
HOLD = 30
SLIDE = 10
TOP = (575, 158, 0.0)                   # centre x, centre y, angle of the print on top
UNDER = [(548, 166, 7.0), (604, 150, -6.0)]


def _print(name, crop):
    path = name if os.path.isabs(name) else os.path.normpath(os.path.join(SRC, name))
    photo = kit.load(path, size=CARD, crop=crop or STAGE).convert("RGBA")
    card = Image.new("RGBA", (CARD[0] + 2 * BORDER, CARD[1] + 2 * BORDER + 10), THEME.paper)
    card.paste(photo, (BORDER, BORDER))
    edge = Image.new("RGBA", card.size, (0, 0, 0, 0))
    from PIL import ImageDraw
    ImageDraw.Draw(edge).rectangle([0, 0, card.width - 1, card.height - 1], outline=THEME.ink)
    card.alpha_composite(edge)
    shadow = Image.new("RGBA", (card.width + 6, card.height + 6), (0, 0, 0, 0))
    shadow.paste(Image.new("RGBA", card.size, THEME.ink[:3] + (90,)), (6, 6))
    shadow.alpha_composite(card)
    return shadow


def _place(canvas, card, pose, alpha=1.0):
    x, y, angle = pose
    img = card.rotate(angle, resample=Image.BICUBIC, expand=True) if abs(angle) > 0.05 else card
    if alpha < 1.0:
        img = img.copy()
        img.putalpha(img.getchannel("A").point(lambda v: int(v * alpha)))
    canvas.alpha_composite(img, (int(round(x - img.width / 2)), int(round(y - img.height / 2))))


def _mix(a, b, t):
    return tuple(p + (q - p) * t for p, q in zip(a, b))


def prints():
    cards = [_print(name, crop) for name, crop, _, _ in PRINTS]
    chips = [kit.label(text, THEME, scale=3, icon_name=icon) for _, _, text, icon in PRINTS]
    count = len(cards)
    period = HOLD + SLIDE
    plate = kit.backdrop(THEME, (800, 320), "glow", focus=(0.72, 0.5))
    title = kit.text_image("ONE STUDIO", 3, THEME.white, outline=THEME.ink)
    sub = kit.text_image("MANY BACKDROPS", 2, THEME.accent_light, outline=THEME.ink)

    def overlay(canvas, i, n):
        k, j = divmod(i, period)
        k %= count
        sliding = j >= HOLD
        p = kit.ease_out_back((j - HOLD + 1) / SLIDE) if sliding else 0.0
        q = min(1.0, max(0.0, p))
        # The stack: two prints under the one on top; when the next one lands, they all move down a place.
        older, old, top = cards[(k - 2) % count], cards[(k - 1) % count], cards[k]
        _place(canvas, older, UNDER[1], alpha=1.0 - q)
        _place(canvas, old, _mix(UNDER[1], UNDER[0], q) if sliding else UNDER[0])
        _place(canvas, top, _mix(TOP, UNDER[0], q) if sliding else TOP)
        if sliding:
            nxt = cards[(k + 1) % count]
            start = (TOP[0] + 260, TOP[1] - 220, -14.0)
            _place(canvas, nxt, _mix(start, TOP, p))
        shown = (k + 1) % count if sliding and p > 0.5 else k
        kit.paste(canvas, title, 40, 92)
        kit.paste(canvas, sub, 42, 132)
        kit.paste(canvas, chips[shown], 40, 186)

    kit.still_scene(out("prints.gif"), THEME, plate, size=(800, 320), length=count * period, overlay=overlay,
                    floaters=False)


# ------------------------------------------------------------------ the pose editor, clicked through

def poses():
    # The real pose page: each click on "Pose: ..." moves to the next ready-made pose (animation, then neutral to frozen run).
    names = ["pose_page"] + [f"pose_{i}" for i in range(1, 10)]
    crop = (560, 100, 1600, 620)        # the player on the atelier set and the panel, 1040 x 520 -> 800 x 400
    button = ((1545 - 560) * 800 / 1040, (152 - 100) * 400 / 520)
    kit.tour_scene(out("poses.gif"), THEME, [src(n + ".png") for n in names], targets=[button] * len(names),
                   crop=crop, size=(800, 400), hold=20, move=6)


BUILDERS = {f.__name__: f for f in (hero, prints, poses)}


def main():
    names = sys.argv[1:] or list(BUILDERS)
    for name in names:
        BUILDERS[name]()


if __name__ == "__main__":
    main()
