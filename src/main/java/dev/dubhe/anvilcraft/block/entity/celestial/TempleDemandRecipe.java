package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Recipe defining temple demand entries for the Temple megastructure.
 */
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

        public Identifier itemResource() {
            return Identifier.parse(itemId);
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

    public static final RecipeSerializer<TempleDemandRecipe> SERIALIZER = new RecipeSerializer<>(
        CODEC, STREAM_CODEC
    );

    @Override
    public boolean matches(TempleDemandInput input, @NotNull Level level) {
        return this.category == input.category();
    }

    @Deprecated
    @Override
    public @NotNull ItemStack assemble(@NotNull TempleDemandInput input) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public @NotNull RecipeType<TempleDemandRecipe> getType() {
        return ModRecipeTypes.TEMPLE_DEMAND.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public @NotNull RecipeSerializer<TempleDemandRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "temple_demand";
    }
}
