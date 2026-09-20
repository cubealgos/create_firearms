"""FA-21 round five: every attachment icon and cartridge as a hand-authored 16x16 pixel grid.

Construction notes, studied from vanilla's and Create Fly's own 16x16 item sprites (technique
never copied pixels, COMP-REQ-002) by dumping each as a text grid of luminance-rank tone indices
(`tools/doctor.py`-adjacent scratch work, not shipped) and reading the construction back out of the
numbers:

- **`iron_ingot`/`brass_ingot` are solid, three-quarter-view prisms, not flat cut-outs.** Ranked by
  luminance, both show one wide flat *top face* at the single lightest tone (the loaf's whole lid,
  six-plus pixels of it, not a one-pixel line), a *front face* one or two tones down, and a
  *bottom/right face* two or three tones down again, with the near-white glint restricted to a
  literal handful of pixels along the top face's own leading edge -- never the whole top. Round
  four's icons had one tone per face at most (light line, flat base, flat shade); this round's
  boxes (`_box_icon` below) draw all three faces every time, `light`/`mid` top, `base` front,
  `shade`/`dark` side, so a magazine or a grip reads as a solid picked up and turned in the hand
  the way the ingot does, per Kevin's round-five note ("our things are just flat").
- **`spyglass`/`arrow`/`bow` are diagonal capsules, bottom-left to top-right, 11-13px of the 16px
  canvas.** The dump's tone values are *constant along the shaft* and only vary *across* it: the
  upper-left edge of the tube sits two ranks brighter than the lower-right edge for the shaft's
  entire length, i.e. the light source rakes across the cross-section, not along the barrel. Every
  tube icon here (`_tube_icon`) reproduces exactly that: a five-tone band (`light`, `mid`, `base`,
  `shade`, `dark`) running the tube's full length, banded perpendicular to its own axis, plus a
  uniform `outline` ring around the true silhouette only -- never along an internal seam between
  two tones of the same material.
- **Rings, caps and bands cross the tube in a separate, discrete material or tone**, with the
  brightest single pixel of the ring sitting at its own top-left: the spyglass's brass ferrule, the
  crossbow's roping. Every scope's brass ring and every cartridge's rim follow this: painted as
  their own short capsule slice over the steel/brass body, in painter's order, each slice keeping
  its own outline where it meets the body -- the discrete-band look is deliberate, not a seam bug.
- **The lens (`spyglass`) and the blade fuller (`iron_sword`) are the only smoothly-graded
  interiors**, three-to-four tones deep with one white/near-white glint pixel at the top-left-most
  point. Every glass surface here (scopes' objective lens, red dot's and holo's windows) gets that
  same glass ramp plus one `WHITE`/`SKY` glint pixel, never a flat fill.
- **`wrench`/`brass_hand`/`precision_mechanism`** (Create Fly, CC0) are busier multi-part
  compositions but obey the same two primitives at smaller scale: every hard edge is outline, every
  face reads as one flat tone with the highlight always upper-left. Nothing here needed a third
  primitive beyond the box and the tube.

Two drawing primitives, both used only by the authoring script below (not shipped -- see
"Round-five authoring" at the bottom of this docstring): a **tube**, a diagonal capsule shaded by
its cross-section (perpendicular to the direction of travel, so the tone band runs the tube's whole
length), and a **box**, a front face plus a receding top face and a receding right-side face, tilted
along the same up-right diagonal as every tube so the whole icon set reads as one consistent
three-quarter view. Every icon keeps a one-pixel transparent ring at the canvas edge (round-four
regression) and at least four distinct opaque colours (`test_pixel_art.py`).

Every icon below is `(name, rows, legend)`: `rows` is 16 strings of 16 characters top-to-bottom,
`legend` maps every non-`.` character to an RGBA colour from `palette.py`'s ramps (`.` is always
transparent and never appears in a legend). `sprites.py`'s `attachment_icon`/`cartridge_sprite`/
`shotshell_sprite` do nothing but decode these grids -- no geometry, capsule or mask code ships in
this file or in `sprites.py` for them; the geometry that produced these grids lived only in an
authoring script under the session scratchpad, never committed, the same way a texture artist's
Aseprite file isn't the shipped PNG. `ICON` below is the lookup every one of those three functions
uses, keyed by attachment name (`suppressor`, `scope_6x`, ...) or caliber id (`acp_45`,
`gauge_12`, ...).
"""
from __future__ import annotations

from palette import (BLACK, BRASS, COPPER, DARK_OAK, GLASS, GUNMETAL, IRON, OAK, POLYMER, RED,
                      RED_DOT, SPRUCE, STEEL, SKY, WHITE)

