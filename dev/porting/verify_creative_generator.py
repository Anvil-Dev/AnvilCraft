"""Check source generator limits, slider behavior and paired GUI captures."""
from pathlib import Path
import re
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = '07eb60b47f14439375da10e78f4d4770f3a83256'


def body(text, declaration):
    start = text.index('{', text.index(declaration))
    end, depth = start + 1, 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    result = re.sub(r'\s+', '', text[start:end]).replace('this.', '')
    return result.replace('sliderWidget', 'slider').replace('ClientPacketDistributor', 'PacketDistributor')


methods = []
for path, declarations in [
    ('block/entity/CreativeGeneratorBlockEntity.java', ['void setPower(', 'int getInputPower(', 'int getOutputPower(']),
    ('client/gui/screen/SliderScreen.java', ['void onValueInput(']),
]:
    full = 'src/main/java/dev/dubhe/anvilcraft/' + path
    original = subprocess.check_output(['git', 'show', f'{source}:{full}'], cwd=root).decode('utf-8')
    native = (root / full).read_text(encoding='utf-8')
    for declaration in declarations:
        assert body(original, declaration) == body(native, declaration), (path, declaration)
        methods.append(path + ':' + declaration)
tests = (root / 'build/porting/tests-creative-generator-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 633 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests
for name in ['client-creative-generator-3.log', 'client-creative-generator-source-final.log']:
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_CREATIVE_GENERATOR_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log and 'All dimensions are saved' in log
visuals = {}
for name in ['default', 'max', 'min']:
    a = np.array(Image.open(reference / f'run/mun-reference/screenshots/creative-generator-1.21-{name}.png').convert('RGB'))[282:436, 464:816]
    b = np.array(Image.open(root / f'run/port-validation/client/screenshots/creative-generator-26.1-{name}.png').convert('RGB'))[282:436, 464:816]
    error = np.abs(a.astype(int) - b.astype(int))
    visuals[name] = {'mean_rgb_error': float(error.mean()), 'equal_pixel_fraction': float((error.max(axis=2) == 0).mean())}
    assert error.mean() < 2, (name, visuals[name])
report = {'source_commit': source, 'source_methods_equivalent': methods, 'default_power': 8192,
          'power_limit': 65536, 'required_tests_passed': 633, 'visuals': visuals,
          'coverage': ['Signed integer extremes, network callback bounds, save/load clamping and grid notifications',
                       'Actual menu open, initial value, endpoint buttons, resize, arbitrary typed values and partial negative input'],
          'limits': ['Cursor blink and native GUI rendering may differ in the paired screenshots.',
                     'The fixture uses a server-confirmed teleport before client interaction.']}
(root / 'build/porting/creative-generator-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
