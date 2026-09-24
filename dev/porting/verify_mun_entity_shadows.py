"""Check source capture limits/primitive rules and native animated caster probes."""
from pathlib import Path
import argparse
import json

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--target-log', default='client-mun-entities-final-4.log')
args = parser.parse_args()
path = 'src/main/java/dev/dubhe/anvilcraft/client/renderer/mun/MunEntityShadows.java'
source = (root / 'build/porting/reference-mun-1.21' / path).read_text(encoding='utf-8')
actual = (root / path).read_text(encoding='utf-8').replace('MunEntityShadows.this.', '')


def function(text, signature):
    start = text.index(signature)
    position = text.index('{', start) + 1
    depth = 1
    while depth:
        depth += (text[position] == '{') - (text[position] == '}')
        position += 1
    return text[start:position]


signatures = ['private void collect(', 'private boolean evictIdleBuffer(',
              'public VertexConsumer addVertex(', 'public VertexConsumer setColor(int red',
              'public VertexConsumer setUv(', 'private void flush(', 'private void reset(']
for signature in signatures:
    assert function(actual, signature) == function(source, signature), signature
for constant in ['MAX_BUFFERS = 64', 'BUFFER_RETENTION_FRAMES = 60',
                 'QUAD_INDICES = {0, 1, 2, 0, 2, 3}', 'TRIANGLE_INDICES = {0, 1, 2}']:
    assert constant in source and constant in actual, constant
log = (root / 'build/porting' / args.target_log).read_text(encoding='utf-8', errors='replace')
assert log.count('PORT_MUN_ENTITY_SHADOWS_PASSED: cow, chest, item, falling block, generator') == 2
for marker in ['PORT_MUN_SHADOW_MESH_PASSED', 'PORT_MUN_SHADOW_CASTER_PASSED: run=2',
               'PORT_MUN_SKY_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
report = {'source_capture_functions_equal': signatures, 'source_limits_equal': True,
          'native_entity_and_block_entity_probe_runs': 2,
          'target_log': args.target_log,
          'checks': ['Actual cow/chest/item/falling-block and AnvilCraft generator geometry',
                     'Animated geometry changes with reused GPU handles',
                     'Textured depth draw, vertex/entity caps and visibility/distance filtering',
                     'Idle eviction and auxiliary buffer cleanup',
                     'Post-screenshot readback state and model reload'],
          'limits': ['World cascade cache and receiver integration remain pending.',
                     'No source-versus-target scene-shadow image parity is claimed.',
                     'Current OpenGL backend only.']}
(root / 'build/porting/mun-entity-shadows-comparison.json').write_text(
    json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
