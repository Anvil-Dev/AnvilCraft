"""Verify source interaction/audio parity and optional tooltip integration."""
from pathlib import Path
import json
import re
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).strip() == subprocess.check_output(
    ['git', 'rev-parse', 'HEAD'], cwd=reference).strip()
logs = {}
for name in ('client-storage-interaction-1.log', 'client-storage-interaction-reference.log',
             'client-storage-interaction-no-jade-final.log', 'client-storage-interaction-no-jei.log'):
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_STORAGE_INTERACTION_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
    logs[name] = text
pattern = r'PORT_STORAGE_TITLE: width=(\d+), right=(\d+), buttonX=(\d+), overlaps=(true|false)'
titles = {name: re.findall(pattern, text) for name, text in logs.items()}
assert all(values == titles['client-storage-interaction-reference.log'] and len(values) == 2 for values in titles.values())
assert 'PORT_OPTIONAL_JADE_ABSENT' in logs['client-storage-interaction-no-jade-final.log']
assert 'PORT_NO_JADE_TOOLTIP_PASSED' in logs['client-storage-interaction-no-jade-final.log']
assert 'jade=true, jei=false' in logs['client-storage-interaction-no-jei.log']
dedup = (root / 'build/porting/client-tooltip-dedup-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_TOOLTIP_DEDUP_PASSED' in dedup and 'ordinary and renamed items verified' in dedup and 'BUILD SUCCESSFUL' in dedup
native = np.asarray(Image.open(root / 'run/port-validation/client/screenshots/container-jei-26.1-hint.png')
                    .convert('RGB')).astype(float)[360:500, 568:1235]
source = np.asarray(Image.open(reference / 'run/mun-reference/screenshots/container-jei-1.21-hint.png')
                    .convert('RGB')).astype(float)[360:500, 568:1235]
ma, mb = native.max(2) > 100, source.max(2) > 100
iou = float((ma & mb).sum() / (ma | mb).sum())
error = float(abs(native - source)[ma | mb].mean())
assert iou > 0.98 and error < 5, (iou, error)
report = {'title_layouts': titles, 'jei_hint_foreground_iou': iou, 'jei_hint_mean_rgb_error': error,
          'checks': ['Actual empty-hand use opens the station and emits exactly one ender-chest opening sound.',
                     'Normal and flipped title coordinates match the source runtime.',
                     'JEI/Jade produce one mod-name line before recipe hints; guide hints and custom item names remain.',
                     'The storage screen works with Jade removed or JEI removed; ordinary tooltips also work without Jade.'],
          'limits': ['The measured two-pixel title/button overlap exists in the source branch too; layout is preserved.',
                     'A single mod-name footer remains subject to the installed Jade version and user settings.']}
(root / 'build/porting/storage-interaction-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
