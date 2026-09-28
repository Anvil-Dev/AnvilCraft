"""Verify source resources and compare matching live storage-station captures."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
assets = ['blockstates/hyperdimension_storage_station.json',
          'models/block/template_large_block.json',
          'textures/block/hyperdimension_storage_station.png',
          'textures/block/transcendium_block_outline.png',
          'textures/block/transcendium_block_outline.png.mcmeta',
          'textures/block/celestial_forging_anvil_particle.png']
assets += ['models/block/hyperdimension_storage_station_' + name + '.json'
           for name in ('center', 'corner', 'full', 'part')]
assets += ['ageratum/' + language + '/004_block/331_hyperdimension_storage_station.md'
           for language in ('en_us', 'zh_cn')]
for name in assets:
    path = Path('src/main/resources/assets/anvilcraft') / name
    assert (root / path).read_bytes() == (reference / path).read_bytes(), name
item = json.loads((root / 'src/generated/resources/assets/anvilcraft/items/hyperdimension_storage_station.json').read_text())
assert item['model']['model'] == 'anvilcraft:block/hyperdimension_storage_station_full'
assert item['oversized_in_gui']
visuals = {}
for scene in ('front', 'corner', 'night', 'item'):
    native = np.asarray(Image.open(root / ('run/port-validation/client/screenshots/storage-station-26.1-' + scene + '.png'))
                        .convert('RGB')).astype(float)
    source = np.asarray(Image.open(reference / ('run/mun-reference/screenshots/storage-station-1.21-' + scene + '.png'))
                        .convert('RGB')).astype(float)
    assert native.shape == source.shape == (720, 1280, 3)
    if scene == 'item':
        a, b = native[272:448, 552:728], source[272:448, 552:728]
        ma, mb = (a != 48).any(2), (b != 48).any(2)
        visuals[scene] = {'mask_iou': float((ma & mb).sum() / (ma | mb).sum()),
                          'mean_rgb_error': float(abs(a - b)[mb].mean())}
        assert visuals[scene]['mask_iou'] > 0.99
    else:
        box = (slice(315, 630), slice(485, 795)) if scene == 'front' else (slice(245, 515), slice(495, 775))
        a, b = native[box], source[box]
        def outline(im):
            red, green, blue = im[:, :, 0], im[:, :, 1], im[:, :, 2]
            return (red > 60) & (blue > red + 5) & (blue > green * 1.3) & (red > green * 1.2)
        ma, mb = outline(a), outline(b)
        assert ma.sum() > 500 and mb.sum() > 500, scene
        visuals[scene] = {'outline_mask_iou': float((ma & mb).sum() / (ma | mb).sum()),
                          'mean_rgb_error': float(abs(a - b).mean())}
        assert visuals[scene]['outline_mask_iou'] > 0.98, scene
for name in ('client-station-visual-final.log', 'client-station-reference-final.log'):
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_STORAGE_STATION_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log, name
for checkout in (root, reference):
    path = 'assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta'
    assert (checkout / 'src/main/resources' / path).read_bytes() == (checkout / 'build/resources/main' / path).read_bytes()
report = {'source_equal_assets': assets, 'visuals': visuals,
          'limits': ['The animated outline is fixed to source frame zero in development output for capture; production metadata is restored.',
                     'World crops include vanilla terrain, atmosphere and lightmap differences retained for 26.1.',
                     'This verifies the default resource pack at the recorded viewpoints, not every shader pack.']}
(root / 'build/porting/storage-station-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
