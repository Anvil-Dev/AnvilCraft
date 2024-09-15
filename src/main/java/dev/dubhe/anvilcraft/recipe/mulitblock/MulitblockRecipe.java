package dev.dubhe.anvilcraft.recipe.mulitblock;

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
public class MulitblockRecipe implements Recipe<MulitblockInput> {
    public final BlockPattern pattern;
    public final ItemStack result;

    public MulitblockRecipe(BlockPattern pattern, ItemStack result) {
        this.pattern = pattern;
        this.result = result;
    }

    @Contract(" -> new")
    public static MulitblockBuilder builder() {
        return new MulitblockBuilder();
    }

    public static MulitblockBuilder builder(ItemLike item, int count) {
        return new MulitblockBuilder(item, count);
    }

    public static MulitblockBuilder builder(ItemLike item) {
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
    public boolean matches(MulitblockInput input, Level level) {
        // 无旋转
        boolean flag = true;
        for (int x = 0; x < 3 && flag; x++) {
            for (int y = 0; y < 3 && flag; y++) {
                for (int z = 0; z < 3 && flag; z++) {
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
        for (int x = 0; x < 3 && flag; x++) {
            for (int y = 0; y < 3 && flag; y++) {
                for (int z = 0; z < 3 && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(z, y, 2 - x))) {
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
        for (int x = 0; x < 3 && flag; x++) {
            for (int y = 0; y < 3 && flag; y++) {
                for (int z = 0; z < 3 && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(2 - x, y, 2 - z))) {
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
        for (int x = 0; x < 3 && flag; x++) {
            for (int y = 0; y < 3 && flag; y++) {
                for (int z = 0; z < 3 && flag; z++) {
                    if (!pattern.getPredicate(x, y, z).test(input.getBlockState(2 - z, y, x))) {
                        flag = false;
                    }
                }
            }
        }
        return flag;
    }

    @Override
    public ItemStack assemble(MulitblockInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    public static class Serializer implements RecipeSerializer<MulitblockRecipe> {

        private static final MapCodec<MulitblockRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        BlockPattern.CODEC.fieldOf("pattern").forGetter(MulitblockRecipe::getPattern),
                        ItemStack.CODEC.fieldOf("result").forGetter(MulitblockRecipe::getResult))
                .apply(ins, MulitblockRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MulitblockRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPattern.STREAM_CODEC,
                        MulitblockRecipe::getPattern,
                        ItemStack.STREAM_CODEC,
                        MulitblockRecipe::getResult,
                        MulitblockRecipe::new);

        @Override
        public MapCodec<MulitblockRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MulitblockRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
