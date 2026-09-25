"""Check source layout/origin fidelity and native blueprint placement evidence."""
from pathlib import Path
import json
import re
import subprocess

import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = '1260db54f6e2b9f88d0a79b8108459d346344e44'
path = 'src/main/java/dev/dubhe/anvilcraft/util/BlockPlacementUtil.java'
source = subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root).decode('utf-8')
target = (root / path).read_text(encoding='utf-8')


def section(text, begin, end):
    return re.sub(r'\s+', '', text[text.index(begin):text.index(end, text.index(begin))])


for begin, end in [
    ('public record BlueprintLayout(', 'public record MultiblockPart('),
    ('private static <P extends Enum<P>> BlockPos getMultiblockPlacementPos(', 'private static void orientPlayerForState('),
    ('public static boolean isSecondaryBlueprintPart(', 'public static boolean isSecondaryMultiblockPart('),
]:
    assert section(source, begin, end) == section(target, begin, end), begin
tests = (root / 'build/porting/tests-blueprint-placement-final-2.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in tests and 'All dimensions are saved' in tests
assert 'PORT_BLUEPRINT_LAYOUT_MATRIX_PASSED: 8000' in tests
passed = re.search(r'All (\d+) required tests passed', tests)
assert passed and int(passed[1]) >= 582
client = (root / 'build/porting/client-smart-blueprint-final-3.log').read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_SMART_BLUEPRINT_INSTALLED', 'PORT_SMART_BLUEPRINT_PREVIEW_PASSED',
               'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in client, marker
assert (root / 'run/port-validation/client/screenshots/smart-placer-26.1-2-blueprint.png').is_file()
source_client = (root / 'build/porting/client-smart-blueprint-source-raw-2.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in source_client and 'PORT_SMART_BLUEPRINT_INSTALLED' in source_client
images = [
    root / 'build/porting/reference-mun-1.21/run/mun-reference/screenshots/smart-placer-1.21-2-blueprint-raw.png',
    root / 'run/port-validation/client/screenshots/smart-placer-26.1-2-blueprint-raw.png',
]
arrays = [np.asarray(Image.open(path).convert('RGB'))[192:372, 656:880].astype(float) for path in images]


def masks(array):
    red, green, blue = array.transpose(2, 0, 1)
    return {
        'red': (red > 60) & (green < .4 * red) & (blue < .5 * red),
        'blue': (blue > 50) & (blue > red * 1.5) & (blue > green * 1.5),
        'yellow': (red > 100) & (green > .5 * red) & (blue < .25 * red),
        'wire': (green > 200) & (blue > 150) & (red < 50),
    }


def dilate(mask):
    height, width = mask.shape
    padded = np.pad(mask, 1)
    return np.logical_or.reduce([padded[y:y + height, x:x + width] for y in range(3) for x in range(3)])


source_masks, target_masks = map(masks, arrays)
visual = {}
for name, source_mask in source_masks.items():
    target_mask = target_masks[name]
    union = np.logical_or(source_mask, target_mask)
    intersection = np.logical_and(source_mask, target_mask)
    outside = int(np.count_nonzero(source_mask & ~dilate(target_mask)) + np.count_nonzero(target_mask & ~dilate(source_mask)))
    visual[name] = {'iou': float(intersection.sum() / union.sum()), 'pixels_beyond_one_pixel': outside}
    if name != 'wire':
        assert np.array_equal(source_mask, target_mask), name
        visual[name]['mean_rgb_difference'] = float(np.abs(arrays[0][intersection] - arrays[1][intersection]).mean())
    else:
        assert np.array_equal(source_mask, target_mask), 'wire'
report = {
    'source_commit': ref,
    'source_layout_and_origin_sections_equal': 3,
    'orientation_combinations': 8000,
    'required_tests_passed': int(passed[1]),
    'source_native_raw_preview': visual,
    'checks': ['All scanner/target headings, auto-rotation and upside-down states',
               'Non-square disk frame bounds and canonical preview orientation',
               'Giant anvil, large cauldron and large tank item origins and ten-tick integrity',
               'Straight, corner and node pipe states through the shared placement entry',
               'Complete large-cake snapshots, single material count and ten-tick survival',
               'Native blueprint preview using an east-facing 3x2x1 disk with auto-rotation disabled',
               'Source pixel-width outline composited after the structure and before the scan effect'],
    'limits': ['Glass pipes are an unported source feature; their material variant path remains pending.',
               'Raw previews use the same east-facing 3x2x1 scene; native vanilla texture sampling is retained.',
               'An initial source comparison exposed missing preview transforms and line-width differences; these were corrected.',
               'One Moon-light fixture failed during concurrent source-client validation; the subsequent serial suite passed.',
               'Other source block-picking/input updates remain separate work.'],
}
(root / 'build/porting/blueprint-placement-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