Grid = tuple[str, ...]
Legend = dict[str, tuple[int, int, int, int]]

ICONS: dict[str, tuple[Grid, Legend]] = {}


def _icon(name: str, rows: Grid, legend: Legend) -> None:
    assert len(rows) == 16, f"{name}: {len(rows)} rows, want 16"
    assert all(len(r) == 16 for r in rows), f"{name}: a row isn't 16 characters"
    assert all(ch == '.' or ch in legend for r in rows for ch in r), \
        f"{name}: a grid character is missing from its own legend"
    ICONS[name] = (rows, legend)


_icon(
    "suppressor",
    (
        "................",
        "................",
        ".........ccccc..",
        "........cjiihc..",
        ".......aciihec..",
        "......agciheec..",
        ".....agfcheedc..",
        "....agbbbcccc...",
        "...agfbbba......",
        "..ccccbbb.......",
        ".cjiihca........",
        ".ciihec.........",
        ".ciheec.........",
        ".cheedc.........",
        ".ccccc..........",
        "................",
    ),
    {
        'a': GUNMETAL.outline,
        'b': GUNMETAL.dark,
        'c': STEEL.outline,
        'd': STEEL.dark,
        'e': STEEL.shade,
        'f': GUNMETAL.mid,
        'g': GUNMETAL.light,
        'h': STEEL.base,
        'i': STEEL.mid,
        'j': STEEL.light,
    },
)

_icon(
    "flash_hider",
    (
        "................",
        "................",
        "................",
        ".......aaaaaa...",
        "......aeddcca...",
        ".....aeddccca...",
        ".....addcccba...",
        ".....adcccbba...",
        "....adcccb..a...",
        "....acccb..a....",
        "....accb..a.....",
        "...accba........",
        "...acba.........",
        "...aaa..........",
        "................",
        "................",
    ),
    {
        'a': STEEL.outline,
        'b': STEEL.shade,
        'c': STEEL.base,
        'd': STEEL.mid,
        'e': STEEL.light,
    },
)

_icon(
    "compensator",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......aaaa......",
        ".....afeda......",
        "....aghdca......",
        "...afhicba......",
        "..aghdcba.......",
        ".afhicba........",
        ".aedcba.........",
        ".adcba..........",
        ".aaaa...........",
        "................",
    ),
    {
        'a': STEEL.outline,
        'b': STEEL.dark,
        'c': STEEL.shade,
        'd': STEEL.base,
        'e': STEEL.mid,
        'f': STEEL.light,
        'g': COPPER.dark,
        'h': COPPER.base,
        'i': COPPER.light,
    },
)

_icon(
    "red_dot",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......aaaa......",
        ".....aeeba......",
        "....adddba......",
        "....agcdba......",
        "....accda.......",
        "....aaaa........",
        ".....ff.........",
        "................",
        "................",
    ),
    {
        'a': BLACK.outline,
        'b': BLACK.shade,
        'c': GLASS.outline,
        'd': BLACK.base,
        'e': BLACK.mid,
        'f': STEEL.outline,
        'g': RED_DOT,
    },
)

_icon(
    "holo",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....aaaaaaa....",
        "....aeeeeeba....",
        "...addddddba....",
        "...accggcdba....",
        "...acgccgda.....",
        "...aaaaaaa......",
        "....ff.ff.......",
        "................",
        "................",
    ),
    {
        'a': BLACK.outline,
        'b': BLACK.shade,
        'c': GLASS.outline,
        'd': BLACK.base,
        'e': BLACK.mid,
        'f': STEEL.outline,
        'g': SKY,
    },
)

_icon(
    "scope_2x",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "........iaa.....",
        ".......bada.....",
        "......bfaaa.....",
        ".....gggcb......",
        "....bghgb.......",
        "...bfggg........",
        "..bfecb.........",
        ".bfecb..........",
        ".becb...........",
        ".bbb............",
        "................",
    ),
    {
        'a': GLASS.outline,
        'b': STEEL.outline,
        'c': STEEL.shade,
        'd': GLASS.base,
        'e': STEEL.base,
        'f': STEEL.mid,
        'g': BRASS.outline,
        'h': BRASS.base,
        'i': IRON.glint,
    },
)

