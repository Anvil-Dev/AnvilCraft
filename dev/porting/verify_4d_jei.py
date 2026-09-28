"""Verify 4D JEI registration, interactions, resources and real layout silhouettes."""
from collections import deque
from pathlib import Path
import hashlib
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / "build/porting/reference-mun-1.21"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()
for name in ["client-4d-jei-3.log", "client-4d-jei-reference-1.log"]:
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert "PORT_4D_JEI_PASSED" in text and "BUILD SUCCESSFUL" in text, name
    assert text.count("PORT_4D_JEI_CAPTURED:") == 6, name
regression = (root / "build/porting/client-4d-jei-3d-regression.log").read_text(encoding="utf-8", errors="replace")
assert "PORT_MULTIBLOCK_CLIENT_PASSED" in regression and "BUILD SUCCESSFUL" in regression
assert "BUILD SUCCESSFUL" in (root / "build/porting/data-4d-jei-2.log").read_text(encoding="utf-8", errors="replace")
assert "BUILD SUCCESSFUL" in (root / "build/porting/checks-4d-jei-final.log").read_text(encoding="utf-8", errors="replace")
asset = Path("src/main/resources/assets/anvilcraft/textures/gui/jei/multiblock/4d_multiblock.png")
assert (root / asset).read_bytes() == (reference / asset).read_bytes()
for lang, base in [("en_us", "src/generated/resources"), ("zh_cn", "src/main/resources")]:
    path = Path(base) / "assets/anvilcraft/lang" / (lang + ".json")
    native = json.loads((root / path).read_text(encoding="utf-8"))
    expected = json.loads((reference / path).read_text(encoding="utf-8"))
    for key in ["gui.anvilcraft.category.4d_multiblock", "gui.anvilcraft.category.4d_multiblock.step"]:
        assert native[key] == expected[key], (lang, key)


def silhouette(path):
    # Isolate the structure, excluding the adjacent controls and size caption.
    pixels = np.array(Image.open(path).convert("RGB").crop((480, 225, 653, 410)))
    mask = np.any(np.abs(pixels.astype(int) - 198) > 2, axis=2)
    mask[175:, 130:] = False
    outside = np.zeros(mask.shape, bool)
    pending = deque((y, x) for y in range(mask.shape[0]) for x in range(mask.shape[1])
                    if y in (0, mask.shape[0]-1) or x in (0, mask.shape[1]-1))
    while pending:
        y, x = pending.popleft()
        if y < 0 or x < 0 or y >= mask.shape[0] or x >= mask.shape[1] or outside[y, x] or mask[y, x]:
            continue
        outside[y, x] = True
        pending.extend([(y-1, x), (y+1, x), (y, x-1), (y, x+1)])
    return ~outside


comparisons = {}
for name in ["step1", "step2", "step3", "layer2", "previous-layer"]:
    actual = silhouette(root / f"run/port-validation/client/screenshots/4d-jei-26.1-{name}.png")
    expected = silhouette(reference / f"run/mun-reference/screenshots/4d-jei-1.21-{name}.png")
    iou = float(np.logical_and(actual, expected).sum() / np.logical_or(actual, expected).sum())
    assert iou > 0.99, (name, iou)
    comparisons[name] = iou
report = {
    "source_commit": source,
    "client_layouts_per_version": 6,
    "structure_silhouette_iou": comparisons,
    "texture_sha256": hashlib.sha256((root / asset).read_bytes()).hexdigest(),
    "contracts": [
        "The 4D category registers recipes, four crafting stations and input/output recipe lookup.",
        "Repeated items aggregate across steps (81 tempering glass); repeated tags aggregate choices with count 54.",
        "Actual JEI layout buttons wrap time steps, share layer mode and retain independent per-step layer indices.",
        "Source preview anchor, scale, rotation and conversion texture are restored through native 26.1 rendering.",
        "Shared zero-based structure coordinates expose all layers; both preview paths restrict block-entity submission to the visible layer.",
        "Existing 3D crafting/conversion client layouts and tag cycling pass regression verification."
    ],
    "limits": [
        "Screenshot comparisons measure structure silhouettes, not pixel-identical vanilla lighting or texture filtering.",
        "Recipe indexing is verified through live JEI focus lookups; keyboard bindings themselves are unchanged.",
        "This node covers JEI presentation; scanner export integration remains a separate audit."
    ]
}
(root / "build/porting/4d-jei-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
