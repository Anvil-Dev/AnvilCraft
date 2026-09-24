"""Require native integration evidence for the source storage interaction updates."""
from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[2]
tests = (root / 'build/porting/tests-storage-source-update-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All dimensions are saved' in tests
passed = re.search(r'All (\d+) required tests passed', tests)
assert passed and int(passed[1]) >= 575
client = (root / 'build/porting/client-storage-source-update-3.log').read_text(encoding='utf-8', errors='replace')
markers = [
    'PORT_STORAGE_CRAFT_AWARDS_CLIENT_PASSED',
    'PORT_CRAFTING_EXECUTION_CLIENT_PASSED',
    'PORT_STORAGE_FLIP_SCENE_PASSED',
    'PORT_STORAGE_EXTENSION_CLICK_PASSED',
    'PORT_STORAGE_MENU_SCENE_PASSED',
    'PORT_STORAGE_JEI_LOCKED_PASSED',
    'PORT_STORAGE_JEI_SCENE_PASSED',
    'All dimensions are saved',
]
for marker in markers:
    assert marker in client, marker
style = (root / 'build/porting/compile-storage-source-update-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in style
report = {
    'source_commit': '1260db54f6e2b9f88d0a79b8108459d346344e44',
    'required_tests_passed': int(passed[1]),
    'source_changes_reviewed': [
        'Shift result consumption already uses the native common prepare/apply path',
        'Craft events, item hooks, recipe criteria and unlocks added to all successful result paths',
        'Merged entries extract live matching resources across storage backends',
        'Merged counts participate in native transaction rollback',
        'Locked recipe transfers reject on client and server',
        'Existing native mirrored slot replacement retained',
        'Extension slotClicked entry no longer sends a duplicate vanilla click',
    ],
    'client_markers': markers[:-1],
    'limits': [
        'The locked JEI probe changes client availability and reopens the recipe page; server unlock authorization is tested separately.',
        'Two initial client runs were terminated after the probe changed availability without rebuilding the JEI button layout.',
        'FakePlayer ignores statistics; the actual connected player validates crafted-item statistics.',
        'Server and client checks passed; the separate final compile/checkPortJava run validates diagnostic-log formatting repairs.',
        'Other source branch features and optional renderer integrations remain pending.',
    ],
}
(root / 'build/porting/storage-source-update-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
