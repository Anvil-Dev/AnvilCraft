package dev.dubhe.anvilcraft.api.fluid;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;

/// 判断手持物品是否属于「流体交互物品」的工具
public final class FluidInteractionItems {
    private FluidInteractionItems() {
    }

    /// 物品是否会与流体容器交互（桶、瓶等携带流体能力的容器，以及空玻璃瓶）
    ///
    /// <p>用于让流体方块在这类物品上吞掉交互，避免落到默认方块行为里误放置方块。</p>
    public static boolean isFluidInteractionItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE)) return true;
        ItemStack single = stack.copyWithCount(1);
        return single.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(single)) != null;
    }
}
