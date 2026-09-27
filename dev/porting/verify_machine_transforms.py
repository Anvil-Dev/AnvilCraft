"""Verify the 1.21 rotation/mirror update and its native all-state regression matrix."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'
prefix = 'src/main/java/dev/dubhe/anvilcraft/'


def body(text, declaration):
    start = text.index('{', text.index(declaration))
    depth, end = 1, start + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    result = re.sub(r'\s+', '', text[start:end])
    for owner in ['FacingWithAxis', 'StructureScannerBlock', 'PumpBlock']:
        result = result.replace(owner + '.', '')
    return result


methods = []
for source, target, declarations in [
    ('block/BlockComparatorBlock.java', 'block/state/FacingWithAxis.java', ['public FacingWithAxis rotate(']),
    ('block/SmartBlockPlacerBlock.java', 'block/power/consumer/SmartBlockPlacerBlock.java',
     ['protected BlockState rotate(', 'protected BlockState mirror(']),
    ('block/StructureScannerBlock.java', 'block/workstation/StructureScannerBlock.java',
     ['protected BlockState rotate(', 'protected BlockState mirror(']),
    ('block/fluid/ControlValveBlock.java', 'block/fluid/ControlValveBlock.java',
     ['public BlockState rotate(', 'public BlockState mirror(']),
    ('block/fluid/PumpBlock.java', 'block/fluid/PumpBlock.java', ['public BlockState mirror(']),
]:
    original = subprocess.check_output(['git', 'show', f'{ref}:{prefix}{source}'], cwd=root).decode('utf-8')
    native = (root / (prefix + target)).read_text(encoding='utf-8')
    for declaration in declarations:
        assert body(original, declaration) == body(native, declaration), (target, declaration)
        methods.append(target + ':' + declaration)
log = (root / 'build/porting/tests-machine-transform-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in log and 'All 596 required tests passed' in log
assert 'PORT_MACHINE_TRANSFORM_MATRIX_PASSED: 1800' in log
report = {'source_commit': ref, 'methods_equivalent': methods, 'state_transform_combinations': 1800,
          'required_tests_passed': 596, 'checks': ['Mirror then rotation follows an independent integer-vector oracle',
          'Unrelated block properties are preserved', 'Four quarter-turns and double mirror restore the full block state']}
(root / 'build/porting/machine-transform-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
