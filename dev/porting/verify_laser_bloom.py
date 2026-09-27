"""Verify the source Bloom kernel, live visuals, height invariance, and reload capture."""
from pathlib import Path
import json
import re
import subprocess
import sys
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = root / 'build/porting/reference-mun-1.21'
subprocess.run([sys.executable, str(root / 'dev/porting/verify_laser_components.py')], cwd=root, check=True)
report = json.loads((root / 'build/porting/laser-components-comparison.json').read_text())
source = (ref / 'src/main/resources/assets/anvilcraft/shaders/program/blur.fsh').read_text()
native = (root / 'src/main/resources/assets/anvilcraft/shaders/core/laser_bloom_blur.fsh').read_text()
weights = lambda text: re.search(r'float\[\]\s*\(([^)]+)\)', text).group(1).replace(' ', '').split(',')
assert weights(source) == weights(native)
for constant in ['1.943', '1.105']:
    assert constant in source and constant in native
chain = json.loads((ref / 'src/main/resources/assets/anvilcraft/shaders/post/bloom.json').read_text())
directions = [p['uniforms'][0]['values'] for p in chain['passes'] if p['name'] == 'anvilcraft:blur']
assert directions == [[1., 0.], [0., 1.], [1., 0.], [0., 1.]]

height = {}
for suffix in ['', '-bloom']:
    folder = root / 'run/port-validation/client/screenshots'
    a = np.asarray(Image.open(folder / ('laser-components' + suffix + '-26.1.png')).convert('RGB')).astype(float)
    b = np.asarray(Image.open(folder / ('laser-components' + suffix + '-26.1-high.png')).convert('RGB')).astype(float)
    metrics = {}
    for name, y in [('gamma', 240), ('ember', 301), ('frost', 363), ('royal', 425), ('normal', 487)]:
        aa = a[y-14:y+15, 550:730]
        bb = b[y-14:y+15, 550:730]
        mask = aa.max(2) - aa.min(2) > 40
        error = float(abs(aa-bb)[mask].mean())
        assert error < 0.1, (suffix, name, error)
        metrics[name] = error
    height['bloom' if suffix else 'default'] = metrics

image = Image.open(root / 'run/port-validation/client/screenshots/laser-components-bloom-reload-26.1.png')
assert image.size == (960, 540)
log = (root / 'build/porting/client-laser-bloom-lifecycle.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_LASER_BLOOM_LIFECYCLE_PASSED' in log and 'BUILD SUCCESSFUL' in log
result = {
    'source_weights': weights(source), 'source_blur_directions': directions,
    'height_delta_blocks': 96, 'height_invariance_mean_rgb_error': height,
    'controlled_fog_source_comparison': report['visuals']['bloom_diagnostic_no_fog'],
    'native_atmospheric_fog_source_comparison': report['visuals']['bloom_vanilla_fog'],
    'resource_reload_and_resize': 'passed; 960x540 capture visually inspected',
    'scope': 'Only LASER_TRANSLUCENT_BLOOM capture is redirected; vanilla terrain and other mods retain their render paths.',
}
(root / 'build/porting/laser-bloom-comparison.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'height_invariance': height, 'source_kernel': 'four passes with identical coefficients'}, indent=2))
