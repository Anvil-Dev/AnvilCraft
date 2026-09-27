"""Compare enchanted-gold data, source behavior and fixed-phase native glint captures."""
from pathlib import Path
import json
import re
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'


def source(path):
    return subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root)


def normalize(value):
    if isinstance(value, list):
        return [normalize(item) for item in value]
    if isinstance(value, dict):
        if set(value) == {'item'}:
            return value['item']
        if set(value) == {'tag'}:
            return '#' + value['tag']
        result = {key: normalize(item) for key, item in value.items()}
        if 'result' in result and isinstance(result['result'], dict):
            result['result'].setdefault('count', 1)
        return result
    return value


recipes = ['enchanted_gold_block', 'enchanted_gold_ingot_from_enchanted_gold_block',
           'enchanted_gold_ingot_from_enchanted_gold_nugget', 'enchanted_gold_nugget', 'enchanted_golden_apple',
           'gold_ingot_from_enchanted_and_cursed', 'gold_nugget_from_enchanted_and_cursed', 'gold_block_from_enchanted_and_cursed']
for name in recipes:
    path = f'src/generated/resources/data/anvilcraft/recipe/{name}.json'
    assert normalize(json.loads((root / path).read_bytes())) == normalize(json.loads(source(path))), path
assets = ['textures/block/enchanted_gold_block.png', 'textures/item/enchanted_gold_ingot.png',
          'textures/item/enchanted_gold_nugget.png', 'ageratum/en_us/002_material/210_enchanted_gold.md',
          'ageratum/zh_cn/002_material/210_enchanted_gold.md']
for name in assets:
    path = 'src/main/resources/assets/anvilcraft/' + name
    assert (root / path).read_bytes() == source(path), path
for name in ['blockstates/enchanted_gold_block.json', 'models/block/enchanted_gold_block.json',
             'models/item/enchanted_gold_ingot.json', 'models/item/enchanted_gold_nugget.json']:
    path = 'src/generated/resources/assets/anvilcraft/' + name
    assert json.loads((root / path).read_bytes()) == json.loads(source(path)), path
source_block_item = json.loads(source('src/generated/resources/assets/anvilcraft/models/item/enchanted_gold_block.json'))
native_block_item = json.loads((root / 'src/generated/resources/assets/anvilcraft/items/enchanted_gold_block.json').read_bytes())
assert native_block_item['model'] == {'type': 'minecraft:model', 'model': source_block_item['parent']}
for filename in ['client-enchanted-gold-final.log', 'client-enchanted-gold-source-fixed.log']:
    log = (root / 'build/porting' / filename).read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in log and 'PORT_ENCHANTED_GOLD_CLIENT_PASSED' in log, filename
    assert 'All dimensions are saved' in log, filename
    for phase in range(5):
        assert f'PORT_ENCHANTED_GOLD_CAPTURE: {phase}' in log
    if filename == 'client-enchanted-gold-final.log':
        assert 'PORT_ENCHANTED_GOLD_JEI_PASSED' in log
validation = (root / 'build/porting/tests-enchanted-gold-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in validation and 'All 610 required tests passed' in validation
images = {}
for name, folder, version in [('source', 'build/porting/reference-mun-1.21/run/mun-reference/screenshots', '1.21'),
                              ('native', 'run/port-validation/client/screenshots', '26.1')]:
    images[name] = [np.asarray(Image.open(root / folder / f'enchanted-gold-{version}-{phase}.png').convert('RGB'), dtype=float)
                    for phase in range(5)]
report = {'source_commit': ref, 'recipes_equivalent': recipes, 'source_assets_equal': assets, 'glint_phases': {},
          'required_tests_passed': 610, 'limits': ['Vanilla 26.1 terrain sampling, light and fog remain active.',
          'The glint phase is fixed only in the paired developer scenes; blue-channel increments avoid saturated gold highlights.']}
for phase in [1, 2]:
    src = images['source'][phase][430:555, 475:965, 2]
    dst = images['native'][phase][430:555, 475:965, 2]
    a = src - images['source'][0][430:555, 475:965, 2]
    b = dst - images['native'][0][430:555, 475:965, 2]
    mask = (a > 10) & (b > 10) & (src < 250) & (dst < 250)
    assert int(mask.sum()) > 15000, (phase, 'missing glint coverage')
    error = np.abs(a[mask] - b[mask])
    assert error.mean() < 2 and error.max() <= 5, (phase, error.mean(), error.max())
    report['glint_phases'][phase] = {'pixels': int(mask.sum()), 'mean_blue_error': float(error.mean()),
                                   'maximum_blue_error': float(error.max())}
    for version in images:
        delta = images[version][phase][510:555, 745:810] - images[version][0][510:555, 745:810]
        assert np.max(np.abs(delta)) <= 2, (version, phase, 'glint leaked onto the occluding stone')
(root / 'build/porting/enchanted-gold-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
