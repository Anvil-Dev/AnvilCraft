package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/// 蜂蜜瓶流体处理器
///
/// 使蜂蜜瓶可以和储罐进行流体交互（参考水瓶行为，每瓶 250mB）。
/// 空玻璃瓶填充蜂蜜后变为蜂蜜瓶，蜂蜜瓶排空后变回空玻璃瓶。
public class HoneyBottleResourceHandler extends ItemAccessResourceHandler<FluidResource> {
    /// 单个蜂蜜瓶对应的流体量（mB）
    public static final int HONEY_PER_BOTTLE = 250;

    public HoneyBottleResourceHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        if (accessResource.is(Items.HONEY_BOTTLE)) {
            return FluidResource.of(ModFluids.HONEY.get());
        }
        return FluidResource.EMPTY;
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return accessResource.is(Items.HONEY_BOTTLE) ? HONEY_PER_BOTTLE : 0;
    }

    @Override
    protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        if (newAmount == 0) {
            // 排空：蜂蜜瓶 -> 空玻璃瓶
            return ItemResource.of(Items.GLASS_BOTTLE);
        } else if (newAmount == HONEY_PER_BOTTLE && newResource.getFluid() instanceof HoneyFluid) {
            // 填满：空玻璃瓶 -> 蜂蜜瓶
            return ItemResource.of(Items.HONEY_BOTTLE);
        }
        // 其他量无法表示
        return ItemResource.EMPTY;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return resource.getFluid() instanceof HoneyFluid;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        return HONEY_PER_BOTTLE;
    }
}
