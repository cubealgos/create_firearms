"""`tools/models.py`, exercised the way the generator is actually used: run it, diff the tree
against a second run (idempotency: no random or wall-clock input anywhere in the generator), and
check every box face's uv rectangle the atlas packer hands out actually lies inside that atlas
(`FA-22`)."""
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


if __name__ == "__main__":
    unittest.main()
