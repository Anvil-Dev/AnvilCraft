package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockRecipe implements Recipe<MultiblockInput> {
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

    public static MultiblockBuilder builder(ItemLike item) {
        return builder(item, 1);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MULITBLOCK_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MULITBLOCK_SERIALIZER.get();
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
        input.rotate();
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(z, y, size - 1 - x))) {
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
        input.rotate();
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(size - 1 - x, y, size - 1 - z))) {
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
        input.rotate();
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(size - 1 - z, y, x))) {
                        flag = false;
                    }
                }
            }
        }
        return flag;
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
