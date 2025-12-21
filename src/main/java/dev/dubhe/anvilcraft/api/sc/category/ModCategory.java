package dev.dubhe.anvilcraft.api.sc.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.sc.ModCategories;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;

public record ModCategory(ItemStack icon, Component name, String modId) implements ICategory {
    public ModCategory(ItemLike icon, String modId) {
        this(
            icon.asItem().getDefaultInstance(),
            Component.translatableWithFallback(
                "category.anvilcraft.mod_name." + modId,
                ModList.get().getModContainerById(modId).orElseThrow().getModInfo().getDisplayName()
            ).append(Component.translatable("category.anvilcraft.mod_name_suffix")),
            modId
        );
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(this.modId);
    }

    @Override
    public Type getType() {
        return ModCategories.MOD.get();
    }

    public static class Type implements ICategory.Type<ModCategory> {
        public static final MapCodec<ModCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(ModCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(ModCategory::name),
            Codec.STRING
                .fieldOf("modId")
                .forGetter(ModCategory::modId)
        ).apply(ins, ModCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ModCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            ModCategory::name,
            ByteBufCodecs.STRING_UTF8,
            ModCategory::modId,
            ModCategory::new
        );

        @Override
        public MapCodec<ModCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ModCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
