package dev.dubhe.anvilcraft.recipe.mulitblock;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MulitblockRecipe implements Recipe<MulitblockRecipe.Input> {
    public final BlockPattern pattern;
    public final ItemStack result;

    public MulitblockRecipe(BlockPattern pattern, ItemStack result) {
        this.pattern = pattern;
        this.result = result;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
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
    public boolean matches(Input input, Level level) {
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
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    public record Input(List<List<List<BlockState>>> blocks) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        public BlockState getBlockState(int x, int y, int z) {
            return blocks.get(y).get(z).get(x);
        }

        public void setBlockState(int x, int y, int z, BlockState state) {
            blocks.get(y).get(z).set(x, state);
        }

        public void rotate() {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    for (int z = 0; z < 3; z++) {
                        BlockState state = getBlockState(x, y, z);
                        if (state.hasProperty(BlockStateProperties.FACING)) {
                            setBlockState(
                                    x,
                                    y,
                                    z,
                                    state.setValue(
                                            BlockStateProperties.FACING,
                                            rotateHorizontal(state.getValue(BlockStateProperties.FACING))));
                        }
                        if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
                            setBlockState(
                                    x,
                                    y,
                                    z,
                                    state.setValue(
                                            BlockStateProperties.FACING_HOPPER,
                                            rotateHorizontal(state.getValue(BlockStateProperties.FACING_HOPPER))));
                        }
                        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            setBlockState(
                                    x,
                                    y,
                                    z,
                                    state.setValue(
                                            BlockStateProperties.HORIZONTAL_FACING,
                                            rotateHorizontal(state.getValue(BlockStateProperties.HORIZONTAL_FACING))));
                        }
                        if (state.hasProperty(BlockStateProperties.AXIS)) {
                            setBlockState(
                                    x,
                                    y,
                                    z,
                                    state.setValue(
                                            BlockStateProperties.AXIS,
                                            rotateAxis(state.getValue(BlockStateProperties.AXIS))));
                        }
                    }
                }
            }
        }

        private static Direction rotateHorizontal(Direction direction) {
            return switch (direction) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> direction;
            };
        }

        private static Direction.Axis rotateAxis(Direction.Axis axis) {
            return switch (axis) {
                case X -> Direction.Axis.Z;
                case Z -> Direction.Axis.X;
                default -> axis;
            };
        }
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

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MulitblockRecipe> {

        private BlockPattern pattern = BlockPattern.create();
        private ItemStack result;

        public Builder layer(String... layers) {
            pattern.layer(layers);
            return this;
        }

        public Builder symbol(char symbol, BlockPredicateWithState predicate) {
            pattern.symbol(symbol, predicate);
            return this;
        }

        public Builder symbol(char symbol, Block block) {
            return symbol(symbol, BlockPredicateWithState.of(block));
        }

        @Override
        public MulitblockRecipe buildRecipe() {
            return new MulitblockRecipe(pattern, result);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (result == null) {
                throw new IllegalArgumentException("Recipe result must not be null, Recipe: " + pId);
            }
            if (!pattern.checkSymbols()) {
                throw new IllegalArgumentException("Recipe pattern must contain all valid symbols: " + pId);
            }
        }

        @Override
        public String getType() {
            return "mulitblock";
        }

        @Override
        public Item getResult() {
            return result.getItem();
        }
    }
}
