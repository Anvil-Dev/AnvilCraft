"""Verify monolith resources, loot and native offering snapshots against 1.21."""
from pathlib import Path
import json
import numpy as np
from PIL import Image, ImageFilter

root = Path(__file__).resolve().parents[2]
source = root / 'build/porting/reference-mun-1.21'
names = ['monolith', 'monolith_core', 'monolith_line', 'giant_monolith_core', 'giant_monolith_line']
assets = []
for folder in ['blockstates', 'models/block', 'textures/block']:
    for original in (source / 'src/main/resources/assets/anvilcraft' / folder).glob('*monolith*'):
        path = original.relative_to(source)
        assert original.read_bytes() == (root / path).read_bytes(), str(path)
        assets.append(str(path))
for name in names:
    path = Path(f'src/generated/resources/data/anvilcraft/loot_table/blocks/{name}.json')
    assert json.loads((source / path).read_text(encoding='utf-8')) == json.loads((root / path).read_text(encoding='utf-8')), name
for name in ['client-monolith-target-final-2.log', 'client-monolith-source-final-3.log']:
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    for marker in ['PORT_MONOLITH_ONLINE_REWARD_PASSED', 'PORT_MONOLITH_VISUAL_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
        assert marker in log, (name, marker)
tests = (root / 'build/porting/tests-monolith-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_MONOLITH_OFFERING_PASSED' in tests and 'BUILD SUCCESSFUL' in tests
report = {'source_assets_equal': assets, 'source_loot_tables_equal': names, 'effects': {},
          'limits': ['World generation, proximity hints and confirmed return remain pending.',
                     'Native 26.1 terrain sampling and sky rendering are retained.']}
folders = [(source / 'run/mun-reference/screenshots', '1.21'), (root / 'run/port-validation/client/screenshots', '26.1')]
for age in [20, 50, 65, 80, 95]:
    masks = []
    for folder, version in folders:
        frame = np.asarray(Image.open(folder / f'monolith-{version}-{age}.png').convert('RGB'), dtype=np.int16)
        baseline = np.asarray(Image.open(folder / f'monolith-{version}-100.png').convert('RGB'), dtype=np.int16)
        mask = np.max(np.abs(frame - baseline), axis=2) > 4
        mask[:245] = False
        mask[535:] = False
        mask[:, :535] = False
        mask[:, 760:] = False
        masks.append(mask)
    union = int(np.logical_or(*masks).sum())
    iou = float(np.logical_and(*masks).sum() / union) if union else 1.0
    dilation = [np.asarray(Image.fromarray(mask.astype('uint8') * 255).filter(ImageFilter.MaxFilter(3))) > 0 for mask in masks]
    outside = int((masks[0] & ~dilation[1]).sum() + (masks[1] & ~dilation[0]).sum())
    assert masks[0].sum() > 0 and masks[1].sum() > 0, age
    assert iou > 0.98 and outside == 0, (age, iou, outside)
    report['effects'][age] = {'source_pixels': int(masks[0].sum()), 'target_pixels': int(masks[1].sum()),
                             'mask_iou': iou, 'pixels_beyond_one_pixel': outside}
(root / 'build/porting/monolith-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
