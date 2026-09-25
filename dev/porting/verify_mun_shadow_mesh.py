"""Check source geometry algorithms and the native client GPU mesh probe."""
from pathlib import Path
import argparse
import json

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--target-log', default='client-mun-shadow-mesh-final.log')
args = parser.parse_args()
path = Path('src/main/java/dev/dubhe/anvilcraft/client/renderer/mun/MunShadowMesh.java')
actual = (root / path).read_text(encoding='utf-8')
source = (root / 'build/porting/reference-mun-1.21' / path).read_text(encoding='utf-8')


def function(text, signature):
    start = text.index(signature)
    position = text.index('{', start) + 1
    depth = 1
    while depth:
        depth += (text[position] == '{') - (text[position] == '}')
        position += 1
    return text[start:position]


signatures = [
    'static float[] triangulate(', 'void part(', 'boolean columns(',
    'private static boolean occupied(', 'private void faces(', 'void box(',
]
for signature in signatures:
    assert function(actual, signature) == function(source, signature), signature
log = (root / 'build/porting' / args.target_log).read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_MUN_SHADOW_MESH_PASSED', 'PORT_MUN_SHADOW_RESOURCES_PASSED',
               'PORT_MUN_SKY_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
report = {
    'source_geometry_functions_equal': signatures,
    'native_gpu_mesh_probe': 'passed',
    'client_save_exit': 'passed',
    'target_log': args.target_log,
    'limits': ['World shadow caster and receiver integration remains pending.',
               'GPU validation covers the current OpenGL backend.'],
}
(root / 'build/porting/mun-shadow-mesh-comparison.json').write_text(
    json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
