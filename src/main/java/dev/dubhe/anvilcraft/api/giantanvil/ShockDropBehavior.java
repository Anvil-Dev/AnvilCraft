package dev.dubhe.anvilcraft.api.giantanvil;

import dev.dubhe.anvilcraft.event.giantanvil.shock.ShockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** 定义撼地破坏模式生成单个掉落物的方式。 */
@FunctionalInterface
public interface ShockDropBehavior {
    Identifier DEFAULT_ID = Identifier.fromNamespaceAndPath("anvilcraft", "default_shock_drop");

    /** 不改变原版掉落物随机抛出的默认处理器。 */
    ShockDropBehavior DEFAULT = (context, pos, stack) -> Block.popResource(context.level(), pos, stack);

    /**
     * 返回行为标识。四个边框位置只有标识一致时才会组成同一种撼地配方；自定义处理器应覆盖此方法。
     */
    default Identifier id() {
        return ShockDropBehavior.DEFAULT_ID;
    }

    void drop(ShockContext context, BlockPos pos, ItemStack stack);
}
