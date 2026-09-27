"""Verify source claw assets, native pose checks and paired first-person captures."""
from pathlib import Path
import json
import subprocess

import numpy as np
from PIL import Image, ImageFilter

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'
assets = []
for name in ['crab_claw', 'crab_claw_holding_item', 'crab_claw_holding_block',
             'crab_claw_holding_block_slab', 'crab_claw_holding_block_panel']:
    path = f'src/main/resources/assets/anvilcraft/models/item/{name}.json'
    assert json.loads((root / path).read_bytes()) == json.loads(
        subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root)), path
    assets.append(path)
for name in ['ember_metal_heavy_halberd', 'ember_metal_heavy_halberd_spear',
             'ember_metal_heavy_halberd_sword', 'ember_metal_heavy_halberd_mace']:
    path = f'src/main/resources/assets/anvilcraft/models/item/{name}.json'
    original = json.loads(subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root))
    if name == 'ember_metal_heavy_halberd':
        original.pop('overrides')
    assert json.loads((root / path).read_bytes()) == original, path
    assets.append(path)
textures = []
for name in ['crab_claw', 'crab_claw_in', 'crab_claw_movable', 'crab_claw_movable_in']:
    path = f'src/main/resources/assets/anvilcraft/textures/item/{name}.png'
    assert (root / path).read_bytes() == subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root), path
    textures.append(path)
cases = ['stone', 'slab', 'panel', 'fence', 'pipe', 'valve', 'torch', 'trident', 'throwing',
         'halberd_spear', 'halberd_throwing', 'cfa', 'cfa_plain', 'halberd_trident', 'halberd_mace', 'halberd_sword']
for name in ['client-crab-claw-final.log', 'client-crab-claw-source-final.log']:
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    for marker in ['PORT_CRAB_CLAW_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
        assert marker in log, (name, marker)
    for hand in ['left', 'right']:
        for case in cases:
            assert f'PORT_CRAB_CAPTURE: {hand}-{case}' in log, (name, hand, case)
    if name == 'client-crab-claw-final.log':
        assert log.count('PORT_CRAB_PROBE_PASSED') == 2
        assert log.count('PORT_CRAB_THROW_POSE_PASSED') == 4
validation = (root / 'build/porting/tests-crab-claw-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in validation and 'All 596 required tests passed' in validation
datagen = (root / 'build/porting/data-crab-claw-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in datagen
report = {'source_commit': ref, 'source_models_equal': assets, 'source_textures_equal': textures, 'paired_captures': 32,
          'reference_hand_light': 'full_bright', 'reference_use_ticks': 20, 'reference_partial_tick': 0,
          'native_model_pose_cases': 18, 'native_throw_pose_cases': 4, 'required_tests_passed': 596, 'held_masks': {},
          'limits': ['Production retains vanilla 26.1 lighting and item rendering; this dev fixture supplies equal full-bright hand lighting.',
                     'Source flat-world sky light remained 1 despite noon and settled lighting, so natural-environment lighting is not compared.',
                     'Per-case hidden-hand backgrounds isolate held geometry; a contrasting wall avoids color ambiguity.',
                     'CFA bypass and carried building rod are checked through the shared native renderer before and after resource reload.']}
for hand in ['left', 'right']:
    for case in [case for case in cases if case not in ['cfa', 'cfa_plain']]:
        masks = []
        for folder, version in [('build/porting/reference-mun-1.21/run/mun-reference/screenshots', '1.21'),
                                ('run/port-validation/client/screenshots', '26.1')]:
            frame = np.asarray(Image.open(root / folder / f'crab-{version}-{hand}-{case}.png').convert('RGB'), dtype=float)
            background = np.asarray(Image.open(root / folder / f'crab-{version}-{hand}-{case}-background.png').convert('RGB'), dtype=float)
            mask = np.max(np.abs(frame - background), axis=2) > 5
            mask[665:] = False
            mask[:70, 400:800] = False
            mask[600:, 400:840] = False
            if hand == 'right':
                mask[:, :640] = False
            else:
                mask[:, 640:] = False
            masks.append(mask)
        assert min(int(mask.sum()) for mask in masks) > 1000, (hand, case, 'missing held geometry')
        union = np.logical_or(*masks).sum()
        iou = float(np.logical_and(*masks).sum() / union)
        dilation = [np.asarray(Image.fromarray(mask.astype('uint8') * 255).filter(ImageFilter.MaxFilter(3))) > 0 for mask in masks]
        outside = int((masks[0] & ~dilation[1]).sum() + (masks[1] & ~dilation[0]).sum())
        report['held_masks'][hand + '-' + case] = {'iou': iou, 'outside_one_pixel': outside,
                                                 'source_pixels': int(masks[0].sum()), 'native_pixels': int(masks[1].sum())}
        assert iou > 0.98 and outside / union < 0.005, (hand, case, iou, outside)
(root / 'build/porting/crab-claw-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
