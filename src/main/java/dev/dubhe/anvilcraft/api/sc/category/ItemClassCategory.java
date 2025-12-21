package dev.dubhe.anvilcraft.api.sc.category;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.sc.ModCategories;
import dev.dubhe.anvilcraft.util.ClassUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.SneakyThrows;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Objects;

public final class ItemClassCategory implements ICategory {
    private final ItemStack icon;
    private final Component name;
    private final String clazz;
    private Class<?> loadedClass;

    public ItemClassCategory(ItemStack icon, Component name, String clazz) {
        this.icon = icon;
        this.name = name;
        this.clazz = clazz;
    }

    public ItemClassCategory(ItemLike icon, String suffix, Class<? extends Item> clazz) {
        this(icon.asItem().getDefaultInstance(), ICategory.constructName(suffix), clazz.getName());
    }

    @Override
    @SneakyThrows
    public boolean test(UnlimitedItemStack stack) {
        if (this.loadedClass == null) {
            this.loadedClass = ClassUtil.getLoadedClass(this.clazz);
        }
        return this.loadedClass != null && this.loadedClass.isAssignableFrom(stack.getItem().getClass());
    }

    @Override
    public Type getType() {
        return ModCategories.ITEM_CLASS.get();
    }

    @Override
    public ItemStack icon() {
        return icon;
    }

    @Override
    public Component name() {
        return name;
    }

    public String clazz() {
        return clazz;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ItemClassCategory) obj;
        return Objects.equals(this.icon, that.icon)
               && Objects.equals(this.name, that.name)
               && Objects.equals(this.clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.icon, this.name, this.clazz);
    }

    @Override
    public String toString() {
        return "ItemClassCategory["
               + "icon=" + this.icon + ", "
               + "name=" + this.name + ", "
               + "clazz=" + this.clazz + ']';
    }

    public static class Type implements ICategory.Type<ItemClassCategory> {
        public static final MapCodec<ItemClassCategory> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemStack.CODEC
                .fieldOf("icon")
                .forGetter(ItemClassCategory::icon),
            ComponentSerialization.FLAT_CODEC
                .fieldOf("name")
                .forGetter(ItemClassCategory::name),
            Codec.STRING
                .fieldOf("class")
                .forGetter(ItemClassCategory::clazz)
        ).apply(ins, ItemClassCategory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemClassCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            ItemClassCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            ItemClassCategory::name,
            ByteBufCodecs.STRING_UTF8,
            ItemClassCategory::clazz,
            ItemClassCategory::new
        );

        @Override
        public MapCodec<ItemClassCategory> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemClassCategory> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
