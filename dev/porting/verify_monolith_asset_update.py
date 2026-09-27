"""Check the latest monolith artwork and clean native/reference client captures."""
from pathlib import Path
import json
import subprocess

from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'
assets = subprocess.check_output(['git', 'diff-tree', '--no-commit-id', '--name-only', '-r', ref], cwd=root).decode().splitlines()
assert len(assets) == 8
for name in assets:
    assert (root / name).read_bytes() == subprocess.check_output(['git', 'show', f'{ref}:{name}'], cwd=root), name
animation_path = root / 'src/main/resources/assets/anvilcraft/textures/block/monolith_inner.png'
with Image.open(animation_path) as image:
    assert image.size == (16, 48)
    frames = [image.convert('RGBA').crop((0, y, 16, y + 16)).tobytes() for y in [0, 16, 32]]
    assert len(set(frames)) == 3
animation = json.loads(animation_path.with_suffix('.png.mcmeta').read_text(encoding='utf-8'))['animation']
assert animation == {'frametime': 60, 'interpolate': True, 'frames': [0, 1, 0, 2]}
for variant in ['target', 'source']:
    log = (root / f'build/porting/client-monolith-assets-{variant}-final.log').read_text(encoding='utf-8', errors='replace')
    for marker in ['PORT_MONOLITH_ONLINE_REWARD_PASSED', 'PORT_MONOLITH_VISUAL_PASSED',
                   'All dimensions are saved', 'BUILD SUCCESSFUL']:
        assert marker in log, (variant, marker)
    for age in [20, 50, 65, 80, 95, 100]:
        assert f'PORT_MONOLITH_SAMPLE: age={age}' in log, (variant, age)
validation = (root / 'build/porting/data-monolith-assets-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in validation
report = {'source_commit': ref, 'source_assets_equal': assets, 'inner_animation': animation,
          'animation_frames': 3, 'client_offering_ages': [20, 50, 65, 80, 95, 100],
          'limits': ['Animation metadata and all pixels match the source; captures do not synchronize animation frame phase.',
                     'Vanilla 26.1 lighting and terrain rendering are retained.',
                     'Initial native client failed after overlapping validation compilation; only clean rerun evidence is accepted.']}
(root / 'build/porting/monolith-assets-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
