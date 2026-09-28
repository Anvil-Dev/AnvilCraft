"""Check live storage tooltip screenshots and source translation contracts."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
keys = ['tooltip.anvilcraft.storage.types' + suffix for suffix in ('', '.value', '.value.infinite')]
for language, folder in [('en_us', 'src/generated/resources'), ('zh_cn', 'src/main/resources')]:
    path = Path(folder) / 'assets/anvilcraft/lang' / (language + '.json')
    a = json.loads((root / path).read_text(encoding='utf-8'))
    b = json.loads((reference / path).read_text(encoding='utf-8'))
    assert all(a[key] == b[key] for key in keys), language
visuals = {}
for scene in ('empty', 'finite', 'many', 'infinite', 'updated'):
    native = np.asarray(Image.open(root / ('run/port-validation/client/screenshots/storage-tooltip-26.1-' + scene + '.png'))
                        .convert('RGB')).astype(float)
    source = np.asarray(Image.open(reference / ('run/mun-reference/screenshots/storage-tooltip-1.21-' + scene + '.png'))
                        .convert('RGB')).astype(float)
    assert native.shape == source.shape == (720, 1280, 3)
    bottom = 360 if scene == 'empty' else 392
    a, b = native[320:bottom, 504:848], source[320:bottom, 504:848]
    ma, mb = a.max(2) > 100, b.max(2) > 100
    iou = float((ma & mb).sum() / (ma | mb).sum())
    error = float(abs(a - b)[ma | mb].mean())
    assert iou > 0.98, (scene, iou)
    assert error < 5, (scene, error)
    visuals[scene] = {'component_foreground_iou': iou, 'mean_rgb_error': error}
for name in ('client-storage-tooltip-final.log', 'client-storage-tooltip-reference-1.log'):
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_STORAGE_TOOLTIP_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log, name
report = {'visuals': visuals, 'source_equal_translations': keys,
          'checks': ['Real inventory items use the registered tooltip factory and RPC path.',
                     'Empty, finite, expanded and infinite capacities; nine representatives and ellipsis.',
                     'The tooltip refreshes after server-side contents change.',
                     'Native pending responses do not repopulate an invalidated cache.',
                     'Simultaneous oversized item densities no longer share and release a pending draw texture.'],
          'limits': ['The comparison isolates the new data component; shared Ageratum/mod-name footer differences remain a separate audit.',
                     'Client checks use an integrated server; remote-server authorization is additionally covered by validator tests.']}
(root / 'build/porting/storage-tooltip-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
