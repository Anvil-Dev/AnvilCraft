"""Compare Hypercube assets, source crafting contracts and live translucency scenarios."""
from pathlib import Path
import json
import hashlib
import numpy as np
from PIL import Image

root=Path(__file__).resolve().parents[2]
ref=root/'build/porting/reference-mun-1.21'
assets=[]
for p in (ref/'src/main/resources/assets/anvilcraft').rglob('*'):
    if p.is_file() and 'hypercube' in p.name:
        relative=p.relative_to(ref)
        assert hashlib.sha256(p.read_bytes()).digest()==hashlib.sha256((root/relative).read_bytes()).digest(),relative
        assets.append(str(relative))
recipes=[]
for name in ['4d_multiblock/hypercube','hyperdimension_terminal']:
    p=Path('src/generated/resources/data/anvilcraft/recipe')/(name+'.json')
    a,b=json.loads((root/p).read_text()),json.loads((ref/p).read_text())
    for data in [a,b]:
        if data['result'].get('count')==1:data['result'].pop('count')
    if name=='hyperdimension_terminal':
        assert a['type']=='anvilcraft:terminal_upgrade' and b['type']=='minecraft:crafting_shaped'
        b['type']=a['type']
        b['key']={key:value['item'] for key,value in b['key'].items()}
    assert a==b,name
    recipes.append(name)

visuals={}
for scene in ['single','overlap','occluded','item']:
    a=np.asarray(Image.open(root/('run/port-validation/client/screenshots/hypercube-26.1-'+scene+'.png')).convert('RGB')).astype(float)
    b=np.asarray(Image.open(ref/('run/mun-reference/screenshots/hypercube-1.21-'+scene+'.png')).convert('RGB')).astype(float)
    if scene=='item':
        a,b=a[296:424,576:704],b[296:424,576:704]
        ma,mb=(a!=48).any(2),(b!=48).any(2)
        visuals[scene]={'mask_iou':float((ma&mb).sum()/(ma|mb).sum()),'mean_rgb_error':float(abs(a-b)[mb].mean())}
        assert visuals[scene]['mask_iou']>0.99
    else:
        a,b=a[270:455,545:750],b[270:455,545:750]
        pink = lambda im: (im[:,:,0]>85) & (im[:,:,0]>im[:,:,1]+6) & (im[:,:,2]>im[:,:,1]+6)
        native_pixels,source_pixels=int(pink(a).sum()),int(pink(b).sum())
        if scene in ['single','overlap']:assert native_pixels>300 and source_pixels>300,scene
        visuals[scene]={'mean_rgb_error':float(abs(a-b).mean()),'native_pink_pixels':native_pixels,'source_pink_pixels':source_pixels}
for path in ['client-hypercube-1.log','client-hypercube-reference-final.log']:
    t=(root/'build/porting'/path).read_text(encoding='utf-8',errors='replace')
    assert 'PORT_HYPERCUBE_CLIENT_PASSED' in t and 'BUILD SUCCESSFUL' in t
assert visuals['occluded']['native_pink_pixels'] < visuals['overlap']['native_pink_pixels']
assert visuals['occluded']['source_pink_pixels'] < visuals['overlap']['source_pink_pixels']
report={'source_equal_assets':assets,'equivalent_recipe_ingredients':recipes,'visuals':visuals,
    'limits':['Native terminal crafting uses the existing content-preserving recipe adapter.',
        'World crops include native 26.1 atmosphere and terrain; transparency scenarios are also visually inspected.']}
(root/'build/porting/hypercube-comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
