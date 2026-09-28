"""Check uploader source resources and paired live-client captures."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
assets = ['models/block/hyperdimension_uploader.json', 'textures/block/hyperdimension_storage_station.png',
          'textures/block/transcendium_block_outline.png', 'textures/block/transcendium_block_outline.png.mcmeta',
          'textures/block/celestial_forging_anvil_particle.png', 'textures/gui/jei/mouse_right.png']
for name in assets:
    path = Path('src/main/resources/assets/anvilcraft') / name
    assert (root / path).read_bytes() == (reference / path).read_bytes(), name
for name in ['assets/anvilcraft/blockstates/hyperdimension_uploader.json',
             'data/anvilcraft/loot_table/blocks/hyperdimension_uploader.json']:
    path = Path('src/generated/resources') / name
    assert json.loads((root / path).read_text()) == json.loads((reference / path).read_text()), name
recipe = Path('src/main/java/dev/dubhe/anvilcraft/integration/jei/recipe/UseItemOnBlockRecipe.java')
assert (root / recipe).read_text(encoding='utf-8') == (reference / recipe).read_text(encoding='utf-8')
visuals = {}
for scene in ('day', 'night', 'item', 'jei'):
    a = np.asarray(Image.open(root / ('run/port-validation/client/screenshots/uploader-26.1-' + scene + '.png'))
                   .convert('RGB')).astype(float)
    b = np.asarray(Image.open(reference / ('run/mun-reference/screenshots/uploader-1.21-' + scene + '.png'))
                   .convert('RGB')).astype(float)
    assert a.shape == b.shape == (720, 1280, 3)
    if scene == 'item':
        a, b = a[286:434, 568:712], b[286:434, 568:712]
        ma, mb = (a != 48).any(2), (b != 48).any(2)
        iou = float((ma & mb).sum() / (ma | mb).sum())
        assert iou > 0.99
        visuals[scene] = {'mask_iou': iou, 'mean_rgb_error': float(abs(a - b)[mb].mean())}
    elif scene == 'jei':
        visuals[scene] = {'card_mean_rgb_error': float(abs(a[180:325, 470:810] - b[180:325, 470:810]).mean())}
    else:
        a, b = a[240:513, 510:770], b[240:513, 510:770]
        def outline(im):
            red, green, blue = im[:, :, 0], im[:, :, 1], im[:, :, 2]
            return (red > 60) & (blue > red + 5) & (blue > green * 1.3) & (red > green * 1.2)
        ma, mb = outline(a), outline(b)
        iou = float((ma & mb).sum() / (ma | mb).sum())
        assert iou > 0.98
        visuals[scene] = {'outline_mask_iou': iou, 'mean_rgb_error': float(abs(a - b).mean())}
for name in ('client-uploader-1.log', 'client-uploader-reference-1.log'):
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_UPLOADER_CLIENT_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
for checkout in (root, reference):
    path = 'assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta'
    assert (checkout / 'src/main/resources' / path).read_bytes() == (checkout / 'build/resources/main' / path).read_bytes()
report = {'source_equal_assets': assets, 'visuals': visuals,
          'checks': ['Actual bound-terminal crystal conversion and rebinding, with synchronized client UUID.',
                     'Populated and emptied buffers synchronize while 150 gold ingots upload to the second global target.',
                     'World day/night, item model and registered JEI conversion recipe.'],
          'limits': ['Captures fix the animated outline to frame zero in development output; original metadata is restored.',
                     'World crops retain native 26.1 terrain, atmosphere and lightmap differences.']}
(root / 'build/porting/uploader-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
