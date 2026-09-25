"""Verify pearl travel source rules and an actual acknowledged client round trip."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[2]
path = 'src/main/java/dev/dubhe/anvilcraft/event/MunTravelEventListener.java'
source = (root / 'build/porting/reference-mun-1.21' / path).read_text(encoding='utf-8')
actual = (root / path).read_text(encoding='utf-8')
assert 'player.isChangingDimension() || !player.connection.hasClientLoaded()' in actual
for new, old in [
    ('net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl',
     'net.minecraft.world.entity.projectile.ThrownEnderpearl'),
    ('TeleportTransition', 'DimensionTransition'), ('.getOverworldClockTime()', '.getDayTime()'),
    ('destination.getWorldBorderAdjustedRespawnData(destination.getRespawnData()).pos()', 'destination.getSharedSpawnPos()'),
    ('data.getBooleanOr(RETURN_LAUNCH_KEY, false)', 'data.getBoolean(RETURN_LAUNCH_KEY)'),
    ('player.teleport(', 'player.changeDimension('),
    ('player.hurtServer(player.level(), player.damageSources().fall(), damage.amount());',
     'player.hurt(player.damageSources().fall(), damage.amount());'),
    ('player.isChangingDimension() || !player.connection.hasClientLoaded()', 'player.isChangingDimension()'),
]:
    actual = actual.replace(new, old)
assert actual == source, 'Travel rules changed beyond native API/loading adaptation'
log = (root / 'build/porting/client-mun-pearl-final.log').read_text(encoding='utf-8', errors='replace')
for marker in ['PORT_MUN_PEARL_DAMAGE: count=1, amount=1.0', 'PORT_MUN_PEARL_DAMAGE: count=2, amount=3.0',
               'PORT_MUN_PEARL_PASSED', 'All dimensions are saved', 'BUILD SUCCESSFUL']:
    assert marker in log, marker
report = {'source_rules_equal_after_api_mapping': True, 'client_round_trip': 'passed',
          'arrival_damage_after_teleport_and_load_ack': [1, 3],
          'checks': ['Height, upward direction, total speed and inclusive midnight boundaries',
                     'Negative-time midnight window', 'Cancellable pearl landing event',
                     'Launch-time return radius and immutable eligibility',
                     'Modified landing coordinates and event damage',
                     'Return near world origin after moving world spawn',
                     'Pearl disposal, velocity/fall reset and facing preservation'],
          'limits': ['Long-term pearl persistence and multiplayer disconnect races were not separately exercised.',
                     'CFA recipe entry and Monolith integration remain separate work.']}
(root / 'build/porting/mun-pearl-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
