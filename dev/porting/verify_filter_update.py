"""Verify upstream filter persistence and native hand/menu integration."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
path = 'src/main/java/dev/dubhe/anvilcraft/block/entity/IFilterBlockEntity.java'
original = subprocess.check_output(['git', 'show', f'{source}:{path}'], cwd=root).decode('utf-8')
native = (root / path).read_text(encoding='utf-8')


def body(text, name):
    start = text.index('{', text.index(name))
    end, depth = start + 1, 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return re.sub(r'\s+', '', text[start:end])


methods = ['void filterChanged(', 'void setFilterEnabled(', 'void setSlotDisabled(', 'boolean setFilter(', 'void setSlotLimit(']
for method in methods:
    assert body(original, method) == body(native, method), method
item = (root / 'src/main/java/dev/dubhe/anvilcraft/item/utility/FilterItem.java').read_text(encoding='utf-8')
assert 'player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND' in item
tests = (root / 'build/porting/tests-filter-focused-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 1 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests
client = (root / 'build/porting/client-filter-final-2.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_FILTER_CLIENT_PASSED' in client and 'All dimensions are saved' in client and 'BUILD SUCCESSFUL' in client
report = {'source_commit': source, 'source_methods_equivalent': methods, 'focused_tests_passed': 1,
          'coverage': ['Real chunk dirty marking for every shared filter mutation; rejected empty filter is unchanged',
                       'Native block-entity save/load preserves enable flag, disabled slots, sample items and limits',
                       'Actual offhand and selected main-hand use opens the corresponding client/server menu',
                       'Content and option packets update each intended filter independently'],
          'limits': ['Client scenario drives native use and packet APIs; manual mouse input is not part of this fixture.',
                     'Client inventory synchronization may replace stack objects; server menu identity and final packet writes are verified.']}
(root / 'build/porting/filter-update-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
