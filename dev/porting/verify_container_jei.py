"""Compare registered container-upgrade layouts against the local source branch."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
recipe = Path('src/main/java/dev/dubhe/anvilcraft/integration/jei/recipe/ContainerUpgradeRecipe.java')
assert (root / recipe).read_text(encoding='utf-8') == (reference / recipe).read_text(encoding='utf-8')
keys = ['gui.anvilcraft.category.container_upgrade' + suffix
        for suffix in ('', '.drop_on_top', '.strike', '.requires_expansion')]
for language, folder in [('en_us', 'src/generated/resources'), ('zh_cn', 'src/main/resources')]:
    path = Path(folder) / 'assets/anvilcraft/lang' / (language + '.json')
    a = json.loads((root / path).read_text(encoding='utf-8'))
    b = json.loads((reference / path).read_text(encoding='utf-8'))
    assert all(a[key] == b[key] for key in keys), language
visuals = {}
for scene in ('crate', 'station', 'raised'):
    a = np.asarray(Image.open(root / ('run/port-validation/client/screenshots/container-jei-26.1-' + scene + '.png'))
                   .convert('RGB')).astype(float)
    b = np.asarray(Image.open(reference / ('run/mun-reference/screenshots/container-jei-1.21-' + scene + '.png'))
                   .convert('RGB')).astype(float)
    assert a.shape == b.shape == (720, 1280, 3)
    layout_error = float(abs(a[288:432, 470:810] - b[288:432, 470:810]).mean())
    a, b = a[306:427, 600:680], b[306:427, 600:680]
    ma, mb = a.mean(2) < 170, b.mean(2) < 170
    assert ma.sum() > 3000 and mb.sum() > 3000
    iou = float((ma & mb).sum() / (ma | mb).sum())
    assert iou > 0.97, (scene, iou)
    assert layout_error < 5, (scene, layout_error)
    visuals[scene] = {'model_mask_iou': iou, 'layout_mean_rgb_error': layout_error,
                      'model_mean_rgb_error': float(abs(a - b)[mb].mean())}
for name in ('client-container-jei-final.log', 'client-container-jei-reference-final.log'):
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_CONTAINER_UPGRADE_JEI_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
for checkout in (root, reference):
    path = 'assets/anvilcraft/textures/block/transcendium_block_outline.png.mcmeta'
    assert (checkout / 'src/main/resources' / path).read_bytes() == (checkout / 'build/resources/main' / path).read_bytes()
report = {'visuals': visuals, 'source_equal_translations': keys,
          'checks': ['Two registered recipes and their focused output lookups.',
                     'Actual ingredient slots preserve source counts, including four prerequisite expansions.',
                     'Anvil/container crafting stations, live JEI browser and expansion hover hint.',
                     'Fixed timer values 0 and 15 exercise both ends of the anvil animation.'],
          'limits': ['Native 26.1 preview rasterization and lighting can differ at individual model pixels.',
                     'The development capture freezes only outline texture timing; original metadata is restored.']}
(root / 'build/porting/container-jei-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
