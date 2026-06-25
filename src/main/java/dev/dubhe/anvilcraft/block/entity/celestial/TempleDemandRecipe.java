package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/// 定义神殿巨构的神殿需求条目的配方。
///
/// 每个配方有一个类别（{@code blessing} 或 {@code punishment}）和一个
/// 加权条目列表。神殿每天随机选取一个条目。
///
/// 对于天体特定的需求，请改用 {@link SpecialCelestialBodyRecipe} 中的
/// {@code temple_blessings} 和 {@code temple_punishments} 字段。
public record TempleDemandRecipe(
    Category category,
    List<Entry> entries
) implements Recipe<TempleDemandInput> {

    public enum Category {
        BLESSING("blessing"),
        PUNISHMENT("punishment");

        public static final Codec<Category> CODEC = Codec.STRING.xmap(
            Category::fromName,
            Category::getSerializedName
        );

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return name;
        }

        public static Category fromName(String name) {
            for (Category value : values()) {
                if (value.name.equals(name)) return value;
            }
            throw new IllegalArgumentException("Unknown temple demand category: " + name);
        }
    }

    public record Entry(String itemId, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(Entry::itemId),
            Codec.INT.fieldOf("count").forGetter(Entry::count)
        ).apply(ins, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::itemId,
                ByteBufCodecs.INT, Entry::count,
                Entry::new
            );

        public ResourceLocation itemResource() {
            return ResourceLocation.parse(itemId);
        }
    }

    public static final MapCodec<TempleDemandRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Category.CODEC.fieldOf("category").forGetter(TempleDemandRecipe::category),
        Entry.CODEC.listOf().fieldOf("entries").forGetter(TempleDemandRecipe::entries)
    ).apply(ins, TempleDemandRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TempleDemandRecipe> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(
                Category::fromName,
                Category::getSerializedName
            ), TempleDemandRecipe::category,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), TempleDemandRecipe::entries,
            TempleDemandRecipe::new
        );

    @Override
    public boolean matches(TempleDemandInput input, @NotNull Level level) {
        return this.category == input.category();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull TempleDemandInput input, HolderLookup.@NotNull Provider registries) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TEMPLE_DEMAND_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipeTypes.TEMPLE_DEMAND_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<TempleDemandRecipe> {
        @Override
        public @NotNull MapCodec<TempleDemandRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, TempleDemandRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
