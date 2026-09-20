"""Every hand-authored grid in `pixel_art.py` (FA-21 round five) is well-formed: 16 rows of 16
characters, every character present in its own legend (or the reserved transparent `.`), the outer
one-pixel ring transparent (round-four regression: a capsule's brush reached column/row 0), and at
least four distinct opaque colours (round four's flat capsules read as one or two tones; round
five's three-quarter-view boxes and tubes need outline, dark-or-shade, base and mid-or-light to read
as a solid, not a silhouette)."""
import unittest

import pixel_art


class GridShapeTest(unittest.TestCase):
    def test_every_grid_is_sixteen_rows_of_sixteen_characters(self):
        offenders = [
            name for name, (rows, _legend) in pixel_art.ICONS.items()
            if len(rows) != 16 or any(len(row) != 16 for row in rows)
        ]
        self.assertEqual([], offenders, f"not 16x16: {offenders}")

    def test_every_grid_character_is_in_its_own_legend(self):
        offenders = []
        for name, (rows, legend) in pixel_art.ICONS.items():
            for row in rows:
                for ch in row:
                    if ch != "." and ch not in legend:
                        offenders.append(f"{name}: {ch!r} not in its legend")
        self.assertEqual([], offenders, f"grid characters missing from their legend: {offenders}")


class OuterRingTest(unittest.TestCase):
    def test_every_grid_keeps_its_outer_ring_transparent(self):
        offenders = []
        for name, (rows, _legend) in pixel_art.ICONS.items():
            top, bottom = rows[0], rows[-1]
            left = "".join(row[0] for row in rows)
            right = "".join(row[-1] for row in rows)
            if any(ring != "." * 16 for ring in (top, bottom)) or \
                    any(ch != "." for ch in left) or any(ch != "." for ch in right):
                offenders.append(name)
        self.assertEqual([], offenders, f"grids reaching the canvas edge: {offenders}")


class DistinctColourTest(unittest.TestCase):
    def test_every_icon_has_at_least_four_distinct_opaque_colours(self):
        offenders = {}
        for name, (rows, legend) in pixel_art.ICONS.items():
            used = {legend[ch] for row in rows for ch in row if ch != "."}
            if len(used) < 4:
                offenders[name] = len(used)
        self.assertEqual({}, offenders, f"icons with fewer than four opaque colours: {offenders}")


if __name__ == "__main__":
    unittest.main()
