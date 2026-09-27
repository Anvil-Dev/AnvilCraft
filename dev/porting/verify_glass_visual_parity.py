"""Compare deterministic source/native glass-pipe captures without changing vanilla lighting."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source_ref = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip()
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference).decode().strip() == source_ref
for name in ['client-glass-parity-final.log', 'client-glass-parity-source-final.log']:
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in text and 'PORT_GLASS_PARITY_PASSED' in text, name
    assert 'All dimensions are saved' in text, name
    for stage in range(6):
        assert f'PORT_GLASS_PARITY_CAPTURED: stage={stage}' in text, (name, stage)

captures = {}
for name, folder, version in [
    ('source', reference / 'run/mun-reference/screenshots', '1.21'),
    ('native', root / 'run/port-validation/client/screenshots', '26.1'),
]:
    captures[name] = [np.asarray(Image.open(folder / f'glass-parity-{version}-{stage}.png').convert('RGB'),
                                dtype=float) for stage in range(6)]

report = {'source_commit': source_ref, 'pipes': {}, 'stages': [0, 0.25, 0.5, 1, 1 / 255, 'water/lava'],
          'limits': ['Fixed view and frozen animation frame isolate glass shells and fluid layers.',
                     'Water in gas-display mode isolates opacity; gas simulation is covered separately.',
                     'Vanilla 26.1 lighting and terrain texture sampling are retained.']}
for pipe, (x, y, end_x, end_y) in {
    'straight': (470, 420, 572, 510), 'corner': (650, 410, 714, 507), 'node': (790, 415, 895, 525),
}.items():
    masks, responses, liquid_masks = {}, {}, {}
    result = {}
    for version, images in captures.items():
        data = [image[y:end_y, x:end_x] for image in images]
        full = data[3] - data[0]
        mask = np.max(np.abs(full), axis=2) > 15
        assert mask.sum() > 800, (pipe, version, 'missing fluid')
        masks[version] = mask
        responses[version] = [float(np.sum((data[stage] - data[0])[mask] * full[mask])
                                    / np.sum(full[mask] ** 2)) for stage in [1, 2, 4]]
        liquid = data[5]
        liquid_masks[version] = ((liquid[:, :, 0] > liquid[:, :, 2] + 50) if pipe == 'corner'
                                 else (liquid[:, :, 2] > liquid[:, :, 0] + 35))
        result[version] = {'changed_pixels': int(mask.sum()), 'opacity_response': responses[version]}
    delta = np.abs(np.array(responses['source']) - responses['native'])
    assert np.max(delta) < 0.006, (pipe, 'opacity response', delta)
    assert delta[2] < 0.0002, (pipe, 'low-density gas was clipped', delta[2])
    overlap = np.sum(masks['source'] & masks['native']) / np.sum(masks['source'] | masks['native'])
    assert overlap > 0.85, (pipe, 'fluid silhouette', overlap)
    liquid_overlap = (np.sum(liquid_masks['source'] & liquid_masks['native'])
                      / np.sum(liquid_masks['source'] | liquid_masks['native']))
    assert liquid_overlap > 0.85, (pipe, 'liquid silhouette', liquid_overlap)
    result.update({'max_opacity_response_delta': float(delta.max()), 'fluid_silhouette_iou': float(overlap),
                   'liquid_silhouette_iou': float(liquid_overlap)})
    report['pipes'][pipe] = result

(root / 'build/porting/glass-visual-parity.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
