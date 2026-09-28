"""Compare the Ruins editor, model transforms and item silhouette in paired client captures."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
native_model = json.loads((root / 'src/main/resources/assets/anvilcraft/models/item/ruins_block.json').read_text())
source_model = json.loads((reference / 'src/generated/resources/assets/anvilcraft/models/item/ruins_block.json').read_text())
assert native_model['display'] == source_model['display']
results = {}
for scene in ('world', 'world-wide', 'invalid', 'editor', 'item'):
    a = np.asarray(Image.open(root / f'run/port-validation/client/screenshots/ruins-foundation-26.1-{scene}.png')
                   .convert('RGB')).astype(float)
    b = np.asarray(Image.open(reference / f'run/mun-reference/screenshots/ruins-foundation-1.21-{scene}.png')
                   .convert('RGB')).astype(float)
    assert a.shape == b.shape == (720, 1280, 3)
    if scene == 'item':
        ma, mb = (a != 48).any(2), (b != 48).any(2)
        iou = float((ma & mb).sum() / (ma | mb).sum())
        assert iou > 0.99, (scene, iou)
        results[scene] = {'silhouette_iou': iou,
                          'note': 'Gateway animation phase differs between independent client runs; texture pixels are not compared.'}
    elif scene in ('invalid', 'editor'):
        a, b = a[148:280, 338:942], b[148:280, 338:942]
        ma, mb = (a >= 230).all(2), (b >= 230).all(2)
        iou = float((ma & mb).sum() / (ma | mb).sum())
        assert iou > 0.98, (scene, iou)
        results[scene] = {'label_and_input_foreground_iou': iou}
        if scene == 'invalid':
            ra = (a[:, :, 0] > 230) & (a[:, :, 1] < 130) & (a[:, :, 2] < 130)
            rb = (b[:, :, 0] > 230) & (b[:, :, 1] < 130) & (b[:, :, 2] < 130)
            red_iou = float((ra & rb).sum() / (ra | rb).sum())
            assert red_iou > 0.98, red_iou
            results[scene]['invalid_input_red_iou'] = red_iou
    else:
        results[scene] = {'note': 'World captures require visual inspection; vanilla terrain/lighting is intentionally retained.'}
for name in ('client-ruins-2.log', 'client-ruins-reference-1.log'):
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_RUINS_FOUNDATION_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log, name
report = {'visuals': results, 'checks': ['Creative conversion through actual client interaction.',
          'Invalid and valid loot IDs, fragile toggle, server-side save and exactly one configured drop.',
          'Source item display transforms and native/source inventory silhouettes.'],
          'pending': ['Fluid rendering', 'Visual-only entity ticking', 'Dynamic selection and expanded culling bounds',
                      'Disguised particles and neighbor face culling', 'Animated effect pixel comparison']}
(root / 'build/porting/ruins-foundation-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
