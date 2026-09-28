"""Check real source/native power-line raster output, lifecycle and laser coexistence."""
from pathlib import Path
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = subprocess.check_output(["git", "rev-parse", "dev/1.21/1.6"], cwd=root, text=True).strip()
reference = root / "build/porting/reference-mun-1.21"
assert source == subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=reference, text=True).strip()


def log(name, marker, success=True):
    text = (root / "build/porting" / name).read_text(encoding="utf-8", errors="replace")
    assert marker in text and (not success or "BUILD SUCCESSFUL" in text), name


def pixels(version, name, high=False):
    folder = root / "run/port-validation/client/screenshots" if version == "26.1" else reference / "run/mun-reference/screenshots"
    path = folder / f"power-lines-{version}-{name}{'-high' if high else ''}.png"
    result = np.asarray(Image.open(path).convert("RGB")).astype(float)
    if name == "reload":
        assert result.shape[:2] == (540, 960)
        return result[130:420, 260:700]
    return result[170:600, 360:920]


def cyan(image):
    return image[:, :, 2] - image[:, :, 0] > 8


for name in ["client-power-lines-2.log", "client-power-lines-high.log", "client-power-lines-reference-1.log"]:
    log(name, "PORT_POWER_LINES_PASSED: 7")
log("client-power-lines-mixed.log", "PORT_POWER_LINES_MIXED_PASSED", False)
log("client-power-lines-reference-mixed.log", "PORT_POWER_LINES_MIXED_PASSED")
log("checks-power-lines-final.log", "Task :checkPortJava")
log("client-laser-power-line-regression.log", "PORT_LASER_CLIENT_PASSED")
comparison = {}
height = {}
for case in ["plain", "bloom", "bloom-hidden", "occluded", "reload"]:
    actual, expected = pixels("26.1", case), pixels("1.21", case)
    mask = cyan(actual) | cyan(expected)
    error = float(abs(actual-expected)[mask].mean())
    assert error < 4, (case, error)
    comparison[case] = error
    high = pixels("26.1", case, True)
    mask = cyan(actual) | cyan(high)
    error = float(abs(actual-high)[mask].mean())
    assert error < 0.01, (case, "height", error)
    height[case] = error
actual, expected = pixels("26.1", "plain"), pixels("1.21", "plain")
a = actual[:, :, 2] - actual[:, :, 0] > 30
b = expected[:, :, 2] - expected[:, :, 0] > 30
assert np.array_equal(a, b) and a.sum() > 2000
for case in ["plain-hidden", "disabled"]:
    assert not cyan(pixels("26.1", case)).any(), case
assert not cyan(pixels("1.21", "plain-hidden")).any()
assert cyan(pixels("1.21", "disabled")).sum() > 1000  # Source returns before clearing its last bloom target.
for version in ["26.1", "1.21"]:
    assert not cyan(pixels(version, "occluded")[:, 250:310]).any(), "Opaque wall must hide the middle segment"
    assert np.array_equal(cyan(pixels(version, "bloom")), cyan(pixels(version, "bloom-hidden")))
mixed = {}
for case in ["plain", "bloom"]:
    actual, expected = pixels("26.1", "mixed-" + case), pixels("1.21", "mixed-" + case)
    actual, expected = actual[50:380, 130:430], expected[50:380, 130:430]
    mask = (actual.max(2)-actual.min(2)>15) | (expected.max(2)-expected.min(2)>15)
    error = float(abs(actual-expected)[mask].mean())
    assert error < 8, (case, "coexistence", error)
    assert (actual[:, :, 0] - actual[:, :, 2] > 60).sum() > 1000
    assert cyan(actual).sum() > 1000
    mixed[case] = error
report = {
    "source_commit": source, "source_native_mean_rgb_difference": comparison,
    "source_plain_line_silhouette": "identical", "height_delta_blocks": 96,
    "height_mean_rgb_difference": height, "mixed_laser_line_mean_rgb_difference": mixed,
    "contracts": [
        "Transmitter lines use the same source four-pass bloom processor as lasers instead of the library's different bloom kernel.",
        "Line width matches source max(2.5, framebufferWidth/1920*2.5); bounds and transmitter lines share that rendering path.",
        "Enhanced lines remain visible with HUD hidden, while the ordinary fallback respects the HUD switch.",
        "Opaque geometry occludes lines, resource reload and resizing to 960x540 preserve output, and moving the scene up 96 blocks is invariant.",
        "Live transmitter snapshots and cached laser geometry coexist without overwriting one another."
    ],
    "limits": [
        "The source disabled-line case retains its old bloom target; native deliberately clears visibility when the option is disabled instead of reproducing that stale image.",
        "Raw RGB comparisons retain different vanilla backgrounds; they establish close visual parity rather than pixel-identical terrain/fog/blending.",
        "The scene supplies client grid geometry to the normal production submit path; networking, topology construction and large-grid HUD data remain separate audits.",
        "The first mixed run passed its live checks and failed only two fixture style issues; final compile and Checkstyle passed after formatting cleanup.",
        "Shader-pack-specific and Fabulous-mode live acceptance are not claimed by these scenes."
    ]
}
(root / "build/porting/power-lines-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2))
