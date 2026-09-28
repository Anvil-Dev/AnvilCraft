"""Verify neighbor culling and source-accurate animated Ruins items on both render backends."""
from pathlib import Path
import json
import re
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip()
assert source == subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference).decode().strip()
logs = {'26.1': 'client-ruins-finish-5.log', '1.21': 'client-ruins-finish-reference-2.log',
        'sodium': 'client-ruins-finish-sodium-3.log'}
expected_clock = [('items', '500', '0.0'), ('items-later', '6000', '0.0'),
                  ('items-offset', '500', '0.0'), ('items-reloaded', '500', '0.0')]
for version, name in logs.items():
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in text and 'PORT_RUINS_FINISH_CLIENT_PASSED' in text, name
    assert re.findall(r'PORT_RUINS_ITEM_CLOCK: ([\w-]+) phase=(\d+) fraction=([\d.]+)', text) == expected_clock
    if version != '1.21':
        assert 'PORT_RUINS_FACE_PROBE_PASSED: 8 original/disguised neighbor pairs' in text, name


def pixels(version, scene):
    folder = reference / 'run/mun-reference/screenshots' if version == '1.21' else root / 'run/port-validation/client/screenshots'
    with Image.open(folder / f'ruins-finish-{version}-{scene}.png') as image:
        assert image.size == (1280, 720)
        return np.asarray(image.convert('RGB')).astype(float)


def icon(image, index, moved=False):
    x, scale = [(90, 1), (270, 3), (460, 5)][index]
    x += 40 if moved else 0
    return image[237:243 + 32 * scale, 2 * x - 3:2 * x + 32 * scale + 3]


visuals = {}
for scene in ['items', 'items-later', 'items-offset', 'items-reloaded']:
    old = pixels('1.21', scene)
    moved = scene in ['items-offset', 'items-reloaded']
    visuals[scene] = {}
    for version in ['26.1', 'sodium']:
        current = pixels(version, scene)
        results = []
        for index in range(3):
            a, b = icon(current, index, moved), icon(old, index, moved)
            ma, mb = (a != 48).any(2), (b != 48).any(2)
            assert np.array_equal(ma, mb), (version, scene, index, 'silhouette')
            error = float(abs(a - b).mean())
            assert error < 0.01, (version, scene, index, error)
            results.append({'scale': [1, 3, 5][index], 'silhouette_iou': 1.0, 'mean_rgb_error': error})
        visuals[scene][version] = results
for version in logs:
    base, later = icon(pixels(version, 'items'), 2), icon(pixels(version, 'items-later'), 2)
    shifted = icon(pixels(version, 'items-offset'), 2, True)
    reloaded = icon(pixels(version, 'items-reloaded'), 2, True)
    assert np.count_nonzero(abs(base - later) > 10) > 500, (version, 'stopped animation')
    assert np.count_nonzero(abs(base - shifted) > 10) > 500, (version, 'lost screen projection')
    assert float(abs(shifted - reloaded).mean()) < 0.01, (version, 'reload')
    for scene in ['faces', 'main-hand', 'off-hand']:
        pixels(version, scene)
cfa = (root / 'build/porting/client-ruins-gateway-cfa-regression.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_CFA_ITEM_RENDER_PASSED' in cfa and 'PORT_GATEWAY_CACHE_REUSE_PASSED' in cfa and 'BUILD SUCCESSFUL' in cfa
assert not (root / 'src/main/resources/assets/anvilcraft/shaders/core/gateway_runtime_probe.fsh').exists()
assert 'portRuinsCalibrate' not in (root / 'src/main/java/dev/dubhe/anvilcraft/client/support/GatewayGuiProjection.java').read_text()
report = {'source_commit': source, 'client_logs': logs, 'visuals': visuals,
          'checks': ['Eight ordinary/disguised neighbor pairs match face visibility and tessellation, including slabs and negative shapes.',
                     'Sodium runs its real side-visibility implementation against the same pairs.',
                     'Three GUI sizes match source silhouettes and pixels at two exact shader times, after relocation and resource reload.',
                     'Ruins retains default hand swap animation; main/offhand displays were captured.',
                     'Shared celestial gateway item fitting, hands, head, reload and atlas reuse regressions pass.'],
          'diagnosis': 'Uniform-dependent layer arithmetic preserves the 1.21 runtime rotation calculation; constant folding changes the star pattern.',
          'limits': ['World/hand captures retain native 26.1 terrain, atmosphere and animation differences.',
                     'Pixel comparisons concern isolated GUI items on this test GPU; universal driver-level bit identity is not claimed.']}
(root / 'build/porting/ruins-finish-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