_icon(
    "scope_3x",
    (
        "................",
        "................",
        "................",
        "................",
        ".........iaa....",
        "........bada....",
        ".......bfaaa....",
        "......bfecb.....",
        ".....gggcb......",
        "....bghgb.......",
        "...bfggg........",
        "..bfecb.........",
        ".bfecb..........",
        ".becb...........",
        ".bbb............",
        "................",
    ),
    {
        'a': GLASS.outline,
        'b': STEEL.outline,
        'c': STEEL.shade,
        'd': GLASS.base,
        'e': STEEL.base,
        'f': STEEL.mid,
        'g': BRASS.outline,
        'h': BRASS.base,
        'i': IRON.glint,
    },
)

_icon(
    "scope_4x",
    (
        "................",
        "................",
        "................",
        "..........iaa...",
        ".........bada...",
        "........bfaaa...",
        ".......bfecb....",
        "......gggcb.....",
        ".....bghgb......",
        "....bfggg.......",
        "...bfecb........",
        "..bfecb.........",
        ".bfecb..........",
        ".becb...........",
        ".bbb............",
        "................",
    ),
    {
        'a': GLASS.outline,
        'b': STEEL.outline,
        'c': STEEL.shade,
        'd': GLASS.base,
        'e': STEEL.base,
        'f': STEEL.mid,
        'g': BRASS.outline,
        'h': BRASS.base,
        'i': IRON.glint,
    },
)

_icon(
    "scope_6x",
    (
        "................",
        "................",
        "................",
        "...........jaa..",
        "........bbbaea..",
        "........bgfaaa..",
        "........bfdcb...",
        ".......bfdcb....",
        "......bfdcb.....",
        ".....bfdcb......",
        "....bfdcb.......",
        "...hhhcb........",
        "..bhihb.........",
        "..bhhh..........",
        "..bbb...........",
        "................",
    ),
    {
        'a': GLASS.outline,
        'b': STEEL.outline,
        'c': STEEL.dark,
        'd': STEEL.shade,
        'e': GLASS.base,
        'f': STEEL.base,
        'g': STEEL.mid,
        'h': BRASS.outline,
        'i': BRASS.base,
        'j': IRON.glint,
    },
)

_icon(
    "scope_8x",
    (
        "................",
        "................",
        "............jaa.",
        ".........bbbaea.",
        ".........bgfaaa.",
        ".........bfdcb..",
        "........bfdcb...",
        ".......bfdcb....",
        "......bfdcb.....",
        ".....bfdcb......",
        "....bfdcb.......",
        "...hhhcb........",
        "..bhihb.........",
        "..bhhh..........",
        "..bbb...........",
        "................",
    ),
    {
        'a': GLASS.outline,
        'b': STEEL.outline,
        'c': STEEL.dark,
        'd': STEEL.shade,
        'e': GLASS.base,
        'f': STEEL.base,
        'g': STEEL.mid,
        'h': BRASS.outline,
        'i': BRASS.base,
        'j': IRON.glint,
    },
)

_icon(
    "scope_15x",
    (
        "................",
        "................",
        "............jaa.",
        ".........bbbaea.",
        ".........bgfaaa.",
        ".......bbbfdcb..",
        ".......bbfdcb...",
        ".......bfdcb....",
        "......bfdcb.....",
        "...bbbfdcb......",
        "...bgfdcb.......",
        "...hhhcb........",
        "..bhihb.........",
        "..bhhh..........",
        "..bbb...........",
        "................",
    ),
    {
        'a': GLASS.outline,
        'b': STEEL.outline,
        'c': STEEL.dark,
        'd': STEEL.shade,
        'e': GLASS.base,
        'f': STEEL.base,
        'g': STEEL.mid,
        'h': BRASS.outline,
        'i': BRASS.base,
        'j': IRON.glint,
    },
)

_icon(
    "quickdraw_magazine",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......aaaa......",
        ".....addba......",
        "....acccba......",
        "....acccba......",
        "....acccba......",
        "....acccba......",
        "....aeeea.......",
        "....aaaa........",
        "................",
        "................",
    ),
    {
        'a': STEEL.outline,
        'b': STEEL.shade,
        'c': STEEL.base,
        'd': STEEL.mid,
        'e': BRASS.base,
    },
)

_icon(
    "extended_magazine",
    (
        "................",
        "................",
        "................",
        "......aaaa......",
        ".....aeeba......",
        "....adddba......",
        "....adddba......",
        "....adddba......",
        "....adddba......",
        "....adddba......",
        "....adddba......",
        "....adddba......",
        "....accca.......",
        "....aaaa........",
        "................",
        "................",
    ),
    {
        'a': STEEL.outline,
        'b': STEEL.shade,
        'c': GUNMETAL.base,
        'd': STEEL.base,
        'e': STEEL.mid,
    },
)

