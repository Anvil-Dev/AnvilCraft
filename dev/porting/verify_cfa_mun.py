"""Validate original Moon discovery data and native CFA entry/return evidence."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
source = root / 'build/porting/reference-mun-1.21'
for tail in ['recipe/special_celestial_body/mun.json', 'advancement/recipes/special_celestial_body/mun.json']:
    path = Path('src/generated/resources/data/anvilcraft') / tail
    assert json.loads((root / path).read_text(encoding='utf-8')) == json.loads((source / path).read_text(encoding='utf-8')), tail
client = (root / 'build/porting/client-cfa-mun-1.log').read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_TRAVEL_PLAYER_OUTBOUND: dimension=ResourceKey[minecraft:dimension / anvilcraft:mun]',
               'PORT_TRAVEL_PLAYER_RETURNED: dimension=ResourceKey[minecraft:dimension / minecraft:overworld]',
               'PORT_TRAVEL_PLAYER_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in client, marker
tests = (root / 'build/porting/tests-cfa-mun-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_MUN_CFA_RECIPE_PASSED' in tests and 'BUILD SUCCESSFUL' in tests
report = {'source_recipe_and_advancement_equal': True, 'native_recipe_matching': 'passed',
          'live_player_entry_and_linked_return': 'passed',
          'parameters': {'time': 34, 'space': 4, 'mass': 1, 'energy': 14, 'seed': 'anvilcraft:lunar_rock'},
          'limits': ['Monolith blocks, generation, offerings and return confirmation remain separate work.']}
(root / 'build/porting/cfa-mun-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
