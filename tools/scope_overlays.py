#!/usr/bin/env python3
"""Render assets/firearms/textures/gui/scope_reticle.png: the overlay
`firearms.mixin.client.ScopeOverlayMixin` swaps in for vanilla's own
`SPYGLASS_SCOPE_LOCATION` while a player scopes through a firearm's magnifying optic
(`docs/spec/domains/combat.md` COMBAT-REQ-008).

One shared reticle for every magnifying optic (`firearms.client.scope.ScopeOverlay`'s own doc:
`docs/spec/domains/attach.md` §3 gives each optic its own zoom factor but no per-tier art of its
own) — our own original art, not a copy of vanilla's `spyglass_scope.png`: an opaque black vignette
with a soft-edged circular cutout to look through, a thin crosshair reticle, and a light tube-shadow
ring, all drawn with Pillow, no external asset read. The actual pixel size does not have to match
vanilla's own texture — `Hud.extractSpyglassOverlay`'s `blit(...)` call always passes the on-screen
destination size as the source texture's width/height too (`javap -p -c` against the 26.2
merged-deobf jar), so the GPU stretches whatever resolution this file ships to fill the circle
either way; 512x512 is chosen only so the crosshair stays crisp at typical GUI scales.
"""
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 512
CENTRE = SIZE // 2
SUPERSAMPLE = 4
OUT = Path("src/main/resources/assets/firearms/textures/gui/scope_reticle.png")

VIGNETTE = (0, 0, 0, 255)          # the opaque scope-tube frame around the cutout
TRANSPARENT = (0, 0, 0, 0)         # the cutout itself: see the world through it
TUBE_SHADOW = (0, 0, 0, 160)       # a soft inner ring, suggesting the tube's own depth
RETICLE = (10, 10, 10, 235)        # near-black crosshair, readable against any background
RETICLE_HIGHLIGHT = (235, 235, 235, 90)  # a faint light edge so the line reads over dark scenes too

CUTOUT_RADIUS = 236                # leaves a visible vignette ring at the square's own edge
SHADOW_RADIUS = 214
GAP = 34                           # the centre gap real optic reticles leave around point-of-aim
ARM_LENGTH = 150
LINE_WIDTH = 6
TICK_LENGTH = 16
TICK_SPACING = 46


def _big_canvas() -> Image.Image:
    return Image.new("RGBA", (SIZE * SUPERSAMPLE, SIZE * SUPERSAMPLE), TRANSPARENT)


def vignette() -> Image.Image:
    """An opaque square with a soft circular cutout at the centre, supersampled for a clean edge."""
    big = _big_canvas()
    draw = ImageDraw.Draw(big)
    draw.rectangle((0, 0, big.width, big.height), fill=VIGNETTE)
    c = CENTRE * SUPERSAMPLE
    r = CUTOUT_RADIUS * SUPERSAMPLE
    draw.ellipse((c - r, c - r, c + r, c + r), fill=TRANSPARENT)
    r2 = SHADOW_RADIUS * SUPERSAMPLE
    draw.ellipse((c - r2, c - r2, c + r2, c + r2), outline=TUBE_SHADOW, width=10 * SUPERSAMPLE)
    return big.resize((SIZE, SIZE), Image.LANCZOS)


def _arm(draw: ImageDraw.ImageDraw, dx: int, dy: int, colour: tuple[int, int, int, int], width: int) -> None:
    """One crosshair arm from the centre gap out to ARM_LENGTH, in the (dx, dy) unit direction."""
    x0, y0 = CENTRE + dx * GAP, CENTRE + dy * GAP
    x1, y1 = CENTRE + dx * (GAP + ARM_LENGTH), CENTRE + dy * (GAP + ARM_LENGTH)
    draw.line((x0, y0, x1, y1), fill=colour, width=width)
    # Range ticks, perpendicular to the arm, evenly spaced along it — our own mil-dot-style detail.
    steps = ARM_LENGTH // TICK_SPACING
    for step in range(1, steps + 1):
        cx = CENTRE + dx * (GAP + step * TICK_SPACING)
        cy = CENTRE + dy * (GAP + step * TICK_SPACING)
        px, py = -dy, dx  # perpendicular unit direction
        draw.line(
            (cx - px * TICK_LENGTH // 2, cy - py * TICK_LENGTH // 2,
             cx + px * TICK_LENGTH // 2, cy + py * TICK_LENGTH // 2),
            fill=colour, width=max(2, width - 2),
        )


def reticle() -> Image.Image:
    """The crosshair: four arms with range ticks, drawn once at full resolution then a faint
    light-coloured duplicate offset by one pixel for contrast, our own take on a mil-dot reticle."""
    img = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)
    draw = ImageDraw.Draw(img)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        _arm(draw, dx, dy, RETICLE_HIGHLIGHT, LINE_WIDTH + 2)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        _arm(draw, dx, dy, RETICLE, LINE_WIDTH)
    return img


def main() -> None:
    img = Image.alpha_composite(vignette(), reticle())
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT, optimize=True)
    print(f"wrote {OUT} ({OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
