package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.IDatagen;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MultiblockRecipe implements Recipe<MultiblockInput>, IDatagen {
    private static final MapCodec<MultiblockRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        BlockPattern.CODEC
            .fieldOf("pattern")
            .forGetter(MultiblockRecipe::getPattern),
        ItemStackTemplate.CODEC
            .fieldOf("result")
            .forGetter(MultiblockRecipe::getResult)
    ).apply(ins, MultiblockRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, MultiblockRecipe> STREAM_CODEC = StreamCodec.composite(
        BlockPattern.STREAM_CODEC,
        MultiblockRecipe::getPattern,
        ItemStackTemplate.STREAM_CODEC,
        MultiblockRecipe::getResult,
        MultiblockRecipe::new
    );
    public static final RecipeSerializer<MultiblockRecipe> SERIALIZER = new RecipeSerializer<>(
        MultiblockRecipe.CODEC,
        MultiblockRecipe.STREAM_CODEC
    );
    public final BlockPattern pattern;
    public final ItemStackTemplate result;
    private @Nullable PlacementInfo placementInfo;

    public MultiblockRecipe(BlockPattern pattern, ItemStackTemplate result) {
        this.pattern = pattern;
        this.result = result;
    }

    public static MultiblockBuilder builder() {
        return new MultiblockBuilder();
    }

    public static MultiblockBuilder builder(ItemLike item, int count) {
        return new MultiblockBuilder(item, count);
    }

    public static MultiblockBuilder builder(ItemLike item) {
        return MultiblockRecipe.builder(item, 1);
    }

    @Override
    public RecipeType<MultiblockRecipe> getType() {
        return ModRecipeTypes.MULTIBLOCK.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        if (this.placementInfo == null) {
            this.placementInfo = PlacementInfo.createFromOptionals(List.of());
        }
        return this.placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<MultiblockRecipe> getSerializer() {
        return MultiblockRecipe.SERIALIZER;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean matches(MultiblockInput input, Level level) {
        int size = input.size();
        if (this.pattern.getLayers().size() != size) {
            return false;
        }
        // 无旋转
        boolean flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!this.pattern.getPredicate(x, y, z).test(input.getBlockState(x, y, z))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            return true;
        }
        // 旋转90
        flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!this.pattern.getPredicate(x, y, z).test(
                        input.getBlockState(z, y, size - 1 - x).rotate(Rotation.CLOCKWISE_90))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            return true;
        }
        // 旋转180
        flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!this.pattern.getPredicate(x, y, z).test(
                        input.getBlockState(size - 1 - x, y, size - 1 - z).rotate(Rotation.CLOCKWISE_180))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            return true;
        }
        // 旋转270
        flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!this.pattern.getPredicate(x, y, z).test(
                        input.getBlockState(size - 1 - z, y, x).rotate(Rotation.COUNTERCLOCKWISE_90))) {
                        flag = false;
                    }
                }
            }
        }
        return flag;
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
    public String group() {
        return "";
    }

    @Override
    public String toDatagen() {
        StringBuilder codeBuilder = new StringBuilder("MultiblockRecipe.builder(\"%s\", %d)"
            .formatted(BuiltInRegistries.ITEM.getKey(this.result.item().value()), this.result.count()));
        codeBuilder.append("\n");

        for (List<String> layer : this.pattern.getLayers()) {
            codeBuilder.append("    .layer(");
            codeBuilder.append(layer.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
            codeBuilder.append(")");
            codeBuilder.append("\n");
        }
        this.pattern.getSymbols().forEach((symbol, predicate) -> {
            codeBuilder.append("    .symbol(");
            codeBuilder.append("'").append(symbol).append("'");
            codeBuilder.append(", ");
            if (predicate.getProperties().isEmpty()) {
                codeBuilder.append("\"");
                codeBuilder.append(predicate.getBlock().<String>map(
                    block -> BuiltInRegistries.BLOCK.getKey(block).toString(),
                    tag -> "#" + tag.location()
                ));
                codeBuilder.append("\"");
                codeBuilder.append(")");
            } else {
                codeBuilder.append("BlockPredicateWithState.of(");
                codeBuilder.append("\"");
                codeBuilder.append(predicate.getBlock().<String>map(
                    block -> BuiltInRegistries.BLOCK.getKey(block).toString(),
                    tag -> "#" + tag.location()
                ));
                codeBuilder.append("\"");
                codeBuilder.append(")");
                codeBuilder.append("\n");
                predicate.getProperties().forEach((property, value) -> {
                    codeBuilder.append("        .hasState(");
                    codeBuilder.append("\"").append(property.getName()).append("\"");
                    codeBuilder.append(", ");
                    codeBuilder.append("\"").append(BlockPredicateWithState.getNameOf(value)).append("\"");
                    codeBuilder.append(")");
                    codeBuilder.append("\n");
                });
                codeBuilder.append("    )");
            }
            codeBuilder.append("\n");
        });
        codeBuilder.append("    .save(provider);");
        return codeBuilder.toString();
    }

    @Override
    public String getSuggestedName() {
        return BuiltInRegistries.ITEM.getKey(this.result.item().value()).getPath();
    }

    @Override
    public ItemStack assemble(MultiblockInput input) {
        return ItemStack.EMPTY;
    }

    public static RecipeSerializer<MultiblockRecipe> serializer() {
        return new RecipeSerializer<>(MultiblockRecipe.CODEC, MultiblockRecipe.STREAM_CODEC);
    }
}
