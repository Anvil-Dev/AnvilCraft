"""Compare creative-laser source contracts, resources and seven actual menu states."""
from pathlib import Path
import hashlib
import json
import re
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = root / 'build/porting/reference-mun-1.21'
assets = Path('src/main/resources/assets/anvilcraft')
resources = []
for source in (ref / assets).rglob('*'):
    if source.is_file() and 'creative_laser' in source.relative_to(ref / assets).as_posix() and source.suffix in ['.png', '.json']:
        native = root / assets / source.relative_to(ref / assets)
        assert hashlib.sha256(source.read_bytes()).digest() == hashlib.sha256(native.read_bytes()).digest(), source
        resources.append(source.relative_to(ref).as_posix())
assert len(resources) == 13

for language, folder in [('en_us', 'src/generated/resources'), ('zh_cn', 'src/main/resources')]:
    relative = Path(folder) / 'assets/anvilcraft/lang' / (language + '.json')
    source = json.loads((ref / relative).read_text(encoding='utf-8'))
    native = json.loads((root / relative).read_text(encoding='utf-8'))
    for key, value in source.items():
        if 'creative_laser' in key:
            assert native[key] == value, (language, key)
item = json.loads((root / 'src/generated/resources/assets/anvilcraft/items/creative_laser.json').read_text())
assert item['model'] == {'type': 'minecraft:model', 'model': 'anvilcraft:block/creative_laser'}

base = Path('src/main/java/dev/dubhe/anvilcraft')
def body(text, name):
    match = re.search(r'(?:public|private|protected) (?:static )?[^;{\n]+\b' + name + r'\(', text)
    assert match, name
    start = text.index('{', match.start())
    depth = 1
    end = start + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    value = re.sub(r'/\*.*?\*/|//[^\n]*', '', text[start:end], flags=re.S)
    value = value.replace('this.', '').replace('ClientPacketDistributor', 'PacketDistributor')
    value = value.replace('CacheableBERenderingPipeline', 'CachedBlockEntityRenderingPipeline')
    value = value.replace('.update(this, true)', '.update(this)').replace('.isClientSide()', '.isClientSide')
    return re.sub(r'\s+', '', value)

methods = []
for filename, names in {
    'block/entity/CreativeLaserBlockEntity.java': ['setConfiguredLevel', 'setLensType', 'setGamma', 'isGammaLaserConfigured',
        'getBaseLaserLevel', 'configureLaserComponents', 'onIrradiated', 'getFacing', 'getLaserOffset', 'tick', 'isRedstoneOff',
        'cancelLaserEmission', 'getDisplayName', 'createMenu', 'writeClientSideData', 'syncTo', 'getUpdateTag', 'clientUpdate'],
    'client/gui/screen/CreativeLaserScreen.java': ['setValue', 'onValueInput', 'update', 'selectLens', 'selectType', 'sendUpdate'],
    'network/CreativeLaserInitPacket.java': ['handleOnClient', 'parseLensType'],
    'network/CreativeLaserUpdatePacket.java': ['handleOnServer', 'parseLensType'],
    'inventory/CreativeLaserMenu.java': ['quickMoveStack', 'stillValid'],
}.items():
    source = (ref / base / filename).read_text(encoding='utf-8')
    native = (root / base / filename).read_text(encoding='utf-8')
    for name in names:
        assert body(source, name) == body(native, name), (filename, name)
        methods.append(filename + '#' + name)

menus = {}
for name in ['default', 'max', 'royal', 'frost', 'ember', 'gamma', 'reopen']:
    a = np.asarray(Image.open(root / ('run/port-validation/client/screenshots/creative-laser-26.1-' + name + '.png')).convert('RGB'))
    b = np.asarray(Image.open(ref / ('run/mun-reference/screenshots/creative-laser-1.21-' + name + '.png')).convert('RGB'))
    a, b = a[282:436, 464:816], b[282:436, 464:816]
    error = float(abs(a.astype(float)-b.astype(float)).mean())
    assert error < 0.1, (name, error)
    menus[name] = {'mean_rgb_error': error, 'equal_pixel_fraction': float((a == b).all(2).mean())}

beams = {}
for bloom in ['', '-bloom']:
    a = np.asarray(Image.open(root / ('run/port-validation/client/screenshots/creative-laser-beams' + bloom + '-26.1.png')).convert('RGB')).astype(float)
    b = np.asarray(Image.open(ref / ('run/mun-reference/screenshots/creative-laser-beams' + bloom + '-1.21.png')).convert('RGB')).astype(float)
    metrics = {}
    for name, y in [('gamma', 240), ('ember', 301), ('frost', 363), ('royal', 425), ('normal', 487)]:
        aa, bb = a[y-14:y+15, 550:730], b[y-14:y+15, 550:730]
        mask = bb.max(2) - bb.min(2) > 40
        metrics[name] = float(abs(aa-bb)[mask].mean())
    beams['bloom' if bloom else 'default'] = metrics
a = np.asarray(Image.open(root / 'run/port-validation/client/screenshots/creative-laser-26.1-item.png').convert('RGB'))[296:424, 576:704]
b = np.asarray(Image.open(ref / 'run/mun-reference/screenshots/creative-laser-1.21-item.png').convert('RGB'))[296:424, 576:704]
mask_a, mask_b = (a != 48).any(2), (b != 48).any(2)
item_visual = {'mask_iou': float((mask_a & mask_b).sum() / (mask_a | mask_b).sum()),
    'mean_rgb_error_on_item': float(abs(a.astype(float)-b.astype(float))[mask_b].mean())}
assert item_visual['mask_iou'] == 1.0
report = {'item_visual': item_visual, 'source_equal_resources': resources, 'source_equivalent_method_bodies': methods, 'menus': menus,
    'beams_native_atmospheric_fog': beams,
    'limits': ['Beam screenshots retain native 26.1 atmospheric fog and randomized impact particles; GUI crops isolate the actual menu.']}
(root / 'build/porting/creative-laser-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'resources': len(resources), 'methods': len(methods), 'menus': menus, 'beams': beams, 'item_visual': item_visual}, indent=2))
