"""Check source interaction semantics and native server/client acceptance evidence."""
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
    return result.replace('Minecraft.getInstance().getConnection().send(', 'PacketDistributor.sendToServer(')


methods = []
for path, declarations in [
    ('util/BlockPlacementPicking.java', ['boolean hasFullPlacementShape(', 'boolean isHoldingPlacementItem(',
                                       'BlockHitResult pickBuildingRodTarget(']),
    ('client/building/BuildingRodClient.java', ['void interaction(', 'void stopDestroying(', 'void press(',
                                              'void confirmSelection(']),
    ('client/building/BuildingRodItemRenderer.java', ['boolean isAttackCanceled(']),
]:
    original = subprocess.check_output(['git', 'show', f'{source}:{prefix}{path}'], cwd=root).decode('utf-8')
    native = (root / (prefix + path)).read_text(encoding='utf-8')
    for declaration in declarations:
        assert body(original, declaration) == body(native, declaration), (path, declaration)
        methods.append(f'{path}:{declaration}')

tests = (root / 'build/porting/tests-building-interaction-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 612 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests
client = (root / 'build/porting/client-building-interaction-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_BUILDING_CLIENT_PASSED' in client and 'mining cancel, undo' in client
assert 'All dimensions are saved' in client and 'BUILD SUCCESSFUL' in client
report = {'source_commit': source, 'source_methods_equivalent': methods, 'required_tests_passed': 612,
          'coverage': ['All ring and cauldron states, both hands, relevant and unrelated placement materials',
                       'Full placement outline with unchanged physical collision and actual cauldron ray picking',
                       'Control release preserves selection; missing target does not discard it; re-aim and confirm once',
                       'Native mining stopped on selection press/cancel; repeated attack suppressed',
                       'Existing placement, undo, blueprint, traditional-control and held-item client workflow'],
          'limits': ['Client input events are driven by the developer fixture in a real native client.',
                     'Native ring geometry and vanilla terrain rendering remain unchanged.']}
(root / 'build/porting/building-interaction-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
