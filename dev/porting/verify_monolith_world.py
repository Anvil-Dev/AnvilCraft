"""Check source generation rules, unchanged templates and native world interaction."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
reference = root / 'build/porting/reference-mun-1.21'
path = Path('src/main/java/dev/dubhe/anvilcraft/worldgen/TheMonolith.java')
source = (reference / path).read_text(encoding='utf-8')
actual = (root / path).read_text(encoding='utf-8')
for new, old in [('level.getWorldBorderAdjustedRespawnData(level.getRespawnData()).pos()', 'level.getSharedSpawnPos()'),
                 ('(level.getMaxY() + 1)', 'level.getMaxBuildHeight()'), ('level.getMinY()', 'level.getMinBuildHeight()'),
                 ('BlockStateProperties.SNOWY', 'SnowyDirtBlock.SNOWY'), ('.identifier()', '.location()')]:
    actual = actual.replace(new, old)


def function(text, signature):
    start = text.index(signature)
    position = text.index('{', start) + 1
    depth = 1
    while depth:
        depth += (text[position] == '{') - (text[position] == '}')
        position += 1
    return text[start:position]


signatures = ['public static void ensureGenerated(', 'private static @Nullable BoundingBox place(',
              'static @Nullable BlockPos findSmallMonolithGround(', 'static @Nullable int[] sampleSmallMonolithGround(',
              'private static boolean isNaturalGround(', 'static boolean prepareSmallMonolithGround(',
              'private static void placeDevelopmentSign(', 'public static Placement placement(']
for signature in signatures:
    assert function(source, signature) == function(actual, signature), signature
assets = ['structure/small_monolith.nbt', 'structure/the_monolith.nbt', 'worldgen/structure/the_monolith.json']
for asset in assets:
    path = Path('src/main/resources/data/anvilcraft') / asset
    assert (root / path).read_bytes() == (reference / path).read_bytes(), asset
client = (root / 'build/porting/client-monolith-world-1.log').read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_MONOLITH_WORLD_GENERATED', 'PORT_MONOLITH_WORLD_PASSED',
               'Offer a giant anvil to the monolith to gain knowledge', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in client, marker
normal = (root / 'build/porting/client-monolith-normal-world-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_MONOLITH_SMALL_WORLD_GENERATED' in normal and 'PORT_MONOLITH_WORLD_PASSED' in normal and 'BUILD SUCCESSFUL' in normal
tests = (root / 'build/porting/tests-monolith-generation-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_MONOLITH_GENERATION_PASSED' in tests and 'BUILD SUCCESSFUL' in tests
report = {'source_generation_functions_equal_after_api_mapping': signatures, 'source_templates_equal': assets,
          'native_new_world_generation_and_respawn_return': 'passed', 'legacy_state_and_terrain_checks': 'passed',
          'limits': ['Normal-world generation was observed for one fixed seed; terrain rejection rules were also checked on controlled fixtures.']}
(root / 'build/porting/monolith-world-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