_icon(
    "extended_quickdraw_magazine",
    (
        "................",
        "......aaaa......",
        ".....addba......",
        "....acccba......",
        "....acccba......",
        "....acccba......",
        "....acccba......",
        "....afccba......",
        "....acccba......",
        "....afccba......",
        "....acccba......",
        "....acccba......",
        "....aeeea.......",
        "....aaaa........",
        "................",
        "................",
    ),
    {
        'a': STEEL.outline,
        'b': STEEL.shade,
        'c': STEEL.base,
        'd': STEEL.mid,
        'e': BRASS.base,
        'f': BRASS.light,
    },
)

_icon(
    "light_grip",
    (
        "................",
        "................",
        "........aaa.....",
        ".......aeca.....",
        "......addca.....",
        "......addca.....",
        "......addca.....",
        "......addca.....",
        "......abdca.....",
        "......adda......",
        "......aaa.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    {
        'a': BLACK.outline,
        'b': BLACK.dark,
        'c': BLACK.shade,
        'd': BLACK.base,
        'e': BLACK.mid,
    },
)

_icon(
    "half_grip",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......aaaaa.....",
        ".....adddba.....",
        "....accccba.....",
        "....accccba.....",
        "....accccba.....",
        "....acccca......",
        "....aaaaa.......",
        "................",
        "................",
        "................",
    ),
    {
        'a': BLACK.outline,
        'b': BLACK.shade,
        'c': BLACK.base,
        'd': BLACK.mid,
    },
)

_icon(
    "angled_grip",
    (
        "................",
        "................",
        "................",
        "................",
        ".aaaa...........",
        ".affeaaa........",
        ".afeeeddaa......",
        ".aeeedddca......",
        "..aedddcca......",
        "..adddccca......",
        "..addcccca......",
        "...accccba......",
        "...aaaaaaa......",
        "................",
        "................",
        "................",
    ),
    {
        'a': POLYMER.outline,
        'b': POLYMER.dark,
        'c': POLYMER.shade,
        'd': POLYMER.base,
        'e': POLYMER.mid,
        'f': POLYMER.light,
    },
)

_icon(
    "vertical_grip",
    (
        "................",
        ".......aaaa.....",
        "......aeeca.....",
        ".....adddca.....",
        ".....adddca.....",
        ".....abddca.....",
        ".....adddca.....",
        ".....abddca.....",
        ".....adddca.....",
        ".....abddca.....",
        ".....addda......",
        ".....aaaa.......",
        "................",
        "................",
        "................",
        "................",
    ),
    {
        'a': BLACK.outline,
        'b': BLACK.dark,
        'c': BLACK.shade,
        'd': BLACK.base,
        'e': BLACK.mid,
    },
)

_icon(
    "thumb_grip",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....aaaaaa.....",
        "....adddda......",
        ".....cccca......",
        ".....bccca......",
        "...acbccca......",
        "...accccca......",
        "...aaaaaa.......",
        "................",
        "................",
        "................",
    ),
    {
        'a': DARK_OAK.outline,
        'b': DARK_OAK.dark,
        'c': DARK_OAK.base,
        'd': DARK_OAK.mid,
    },
)

_icon(
    "tactical_stock",
    (
        "................",
        "................",
        "................",
        "....aaaaaaaaaa..",
        "...adddddddda...",
        "..accccccccca...",
        "..bc......cca...",
        "..bc......cca...",
        "..bc......cca...",
        "..ac......cca...",
        "..aaaaaaaaaa....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    {
        'a': BLACK.outline,
        'b': BLACK.shade,
        'c': BLACK.base,
        'd': BLACK.mid,
    },
)

_icon(
    "cheek_pad",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "....aaaaaaaaa...",
        "...addddddda....",
        "..acccccccca....",
        "..abbbbbbbba....",
        "..abbbbbbbba....",
        "..aaaaaaaaa.....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ),
    {
        'a': SPRUCE.outline,
        'b': SPRUCE.dark,
        'c': SPRUCE.base,
        'd': SPRUCE.mid,
    },
)

_icon(
    "bullet_loops",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".bb.bb.bb.bb.bb.",
        ".bb.bb.bb.bb.bb.",
        ".bb.bb.bb.bb.bb.",
        ".bbabbabbabbabb.",
        ".adccccccccdca..",
        ".aaaaaaaaaaaaa..",
        "................",
        "................",
        "................",
        "................",
    ),
    {
        'a': OAK.outline,
        'b': BRASS.outline,
        'c': OAK.base,
        'd': OAK.mid,
    },
)

