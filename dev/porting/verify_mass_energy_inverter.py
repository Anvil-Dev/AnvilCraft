"""Check source machine behavior, recipe data, assets and matched animation-frame visuals."""
from pathlib import Path
import hashlib
import json
import re
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = root / 'build/porting/reference-mun-1.21'
base = Path('src/main/java/dev/dubhe/anvilcraft')

def normalized(text):
    text = re.sub(r'^package .*;|^import .*;', '', text, flags=re.M)
    text = re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)
    text = text.replace('this.', '').replace('.isClientSide()', '.isClientSide')
    return re.sub(r'\s+', '', text)

classes = [('block/power/consumer/MassEnergyInverterBlock.java', 'block/MassEnergyInverterBlock.java'),
    ('block/entity/MassEnergyInverterBlockEntity.java', 'block/entity/MassEnergyInverterBlockEntity.java')]
for native, source in classes:
    assert normalized((root / base / native).read_text(encoding='utf-8')) == normalized((ref / base / source).read_text(encoding='utf-8'))

assets = []
folder = Path('src/main/resources/assets/anvilcraft')
for path in (ref / folder).rglob('*'):
    if path.is_file() and 'mass_energy_inverter' in path.name:
        native = root / folder / path.relative_to(ref / folder)
        assert hashlib.sha256(path.read_bytes()).digest() == hashlib.sha256(native.read_bytes()).digest(), path
        assets.append(path.relative_to(ref).as_posix())

recipes = []
for name in ['procedural_process/mass_energy_inverter_mass_first', 'procedural_process/mass_energy_inverter_energy_first',
    'multiblock/celestial_forging_anvil']:
    path = Path('src/generated/resources/data/anvilcraft/recipe') / (name + '.json')
    a, b = json.loads((root/path).read_text()), json.loads((ref/path).read_text())
    for data in [a, b]:
        for key in ['icon', 'result']:
            if isinstance(data.get(key), dict) and data[key].get('count') == 1:
                data[key].pop('count')
    assert a == b, name
    recipes.append(name)

for language, folder in [('en_us', 'src/generated/resources'), ('zh_cn', 'src/main/resources')]:
    path = Path(folder) / 'assets/anvilcraft/lang' / (language + '.json')
    a, b = json.loads((root/path).read_text(encoding='utf-8')), json.loads((ref/path).read_text(encoding='utf-8'))
    for k,v in b.items():
        if 'mass_energy_inverter' in k: assert a[k] == v, (language,k)

screens = root/'run/port-validation/client/screenshots'
source_screens = ref/'run/mun-reference/screenshots'
a = np.asarray(Image.open(screens/'mass-energy-inverter-26.1-item.png').convert('RGB'))[296:424,576:704]
b = np.asarray(Image.open(source_screens/'mass-energy-inverter-1.21-item.png').convert('RGB'))[296:424,576:704]
ma, mb = (a != 48).any(2), (b != 48).any(2)
item = {'mask_iou': float((ma & mb).sum()/(ma | mb).sum()), 'mean_rgb_error': float(abs(a.astype(float)-b.astype(float))[mb].mean())}
assert item['mask_iou'] == 1.0

a = np.asarray(Image.open(screens/'mass-energy-inverter-26.1-world.png').convert('RGB')).astype(float)
b = np.asarray(Image.open(source_screens/'mass-energy-inverter-1.21-world.png').convert('RGB')).astype(float)
world = {}
for name,x in [('wip_1',375),('wip_2',535),('wip_3',695),('finished',855)]:
    aa,bb = a[355:424,x-44:x+44],b[355:424,x-44:x+44]
    world[name] = {'mean_rgb_error': float(abs(aa-bb).mean())}

for checkout in [root, ref]:
    relative = Path('assets/anvilcraft/textures/block/wip_block.png.mcmeta')
    assert (checkout/'build/resources/main'/relative).read_bytes() == (checkout/'src/main/resources'/relative).read_bytes()

report = {'source_equal_machine_classes': 2, 'source_equal_resources': assets, 'source_equal_recipes': recipes,
    'item_visual': item, 'world_patch_visuals': world,
    'limits': ['WIP animation is fixed to frame 0 only in test build output and restored after each client run.',
        'World patches include retained vanilla 26.1 lighting and terrain sampling.']}
(root/'build/porting/mass-energy-inverter-comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'assets':len(assets),'recipes':recipes,'item':item,'world':world},indent=2))
