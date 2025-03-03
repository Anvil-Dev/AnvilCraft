package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;

import dev.dubhe.anvilcraft.recipe.IDatagen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockRecipe implements Recipe<MultiblockInput>, IDatagen {
    public final BlockPattern pattern;
    public final ItemStack result;

    public MultiblockRecipe(BlockPattern pattern, ItemStack result) {
        this.pattern = pattern;
        this.result = result;
    }

    @Contract(" -> new")
    public static MultiblockBuilder builder() {
        return new MultiblockBuilder();
    }

    @Contract(" _, _ -> new")
    public static MultiblockBuilder builder(ItemLike item, int count) {
        return new MultiblockBuilder(item, count);
    }

    public static MultiblockBuilder builder(String item, int count) {
        return builder(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)), count);
    }

    public static MultiblockBuilder builder(ItemLike item) {
        return builder(item, 1);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MULTIBLOCK_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MULTIBLOCK_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result;
    }

    @Override
    public boolean matches(MultiblockInput input, Level level) {
        int size = input.size();
        if (pattern.getLayers().size() != size) {
            return false;
        }
        // 无旋转
        boolean flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(x, y, z))) {
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
                    if (!pattern.getPredicate(x, y, z).test(
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
                    if (!pattern.getPredicate(x, y, z).test(
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
                    if (!pattern.getPredicate(x, y, z).test(
                        input.getBlockState(size - 1 - z, y, x).rotate(Rotation.COUNTERCLOCKWISE_90))) {
                        flag = false;
                    }
                }
            }
        }
        return flag;
    }

    @Override
    public String toDatagen() {
        StringBuilder codeBuilder = new StringBuilder("MultiblockRecipe.builder(\"%s\", %d)"
            .formatted(BuiltInRegistries.ITEM.getKey(result.getItem()), result.getCount()));
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
                codeBuilder.append(BuiltInRegistries.BLOCK.getKey(predicate.getBlock()));
                codeBuilder.append("\"");
                codeBuilder.append(")");
            } else {
                codeBuilder.append("BlockPredicateWithState.of(");
                codeBuilder.append("\"");
                codeBuilder.append(BuiltInRegistries.BLOCK.getKey(predicate.getBlock()));
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
        return BuiltInRegistries.ITEM.getKey(this.result.getItem()).getPath();
    }

    @Override
    public ItemStack assemble(MultiblockInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    public static class Serializer implements RecipeSerializer<MultiblockRecipe> {

        private static final MapCodec<MultiblockRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                BlockPattern.CODEC.fieldOf("pattern").forGetter(MultiblockRecipe::getPattern),
                ItemStack.CODEC.fieldOf("result").forGetter(MultiblockRecipe::getResult))
            .apply(ins, MultiblockRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MultiblockRecipe> STREAM_CODEC =
            StreamCodec.composite(
                BlockPattern.STREAM_CODEC,
                MultiblockRecipe::getPattern,
                ItemStack.STREAM_CODEC,
                MultiblockRecipe::getResult,
                MultiblockRecipe::new);

        @Override
        public MapCodec<MultiblockRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MultiblockRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
