"""Check paired live fluid scenes and the native extraction/section-boundary probe."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip()
assert source == subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference).decode().strip()
for name in ['client-ruins-fluid-2.log', 'client-ruins-fluid-reference-2.log']:
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_RUINS_FLUID_CLIENT_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
native = (root / 'build/porting/client-ruins-fluid-2.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_RUINS_FLUID_GEOMETRY_PASSED:' in native and '10 fluid/section fixtures' in native
visuals = {}
for scene in ['day', 'night']:
    a = np.asarray(Image.open(root / f'run/port-validation/client/screenshots/ruins-fluid-26.1-{scene}.png')
                   .convert('RGB')).astype(float)
    b = np.asarray(Image.open(reference / f'run/mun-reference/screenshots/ruins-fluid-1.21-{scene}.png')
                   .convert('RGB')).astype(float)
    assert a.shape == b.shape == (720, 1280, 3)
    crops = {'water': (735, 249, 789, 280), 'waterlogged_slab': (750, 287, 807, 339),
             'lava': (760, 340, 842, 415), 'oil': (796, 435, 894, 535)}
    differences = {}
    for fluid, (x0, y0, x1, y1) in crops.items():
        first, second = a[y0:y1, x0:x1], b[y0:y1, x0:x1]
        differences[fluid] = float(abs(first - second).mean())
    visuals[scene] = {'crop_mean_rgb_error': differences}
report = {
    'source_commit': source,
    'visuals': visuals,
    'checks': ['Water, waterlogged slab, lava and oil are visible in paired day/night client scenes.',
               'Native captured quads contain visible vertices confined to local block coordinates.',
               'Positive and negative section boundaries preserve local placement.',
               'Adjacent disguised water omits internal faces.'],
    'limits': ['Image errors include surrounding glass, native 26.1 terrain lighting and independent texture animation phases.',
               'Crops are diagnostic measurements; visual inspection and geometry assertions determine this checkpoint.',
               'Neighbor solid-block face culling, dynamic selection and expanded render bounds remain separate work.']
}
(root / 'build/porting/ruins-fluid-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
