"""`tools/models.py`, exercised the way the generator is actually used: run it, diff the tree
against a second run (idempotency: no random or wall-clock input anywhere in the generator), check
every box face's uv rectangle the atlas packer hands out actually lies inside that atlas, and check
that no element floats -- every base's own boxes form one connected assembly, and every attachment
part touches the base geometry at its class's anchor (`FA-22`, round 2, Kevin's review: "every
element of a base must share a face or overlap by at least 0.5 units with another element of the
same base, and every part's elements must touch or overlap the base's geometry at that class's
anchor")."""
import subprocess
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
TOOL = TOOLS / "models.py"
ASSET_ROOTS = [
    ROOT / "src/main/resources/assets/firearms/models/item/weapon",
    ROOT / "src/main/resources/assets/firearms/textures/item/weapon",
    ROOT / "src/main/resources/assets/firearms/items/weapon.json",
]

sys.path.insert(0, str(TOOLS))
import models  # noqa: E402  (needs the sys.path insert above)


def _tree_snapshot() -> dict[str, bytes]:
    snapshot: dict[str, bytes] = {}
    for root in ASSET_ROOTS:
        if root.is_file():
            snapshot[str(root)] = root.read_bytes()
            continue
        for path in sorted(root.rglob("*")):
            if path.is_file():
                snapshot[str(path)] = path.read_bytes()
    return snapshot


class ModelsCommandTest(unittest.TestCase):
    def test_regenerating_is_idempotent(self):
        first = subprocess.run([sys.executable, str(TOOL)], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(0, first.returncode, first.stderr)
        before = _tree_snapshot()

        second = subprocess.run([sys.executable, str(TOOL)], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(0, second.returncode, second.stderr)
        after = _tree_snapshot()

        self.assertEqual(before, after, "python3 tools/models.py twice in a row must write byte-identical output")


class AtlasBoundsTest(unittest.TestCase):
    """Every box face the atlas packer hands out must be a rectangle that actually fits inside the
    atlas it packed it into -- the property that keeps `models.py`'s own uvs valid without needing
    to load Minecraft, the same spirit as `ModelAssetsTest`'s Java-side check of the committed
    output."""

    def _assert_every_face_in_bounds(self, label: str, boxes: list[models.Box]) -> None:
        size, _atlas, uv_map = models.build_atlas(boxes)
        for (box_index, face_name), (x0, y0, x1, y1) in uv_map.items():
            for coord in (x0, y0, x1, y1):
                self.assertGreaterEqual(coord, 0, f"{label} box {box_index} face {face_name}: uv {(x0, y0, x1, y1)} in a {size}x{size} atlas")
                self.assertLessEqual(coord, size, f"{label} box {box_index} face {face_name}: uv {(x0, y0, x1, y1)} in a {size}x{size} atlas")
            self.assertLess(x0, x1, f"{label} box {box_index} face {face_name}: uv has zero width")
            self.assertLess(y0, y1, f"{label} box {box_index} face {face_name}: uv has zero height")

    def test_every_weapon_faces_uv_is_inside_its_own_atlas(self):
        for weapon_id, boxes in models.WEAPON_BOXES.items():
            self._assert_every_face_in_bounds(weapon_id, boxes)

    def test_every_part_faces_uv_is_inside_its_own_atlas(self):
        for slot, names in models.ATTACHMENTS.items():
            for name in names:
                self._assert_every_face_in_bounds(f"{slot}_{name}", models.PART_BOXES[slot][name])

    def test_every_class_has_an_anchor_for_every_slot_it_carries(self):
        for class_name, slots in models.WEAPON_CLASSES.items():
            for slot in slots:
                self.assertIn(slot, models.PART_ANCHOR[class_name], f"{class_name} has no anchor for its own {slot} slot")


class NoFloatingElementsTest(unittest.TestCase):
    """Kevin's round-2 connectivity rule, enforced: every element of a base weapon reaches every
    other element of that same base through `models.boxes_connected` edges, and every attachment
    part -- translated to a given class's own `PART_ANCHOR` -- touches or overlaps that class's own
    weapon geometry."""

    def test_every_base_is_one_connected_assembly(self):
        for weapon_id, boxes in models.WEAPON_BOXES.items():
            ok, missing = models.is_internally_connected(boxes)
            self.assertTrue(ok, f"{weapon_id}: element(s) {missing} of {len(boxes)} do not share a face or "
                                 f"overlap by >=0.5 units with the rest of the assembly")

    def test_every_part_touches_its_classs_own_weapon_geometry(self):
        floating = []
        for class_name, slots in models.WEAPON_CLASSES.items():
            base_boxes = models.WEAPON_BOXES[models.WEAPON_FOR_CLASS[class_name]]
            for slot in slots:
                for name in models.ATTACHMENTS[slot]:
                    anchor = models.PART_ANCHOR[class_name][slot]
                    translated = [
                        models.Box(
                            (b.frm[0] + anchor[0], b.frm[1] + anchor[1], b.frm[2] + anchor[2]),
                            (b.to[0] + anchor[0], b.to[1] + anchor[1], b.to[2] + anchor[2]),
                            b.material,
                        )
                        for b in models.PART_BOXES[slot][name]
                    ]
                    if not models.part_touches_base(base_boxes, translated):
                        floating.append(f"{class_name} {slot}_{name}")
        self.assertEqual([], floating, f"parts with no element touching their base's geometry: {floating}")


if __name__ == "__main__":
    unittest.main()
