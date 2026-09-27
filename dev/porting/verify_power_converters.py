"""Verify all converter tiers, source recipes/resources and paired model/GUI evidence."""
from pathlib import Path
import json
import re
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root, text=True).strip() == source


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


recipes = []
folder = Path('src/generated/resources/data/anvilcraft/recipe')
for path in (reference / folder).rglob('power_converter*.json'):
    relative = path.relative_to(reference / folder)
    assert normalize(json.loads(path.read_text())) == normalize(json.loads((root / folder / relative).read_text())), relative
    recipes.append(str(relative))
assets = []
folder = Path('src/main/resources/assets/anvilcraft')
for path in (reference / folder).rglob('power_converter*'):
    if path.is_file():
        relative = path.relative_to(reference / folder)
        native_path = root / folder / relative
        if path.suffix == '.json':
            assert json.loads(path.read_text()) == json.loads(native_path.read_text()), relative
        else:
            assert path.read_bytes() == native_path.read_bytes(), relative
        assets.append(str(relative))

path = 'src/main/java/dev/dubhe/anvilcraft/block/entity/PowerConverterBlockEntity.java'
original = subprocess.check_output(['git', 'show', f'{source}:{path}'], cwd=root).decode('utf-8')
native = (root / path).read_text(encoding='utf-8')


def body(text, declaration):
    start = text.index('{', text.index(declaration))
    end, depth = start + 1, 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return re.sub(r'\s+', '', text[start:end]).replace('this.', '')


assert body(original, 'void gridTick(') == body(native, 'void gridTick(')
tests = (root / 'build/porting/tests-power-converters-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 631 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests and '> Task :runData' in tests
focused = (root / 'build/porting/tests-power-converters-focused-final-2.log').read_text(encoding='utf-8', errors='replace')
assert 'All 4 required tests passed' in focused and 'BUILD SUCCESSFUL' in focused
for name in ['client-power-converters-final.log', 'client-power-converters-source-final-2.log']:
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_POWER_CONVERTERS_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log and 'All dimensions are saved' in log
source_models = json.loads((reference / 'run/mun-reference/power-converter-models-1.21.json').read_text())
native_models = json.loads((root / 'run/port-validation/client/power-converter-models-26.1.json').read_text())
assert len(source_models) == len(native_models) == 120 and source_models.keys() == native_models.keys()
for key in source_models:
    assert np.allclose(source_models[key], native_models[key], atol=0.00001), key

a = np.array(Image.open(reference / 'run/mun-reference/screenshots/power-converters-1.21.png').convert('RGB'))
b = np.array(Image.open(root / 'run/port-validation/client/screenshots/power-converters-26.1.png').convert('RGB'))
visuals = {}
for index, tier in enumerate(['small', 'middle', 'big', 'super_big', 'extremely_big']):
    x, y = (320 - 176 + index * 72) * 2, (180 - 32) * 2
    original, native = a[y:y + 128, x:x + 128], b[y:y + 128, x:x + 128]
    ma, mb = np.any(original != 48, axis=2), np.any(native != 48, axis=2)
    error = np.abs(original.astype(int) - native.astype(int))
    iou = float((ma & mb).sum() / (ma | mb).sum())
    visuals[tier] = {'silhouette_iou': iou, 'mean_rgb_error': float(error[ma | mb].mean())}
    assert iou > 0.99 and error[ma | mb].mean() < 3.0, (tier, visuals[tier])
report = {'source_commit': source, 'required_tests_passed': 631, 'recipes_equivalent': recipes,
          'source_assets_equal': assets, 'paired_model_states': 120, 'grid_generation_body_equivalent': True, 'visuals': visuals,
          'coverage': ['Five tier capacities, facing shapes, tags, redstone/overload light states and translation keys',
                       'Grid dispatch, saturation, save/load, native capability rollback and accepted-output conservation',
                       'Live client/server power-grid generation and all baked model states'],
          'limits': ['Native GUI lighting is retained.',
                     'The overflow regression injects a synthetic rate above the normal configuration range to exercise long addition.',
                     'Some source model bounds differ from collision boxes; each is preserved and checked independently.']}
(root / 'build/porting/power-converters-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
