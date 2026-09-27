"""Collect native acceptance evidence for the source production-behavior update."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root, text=True).strip() == source
focused = (root / 'build/porting/tests-production-update-focused-2.log').read_text(encoding='utf-8', errors='replace')
assert 'All 4 required tests passed' in focused and 'BUILD SUCCESSFUL' in focused
final = (root / 'build/porting/tests-production-update-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 617 required tests passed' in final and 'BUILD SUCCESSFUL' in final
assert '> Task :runData' in final and '> Task :checkPortJava' in final
report = {'source_commit': source, 'source_update': '1260db54f', 'required_tests_passed': 617,
          'coverage': ['Fountain interval, one-bucket lava production, full/partial/incompatible drain contents and height restriction',
                       'Fish-tank commit guard, repeated cache writes, once-per-tick output processing, closed/blocked/unblocked outlet',
                       'Recipe item-spawn event returns consumed catalysts, never over-refunds or ignores component differences',
                       'Shaped/shapeless compression intersects all ingredients, accepts simple compound ingredients, preserves count and source ID',
                       'Disjoint, component-sensitive and empty-cell recipes are rejected'],
          'limits': ['Acceptance uses native dedicated-server GameTests and runtime recipe wrappers; no client visual changes are introduced.',
                     'This node does not prove completion of the broader source-branch feature inventory.']}
(root / 'build/porting/production-update-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
