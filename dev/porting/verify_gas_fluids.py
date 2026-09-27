"""Verify gas semantics, bucket models, paired geometry and fixed-frame GUI captures."""
from pathlib import Path
import json
import subprocess
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
source = '07eb60b47f14439375da10e78f4d4770f3a83256'
assert subprocess.check_output(['git', 'rev-parse', 'dev/1.21/1.6'], cwd=root, text=True).strip() == source
names = ['hydrogen', 'oxygen', 'helium', 'deuterium', 'xenon', 'krypton', 'primordial_matter']
for name in ['GasFluid', 'PrimordialMatterFluid']:
    path = f'src/main/java/dev/dubhe/anvilcraft/fluid/{name}.java'
    original = subprocess.check_output(['git', 'show', f'{source}:{path}'], cwd=root).decode('utf-8').replace('\r\n', '\n')
    assert (root / path).read_text(encoding='utf-8') == original
for name in names:
    original = json.loads((reference / f'src/generated/resources/assets/anvilcraft/models/item/{name}_bucket.json').read_text())
    native = json.loads((root / f'src/generated/resources/assets/anvilcraft/items/{name}_bucket.json').read_text())['model']
    assert original['fluid'] == native['fluid'] == f'anvilcraft:{name}'
    assert original['flip_gas'] and native['flip_gas'] and not native['force_opaque_fluid']
    assert native['textures']['base'] == {'sprite': 'anvilcraft:block/bucket', 'force_translucent': True}
    assert native['textures']['fluid'] == 'neoforge:item/mask/bucket_fluid_drip'
for name in ['water_still', 'water_flow']:
    a = np.array(Image.open(root / f'build/porting/gas-fluids-source-assets/assets/minecraft/textures/block/{name}.png').convert('RGBA'))
    b = np.array(Image.open(root / f'build/porting/gas-fluids-target-assets/assets/minecraft/textures/block/{name}.png').convert('RGBA'))
    assert np.array_equal(a, b)
    for folder in [root, reference]:
        for suffix in ['.png', '.png.mcmeta']:
            assert not (folder / f'build/resources/main/assets/minecraft/textures/block/{name}{suffix}').exists()

for name in ['client-gas-fluids-final-6.log', 'client-gas-fluids-source-mesh.log']:
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    assert 'PORT_GAS_FLUIDS_CLIENT_PASSED' in log and 'BUILD SUCCESSFUL' in log and 'All dimensions are saved' in log
    assert '> Task :clearGasFluidReferenceTextures' in log
tests = (root / 'build/porting/tests-gas-fluids-final-3.log').read_text(encoding='utf-8', errors='replace')
assert 'All 627 required tests passed' in tests and 'BUILD SUCCESSFUL' in tests and '> Task :runData' in tests

a = np.array(Image.open(reference / 'run/mun-reference/screenshots/gas-fluids-1.21.png').convert('RGB'))
b = np.array(Image.open(root / 'run/port-validation/client/screenshots/gas-fluids-26.1.png').convert('RGB'))
visuals = {}
for index, name in enumerate(names):
    x, y = (320 - 144 + index % 4 * 80) * 2, (180 - 82 + index // 4 * 90) * 2
    original, native = a[y:y + 128, x:x + 128], b[y:y + 128, x:x + 128]
    mask = np.any(original != 48, axis=2)
    assert np.array_equal(mask, np.any(native != 48, axis=2)), name
    error = np.abs(original.astype(int) - native.astype(int))
    assert error.max() <= 3 and error[mask].mean() < 2.0, name
    visuals[name] = {'silhouette_iou': 1.0, 'mean_rgb_error': float(error[mask].mean()), 'maximum_rgb_error': int(error.max())}

source_mesh = json.loads((reference / 'run/mun-reference/gas-bucket-mesh-1.21.json').read_text())
native_mesh = json.loads((root / 'run/port-validation/client/gas-bucket-mesh-26.1.json').read_text())
source_mesh = [quad for quad in source_mesh if quad['sprite'] == 'minecraft:block/water_still']
native_mesh = [quad for quad in native_mesh if quad['sprite'] == 'minecraft:block/water_still']
assert len(source_mesh) == len(native_mesh) == 58
maximum_uv_error = 0.0
for quad in source_mesh:
    original = np.array(sorted(quad['vertices'], key=lambda vertex: tuple(round(value, 6) for value in vertex[:3])))
    candidates = [np.array(sorted(candidate['vertices'], key=lambda vertex: tuple(round(value, 6) for value in vertex[:3])))
                  for candidate in native_mesh if candidate['direction'] == quad['direction']]
    native = min(candidates, key=lambda candidate: np.linalg.norm(candidate[:, :3] - original[:, :3]))
    assert np.array_equal(original[:, :3], native[:, :3])
    maximum_uv_error = max(maximum_uv_error, float(np.abs(original[:, 3:] - native[:, 3:]).max()))
assert maximum_uv_error < 0.001
report = {'source_commit': source, 'fluids_and_buckets': names, 'required_tests_passed': 627,
          'source_fluid_classes_equal': 2, 'fluid_quads_with_identical_positions': 58,
          'maximum_normalized_uv_error': maximum_uv_error, 'visuals': visuals,
          'coverage': ['Native transactional bucket drain/rollback and primordial-matter refill',
                       'Actual registered gas equilibrium, complete glass-pipe display and tank persistence',
                       'Source material profiles, tints, gas flipping and contained-only world behavior'],
          'limits': ['Paired renders freeze vanilla water animation at frame zero only in developer fixtures; output overrides are cleaned afterward.',
                     'Atlas sizes change the legacy UV inset slightly; native GUI lighting retains a maximum three-channel-value difference.',
                     'The six chemical gases retain source getBucket=AIR behavior; primordial matter provides the registered refill bucket.']}
(root / 'build/porting/gas-fluids-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
