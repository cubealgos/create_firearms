"""The shared pixel palette for every firearms texture and sprite (WEAPON-REQ-017, DEC-018).

Each material is a ramp of six tones lit from the top-left, darkest to lightest: ``outline`` (the
one-pixel border every shape gets, never pure black), ``dark`` (a second shade step, for the far
side of a tube's bore or a box's deepest edge), ``shade`` (the lower/right faces), ``base`` (the
main faces), ``mid`` (a second highlight step, between ``base`` and ``light``) and ``light`` (the
top/left highlight, and a box's whole lit top face). ``dark`` and ``mid`` are interpolated
(`_fill`) between their neighbours for every ramp below so nothing has to hand-place them, giving
six tones per material -- round four's four-tone ramps could only ever manage a flat highlight
line and a flat shade band; round five's boxes and tubes (`docs/spec` FA-21 round-five brief, "our
things are just flat") need a lit top face, a base front face and a shaded side face, which is
three steps beyond the outline, so a fourth tone was never going to be enough. A few ramps also
carry a ``glint`` for single specular pixels on metal or glass.

Colours are sampled from Create Fly's (CC0) and vanilla's block and item textures so the mod sits
next to them: industrial iron and railway casing for steel and gunmetal, brass block for brass,
copper casing for copper, oak/spruce/dark oak planks for wood, green wool darkened for polymer.
Sampled tones, never copied pixels (COMP-REQ-002). Both generators, ``sprites.py`` (flat icons)
and ``models.py`` (cuboid weapon models and their atlases), import from here and nowhere else.
"""
from __future__ import annotations

from dataclasses import dataclass, replace


@dataclass(frozen=True)
class Ramp:
    outline: tuple[int, int, int, int]
    shade: tuple[int, int, int, int]
    base: tuple[int, int, int, int]
    light: tuple[int, int, int, int]
    glint: tuple[int, int, int, int] | None = None
    dark: tuple[int, int, int, int] | None = None
    mid: tuple[int, int, int, int] | None = None


def _c(hex_: str) -> tuple[int, int, int, int]:
    h = hex_.lstrip("#")
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255


def _lerp(a: tuple[int, int, int, int], b: tuple[int, int, int, int], t: float) -> tuple[int, int, int, int]:
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(4))  # type: ignore[return-value]


def _fill(ramp: Ramp) -> Ramp:
    """Every ramp's own ``dark`` (outline..shade, at 55%: closer to shade, still clearly a step up
    from the outline) and ``mid`` (base..light, at 45%: closer to base) tone, interpolated so the
    six-tone ramp still reads as one smooth material rather than two mismatched halves."""
    dark = ramp.dark if ramp.dark is not None else _lerp(ramp.outline, ramp.shade, 0.55)
    mid = ramp.mid if ramp.mid is not None else _lerp(ramp.base, ramp.light, 0.45)
    return replace(ramp, dark=dark, mid=mid)


# Receivers, slides, barrels: Create's industrial iron.
STEEL = _fill(Ramp(_c("353438"), _c("3f3e42"), _c("595858"), _c("6f6b6b"), _c("8a8686")))
# Blued/black steel for the darkest parts (Uzi receiver, AWM bolt, muzzle devices): railway casing.
GUNMETAL = _fill(Ramp(_c("1c1c20"), _c("303639"), _c("3f4548"), _c("4b4948"), _c("5e5b5a")))
# Bright iron for small hardware (bolt handles, sights, pins): vanilla iron.
IRON = _fill(Ramp(_c("444444"), _c("6b6b6b"), _c("bebebe"), _c("d8d8d8"), _c("ffffff")))
# Create brass: cartridge cases, the Winchester's tube, trims.
BRASS = _fill(Ramp(_c("724731"), _c("9e6947"), _c("cea05a"), _c("e4b763"), _c("f7cb6c")))
# Create copper: bullet tips, the compensator's ports.
COPPER = _fill(Ramp(_c("602f24"), _c("9a5038"), _c("b26247"), _c("d67b5b")))
# Vanilla oak: light wooden furniture (AKM handguard, Winchester forend).
OAK = _fill(Ramp(_c("67502c"), _c("9f844d"), _c("b8945f"), _c("c29d62")))
# Vanilla spruce: rifle stocks (Mini-14, AKM stock).
SPRUCE = _fill(Ramp(_c("553a1f"), _c("70522e"), _c("82613a"), _c("886539")))
# Vanilla dark oak: pistol grips, the thumb grip.
DARK_OAK = _fill(Ramp(_c("291a0c"), _c("3e2912"), _c("4f3218"), _c("5c3f1f")))
# Olive polymer for the AWM's stock and the 5.56 tip: vanilla green wool, darkened.
POLYMER = _fill(Ramp(_c("2a3312"), _c("3f4f1c"), _c("50661e"), _c("658619")))
# Black polymer for magazines, the tactical stock and the vertical grip: vanilla black wool,
# widened past black wool's own narrow range (round-five review: the red dot, holo and half grip
# icons read as flat near-black blobs at this size) so a `light` top face still pops against an
# `outline`/`shade` this dark -- the widest range any ramp here carries for exactly that reason.
BLACK = _fill(Ramp(_c("0a0a0d"), _c("17171c"), _c("222226"), _c("4a4a52")))
# Optic glass.
GLASS = _fill(Ramp(_c("1f3a4a"), _c("2e6b8a"), _c("4fa3c7"), _c("a6e0f0"), _c("e6f7fb")))
# Shotshell hull.
RED = _fill(Ramp(_c("5a1a12"), _c("8f2a1c"), _c("c0392b"), _c("d9574a")))

RED_DOT = _c("ff3b30")
WHITE = _c("ffffff")
SKY = _c("c9eef7")  # pale cyan: lens/glass glints, the reticle cross.
TRANSPARENT = (0, 0, 0, 0)

RAMPS: dict[str, Ramp] = {
    "steel": STEEL, "gunmetal": GUNMETAL, "iron": IRON, "brass": BRASS, "copper": COPPER,
    "oak": OAK, "spruce": SPRUCE, "dark_oak": DARK_OAK, "polymer": POLYMER, "black": BLACK,
    "glass": GLASS, "red": RED,
}
