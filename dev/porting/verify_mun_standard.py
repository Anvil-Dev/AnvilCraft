"""Compare Standard Moon images, source shadow logic and native receiver-state checks."""
from pathlib import Path
import argparse
import json
import re
import zipfile

import numpy as np
from PIL import Image, ImageFilter

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--target-log', default='client-mun-standard-target-final-3.log')
parser.add_argument('--source-log', default='client-mun-standard-source-final.log')
args = parser.parse_args()
source = root / 'build/porting/reference-mun-1.21/run/mun-reference/screenshots'
target = root / 'run/port-validation/client/screenshots'
shader_path = 'src/main/resources/assets/anvilcraft/shaders/include/mun/mun_surface.glsl'
original = (root / 'build/porting/reference-mun-1.21' / shader_path).read_text(encoding='utf-8')
actual = (root / shader_path).read_text(encoding='utf-8')


def function(text, signature):
    start = text.index(signature)
    position = text.index('{', start) + 1
    depth = 1
    while depth:
        depth += (text[position] == '{') - (text[position] == '}')
        position += 1
    return text[start:position]


functions = re.findall(r'^((?:void|float|bool|uint|vec[234]) \w+\()', original, re.M)
for signature in functions:
    body = function(actual, signature)
    if signature == 'vec4 surfaceLight(':
        body = body.replace('    #ifdef MUN_SHADOWS\n', '').replace('    #endif\n', '')
    assert body == function(original, signature), signature
with zipfile.ZipFile(root / 'build/moddev/artifacts/minecraft-patched-26.1.2.75.jar') as jar:
    vanilla = jar.read('assets/minecraft/shaders/core/terrain.fsh').decode('utf-8')
fragment = (root / 'src/main/resources/assets/anvilcraft/shaders/core/mun/mun_surface.fsh').read_text(encoding='utf-8')
for signature in ['vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize,',
                  'vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize)', 'vec4 sampleRGSS(']:
    assert function(vanilla, signature) == function(fragment, signature), signature


def read(path):
    return np.asarray(Image.open(path).convert('RGB'), dtype=np.int16)


assert np.array_equal(read(source / 'mun-lightmap-1.21.png'), read(target / 'mun-lightmap-26.1.png'))
timings = {}
for name in [args.source_log, args.target_log]:
    log = (root / 'build/porting' / name).read_text(encoding='utf-8', errors='replace')
    timings[name] = {case: {'frames': int(frames), 'milliseconds': int(milliseconds)}
                     for case, frames, milliseconds in re.findall(
                         r'PORT_MUN_SURFACE_TIMING: ([\w-]+), frames=(\d+), milliseconds=(\d+)', log)}
    for marker in ['PORT_MUN_SURFACE_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
        assert marker in log, (name, marker)
    if name == args.target_log:
        assert 'PORT_MUN_RECEIVER_SCOPE_PASSED' in log and 'PORT_MUN_RECEIVER_PASSED' in log

report = {'cases': {}, 'source_surface_functions_equal': len(functions), 'native_terrain_sampling_equal': True,
          'lightmap_equal_pixels': 256, 'capture_timings': timings, 'target_log': args.target_log, 'source_log': args.source_log,
          'limits': ['Native 26.1 albedo filtering, AO and vanilla OFF rendering remain in use.',
                     'Small raster/texture-boundary differences remain, particularly at large coordinates.',
                     'Third-party rendering and non-OpenGL backends have not been runtime-validated.']}
for case in ['day', 'night', 'block-light', 'ao-off', 'off', 'day-again', 'large-coordinate']:
    a = read(source / f'mun-standard-1.21-{case}.png')
    b = read(target / f'mun-standard-26.1-{case}.png')
    delta = np.abs(a - b)
    white = np.abs(a[310:350, 545:570].mean((0, 1)) - b[310:350, 545:570].mean((0, 1)))
    chest = np.abs(a[398:414, 625:647].mean((0, 1)) - b[398:414, 625:647].mean((0, 1)))
    entry = {'material_region_mean_rgb_difference': float(delta[260:700, 200:1000].mean()),
             'max_rgb_difference': int(delta.max()), 'white_column_mean_color_difference': white.tolist(),
             'chest_mean_color_difference': chest.tolist()}
    if case != 'off':
        assert entry['material_region_mean_rgb_difference'] < 5, (case, entry)
        assert white.max() < 0.3 and chest.max() < 1.5, (case, white, chest)
    if case in ['day', 'ao-off', 'day-again', 'large-coordinate']:
        masks = []
        for folder, version, standard in [(source, '1.21', a), (target, '26.1', b)]:
            potato = read(folder / f'mun-surface-{version}-{case}.png')
            mask = (potato - standard).mean(2) > 8
            mask[:380] = False
            mask[620:] = False
            mask[:, :510] = False
            mask[:, 1020:] = False
            masks.append(mask)
        union = np.logical_or(*masks).sum()
        iou = float(np.logical_and(*masks).sum() / union)
        dilation = [np.asarray(Image.fromarray(mask.astype('uint8') * 255).filter(ImageFilter.MaxFilter(3))) > 0 for mask in masks]
        outside = int((masks[0] & ~dilation[1]).sum() + (masks[1] & ~dilation[0]).sum())
        total = int(sum(mask.sum() for mask in masks))
        assert iou > (0.93 if case == 'large-coordinate' else 0.99), (case, iou)
        assert outside / total < 0.001, (case, outside, total)
        entry.update(shadow_mask_iou=iou, shadow_pixels_beyond_one_pixel=outside, compared_shadow_pixels=total)
    report['cases'][case] = entry
(root / 'build/porting/mun-standard-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
