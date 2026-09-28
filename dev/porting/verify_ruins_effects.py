"""Collect evidence from identical Ruins effect probes in the native and reference clients."""
from pathlib import Path
import json
import subprocess

from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root).decode().strip()
assert source == subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=reference).decode().strip()
logs = ['client-ruins-effects-3.log', 'client-ruins-effects-reference-2.log']
for name in logs:
    text = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_RUINS_EFFECTS_PASSED' in text and 'BUILD SUCCESSFUL' in text, name
captures = [root / 'run/port-validation/client/screenshots/ruins-effects-26.1-books.png',
            reference / 'run/mun-reference/screenshots/ruins-effects-1.21-books.png']
for path in captures:
    with Image.open(path) as image:
        assert image.size == (1280, 720)
report = {
    'source_commit': source,
    'client_logs': logs,
    'checks': [
        'The real disguised enchanting table opens toward a nearby player and advances across game ticks.',
        'Twenty additional render preparations during one game tick do not advance its animation.',
        'A disguised celestial forging anvil advances rotation once per game tick without registering amplifier previews.',
        'Grass destruction emits 64 original terrain particles; slab destruction emits 32; a grass hit emits one.',
        'Every emitted particle uses the original sprite and RGB tint; ordinary stone particles remain unchanged.',
        'After server destruction and client removal, the original grass sprite and count remain available through the cache.'
    ],
    'visual_limits': [
        'Paired book screenshots require visual review; randomized page flips are not phase-synchronized.',
        'This verifies particle sprite, tint and count through actual client effect calls, not every possible block effect.',
        'Vanilla 26.1 terrain and atmospheric rendering is retained.'
    ],
    'pending': ['Fluid rendering', 'Dynamic selection and expanded culling bounds',
                'Neighbor face culling', 'Gateway item animation pixel comparison']
}
(root / 'build/porting/ruins-effects-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
