"""Verify Ruins selection/bounds probes and record native, source and Sodium visual crops."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip()
assert source == subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference).decode().strip()
logs = ['client-ruins-geometry-4.log', 'client-ruins-geometry-reference-1.log', 'client-ruins-geometry-sodium-3.log']
for name in logs:
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_RUINS_GEOMETRY_CLIENT_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
    if 'reference' not in name:
        assert 'PORT_RUINS_GEOMETRY_PROBE_PASSED' in text, name
sodium_log = (root / 'build/porting' / logs[-1]).read_text(encoding='utf-8', errors='replace')
assert 'SodiumRuinsMeshingMixin' in sodium_log
visuals = {}
for scene, crop in {'crate': (410, 220, 870, 530), 'arm': (570, 205, 710, 449), 'slab': (570, 300, 710, 420)}.items():
    images = {}
    for version in ['26.1', '1.21', 'sodium']:
        folder = reference / 'run/mun-reference/screenshots' if version == '1.21' else root / 'run/port-validation/client/screenshots'
        with Image.open(folder / f'ruins-geometry-{version}-{scene}.png') as image:
            assert image.size == (1280, 720)
            images[version] = np.asarray(image.convert('RGB').crop(crop)).astype(float)
    visuals[scene] = {
        'source_crop_mean_rgb_error': float(abs(images['26.1'] - images['1.21']).mean()),
        'sodium_crop_mean_rgb_error': float(abs(images['26.1'] - images['sodium']).mean())
    }
report = {
    'source_commit': source,
    'client_logs': logs,
    'visuals': visuals,
    'checks': [
        'CubeSelection targets retain the original block state, geometry and dynamic pose through the disguise.',
        'Multipart render bounds include the whole structure; selected geometry lies within render bounds.',
        'Render distance is measured from model extent, preserving objects near the view-distance boundary.',
        'Actual extraction events produce outlines for the targeted crate, mechanical arm and vanilla slab.',
        'Native and Sodium section/global lists contain each disguise on exactly one path.',
        'Switching a beacon disguise to stone and back updates both native and Sodium collection lists.'
    ],
    'limits': ['Crops omit UI overlays and retain native 26.1 lighting/sampling differences.',
               'Neighbor solid-block face culling and gateway item animation comparison remain separate work.']
}
(root / 'build/porting/ruins-geometry-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
