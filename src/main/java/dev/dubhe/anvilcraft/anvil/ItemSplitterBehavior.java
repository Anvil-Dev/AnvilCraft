package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.ItemSplitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铁砧砸到物品分配器时，把内部物品均分到正前方没有遮挡的空间中。
 *
 * <p>均分份数即铁砧下落高度。下落距离是逐刻累加的浮点数，需向上取整才能得到
 * 实际掉落的格数（与巨型铁砧震波半径等处一致）。</p>
 */
public class ItemSplitterBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!(level.getBlockEntity(hitBlockPos) instanceof ItemSplitterBlockEntity splitter)) return false;
        return splitter.splitToSpace(Math.max(1, (int) Math.ceil(fallDistance)));
    }
}
