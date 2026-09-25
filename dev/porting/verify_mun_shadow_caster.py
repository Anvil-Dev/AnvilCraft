"""Check original caster shader bodies and actual depth/transmission rendering."""
from pathlib import Path
import argparse
import json

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--target-log', default='client-mun-caster-final-2.log')
args = parser.parse_args()
source = root / 'build/porting/reference-mun-1.21/src/main/resources/assets/anvilcraft/shaders'
target = root / 'src/main/resources/assets/anvilcraft/shaders'
names = ['mun_shadow.vsh', 'mun_shadow.fsh', 'mun_translucent_shadow.fsh']
for name in names:
    original = (source / 'core/mun' / name).read_text(encoding='utf-8')
    actual = (target / 'core/mun' / name).read_text(encoding='utf-8')
    actual = actual.replace('#moj_import <anvilcraft:mun/mun_shadow_uniforms.glsl>\n', '')
    assert actual == original, name
solar = (source / 'include/mun/mun_solar.glsl').read_text(encoding='utf-8')
declarations = '\n'.join(line for line in solar.splitlines() if line.startswith('uniform ')) + '\n'
assert (target / 'include/mun/mun_shadow_uniforms.glsl').read_text(encoding='utf-8') == declarations
log = (root / 'build/porting' / args.target_log).read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_MUN_SHADOW_CASTER_PASSED: run=1, thirteen',
               'PORT_MUN_SHADOW_CASTER_PASSED: run=2, thirteen',
               'PORT_MUN_SHADOW_MESH_PASSED', 'PORT_MUN_SHADOW_RESOURCES_PASSED',
               'PORT_MUN_SKY_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
report = {'source_caster_shaders_equal': names, 'source_solar_uniforms_equal': True,
          'gpu_cases_before_reload': 13, 'gpu_cases_after_reload': 13,
          'target_log': args.target_log,
          'limits': ['Scene caster cache, dynamic capture and receiver bindings remain pending.',
                     'Validated on the current OpenGL backend.']}
(root / 'build/porting/mun-shadow-caster-comparison.json').write_text(
    json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