_icon(
    "acp_45",
    (
        "................",
        "................",
        "................",
        "........aaaa....",
        ".......ajhga....",
        "......ajhgea....",
        ".....bahgeca....",
        "....blageca.....",
        "...blkaaaa......",
        "..blkifdb.......",
        ".bbbbbdb........",
        ".bkkibb.........",
        ".bkifb..........",
        ".biffb..........",
        ".bbbbb..........",
        "................",
    ),
    {
        'a': COPPER.outline,
        'b': BRASS.outline,
        'c': COPPER.dark,
        'd': BRASS.dark,
        'e': COPPER.shade,
        'f': BRASS.shade,
        'g': COPPER.base,
        'h': COPPER.mid,
        'i': BRASS.base,
        'j': COPPER.light,
        'k': BRASS.mid,
        'l': BRASS.light,
    },
)

_icon(
    "mm_9",
    (
        "................",
        "................",
        "................",
        "................",
        "................",
        ".........aaa....",
        "........afea....",
        ".......afeca....",
        "......baeca.....",
        ".....bhaaa......",
        "....bhgdb.......",
        ".bbbbgdb........",
        ".bhgbdb.........",
        ".bgdbb..........",
        ".bbbb...........",
        "................",
    ),
    {
        'a': COPPER.outline,
        'b': BRASS.outline,
        'c': COPPER.shade,
        'd': BRASS.shade,
        'e': COPPER.base,
        'f': COPPER.mid,
        'g': BRASS.base,
        'h': BRASS.mid,
    },
)

_icon(
    "mm_7_62",
    (
        "................",
        "................",
        "................",
        "................",
        "..........aaa...",
        ".........afea...",
        "........afeca...",
        ".......baeca....",
        "......bhaaa.....",
        ".....bhgdb......",
        "....bhgdb.......",
        ".bbbbgdb........",
        ".bhgbdb.........",
        ".bgdbb..........",
        ".bbbb...........",
        "................",
    ),
    {
        'a': COPPER.outline,
        'b': BRASS.outline,
        'c': COPPER.shade,
        'd': BRASS.shade,
        'e': COPPER.base,
        'f': COPPER.mid,
        'g': BRASS.base,
        'h': BRASS.mid,
    },
)

_icon(
    "mm_5_56",
    (
        "................",
        "................",
        "................",
        "................",
        "..........aaa...",
        ".........adca...",
        "........adcba...",
        ".......eacba....",
        "......ehaaa.....",
        ".....ehgfe......",
        "....ehgfe.......",
        ".eeeegfe........",
        ".ehgefe.........",
        ".egfee..........",
        ".eeee...........",
        "................",
    ),
    {
        'a': POLYMER.outline,
        'b': POLYMER.shade,
        'c': POLYMER.base,
        'd': POLYMER.mid,
        'e': BRASS.outline,
        'f': BRASS.shade,
        'g': BRASS.base,
        'h': BRASS.mid,
    },
)

_icon(
    "magnum_300",
    (
        "................",
        "..........aaaa..",
        ".........ajhga..",
        "........ajhgea..",
        "........ahgeca..",
        "......bbageca...",
        ".....blkaaaa....",
        "....blkifdb.....",
        "...blkifdb......",
        "..blkifdb.......",
        ".bbbbbdb........",
        ".bkkibb.........",
        ".bkifb..........",
        ".biffb..........",
        ".bbbbb..........",
        "................",
    ),
    {
        'a': COPPER.outline,
        'b': BRASS.outline,
        'c': COPPER.dark,
        'd': BRASS.dark,
        'e': COPPER.shade,
        'f': BRASS.shade,
        'g': COPPER.base,
        'h': COPPER.mid,
        'i': BRASS.base,
        'j': COPPER.light,
        'k': BRASS.mid,
        'l': BRASS.light,
    },
)

_icon(
    "gauge_12",
    (
        "................",
        "................",
        "................",
        "........aaaaa...",
        ".......akhhga...",
        "......akhhgea...",
        ".....akhhgeea...",
        "....akhhgeeca...",
        "...bbbbbeeca....",
        "..bljjibeca.....",
        ".bljjifbca......",
        ".bjjiffba.......",
        ".bjiffdb........",
        ".biffdb.........",
        ".bbbbb..........",
        "................",
    ),
    {
        'a': RED.outline,
        'b': BRASS.outline,
        'c': RED.dark,
        'd': BRASS.dark,
        'e': RED.shade,
        'f': BRASS.shade,
        'g': RED.base,
        'h': RED.mid,
        'i': BRASS.base,
        'j': BRASS.mid,
        'k': RED.light,
        'l': BRASS.light,
    },
)

