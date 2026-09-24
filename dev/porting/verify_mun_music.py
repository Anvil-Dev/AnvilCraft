"""Check original audio bytes, streamed definitions and the real MusicManager probe."""
from pathlib import Path
import argparse
import hashlib
import json
import re

root = Path(__file__).resolve().parents[2]
source = root / 'build/porting/reference-mun-1.21'
parser = argparse.ArgumentParser()
parser.add_argument('--client-log', default='client-mun-music-2.log')
args = parser.parse_args()
path = 'src/main/java/dev/dubhe/anvilcraft/client/event/MunMusicHandler.java'
actual = (root / path).read_text(encoding='utf-8').replace('.getIdentifier()', '.getLocation()').replace('.location()', '.getLocation()')
assert actual == (source / path).read_text(encoding='utf-8'), 'Music selection logic differs from source'
definitions = json.loads((root / 'src/generated/resources/assets/anvilcraft/sounds.json').read_text(encoding='utf-8'))
english = json.loads((root / 'src/generated/resources/assets/anvilcraft/lang/en_us.json').read_text(encoding='utf-8'))
chinese = json.loads((root / 'src/main/resources/assets/anvilcraft/lang/zh_cn.json').read_text(encoding='utf-8'))
report = {'source_handler_equal_after_api_mapping': True, 'tracks': {}, 'client_log': args.client_log}
for track, title in [('above_the_moon_dust', 'Above the Moon Dust'), ('far_side_glow', 'Far Side Glow')]:
    path = Path(f'src/main/resources/assets/anvilcraft/sounds/music/mun/{track}_loop.ogg')
    data = (root / path).read_bytes()
    assert data == (source / path).read_bytes(), track
    sounds = definitions[f'music.mun.{track}']['sounds']
    assert len(sounds) == 1 and sounds[0]['name'] == f'anvilcraft:music/mun/{track}_loop' and sounds[0]['stream']
    key = f'anvilcraft.music.mun.{track}_loop'
    assert english[key] == chinese[key] == title
    report['tracks'][track] = {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest(), 'stream': True}
log = (root / 'build/porting' / args.client_log).read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_MUN_MUSIC_DECODED: above_the_moon_dust_loop', 'PORT_MUN_MUSIC_DECODED: far_side_glow_loop',
               'PORT_MUN_MUSIC_FIRST:', 'PORT_MUN_MUSIC_SECOND:', 'PORT_MUN_MUSIC_PASSED:',
               'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
first = re.search(r'PORT_MUN_MUSIC_FIRST: ([^,]+), silenceTicks=(\d+)', log)
second = re.search(r'PORT_MUN_MUSIC_SECOND: ([^,]+), remainingSilenceTicks=(\d+)', log)
assert first and second and first[1] != second[1]
report['first_silence_ticks'] = int(first[2])
report['observed_between_silence_ticks'] = int(second[2])
report['limits'] = ['Silence selection ticks were accelerated in the client probe; full song duration was not waited out.',
                    'Audio bytes and native streaming were verified; subjective listening was not used as evidence.']
(root / 'build/porting/mun-music-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
