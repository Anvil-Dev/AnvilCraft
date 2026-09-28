"""Validate the source four-sided progress contract and completion clearing in live clients."""
from pathlib import Path
import json
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[2]
ref = root/'build/porting/reference-mun-1.21'
results = {}
for scene in ['step1-face0','step1-face1','step1-face2','step1-face3','step2','complete']:
    a = np.asarray(Image.open(root/('run/port-validation/client/screenshots/multiblock-4d-26.1-'+scene+'.png')).convert('RGB')).astype(int)
    b = np.asarray(Image.open(ref/('run/mun-reference/screenshots/multiblock-4d-1.21-'+scene+'.png')).convert('RGB')).astype(int)
    a,b = a[330:390,460:820],b[330:390,460:820]
    mask = lambda image: (image[:,:,0] > 150) & (image[:,:,2] > 120) & (image[:,:,0]-image[:,:,1] > 35) & (image[:,:,2]-image[:,:,1] > 35)
    ma,mb = mask(a),mask(b)
    if scene == 'complete':
        assert ma.sum() < 5 and mb.sum() < 5, 'Completed progress remains visible'
        results[scene] = {'native_text_pixels':int(ma.sum()),'source_text_pixels':int(mb.sum())}
    else:
        assert ma.sum() > 100 and mb.sum() > 100, scene
        overlap = float((ma & mb).sum()/(ma | mb).sum())
        assert overlap > 0.8, (scene,overlap)
        results[scene] = {'text_mask_iou':overlap,'mean_rgb_error_on_source_text':float(abs(a-b)[mb].mean())}

for log in [root/'build/porting/client-multiblock-4d-final.log',root/'build/porting/client-multiblock-4d-reference.log']:
    text = log.read_text(encoding='utf-8',errors='replace')
    assert 'PORT_4D_CLIENT_PASSED' in text and 'BUILD SUCCESSFUL' in text
report = {'visuals':results,'contracts':['Four face orientations','Step 1 and step 2','Completion removes all progress text'],
    'limits':['World text uses native 26.1 font/lightmap rendering; geometry and glyph masks are compared separately from brightness.']}
(root/'build/porting/multiblock-4d-comparison.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
