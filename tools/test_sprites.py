"""Every icon and cartridge `sprites.py` draws keeps its outer 1px ring transparent
(`DEC-018-art-direction.md`; round-four review: a fixed diagonal anchor let several capsules'
brushes reach column/row 0) -- exercised against the actual drawing functions, not the committed
PNGs, so a regression fails before anyone runs `python3 tools/sprites.py`."""
import unittest

import sprites


def touches_outer_ring(img) -> bool:
    w, h = img.size
    for x in range(w):
        if img.getpixel((x, 0))[3] or img.getpixel((x, h - 1))[3]:
            return True
    for y in range(h):
        if img.getpixel((0, y))[3] or img.getpixel((w - 1, y))[3]:
            return True
    return False


class OuterRingTest(unittest.TestCase):
    def test_every_attachment_icon_keeps_its_outer_ring_transparent(self):
        offenders = [
            f"{slot}/{name}"
            for slot, names in sprites.ATTACHMENTS.items()
            for name in names
            if touches_outer_ring(sprites.attachment_icon(slot, name))
        ]
        self.assertEqual([], offenders, f"attachment icons reaching the canvas edge: {offenders}")

    def test_every_cartridge_keeps_its_outer_ring_transparent(self):
        offenders = [
            caliber
            for caliber, (rows, width, tip_ramp, boat_tail) in sprites.CARTRIDGES.items()
            if touches_outer_ring(sprites.cartridge_sprite(rows, width, tip_ramp, boat_tail))
        ]
        if touches_outer_ring(sprites.shotshell_sprite()):
            offenders.append("gauge_12")
        self.assertEqual([], offenders, f"cartridges reaching the canvas edge: {offenders}")


if __name__ == "__main__":
    unittest.main()
