package dev.dubhe.anvilcraft.api.sc.category.hidden;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.sc.ModCategories;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;

public record UpgradeCategory() implements IHiddenCategory {
    @Override
    public Type getType() {
        return ModCategories.UPGRADE.get();
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return stack.isAny(
            Items.SHULKER_BOX,
            ModBlocks.NESTING_SHULKER_BOX,
            ModBlocks.OVER_NESTING_SHULKER_BOX,
            ModBlocks.SUPERCRITICAL_NESTING_SHULKER_BOX,
            ModBlocks.SPACE_OVERCOMPRESSOR,
            ModBlocks.CONFINED_SPACE_ANVILON,
            ModBlocks.CONFINED_TIME_ANVILON,
            ModBlocks.SINGULARITY_CRYSTAL,
            Items.ENDER_CHEST,
            ModBlocks.CHUTE
        ) || stack.is(ModItemTags.ANVIL_HAMMER);
    }

    public static class Type implements ICategory.Type<UpgradeCategory> {
        public static final MapCodec<UpgradeCategory> CODEC = MapCodec.unit(UpgradeCategory::new);
        public static final StreamCodec<ByteBuf, UpgradeCategory> STREAM_CODEC = StreamCodec.unit(new UpgradeCategory());

        @Override
        public MapCodec<UpgradeCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UpgradeCategory> streamCodec() {
            return STREAM_CODEC.cast();
        }
    }
}
