"""Compare cache algorithms and native cascade generation/invalidation probes."""
from pathlib import Path
import argparse
import json

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--target-log', default='client-mun-map-final-2.log')
args = parser.parse_args()
path = 'src/main/java/dev/dubhe/anvilcraft/client/renderer/mun/MunShadowMap.java'
source = (root / 'build/porting/reference-mun-1.21' / path).read_text(encoding='utf-8')
actual = (root / path).read_text(encoding='utf-8')
for new, old in [('.pack()', '.toLong()'), ('.x()', '.x'), ('.z()', '.z'),
                 ('ChunkPos.unpack(key)', 'new ChunkPos(key)'),
                 ('ChunkPos.containing(BlockPos.containing(this.anchor))', 'new ChunkPos(BlockPos.containing(this.anchor))'),
                 ('ChunkPos.containing(pos)', 'new ChunkPos(pos)'),
                 ('state.getOffset(pos)', 'state.getOffset(level, pos)'),
                 ('Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state)',
                  'Minecraft.getInstance().getBlockRenderer().getBlockModel(state)'),
                 ('state.getLightDampening() >= 15', 'state.getLightBlock(level, pos) >= level.getMaxLightLevel()'),
                 ('RenderSystem.getDevice().getMaxTextureSize()', 'RenderSystem.maxSupportedTextureSize()')]:
    actual = actual.replace(new, old)


def function(text, signature):
    start = text.index(signature)
    position = text.index('{', start) + 1
    depth = 1
    while depth:
        depth += (text[position] == '{') - (text[position] == '}')
        position += 1
    return text[start:position]


signatures = ['private void moveWindow(', 'private double distance(', 'private void updateMeshes(',
              'private void checkLoadedChunks(', 'private ChunkPos nextChunk(', 'private void admit(',
              'private void refreshDynamic(', 'private void selectMeshes(', 'public void blockChanged(',
              'public void chunkChanged(', 'private boolean contains(', 'boolean step(', 'private void scan(']
for signature in signatures:
    assert function(actual, signature) == function(source, signature), signature
start, end = '        Vec3 projectedCenter = solar.project(center);', '        if (!dirty) return;'
assert actual[actual.index(start):actual.index(end)] == source[source.index(start):source.index(end)]
log = (root / 'build/porting' / args.target_log).read_text(encoding='utf-8', errors='replace')
assert log.count('PORT_MUN_SHADOW_MAP_PASSED:') == 2
for marker in ['PORT_MUN_SHADOW_CACHE_SETTLED:', 'PORT_MUN_ENTITY_SHADOWS_PASSED',
               'PORT_MUN_SHADOW_CASTER_PASSED: run=2', 'PORT_MUN_SKY_PASSED',
               'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
report = {'source_cache_functions_equal_after_api_mapping': signatures,
          'source_cascade_invalidation_equal': True, 'client_probe_runs': 2,
          'target_log': args.target_log, 'probe_resolution': 128, 'probe_render_distance': 2,
          'limits': ['Standard world receiver integration remains pending.',
                     'Default-resolution scene parity and normal-view-distance build performance remain unverified.',
                     'Current OpenGL backend only.']}
(root / 'build/porting/mun-shadow-map-comparison.json').write_text(
    json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
