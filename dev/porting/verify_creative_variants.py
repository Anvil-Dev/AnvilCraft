"""Verify creative variants, source resources and the paired native/reference GUI captures."""
from pathlib import Path
import hashlib
import json
import subprocess
import zipfile
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
path = 'src/main/java/dev/dubhe/anvilcraft/client/init/ModCreativeVariantGroups.java'
original = subprocess.check_output(['git', 'show', f'{source}:{path}'], cwd=root).decode('utf-8').replace('\r\n', '\n')
assert (root / path).read_text(encoding='utf-8') == original
logs = {
    'tests': 'build/porting/tests-creative-variants-final.log',
    'native': 'build/porting/client-creative-variants-final-2.log',
    'source': 'build/porting/client-creative-variants-source-final.log',
}
for name, path in logs.items():
    log = (root / path).read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in log, path
    if name == 'tests':
        assert 'All 619 required tests passed' in log and '> Task :runData' in log
    else:
        assert 'PORT_CREATIVE_VARIANTS_CLIENT_PASSED' in log and 'All dimensions are saved' in log

cache = Path.home() / '.gradle/caches/modules-2/files-2.1/dev.anvilcraft.lib'
hashes = {}
for platform, version in [('26.1', '529'), ('1.21.1', '534')]:
    for jar in (cache / f'anvillib-registrum-neoforge-{platform}' / f'2.0.0+snapshot.{version}').rglob('*.jar'):
        if 'sources' in jar.name or 'javadoc' in jar.name:
            continue
        with zipfile.ZipFile(jar) as archive:
            asset = 'assets/anvillib/textures/gui/background/16_color_overlay.png'
            if asset in archive.namelist():
                hashes[platform] = hashlib.sha256(archive.read(asset)).hexdigest()
assert len(hashes) == 2 and len(set(hashes.values())) == 1

images = {}
markers = {}
for platform, folder in [('26.1', root / 'run/port-validation/client'),
                         ('1.21', root / 'build/porting/reference-mun-1.21/run/mun-reference')]:
    metadata = json.loads((folder / f'creative-variants-{platform}.json').read_text())
    scale = int(metadata['scale'])
    assert scale == 2
    image = np.array(Image.open(folder / f'screenshots/creative-variants-{platform}-overlay.png').convert('RGB'))
    x, y = metadata['overlayX'] * scale, metadata['overlayY'] * scale
    images[platform] = image[y:y + 80 * scale, x:x + 78 * scale]
    image = np.array(Image.open(folder / f'screenshots/creative-variants-{platform}-indicator.png').convert('RGB'))
    x, y = (metadata['slotX'] + 10) * scale, (metadata['slotY'] + 1) * scale
    markers[platform] = int((image[y:y + 16, x:x + 12].min(axis=2) > 245).sum())

error = np.abs(images['26.1'].astype(int) - images['1.21'].astype(int))
frame = np.ones(error.shape[:2], dtype=bool)
icons = np.zeros_like(frame)
for row in range(4):
    for column in range(4):
        icons[(4 + row * 18) * 2:(20 + row * 18) * 2, (4 + column * 18) * 2:(20 + column * 18) * 2] = True
frame[icons] = False
frame[:2] = frame[-2:] = False
frame[:, :2] = frame[:, -2:] = False
assert error[frame].max() == 0
assert error[icons].mean() < 1.0
assert markers['26.1'] == 36
report = {
    'source_commit': source, 'groups_equal': 5, 'colors_per_group': 16, 'required_tests_passed': 619,
    'overlay_texture_sha256': hashes['26.1'], 'frame_maximum_error': int(error[frame].max()),
    'icon_mean_rgb_error': float(error[icons].mean()), 'marker_white_pixels': markers,
    'coverage': ['Mutable exposed tab collections, complete/incomplete families, disabled configuration and copied stack state',
                 'Native category/search contents, right-click overlay, color selection, configuration invalidation and Shift trash'],
    'limits': ['Both paired scenes use the legacy tab and replace the visible page with one representative after verifying real tab contents.',
               'Source item depth partly occludes the plus glyph; native GUI draws the full source-specified glyph above the item.',
               'Vanilla native item lighting is retained; sectioned creative tabs and their content prerequisites remain a separate node.'],
}
(root / 'build/porting/creative-variants-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
