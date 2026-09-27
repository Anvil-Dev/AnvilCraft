"""Audit migrated source logic, generated recipes, and paired live laser captures."""
from pathlib import Path
import json
import re
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / 'build/porting/reference-mun-1.21'

def normalized(text):
    text = re.sub(r'^package .*;|^import .*;', '', text, flags=re.M)
    text = re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)
    return re.sub(r'\s+', '', text.replace('this.', '').replace('@Nullable', ''))

components = []
folder = Path('src/main/java/dev/dubhe/anvilcraft/api/laser')
for path in sorted((ROOT / folder).glob('*.java')):
    if path.name == 'package-info.java':
        continue
    native = path.read_text(encoding='utf-8')
    source = (REF / folder / path.name).read_text(encoding='utf-8')
    source = source.replace('getNormal()', 'getUnitVec3i()')
    source = source.replace('Vec3.fromRGB24(color).toVector3f(), DUST_SCALE', 'color, DUST_SCALE')
    if path.name == 'LaserDamageBehavior.java':
        source = source.replace('instanceof ServerLevel)', 'instanceof ServerLevel serverLevel)')
        source = source.replace('entity.hurt(', 'entity.hurtServer(serverLevel, ')
    if path.name == 'GammaLaserEffects.java':
        source = source.replace('int damage = Math.min',
            'if (!(level instanceof ServerLevel serverLevel)) return;\n        int damage = Math.min')
        source = source.replace('entity.hurt(', 'entity.hurtServer(serverLevel, ')
    assert normalized(native) == normalized(source), path.name
    components.append(path.name)

recipes = []
folder = Path('src/generated/resources/data/anvilcraft/recipe/laser_hit')
for path in sorted((ROOT / folder).rglob('*.json')):
    relative = path.relative_to(ROOT / folder)
    assert json.loads(path.read_text()) == json.loads((REF / folder / relative).read_text()), relative
    recipes.append(str(relative))
assert len(recipes) == 36

visuals = {}
for bloom in ['', '-bloom']:
    source = np.asarray(Image.open(REF / ('run/mun-reference/screenshots/laser-components' + bloom + '-1.21.png')).convert('RGB')).astype(float)
    for fog in ['', '-no-fog']:
        native = np.asarray(Image.open(ROOT / ('run/port-validation/client/screenshots/laser-components' + bloom + fog + '-26.1.png')).convert('RGB')).astype(float)
        metrics = {}
        for name, y in [('gamma', 240), ('ember', 301), ('frost', 363), ('royal', 425), ('normal', 487)]:
            a = native[y-14:y+15, 550:730]
            b = source[y-14:y+15, 550:730]
            mask_a = a.max(2) - a.min(2) > 40
            mask_b = b.max(2) - b.min(2) > 40
            metrics[name] = {
                'mean_rgb_error_on_source_beam': float(abs(a-b)[mask_b].mean()),
                'saturated_mask_iou': float((mask_a & mask_b).sum() / (mask_a | mask_b).sum()),
                'native_center_rgb': native[y, 640].tolist(),
                'source_center_rgb': source[y, 640].tolist(),
            }
        visuals[('bloom' if bloom else 'default') + ('_diagnostic_no_fog' if fog else '_vanilla_fog')] = metrics

report = {
    'source_equivalent_components': components,
    'source_equal_recipes': recipes,
    'visuals': visuals,
    'limits': [
        'No-fog captures are a development-only diagnostic. Production retains vanilla 26.1 atmospheric fog.',
        'Native fog probe: environmental 0..1024, render distance 230.4..256; default 1.21 near-field has no atmospheric fog.',
        'Bloom enabled remains pending: source uses four full-resolution weighted blur passes, native library uses down/up sampling.',
        'Particles are randomized; central beam strips exclude impact particles and the vanilla background.',
    ],
}
(ROOT / 'build/porting/laser-components-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'components': len(components), 'recipes': len(recipes), 'default_no_fog_rgb_error': {
    k: round(v['mean_rgb_error_on_source_beam'], 3) for k, v in visuals['default_diagnostic_no_fog'].items()
}}, indent=2))
