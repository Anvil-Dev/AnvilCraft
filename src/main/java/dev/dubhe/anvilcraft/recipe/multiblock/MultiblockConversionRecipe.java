package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.IDatagen;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class MultiblockConversionRecipe implements IMultiblockRecipe, IDatagen {
    private final MultiblockDefinition inputPattern;
    private final MultiblockDefinition outputPattern;
    private final Optional<ModifySpawnerAction> modifySpawnerAction;
    private Rotation matchedRotation = Rotation.NONE;

    public MultiblockConversionRecipe(MultiblockDefinition inputPattern, MultiblockDefinition outputPattern) {
        this(inputPattern, outputPattern, Optional.empty());
    }

    public MultiblockConversionRecipe(
        MultiblockDefinition inputPattern,
        MultiblockDefinition outputPattern,
        @Nullable ModifySpawnerAction modifySpawnerAction
    ) {
        this(inputPattern, outputPattern, Optional.ofNullable(modifySpawnerAction));
    }

    public MultiblockConversionRecipe(
        MultiblockDefinition inputPattern,
        MultiblockDefinition outputPattern,
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
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(MultiblockInput input, Level level) {
        return MultiblockUtil.match(this.inputPattern, input, level)
            .map(rotation -> {
                this.matchedRotation = rotation;
                return true;
            })
            .orElse(false);
    }

    public int getSize() {
        return DefinitionSerialization.fromDefinition(this.inputPattern).grid().length;
    }

    @Nullable
    public Block centerOutput() {
        int t = this.getSize() / 2;
        return this.toOutputBlockPattern().getPredicate(t, t, t).getBlock();
    }

    @Override
    public void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput ctx) {
        int size = ctx.size();
        Rotation rotation = this.matchedRotation;
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(this.outputPattern);
        String[][] grid = serialization.grid();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < grid.length; y++) {
            for (int z = 0; z < grid[y].length; z++) {
                String line = grid[y][z];
                for (int x = 0; x < line.length(); x++) {
                    char symbol = line.charAt(x);
                    BlockState newState;
                    if (symbol == ' ') {
                        newState = Blocks.AIR.defaultBlockState();
                    } else {
                        newState = MultiblockUtil.toBlockPredicate(serialization.mapping().get(symbol))
                            .getDefaultState();
                    }
                    BlockPos world = MultiblockUtil.rotatePatternToWorld(x, y, z, rotation, size);
                    mpos.setWithOffset(inputCorner, world);
                    level.setBlock(mpos, newState.rotate(rotation), 18);
                }
            }
        }
        // NC update (Block#neighborChanged) after structure converted
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    if (x > 0 && x < size - 1 && y > 0 && y < size - 1 && z > 0 && z < size - 1) {
                        continue;
                    }
                    mpos.setWithOffset(inputCorner, x, y, z);
                    level.blockUpdated(mpos, ctx.getBlockState(x, y, z).getBlock());
                    BlockState newState = level.getBlockState(mpos);
                    if (newState.hasAnalogOutputSignal()) {
                        level.updateNeighbourForOutputSignal(mpos, newState.getBlock());
                    }
                }
            }
        }
        // PP update (Block#updateShape) after structure converted
        DiscreteVoxelShape shape = BitSetDiscreteVoxelShape.withFilledBounds(
            size, size, size,
            0, 0, 0,
            size, size, size
        );
        BlockPos.MutableBlockPos mpos2 = new BlockPos.MutableBlockPos();
        shape.forAllFaces(
            (direction, x, y, z) -> {
                BlockPos innerPos = mpos.setWithOffset(inputCorner, x, y, z);
                BlockPos outerPos = mpos2.setWithOffset(innerPos, direction);
                BlockState innerState = level.getBlockState(innerPos);
                if (innerState != ctx.getBlockState(x, y, z)) {
                    level.neighborShapeChanged(direction.getOpposite(), level.getBlockState(innerPos),
                        outerPos, innerPos, 3, 512);
                }
                level.neighborShapeChanged(direction, level.getBlockState(outerPos),
                    innerPos, outerPos, 3, 512);
            }
        );
        Optional<EntityType<?>> entity = this.getModifySpawnerAction()
            .map(ModifySpawnerAction::fromPos)
            .map(pos -> MultiblockConversionRecipe.rotatePos(pos, size, rotation))
            .map(inputCorner::offset)
            .map(level::getBlockEntity)
            .filter(be -> be instanceof HasMobBlockEntity)
            .map(be -> ((HasMobBlockEntity) be).getOrCreateDisplayEntity(level))
            .map(Entity::getType);
        entity.ifPresent(entityType -> {
            BlockPos offset = MultiblockConversionRecipe.rotatePos(
                this.getModifySpawnerAction().get().toPos(), size, rotation);
            Optional.ofNullable(level.getBlockEntity(inputCorner.offset(offset)))
                .filter(be -> be instanceof Spawner)
                .ifPresent(be -> ((Spawner) be).setEntityId(entityType, level.getRandom()));
        });
    }

    @Override
    public boolean isValidCenterBlock(Level level, BlockPos pos, BlockState state) {
        return state.is(net.neoforged.neoforge.common.Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES);
    }

    public BlockPattern toInputBlockPattern() {
        return MultiblockUtil.toBlockPattern(this.inputPattern);
    }

    public BlockPattern toOutputBlockPattern() {
        return MultiblockUtil.toBlockPattern(this.outputPattern);
    }

    private static BlockPos rotatePos(BlockPos pos, int size, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), size - 1 - pos.getX());
            case CLOCKWISE_180 -> new BlockPos(size - 1 - pos.getX(), pos.getY(), size - 1 - pos.getZ());
            case CLOCKWISE_90 -> new BlockPos(size - 1 - pos.getZ(), pos.getY(), pos.getX());
            default -> pos;
        };
    }

    @SuppressWarnings("CodeBlock2Expr")
    private static void datagenForPattern(StringBuilder codeBuilder, MultiblockDefinition definition, String role) {
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(definition);
        for (String[] layer : serialization.grid()) {
            codeBuilder.append("    .")
                .append(role)
                .append("Layer(")
                .append(Arrays.stream(layer).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")))
                .append(")\n");
        }
        serialization.mapping().forEach((symbol, predicate) -> {
            BlockPredicateWithState converted = MultiblockUtil.toBlockPredicate(predicate);
            codeBuilder.append("    .")
                .append(role)
                .append("Symbol('")
                .append(symbol)
                .append("', ");
            if (converted.getTag() != null) {
                codeBuilder.append("BlockPredicateWithState.ofTag(\"")
                    .append(converted.getTag().location())
                    .append("\")");
            } else if (converted.getProperties().isEmpty()) {
                codeBuilder.append("\"")
                    .append(BuiltInRegistries.BLOCK.getKey(converted.getBlock()))
                    .append("\")");
            } else {
                codeBuilder.append("BlockPredicateWithState.of(\"")
                    .append(BuiltInRegistries.BLOCK.getKey(converted.getBlock()))
                    .append("\")\n");
                converted.getProperties().forEach((property, value) -> {
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
        Block center = this.centerOutput();
        return center != null
            ? BuiltInRegistries.BLOCK.getKey(center).getPath()
            : Integer.toHexString(this.hashCode());
    }

    public static class Builder extends AbstractRecipeBuilder<MultiblockConversionRecipe> {
        private final MultiblockDefinition.SeriaBuilder inputPattern = MultiblockDefinition.seriaBuilder();
        private final MultiblockDefinition.SeriaBuilder outputPattern = MultiblockDefinition.seriaBuilder();
        private ModifySpawnerAction postAction = null;

        public Builder() {
        }

        public Builder inputLayer(String... layers) {
            inputPattern.layer(layers);
            return this;
        }

        public Builder inputLayer(List<String> layers) {
            inputPattern.layer(layers.toArray(String[]::new));
            return this;
        }

        public Builder outputLayer(String... layers) {
            outputPattern.layer(layers);
            return this;
        }

        public Builder outputLayer(List<String> layers) {
            outputPattern.layer(layers.toArray(String[]::new));
            return this;
        }

        public Builder symbol(char symbol, BlockPredicateWithState predicate) {
            inputPattern.map(symbol, MultiblockUtil.toBlockStatePredicateBuilder(predicate));
            outputPattern.map(symbol, MultiblockUtil.toBlockStatePredicateBuilder(predicate));
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

        public Builder symbol(char symbol, TagKey<Block> tag) {
            inputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            outputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            return this;
        }

        public Builder inputSymbol(char symbol, BlockPredicateWithState predicate) {
            inputPattern.map(symbol, MultiblockUtil.toBlockStatePredicateBuilder(predicate));
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

        public Builder inputSymbol(char symbol, TagKey<Block> tag) {
            inputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            return this;
        }

        public Builder outputSymbol(char symbol, BlockPredicateWithState predicate) {
            outputPattern.map(symbol, MultiblockUtil.toBlockStatePredicateBuilder(predicate));
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

        public Builder outputSymbol(char symbol, TagKey<Block> tag) {
            outputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            return this;
        }

        public Builder modifySpawnerAction(ModifySpawnerAction postAction) {
            this.postAction = postAction;
            return this;
        }

        @Override
        public MultiblockConversionRecipe buildRecipe() {
            return new MultiblockConversionRecipe(inputPattern.build(), outputPattern.build(), postAction);
        }

        @Override
        public void validate(ResourceLocation id) {
            int inputSize = DefinitionSerialization.fromDefinition(this.inputPattern.build()).grid().length;
            int outputSize = DefinitionSerialization.fromDefinition(this.outputPattern.build()).grid().length;
            if (inputSize != outputSize) {
                throw new IllegalArgumentException("Input size must be same as output size: %s input size: %d, output size: %d"
                    .formatted(id, inputSize, outputSize));
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
                    MultiblockDefinition.CODEC.fieldOf("inputPattern")
                        .forGetter(MultiblockConversionRecipe::getInputPattern),
                    MultiblockDefinition.CODEC.fieldOf("outputPattern")
                        .forGetter(MultiblockConversionRecipe::getOutputPattern),
                    ModifySpawnerAction.CODEC.codec().optionalFieldOf("modifySpawnerAction")
                        .forGetter(MultiblockConversionRecipe::getModifySpawnerAction)
                )
                .apply(ins, MultiblockConversionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MultiblockConversionRecipe> STREAM_CODEC =
            StreamCodec.composite(
                MultiblockDefinition.STREAM_CODEC,
                MultiblockConversionRecipe::getInputPattern,
                MultiblockDefinition.STREAM_CODEC,
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
