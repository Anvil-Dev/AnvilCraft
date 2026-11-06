package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** 匹配仓储端口中的流体和装有流体的物品。 */
public record FluidCategory() implements ICategory {
    public static final FluidCategory INSTANCE = new FluidCategory();

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    public Component name() {
        return Component.translatable("category.anvilcraft.fluid");
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        IFluidHandlerItem handler = stack.getStack().copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return false;
        }
        for (int index = 0; index < handler.getTanks(); index++) {
            if (!handler.getFluidInTank(index).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean testFluid(FluidStack fluid) {
        return !fluid.isEmpty();
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.FLUID.get();
    }

    public static class Type implements ICategory.Type<FluidCategory> {
        public static final MapCodec<FluidCategory> CODEC = MapCodec.unit(FluidCategory.INSTANCE);
        public static final StreamCodec<ByteBuf, FluidCategory> STREAM_CODEC = StreamCodec.unit(FluidCategory.INSTANCE);

        @Override
        public MapCodec<FluidCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidCategory> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
