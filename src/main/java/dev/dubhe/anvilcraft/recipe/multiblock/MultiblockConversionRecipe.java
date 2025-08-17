package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.IDatagen;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockConversionRecipe implements Recipe<MultiblockInput>, IDatagen {
    private final BlockPattern inputPattern;
    private final BlockPattern outputPattern;
    private final Optional<ModifySpawnerAction> modifySpawnerAction;
    private Rotation matchedRotation = Rotation.NONE;

    public MultiblockConversionRecipe(BlockPattern inputPattern, BlockPattern outputPattern) {
        this(inputPattern, outputPattern, Optional.empty());
    }

    public MultiblockConversionRecipe(
        BlockPattern inputPattern,
        BlockPattern outputPattern,
        @Nullable ModifySpawnerAction modifySpawnerAction
    ) {
        this(inputPattern, outputPattern, Optional.ofNullable(modifySpawnerAction));
    }

    public MultiblockConversionRecipe(
        BlockPattern inputPattern,
        BlockPattern outputPattern,
        Optional<ModifySpawnerAction> modifySpawnerAction
    ) {
        this.inputPattern = inputPattern;
        this.outputPattern = outputPattern;
        this.modifySpawnerAction = modifySpawnerAction;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MULTIBLOCK_CONVERSION_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MULTIBLOCK_CONVERSION_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean matches(MultiblockInput input, Level level) {
        int size = input.size();
        if (this.getSize() != size) {
            return false;
        }
        // 无旋转
        boolean flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!inputPattern.getPredicate(x, y, z).test(input.getBlockState(x, y, z))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            matchedRotation = Rotation.NONE;
            return true;
        }
        // 旋转90
        flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!inputPattern.getPredicate(x, y, z).test(
                        input.getBlockState(z, y, size - 1 - x).rotate(Rotation.CLOCKWISE_90))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            matchedRotation = Rotation.COUNTERCLOCKWISE_90;
            return true;
        }
        // 旋转180
        flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!inputPattern.getPredicate(x, y, z).test(
                        input.getBlockState(size - 1 - x, y, size - 1 - z).rotate(Rotation.CLOCKWISE_180))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            matchedRotation = Rotation.CLOCKWISE_180;
            return true;
        }
        // 旋转270
        flag = true;
        for (int x = 0; x < size && flag; x++) {
            for (int y = 0; y < size && flag; y++) {
                for (int z = 0; z < size && flag; z++) {
                    if (!inputPattern.getPredicate(x, y, z).test(
                        input.getBlockState(size - 1 - z, y, x).rotate(Rotation.COUNTERCLOCKWISE_90))) {
                        flag = false;
                    }
                }
            }
        }
        if (flag) {
            matchedRotation = Rotation.CLOCKWISE_90;
            return true;
        }
        return false;
    }

    public int getSize() {
        return this.inputPattern.getSize();
    }

    public Block centerOutput() {
        int t = this.getSize() / 2;
        return this.getOutputPattern().getPredicate(t, t, t).getBlock();
    }

    @Override
    public ItemStack assemble(MultiblockInput input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static void datagenForPattern(StringBuilder codeBuilder, BlockPattern pattern, String role) {
        for (List<String> layer : pattern.getLayers()) {
            codeBuilder.append("    .")
                .append(role)
                .append("Layer(")
                .append(layer.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")))
                .append(")\n");
        }
        pattern.getSymbols().forEach((symbol, predicate) -> {
            codeBuilder.append("    .")
                .append(role)
                .append("Symbol('")
                .append(symbol)
                .append("', ");
            if (predicate.getProperties().isEmpty()) {
                codeBuilder.append("\"")
                    .append(BuiltInRegistries.BLOCK.getKey(predicate.getBlock()))
                    .append("\")");
            } else {
                codeBuilder.append("BlockPredicateWithState.of(\"")
                    .append(BuiltInRegistries.BLOCK.getKey(predicate.getBlock()))
                    .append("\")\n");
                predicate.getProperties().forEach((property, value) -> {
                    codeBuilder.append("        .hasState(\"")
                        .append(property.getName())
                        .append("\", \"")
                        .append(BlockPredicateWithState.getNameOf(value))
                        .append("\")\n");
                });
                codeBuilder.append("    )");
            }
            codeBuilder.append("\n");
        });
    }

    @Override
    public String toDatagen() {
        StringBuilder codeBuilder = new StringBuilder("MultiblockConversionRecipe.builder()\n");

        datagenForPattern(codeBuilder, this.inputPattern, "input");
        datagenForPattern(codeBuilder, this.outputPattern, "output");

        codeBuilder.append("    .save(provider);");
        return codeBuilder.toString();
    }

    @Override
    public String getSuggestedName() {
        return BuiltInRegistries.BLOCK.getKey(this.centerOutput()).getPath();
    }

    public static class Builder extends AbstractRecipeBuilder<MultiblockConversionRecipe> {
        private final BlockPattern inputPattern = BlockPattern.create();
        private final BlockPattern outputPattern = BlockPattern.create();
        private ModifySpawnerAction postAction = null;

        public Builder() {
        }

        public Builder inputLayer(String... layers) {
            inputPattern.layer(layers);
            return this;
        }

        public Builder outputLayer(String... layers) {
            outputPattern.layer(layers);
            return this;
        }

        public Builder symbol(char symbol, BlockPredicateWithState predicate) {
            inputPattern.symbol(symbol, predicate);
            outputPattern.symbol(symbol, predicate);
            return this;
        }

        public Builder symbol(char symbol, Block block) {
            return this.symbol(symbol, BlockPredicateWithState.of(block));
        }

        public Builder symbol(char symbol, Holder<Block> block) {
            return this.symbol(symbol, block.value());
        }

        public Builder symbol(char symbol, String blockName) {
            return this.symbol(symbol, BlockPredicateWithState.of(blockName));
        }

        public Builder inputSymbol(char symbol, BlockPredicateWithState predicate) {
            inputPattern.symbol(symbol, predicate);
            return this;
        }

        public Builder inputSymbol(char symbol, Block block) {
            return this.inputSymbol(symbol, BlockPredicateWithState.of(block));
        }

        public Builder inputSymbol(char symbol, Holder<Block> block) {
            return this.inputSymbol(symbol, block.value());
        }

        public Builder inputSymbol(char symbol, String blockName) {
            return this.inputSymbol(symbol, BlockPredicateWithState.of(blockName));
        }

        public Builder outputSymbol(char symbol, BlockPredicateWithState predicate) {
            outputPattern.symbol(symbol, predicate);
            return this;
        }

        public Builder outputSymbol(char symbol, Block block) {
            return this.outputSymbol(symbol, BlockPredicateWithState.of(block));
        }

        public Builder outputSymbol(char symbol, Holder<Block> block) {
            return this.outputSymbol(symbol, block.value());
        }

        public Builder outputSymbol(char symbol, String blockName) {
            return this.outputSymbol(symbol, BlockPredicateWithState.of(blockName));
        }

        public Builder modifySpawnerAction(ModifySpawnerAction postAction) {
            this.postAction = postAction;
            return this;
        }

        @Override
        public MultiblockConversionRecipe buildRecipe() {
            return new MultiblockConversionRecipe(inputPattern, outputPattern, postAction);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (inputPattern.getSize() != outputPattern.getSize()) {
                throw new IllegalArgumentException(("Input size must be same as output size: %s input size: %d, output size: %d")
                    .formatted(pId, inputPattern.getSize(), outputPattern.getSize()));
            }
            if (!inputPattern.checkSymbols()) {
                throw new IllegalArgumentException("Input pattern must contain all valid symbols: " + pId);
            }
            if (!outputPattern.checkSymbols()) {
                throw new IllegalArgumentException("Output pattern must contain all valid symbols: " + pId);
            }
        }

        @Override
        public String getType() {
            return "multiblock_conversion";
        }

        @Override
        public Item getResult() {
            return Items.AIR;
        }
    }

    public static class Serializer implements RecipeSerializer<MultiblockConversionRecipe> {

        private static final MapCodec<MultiblockConversionRecipe> CODEC =
            RecordCodecBuilder.mapCodec(ins -> ins.group(
                    BlockPattern.CODEC.fieldOf("inputPattern").forGetter(MultiblockConversionRecipe::getInputPattern),
                    BlockPattern.CODEC.fieldOf("outputPattern").forGetter(MultiblockConversionRecipe::getOutputPattern),
                    ModifySpawnerAction.CODEC.codec().optionalFieldOf("modifySpawnerAction")
                        .forGetter(MultiblockConversionRecipe::getModifySpawnerAction)
                )
                .apply(ins, MultiblockConversionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MultiblockConversionRecipe> STREAM_CODEC =
            StreamCodec.composite(
                BlockPattern.STREAM_CODEC,
                MultiblockConversionRecipe::getInputPattern,
                BlockPattern.STREAM_CODEC,
                MultiblockConversionRecipe::getOutputPattern,
                ByteBufCodecs.optional(ModifySpawnerAction.STREAM_CODEC),
                MultiblockConversionRecipe::getModifySpawnerAction,
                MultiblockConversionRecipe::new
            );

        @Override
        public MapCodec<MultiblockConversionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MultiblockConversionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
