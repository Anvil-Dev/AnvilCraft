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

public record UnstackableCategory() implements ICategory {
    public static final UnstackableCategory INSTANCE = new UnstackableCategory();

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.DIAMOND_SWORD);
    }

    @Override
    public Component name() {
        return Component.translatable("category.anvilcraft.unstackable");
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return !stack.isStackable();
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.UNSTACKABLE.get();
    }

    public static class Type implements ICategory.Type<UnstackableCategory> {
        public static final MapCodec<UnstackableCategory> CODEC = MapCodec.unit(UnstackableCategory.INSTANCE);
        public static final StreamCodec<ByteBuf, UnstackableCategory> STREAM_CODEC = StreamCodec.unit(UnstackableCategory.INSTANCE);

        @Override
        public MapCodec<UnstackableCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UnstackableCategory> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}