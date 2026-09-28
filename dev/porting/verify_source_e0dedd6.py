"""Verify the incremental local 1.21 source update through e0dedd6."""
from pathlib import Path
import json
import subprocess

root = Path(__file__).resolve().parents[2]
source = 'e0dedd6e8f1dd8667090eae161ba372c4da10bc2'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip() == source
server = (root / 'build/porting/tests-source-e0dedd6-all-2.log').read_text(encoding='utf-8', errors='replace')
assert 'All 688 required tests passed' in server and 'BUILD SUCCESSFUL' in server
client = (root / 'build/porting/client-source-e0dedd6-3.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_SOURCE_UPDATE_CLIENT_PASSED' in client and 'BUILD SUCCESSFUL' in client
key = 'tooltip.anvilcraft.item.weatherproof_spacesuit_helmet'
english = json.loads((root / 'src/generated/resources/assets/anvilcraft/lang/en_us.json').read_text(encoding='utf-8'))
chinese = json.loads((root / 'src/main/resources/assets/anvilcraft/lang/zh_cn.json').read_text(encoding='utf-8'))
assert 'Endermen remain calm when stared at' in english[key]
assert '视线不会激怒末影人' in chinese[key]
report = {
    'source_commit': source,
    'required_tests_passed': 688,
    'checks': ['Integer and fractional anvil distances produce the source splitter share counts.',
               'Ruby/Sapphire effects refresh to 210 ticks inside lava/water; native milk and honey removal remain functional.',
               'Food effects cover component listeners and direct consumption; nested, exceptional and cross-entity contexts are isolated.',
               'Actual client stew consumption blocks harmful effects, retains beneficial effects and clears its food context.',
               'Weatherproof helmets suppress the vanilla stare predicate independently of night vision and preserve normal aggression.',
               'Creative rejected writes produce only the affected-slot packets; the live client retains its cursor stack.',
               'English and Chinese helmet tooltips describe Enderman protection.'],
    'adaptation': '26.1 removed per-instance cure sets; there is no getCures state to copy. Native consumable removal behavior is verified instead.',
    'limits': ['The first broad run failed the existing unticked-chunk setup watchdog; the subsequent complete run passed all 688 tests.']
}
(root / 'build/porting/source-e0dedd6-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
