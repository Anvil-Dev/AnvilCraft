"""Compare cement behavior with the source branch and require native test evidence."""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[2]
path = 'src/main/java/dev/dubhe/anvilcraft/fluid/CementFluid.java'
source_ref = '1260db54f6e2b9f88d0a79b8108459d346344e44'
source = subprocess.check_output(['git', 'show', f'{source_ref}:{path}'], cwd=root).decode('utf-8')
target = (root / path).read_text(encoding='utf-8')
source = source.replace('ResourceLocation', 'Identifier')
source = source.replace('import net.minecraft.util.RandomSource;',
                        'import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.util.RandomSource;')
source = source.replace('spread(Level level, BlockPos pos, FluidState state)',
                        'spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState state)')
source = source.replace('super.spread(level, pos, state)', 'super.spread(level, pos, blockState, state)')
for method in ['transferSourceDown', 'canFlowDown', 'randomTick']:
    source = source.replace(f'{method}(Level level', f'{method}(ServerLevel level')
source = source.replace('BuiltInRegistries.BLOCK.get(', 'BuiltInRegistries.BLOCK.getValue(')
# Native FlowingFluid removed canSpreadTo; retain its three checks in their original order.
source = source.replace('''return this.canSpreadTo(
            level, pos, level.getBlockState(pos), Direction.DOWN, below, belowState,
            level.getFluidState(below), newLiquid.getType()
        );''', '''return level.getFluidState(below).canBeReplacedWith(level, below, newLiquid.getType(), Direction.DOWN)
            && canPassThroughWall(Direction.DOWN, level, pos, level.getBlockState(pos), below, belowState)
            && canHoldFluid(level, below, belowState, newLiquid.getType());''')
assert source == target, 'Cement behavior differs beyond the reviewed native API mapping'
log = (root / 'build/porting/tests-cement-fluid-final.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in log and 'All dimensions are saved' in log
passed = re.search(r'All (\d+) required tests passed', log)
assert passed and int(passed[1]) >= 563
report = {
    'source_commit': source_ref,
    'full_source_equal_after_native_api_mapping': True,
    'required_tests_passed': int(passed[1]),
    'checks': ['16 colors and two solidification rings', 'Source descent and edge/vertical source relocation',
               'Sugar, slime and honey blocking rules', 'Duplicate dispatch and stale state',
               'Nearest source, no downward search and bounded frontier'],
    'limits': ['No client visual comparison was needed for this behavior-only change; existing assets are unchanged.']
}
(root / 'build/porting/cement-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
