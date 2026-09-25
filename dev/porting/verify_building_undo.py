"""Verify source settlement algorithms and native server/client evidence."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
ref = '1260db54f6e2b9f88d0a79b8108459d346344e44'


def body(text, name):
    match = re.search(r'^    \S[^\n]*\b' + name + r'\(', text, re.M)
    assert match, name
    start = text.index('{', match.start())
    depth = 1
    end = start + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return re.sub(r'\s+', '', text[start:end])


checked = []
for file, names in {
    'BuildingUndoResources': ['item', 'fluid', 'cancel'],
    'BuildingRegionSnapshot': ['resources', 'checkParts'],
    'BuildingMaterials': ['excludeFluidSources', 'reserveRefundContainers'],
    'BuildingRodUndo': ['owned', 'entityResources', 'hasPendingRefund', 'mobResource'],
}.items():
    path = f'src/main/java/dev/dubhe/anvilcraft/building/{file}.java'
    source = subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root).decode('utf-8')
    source = source.replace('.getInt("BurnTime")', '.getIntOr("lit_time_remaining", 0)')
    source = source.replace('.getBoolean("Sheared")', '.getBooleanOr("Sheared", false)')
    source = source.replace('egg.getType(original)', 'SpawnEggItem.getType(original)')
    source = source.replace('EntityType.by(saved.tag()).orElse(null)', 'saved.type()')
    source = source.replace('new SavedEntity(contents, saved.isMonster())', 'new SavedEntity(saved.type(), contents, saved.isMonster())')
    target = (root / path).read_text(encoding='utf-8')
    for name in names:
        assert body(source, name) == body(target, name), f'{file}.{name}'
        checked.append(f'{file}.{name}')
tests = (root / 'build/porting/validate-building-undo-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All dimensions are saved' in tests
passed = re.search(r'All (\d+) required tests passed', tests)
assert passed and int(passed[1]) >= 572
client = (root / 'build/porting/client-building-undo-final.log').read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_BUILDING_SERVICE_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in client, marker
report = {
    'source_commit': ref,
    'source_algorithms_equal_after_api_mapping': checked,
    'required_tests_passed': int(passed[1]),
    'checks': ['Picked-up drops are not refunded twice', 'Live entity products replace original receipts',
               'Missing original chest contents require exact payment', 'Eaten cake and consumed furnace fuel reject undo',
               'Missing containers and full fluid storage reject before world restoration',
               'Execution-time refill refusal retains debt and blocks new construction',
               'Retry pays fluid debt once without restoring the world again',
               'Existing material transaction, placement and activation tests',
               'Real client place/undo packets, inventory and energy synchronization'],
    'limits': ['Client validation covers ordinary place/undo; deficit and refill failure paths use server GameTests.',
               'Other source interaction, rendering compatibility and blueprint placement changes remain separate work.']
}
(root / 'build/porting/building-undo-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
