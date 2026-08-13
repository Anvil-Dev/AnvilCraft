package dev.dubhe.anvilcraft.api.plasma;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

/**
 * 等离子喷流燃料来源。{@link TriState#DEFAULT} 表示不处理，交给后续处理器或原版逻辑。
 * {@link TriState#FALSE} 否决原版判定。查询与消耗均在服务器线程调用；
 * 模拟性查询不得改变世界状态，{@code tryConsume*} 仅在实际消耗时改变状态。
 */
public interface PlasmaJetFuelHandler {
    default TriState isIgnitedFuel(Level level, BlockPos cauldronPos) {
        return TriState.DEFAULT;
    }

    default TriState isValidBase(Level level, BlockPos cauldronPos) {
        return TriState.DEFAULT;
    }

    default TriState tryConsumeOnce(Level level, BlockPos cauldronPos) {
        return TriState.DEFAULT;
    }

    default TriState usesContinuousFuel(Level level, BlockPos cauldronPos) {
        return TriState.DEFAULT;
    }

    default TriState tryConsumeContinuousFuel(Level level, BlockPos cauldronPos, int amount) {
        return TriState.DEFAULT;
    }

    /** 非空时覆盖原版炼药锅液量检查；小于 250 会阻止喷流生成。 */
    default @Nullable Integer fuelAmount(Level level, BlockPos cauldronPos) {
        return null;
    }
}
