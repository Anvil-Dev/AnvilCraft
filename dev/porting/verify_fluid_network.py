"""Verify source network algorithms and real native transfer/display evidence."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'
path = 'src/main/java/dev/dubhe/anvilcraft/api/fluid/network/FluidPipeNetwork.java'
source = subprocess.check_output(['git', 'show', f'{ref}:{path}'], cwd=root).decode('utf-8')
target = (root / path).read_text(encoding='utf-8')


def method(text, name):
    match = re.search(r'^    (?:public|private|protected)[^\n]*\b' + name + r'\(', text, re.M)
    assert match, name
    start = text.index('{', match.start())
    end, depth = start + 1, 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return re.sub(r'\s+', '', text[start:end]).replace('this.', '')


names = ['showFluidAlongPipePath', 'expireGlassDisplays', 'displayDirectionsByPipe', 'addPipePathDirections',
         'addEndpointDirection', 'directionalPipePath', 'undirectedPipePath', 'sourceEntry', 'reachableEntry',
         'canDrainFromEntry', 'equilibrateGasType', 'gasPressure', 'isInfiniteGasPressureSource']
for name in names:
    assert method(source, name) == method(target, name), name
tests = (root / 'build/porting/tests-fluid-network-final.log').read_text(encoding='utf-8', errors='replace')
assert 'All 595 required tests passed' in tests and 'All dimensions are saved' in tests
client = (root / 'build/porting/client-glass-network-final.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_GLASS_NETWORK_CLIENT_PASSED' in client and 'BUILD SUCCESSFUL' in client
assert 'total=16000' in client and 'All dimensions are saved' in client
style = (root / 'build/porting/compile-fluid-network-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in style
report = {
    'source_commit': ref, 'source_algorithm_bodies_equal': names, 'required_tests_passed': 595,
    'checks': ['Real scanned glass network moves water and synchronizes display directions',
               'Native client observes automatic display expiry and 16000 mB conservation',
               'Alternate container entry remains usable when the primary entry is blocked',
               'Finite gas equalization and persistent half-filled gas display',
               'Infinite-pressure finite supply fills a higher-pressure destination without creating gas',
               'Native transfer failure rolls back both handlers',
               'Creative sinks do not change on simulation or transport insertion',
               'Player bucket configuration, creative clearing and powered CFA interface pressure gate'],
    'limits': ['Gas algorithms use registered developer gas and controlled handlers in GameTests.',
               'The real client scene validates liquid transport; a source/native gas visual comparison remains pending.'],
}
(root / 'build/porting/fluid-network-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
