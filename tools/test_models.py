"""`tools/models.py`, exercised the way the generator is actually used: run it, diff the tree
against a second run (idempotency: no random or wall-clock input anywhere in the generator), check
every box face's uv rectangle the atlas packer hands out actually lies inside that atlas, check that
the written model JSON's own `uv` bakes inside its real atlas PNG the way vanilla's `FaceBakery`
actually reads it (`FA-26`), and check that no element floats -- every base's own boxes form one
connected assembly, and every attachment part touches the base geometry at its class's anchor
(`FA-22`, round 2, Kevin's review: "every element of a base must share a face or overlap by at least
0.5 units with another element of the same base, and every part's elements must touch or overlap the
base's geometry at that class's anchor")."""
import json
import subprocess
import sys
import unittest
from pathlib import Path

from PIL import Image

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
TOOL = TOOLS / "models.py"
WEAPON_MODELS = ROOT / "src/main/resources/assets/firearms/models/item/weapon"
ASSET_ROOTS = [
    WEAPON_MODELS,
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


class RealBakeUvBoundsTest(unittest.TestCase):
    """`FA-26` (Kevin, "the 3D models still show the missing-texture default" --
    `Cannot compute translucency out of bounds: [52, 0, 60, 4] in 32x32 image`): 26.2's cuboid
    item-model bake (`FaceBakery.bakeQuad` -> `computeMaterialTransparency` ->
    `SpriteContents.computeTransparency` -> `NativeImage.computeTransparency`) never reads a model's
    declared `texture_size` -- decompiling the shipped client showed `CuboidModel$Deserializer`
    does not parse that key at all, and both `CuboidFace.getU`/`getV` and
    `computeMaterialTransparency` divide every raw `uv` number by a hardcoded `16.0F` before
    multiplying by the atlas PNG's *real* pixel dimensions (`x0 = floor(u0 * this.width)` with
    `u0 = rawU / 16`). `uv` must always be authored in that fixed nominal 16-unit face space,
    exactly like `from`/`to`, regardless of how large the backing atlas actually is; a declared
    `texture_size` is dead JSON vanilla never consults. This test mimics vanilla's own bake math
    directly against the committed model JSON and its real atlas PNG, the same contract
    `ModelAssetsTest.everyWeaponModelElementFaceUvLiesInsideItsRealAtlasPng` checks on the Java
    side -- it would have failed before the `_px_to_uv16` fix in `models.py`, since round 1/2 wrote
    `uv` directly in atlas-pixel units (only correct for faces packed inside the first 16x16
    pixels of their atlas)."""

    def test_every_committed_model_uv_bakes_inside_its_real_atlas_png(self):
        offenders = []
        for model_file in sorted(WEAPON_MODELS.rglob("*.json")):
            data = json.loads(model_file.read_text())
            elements = data.get("elements")
            if not elements:
                continue  # a bare per-class display parent carries no elements.
            texture_ref = data["textures"]["0"]
            png_path = ROOT / "src/main/resources/assets" / texture_ref.replace(":", "/textures/", 1)
            png_path = png_path.with_suffix(".png")
            with Image.open(png_path) as image:
                real_w, real_h = image.size
            for element in elements:
                for face_name, face in element["faces"].items():
                    u0, v0, u1, v1 = face["uv"]
                    # Vanilla's own math: pixel = (uv / 16) * the atlas PNG's real pixel dimension.
                    x0, y0, x1, y1 = u0 / 16 * real_w, v0 / 16 * real_h, u1 / 16 * real_w, v1 / 16 * real_h
                    for x in (x0, x1):
                        if not (-1e-6 <= x <= real_w + 1e-6):
                            offenders.append(f"{model_file}: {face_name} uv {face['uv']} -> x={x} outside 0..{real_w}")
                    for y in (y0, y1):
                        if not (-1e-6 <= y <= real_h + 1e-6):
                            offenders.append(f"{model_file}: {face_name} uv {face['uv']} -> y={y} outside 0..{real_h}")
        self.assertEqual([], offenders, "every committed model's face uv bakes inside its real atlas PNG (vanilla's own /16 math)")


class OrientationTest(unittest.TestCase):
    """Round 3 (Kevin's in-game verdict on round 2: "they point at the player instead of away from
    him"): every base's own `role="muzzle"` box must sit at the assembly's own smallest Z and its
    `role="stock"` box at the largest -- -Z is the muzzle end, +Z is the stock/grip end, matching
    Create Fly's own potato cannon (`models.py`'s module docstring, orientation section)."""

    def test_muzzle_end_is_the_smallest_z(self):
        for weapon_id, boxes in models.WEAPON_BOXES.items():
            muzzle_boxes = [b for b in boxes if b.role == "muzzle"]
            stock_boxes = [b for b in boxes if b.role == "stock"]
            self.assertEqual(1, len(muzzle_boxes), f"{weapon_id}: exactly one box should carry role='muzzle'")
            self.assertEqual(1, len(stock_boxes), f"{weapon_id}: exactly one box should carry role='stock'")
            muzzle_min_z = min(muzzle_boxes[0].frm[2], muzzle_boxes[0].to[2])
            stock_max_z = max(stock_boxes[0].frm[2], stock_boxes[0].to[2])
            all_z = [c for b in boxes for c in (b.frm[2], b.to[2])]
            self.assertEqual(min(all_z), muzzle_min_z,
                              f"{weapon_id}: the muzzle box's own smallest Z ({muzzle_min_z}) is not the "
                              f"whole assembly's smallest Z ({min(all_z)}) -- some other element pokes out further")
            self.assertEqual(max(all_z), stock_max_z,
                              f"{weapon_id}: the stock/grip box's own largest Z ({stock_max_z}) is not the "
                              f"whole assembly's largest Z ({max(all_z)}) -- some other element pokes out further")
            self.assertLess(muzzle_min_z, stock_max_z, f"{weapon_id}: muzzle end is not forward of the stock/grip end")


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
