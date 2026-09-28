"""Compare every registered multipart blockstate against the live 1.21 reference."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip()
assert source == subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference).decode().strip()
expected = json.loads((reference / 'run/mun-reference/multipart-shapes-1.21.json').read_text())
checks = {}
for version in ['26.1', 'sodium']:
    actual = json.loads((root / f'run/port-validation/client/multipart-shapes-{version}.json').read_text())
    assert actual['states'].keys() == expected['states'].keys()
    compared = 0
    for state, fields in expected['states'].items():
        for name, value in fields.items():
            current = actual['states'][state][name]
            if name == 'skylight':
                assert current == value, (version, state, name)
            else:
                assert actual['shapes'][current] == expected['shapes'][value], (version, state, name)
            compared += 1
    checks[version] = {'states': len(actual['states']), 'shape_and_light_checks': compared,
                       'unique_shapes': len(actual['shapes'])}
logs = ['client-multipart-shapes-2.log', 'client-multipart-shapes-reference-2.log', 'client-multipart-shapes-sodium-1.log']
for name in logs:
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_RUINS_FALLBACK_CLIENT_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
server = (root / 'build/porting/tests-multipart-shapes-all.log').read_text(encoding='utf-8', errors='replace')
assert 'All 691 required tests passed' in server and 'BUILD SUCCESSFUL' in server
visuals = {}
for scene in ['ordinary', 'ruins']:
    images = {}
    for version in ['26.1', '1.21', 'sodium']:
        folder = reference / 'run/mun-reference/screenshots' if version == '1.21' else root / 'run/port-validation/client/screenshots'
        with Image.open(folder / f'ruins-fallback-{version}-{scene}.png') as image:
            assert image.size == (1280, 720)
            images[version] = np.asarray(image.convert('RGB').crop((410, 220, 870, 510))).astype(float)
    visuals[scene] = {'source_crop_mean_rgb_error': float(abs(images['26.1'] - images['1.21']).mean()),
                     'sodium_crop_mean_rgb_error': float(abs(images['26.1'] - images['sodium']).mean())}
report = {'source_commit': source, 'blocks': len({state.split(';')[0] for state in expected['states']}),
          'comparisons': checks, 'visuals': visuals, 'required_tests_passed': 691,
          'contracts': ['Whole outlines are cached and translated consistently for every member.',
                        'Part, whole, ordinary outline, empty/entity collision, effective occlusion and two placement contexts match source.',
                        'Large crates and shulker containers retain full collision; cauldron/trading-station exceptions remain part-local.',
                        'Ruins picking ignores placement-guide expansion and model-disabled highlighting uses the original whole fallback.'],
          'limits': ['Disabled occlusion is normalized to empty on both sides because 26.1 caches this effective result.',
                     'World screenshots retain native terrain/lightmap differences; exact assertions concern shape data.']}
(root / 'build/porting/multipart-shapes-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
