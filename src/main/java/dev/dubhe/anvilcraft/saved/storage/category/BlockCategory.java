package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

public record BlockCategory() implements ICategory {
    public static final BlockCategory INSTANCE = new BlockCategory();

    @Override
    public ItemStackTemplate icon() {
        return new ItemStackTemplate(Items.BRICKS);
    }

    @Override
    public Component name() {
        return Component.translatable("category.anvilcraft.block");
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.BLOCK.get();
    }

    public static class Type implements ICategory.Type<BlockCategory> {
        public static final MapCodec<BlockCategory> CODEC = MapCodec.unit(BlockCategory.INSTANCE);
        public static final StreamCodec<ByteBuf, BlockCategory> STREAM_CODEC = StreamCodec.unit(BlockCategory.INSTANCE);

        @Override
        public MapCodec<BlockCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockCategory> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
