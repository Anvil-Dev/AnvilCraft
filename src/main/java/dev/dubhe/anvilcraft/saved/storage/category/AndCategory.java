package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Objects;

public record AndCategory(ItemStack icon, Component name, List<ICategory> categories) implements ICategory {
    public AndCategory(ItemLike icon, ResourceLocation suffix, ICategory... categories) {
        this(new ItemStack(icon.asItem()), ICategory.constructName(suffix), List.of(categories));
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        for (ICategory category : this.categories) {
            if (!category.test(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.AND.get();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AndCategory(ItemStack icon1, Component name1, List<ICategory> categories1))) return false;
        return ItemStack.isSameItemSameComponents(this.icon(), icon1)
               && Objects.equals(this.name(), name1)
               && Objects.equals(this.categories(), categories1);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.icon()) * 31 + Objects.hash(this.name(), this.categories());
    }

    public static class Type implements ICategory.Type<AndCategory> {
        public static final MapCodec<AndCategory> CODEC = CodecUtil.mapCodec(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(AndCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(AndCategory::name),
            ICategory.CODEC
                .listOf()
                .fieldOf("categories")
                .forGetter(AndCategory::categories),
            AndCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, AndCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            AndCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            AndCategory::name,
            ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
            AndCategory::categories,
            AndCategory::new
        );

        @Override
        public MapCodec<AndCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AndCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
