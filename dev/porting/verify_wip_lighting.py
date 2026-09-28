"""Validate native WIP extraction contracts and paired daylight/side-lit screenshots."""
from pathlib import Path
import json
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = root/'build/porting/reference-mun-1.21'
log = (root/'build/porting/client-wip-lighting-final.log').read_text(encoding='utf-8',errors='replace')
for marker in ['PORT_WIP_LIGHTING_DAY_PASSED', 'PORT_WIP_LIGHTING_NIGHT_PASSED', 'PORT_MASS_ENERGY_CLIENT_PASSED', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
assert 'biome tint, translucent layer and empty fallback' in log
source_log = (root/'build/porting/client-wip-lighting-reference.log').read_text(encoding='utf-8',errors='replace')
assert 'PORT_MASS_ENERGY_CLIENT_PASSED' in source_log and 'BUILD SUCCESSFUL' in source_log

visuals = {}
for scene in ['world','night']:
    a = np.asarray(Image.open(root/('run/port-validation/client/screenshots/mass-energy-inverter-26.1-'+scene+'.png')).convert('RGB')).astype(float)
    b = np.asarray(Image.open(ref/('run/mun-reference/screenshots/mass-energy-inverter-1.21-'+scene+'.png')).convert('RGB')).astype(float)
    visuals[scene] = {name: float(abs(a[355:424,x-44:x+44]-b[355:424,x-44:x+44]).mean())
        for name,x in [('wip_1',375),('wip_2',535),('wip_3',695),('finished',855)]}
for checkout in [root,ref]:
    path = Path('assets/anvilcraft/textures/block/wip_block.png.mcmeta')
    assert (checkout/'build/resources/main'/path).read_bytes() == (checkout/'src/main/resources'/path).read_bytes()

report = {'contracts': ['Four distinct direction colors', 'Four distinct side-light coordinates',
    'Quad snapshots remain stable after later model extraction', 'Grass fallback retains biome tint',
    'Glass fallback retains translucent layer', 'Air fallback clears old geometry'],
    'visuals_mean_rgb_error': visuals,
    'limits': ['World patches include native 26.1 lightmap, atmospheric fog and terrain sampling; they are not pixel-identical to 1.21.',
        'Only extraction reads world state; submitted geometry carries independent vertex light and color values.']}
(root/'build/porting/wip-lighting-comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
