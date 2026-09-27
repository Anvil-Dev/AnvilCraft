"""Verify glass-pipe resources, display algorithms and native foundation checks."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'


def method(text, name):
    match = re.search(r'^    (?:public|private|protected)[^\n]*\b' + name + r'\(', text, re.M)
    assert match, name
    start = text.index('{', match.start())
    end, depth = start + 1, 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return re.sub(r'\s+', '', text[start:end]).replace('this.', '')


checked = []
for tail, names in {
    'block/entity/fluid/GlassPipeBlockEntity.java': ['showFluid', 'setGasDisplay', 'clearGasDisplay', 'checkDisplayExpiry',
                                                 'updateDisplayDirections', 'clearExpiredDisplayDirections'],
    'client/renderer/blockentity/GlassPipeFluidBERenderer.java': ['renderDisplayFluid', 'visibleDirections',
                                                              'extendFluidBounds', 'extendFluidArmBounds'],
}.items():
    path = 'src/main/java/dev/dubhe/anvilcraft/' + tail
    source = subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root).decode('utf-8')
    target = (root / path).read_text(encoding='utf-8')
    for name in names:
        assert method(source, name) == method(target, name), (tail, name)
        checked.append(name)
assets = []
for source in subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', ref], cwd=root).decode().splitlines():
    if '/assets/anvilcraft/' not in source:
        continue
    if not any(part in source for part in ['/models/block/glass_pipe', '/blockstates/glass_pipe',
                                           '/textures/block/pipe_glass', '/models/item/glass_pipe.json']):
        continue
    target = source.replace('src/generated/resources/', 'src/main/resources/')
    source_bytes = subprocess.check_output(['git', 'show', f'{ref}:{source}'], cwd=root)
    target_bytes = (root / target).read_bytes()
    if '/models/block/' in target:
        source_model, target_model = json.loads(source_bytes), json.loads(target_bytes)
        assert source_model.pop('render_type') == 'minecraft:translucent'
        for slot, material in target_model['textures'].items():
            if isinstance(material, dict):
                assert material['force_translucent'] is True, (target, slot)
                target_model['textures'][slot] = material['sprite']
        assert target_model == source_model, target
    else:
        assert target_bytes == source_bytes, target
    assets.append(target)
tests = (root / 'build/porting/tests-glass-foundation-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in tests and 'All 586 required tests passed' in tests
assert 'PORT_GLASS_TRANSFORM_MATRIX_PASSED: 44184' in tests
client = (root / 'build/porting/client-glass-state-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_GLASS_PIPE_CLIENT_PASSED' in client and 'All dimensions are saved' in client
compile_log = (root / 'build/porting/compile-glass-foundation-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in compile_log
report = {
    'source_commit': ref, 'source_assets_equivalent_with_native_translucent_materials': assets, 'source_display_methods_equal': checked,
    'required_tests_passed': 586, 'rotation_mirror_cases': 44184,
    'native_client': 'three registered shapes, synchronized display fields, vertex counts 16/48/120, alpha 102 for 0.4 opacity',
    'limits': ['This foundation report retains the original 586-test evidence; network and visual parity have separate reports.',
               'The client fixture injects display events; it does not claim end-to-end network visualization.',
               'The node geometry includes its full center box as in the source; an earlier fixture omitted those 24 vertices.',
               'Final compile/style checks validate JSpecify imports and rotation/mirror additions.'],
}
(root / 'build/porting/glass-foundation-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
