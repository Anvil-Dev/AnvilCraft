"""Verify source button interaction guards and the native portal hammer tag."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
prefix = 'src/main/java/dev/dubhe/anvilcraft/'


def body(text, declaration):
    start = text.index('{', text.index(declaration))
    end, depth = start + 1, 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    result = re.sub(r'\s+', '', text[start:end])
    return result.replace('.isClientSide()', '.isClientSide').replace('.isWithinBlockInteractionRange(', '.canInteractWithBlock(')


methods = []
for path, declarations in [
    ('block/entity/BigRedButtonBlockEntity.java', ['void press(', 'void checkPressed(']),
    ('client/event/BigRedButtonInputListener.java', ['BlockPos targetedButton(']),
]:
    original = subprocess.check_output(['git', 'show', f'{source}:{prefix}{path}'], cwd=root).decode('utf-8')
    native = (root / (prefix + path)).read_text(encoding='utf-8')
    for declaration in declarations:
        assert body(original, declaration) == body(native, declaration), (path, declaration)
        methods.append(f'{path}:{declaration}')
tag = json.loads((root / 'src/generated/resources/data/anvilcraft/tags/block/anvil_hammer_blacklist.json').read_text())
assert 'anvilcraft:celestial_forging_anvil_portal' in tag['values']
tests = (root / 'build/porting/tests-button-interaction-focused-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 3 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests
client = (root / 'build/porting/client-button-interaction-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_BUTTON_INTERACTION_PASSED' in client and 'All dimensions are saved' in client and 'BUILD SUCCESSFUL' in client
report = {'source_commit': source, 'source_methods_equivalent': methods, 'focused_tests_passed': 3,
          'coverage': ['Crouching block/item use passes through; server press and held state honor crouch and main-hand building rod',
                       'Native client hold, crouch release, main/off-hand rod bypass and ordinary-item hold/release',
                       'Generated portal tag prevents native anvil-hammer rotation'],
          'limits': ['Native block InteractionResult.PASS replaces source SKIP_DEFAULT_BLOCK_INTERACTION.',
                     'The native tag remains named anvil_hammer_blacklist, as used by native hammer consumers.']}
(root / 'build/porting/button-interaction-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
