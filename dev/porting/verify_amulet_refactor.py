"""Check the source effect model, native adaptations and completed runtime coverage."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
ref = '07eb60b47f14439375da10e78f4d4770f3a83256'
prefix = 'src/main/java/dev/dubhe/anvilcraft/'


def canonical(text):
    text = text.replace('\r\n', '\n')
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    text = re.sub(r'//[^\n]*', '', text)
    text = re.sub(r'^(?:package|import) [^\n]*\n', '', text, flags=re.M)
    text = re.sub(r'@(Nullable|Unmodifiable|NotNull|NullMarked)\b', '', text)
    text = text.replace('ResourceLocation', 'Identifier')
    text = text.replace('ModRegistries.AMULET.get(', 'ModRegistries.AMULET.getValue(')
    text = text.replace('.getGameProfile().getId()', '.getGameProfile().id()')
    text = text.replace('.builtInRegistryHolder()', '')
    text = text.replace('EntityTypePredicate.of(BuiltInRegistries.ENTITY_TYPE, ', 'EntityTypePredicate.of(')
    text = text.replace('MobEffects.DIG_SPEED', 'MobEffects.HASTE')
    text = text.replace('MobEffects.DAMAGE_BOOST', 'MobEffects.STRENGTH')
    text = text.replace('MobEffects.DAMAGE_RESISTANCE', 'MobEffects.RESISTANCE')
    text = text.replace('InteractionResult.sidedSuccess(event.getLevel().isClientSide())',
                        'event.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME')
    text = re.sub(r'HolderSet.Named<EntityType<\?>> valid = BuiltInRegistries.ENTITY_TYPE.getOrCreateTag\(ModEntityTypeTags.ANVIL_AMULET_VALID\);',
                  '', text)
    text = text.replace('direct.getType().is(valid)', 'direct.is(ModEntityTypeTags.ANVIL_AMULET_VALID)')
    return re.sub(r'\s+', '', text)


paths = [root / prefix / 'api/amulet/Amulet.java', root / prefix / 'api/amulet/AmuletManager.java',
         root / prefix / 'api/event/AmuletEvent.java', root / prefix / 'init/item/ModAmulets.java',
         root / prefix / 'init/item/ModAmuletEffectContextKeys.java', root / prefix / 'event/AmuletAbilitiesEventListener.java',
         root / prefix / 'event/AmuletEventListener.java']
paths += list((root / prefix / 'api/amulet/ctx').glob('*.java'))
paths += list((root / prefix / 'api/amulet/effect').glob('*.java'))
paths = [path for path in paths if path.name != 'package-info.java']
equal = []
for path in paths:
    name = path.relative_to(root).as_posix()
    original = subprocess.check_output(['git', 'show', f'{ref}:{name}'], cwd=root).decode('utf-8')
    assert canonical(original) == canonical(path.read_text(encoding='utf-8')), name
    equal.append(name)
assert not (root / prefix / 'item/property/component/amulet/IAmulet.java').exists()
assert not (root / prefix / 'item/AmuletAbilities.java').exists()
assert not (root / 'src/main/resources/assets/anvilcraft/textures/item/cogwheel_amulet.png').exists()
tag_path = 'src/generated/resources/data/anvilcraft/tags/item/amulet.json'
assert json.loads((root / tag_path).read_bytes()) == json.loads(subprocess.check_output(['git', 'show', f'{ref}:{tag_path}'], cwd=root))
tests = (root / 'build/porting/tests-amulet-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in tests and 'All 604 required tests passed' in tests
assert 'PORT_AMULET_CURIOS_PASSED' in tests
client = (root / 'build/porting/client-amulet-1.log').read_text(encoding='utf-8', errors='replace')
assert 'PORT_AMULET_CLIENT_PASSED' in client and 'All dimensions are saved' in client
for stage in range(5):
    assert f'PORT_AMULET_CLIENT_STAGE: {stage}' in client
compile_log = (root / 'build/porting/compile-amulet-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in compile_log
report = {'source_commit': ref, 'source_classes_equivalent_after_api_adaptation': equal,
          'required_tests_passed': 604, 'amulet_focused_tests': 18,
          'client': ['live player lifecycle and effect synchronization', 'real crouch input cancels slow falling',
                     'server knockback modifier removal and levitation rejection', 'client gravity query', 'server reload invalidation'],
          'native_adaptations': ['Holder-based entity predicates and current registry lookup APIs',
             'Food effect query at ApplyStatusEffectsConsumeEffect with the FOOD-component guard',
             'Server-thread reload listener and native AI hook signatures',
             'Level-aware dummy caches retain native unload and damage protection',
             'Dedicated-server integration candidates are filtered before the existing AnvilLib loader loop'],
          'limits': ['Client scenario completed cleanly; a subsequent fixture indentation issue was fixed in final compile/style checks.',
                     'Native AnvilLib rendering dependencies are retained because 26.1 renderers use them.',
                     'Enchanted Gold itself is absent in the native branch; its related source change is tracked with that pending feature.']}
(root / 'build/porting/amulet-refactor-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
