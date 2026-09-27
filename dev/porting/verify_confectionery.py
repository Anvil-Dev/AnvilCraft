"""Verify source confectionery resources, recipes, behavior checks and paired item captures."""
from pathlib import Path
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root, text=True).strip() == source
names = ['honey_cream_block', 'honey_cake_block', 'matcha_cream_block', 'matcha_cake_block',
         'cookie_block', 'cookie_pillar', 'black_white_chocolate_block']
prefixes = ('honey_cake', 'honey_cream', 'matcha_cake', 'matcha_cream', 'cookie_', 'black_white_chocolate')
assets = []
original_assets = reference / 'src/main/resources/assets/anvilcraft'
native_assets = root / 'src/main/resources/assets/anvilcraft'
for path in original_assets.rglob('*'):
    if path.is_file() and path.name.startswith(prefixes) and path.suffix in ['.png', '.json']:
        relative = path.relative_to(original_assets)
        assert path.read_bytes() == (native_assets / relative).read_bytes(), relative
        assets.append(str(relative))
for language in ['en_us', 'zh_cn']:
    for name in ['011_chocolate.md', '211_large_cake.md']:
        relative = Path('ageratum') / language / '006_prop' / name
        assert (original_assets / relative).read_bytes() == (native_assets / relative).read_bytes()
        assets.append(str(relative))


def normalize(value):
    if isinstance(value, list):
        return [normalize(entry) for entry in value]
    if isinstance(value, dict):
        if set(value) == {'item'}:
            return value['item']
        if set(value) == {'tag'}:
            return '#' + value['tag']
        return {key: normalize(entry) for key, entry in value.items() if not (key == 'count' and entry == 1)}
    return value


recipes = ['black_white_chocolate_block', 'cookie_block', 'cookie_from_cookie_block', 'cookie_pillar',
           'block_compress/honey_cake_block', 'block_compress/matcha_cake_block',
           'item_compress/cookie_block', 'item_compress/matcha_cream_block', 'solid_liquid/honey_cream_block']
for name in recipes:
    path = Path('src/generated/resources/data/anvilcraft/recipe') / (name + '.json')
    assert normalize(json.loads((reference / path).read_text())) == normalize(json.loads((root / path).read_text())), name
for name in names:
    path = Path('assets/anvilcraft/blockstates') / (name + '.json')
    original = reference / 'src/generated/resources' / path
    native = root / ('src/main/resources' if name == 'cookie_pillar' else 'src/generated/resources') / path
    assert json.loads(original.read_text()) == json.loads(native.read_text()), name

tests = (root / 'build/porting/tests-confectionery-final-2.log').read_text(encoding='utf-8', errors='replace')
assert 'All 623 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests and '> Task :runData' in tests
for path in ['client-confectionery-1.log', 'client-confectionery-source-final.log']:
    log = (root / 'build/porting' / path).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_CONFECTIONERY_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log and 'All dimensions are saved' in log

a = np.array(Image.open(reference / 'run/mun-reference/screenshots/confectionery-1.21.png').convert('RGB'))
b = np.array(Image.open(root / 'run/port-validation/client/screenshots/confectionery-26.1.png').convert('RGB'))
visuals = {}
for index, name in enumerate(names):
    x = (320 - 144 + index % 4 * 80) * 2
    y = (180 - 82 + index // 4 * 90) * 2
    original, native = a[y:y + 128, x:x + 128], b[y:y + 128, x:x + 128]
    original_mask, native_mask = np.any(original != 48, axis=2), np.any(native != 48, axis=2)
    assert np.array_equal(original_mask, native_mask), name
    error = np.abs(original.astype(int) - native.astype(int))
    mean = float(error[original_mask].mean())
    assert mean < 2.0, (name, mean)
    visuals[name] = {'silhouette_iou': 1.0, 'mean_rgb_error': mean, 'maximum_rgb_error': int(error.max())}
report = {'source_commit': source, 'source_assets_equal': assets, 'recipes_equivalent': recipes,
          'required_tests_passed': 623, 'visuals': visuals,
          'limits': ['Vanilla 26.1 GUI item sampling and lighting are retained; color differences remain despite identical silhouettes/assets.',
                     'Source guide resources are copied verbatim; honey-food values in the source guide differ from source code, whose behavior is preserved.',
                     'Initial full suite hit Windows AccessDenied in an existing world-reset manifest fixture; isolated recheck and final full rerun passed.']}
(root / 'build/porting/confectionery-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
