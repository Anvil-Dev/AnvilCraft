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
import net.minecraft.core.HolderSet;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@Getter
public class MultiblockConversionRecipe implements IMultiblockRecipe, IDatagen {
    private final MultiblockDefinition inputPattern;
    private final MultiblockDefinition outputPattern;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<ModifySpawnerAction> modifySpawnerAction;

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
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        Optional<ModifySpawnerAction> modifySpawnerAction
    ) {
        this.inputPattern = inputPattern;
        this.outputPattern = outputPattern;
        this.modifySpawnerAction = modifySpawnerAction;
    }

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
        return MultiblockUtil.match(this.inputPattern, input, level).isPresent();
    }

    public int getSize() {
        return DefinitionSerialization.fromDefinition(this.inputPattern).grid().length;
    }

    @Nullable
    public Block centerOutput() {
        int t = this.getSize() / 2;
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(this.outputPattern);
        String[][] grid = serialization.grid();
        if (t >= grid.length || t >= grid[0].length || t >= grid[0][t].length()) {
            return null;
        }
        char symbol = grid[t][t].charAt(t);
        if (symbol == ' ') {
            return Blocks.AIR;
        }
        return serialization.mapping().get(symbol).getBlocks().stream()
            .findFirst()
            .map(Holder::value)
            .orElse(null);
    }

    @Override
    public void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput ctx) {
        Optional<Rotation> matched = MultiblockUtil.match(this.inputPattern, ctx, level);
        if (matched.isEmpty()) {
            return;
        }
        Rotation rotation = matched.get();
        int size = ctx.size();
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(this.outputPattern);
        String[][] grid = serialization.grid();
        int[] offsets = MultiblockUtil.offsets(size, grid);
        final Optional<EntityType<?>> entity = this.getModifySpawnerAction()
            .map(ModifySpawnerAction::fromPos)
            .map(pos -> MultiblockConversionRecipe.rotatePos(pos, size, rotation))
            .map(inputCorner::offset)
            .map(level::getBlockEntity)
            .filter(be -> be instanceof HasMobBlockEntity)
            .map(be -> ((HasMobBlockEntity) be).getOrCreateDisplayEntity(level))
            .map(Entity::getType);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < grid.length; y++) {
            String[] layer = grid[y];
            for (int z = 0; z < layer.length; z++) {
                String line = layer[z];
                for (int x = 0; x < line.length(); x++) {
                    char symbol = line.charAt(x);
                    BlockState newState;
                    if (symbol == ' ') {
                        newState = Blocks.AIR.defaultBlockState();
                    } else {
                        newState = MultiblockUtil.getDefaultState(serialization.mapping().get(symbol));
                    }
                    BlockPos world = MultiblockUtil.rotatePatternToWorld(
                        x + offsets[0], y + offsets[1], z + offsets[2], rotation, size);
                    mpos.setWithOffset(inputCorner, world);
                    // noinspection deprecation
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

    private static BlockPos rotatePos(BlockPos pos, int size, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), size - 1 - pos.getX());
            case CLOCKWISE_180 -> new BlockPos(size - 1 - pos.getX(), pos.getY(), size - 1 - pos.getZ());
            case CLOCKWISE_90 -> new BlockPos(size - 1 - pos.getZ(), pos.getY(), pos.getX());
            default -> pos;
        };
    }

    private static void datagenForPattern(StringBuilder codeBuilder, MultiblockDefinition definition, String role) {
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(definition);
        for (String[] layer : serialization.grid()) {
            codeBuilder.append("    .")
                .append(role)
                .append("Layer(")
                .append(Arrays.stream(layer).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")))
                .append(")\n");
        }
        serialization.mapping().forEach((symbol, predicate) -> codeBuilder
            .append("    .")
            .append(role)
            .append("Symbol('")
            .append(symbol)
            .append("', ")
            .append(MultiblockConversionRecipe.toDatagenPredicate(predicate))
            .append(")\n"));
    }

    private static String toDatagenPredicate(BlockStatePredicate predicate) {
        boolean named = predicate.getBlocks() instanceof HolderSet.Named<?>;
        int blockCount = predicate.getBlocks().size();

        if (predicate.getProperties().isEmpty()) {
            if (!named && blockCount == 1) {
                return "\"" + MultiblockConversionRecipe.firstBlockId(predicate) + "\"";
            }
            if (named) {
                return "TagKey.create(Registries.BLOCK, ResourceLocation.parse(\""
                    + MultiblockConversionRecipe.tagId(predicate) + "\"))";
            }
            if (blockCount == 0) {
                return "BlockStatePredicate.builder()";
            }
        }

        StringBuilder builder = new StringBuilder("BlockStatePredicate.builder()");
        if (blockCount > 0) {
            builder.append(".of(");
            if (named) {
                builder.append("TagKey.create(Registries.BLOCK, ResourceLocation.parse(\"")
                    .append(MultiblockConversionRecipe.tagId(predicate)).append("\"))");
            } else {
                builder.append(predicate.getBlocks().stream()
                    .map(holder -> "BuiltInRegistries.BLOCK.get(ResourceLocation.parse(\""
                        + BuiltInRegistries.BLOCK.getKey(holder.value()) + "\"))")
                    .collect(Collectors.joining(", ")));
            }
            builder.append(")");
        }
        String blockId = MultiblockConversionRecipe.firstBlockId(predicate);
        List<List<BlockStatePredicate.PropertyMatcher>> groups = predicate.getProperties();
        for (int i = 0; i < groups.size(); i++) {
            for (BlockStatePredicate.PropertyMatcher matcher : groups.get(i)) {
                builder.append(MultiblockConversionRecipe.propertyCall(matcher, blockId));
            }
            if (i < groups.size() - 1) {
                builder.append(".or()");
            }
        }
        return builder.toString();
    }

    @Nullable
    private static String firstBlockId(BlockStatePredicate predicate) {
        return predicate.getBlocks().stream().findFirst()
            .map(holder -> BuiltInRegistries.BLOCK.getKey(holder.value()).toString())
            .orElse(null);
    }

    private static String tagId(BlockStatePredicate predicate) {
        if (predicate.getBlocks() instanceof HolderSet.Named<?> named) {
            return named.key().location().toString();
        }
        return "";
    }

    private static String propertyCall(BlockStatePredicate.PropertyMatcher matcher, @Nullable String blockId) {
        if (blockId == null) {
            return "";
        }
        String property = "BuiltInRegistries.BLOCK.get(ResourceLocation.parse(\""
            + blockId + "\")).getStateDefinition().getProperty(\"" + matcher.name() + "\")";
        if (matcher.valueMatcher() instanceof BlockStatePredicate.ExactMatcher(String value)) {
            return ".with(" + property + ", \"" + value + "\")";
        }
        if (matcher.valueMatcher() instanceof BlockStatePredicate.RangedMatcher(
            Optional<String> minValue, Optional<String> maxValue)) {
            return ".with((Property) " + property + ", "
                + minValue.map(value -> "\"" + value + "\"").orElse("null") + ", "
                + maxValue.map(value -> "\"" + value + "\"").orElse("null") + ")";
        }
        return "";
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

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder extends AbstractRecipeBuilder<MultiblockConversionRecipe> {
        private final MultiblockDefinition.SeriaBuilder inputPattern = MultiblockDefinition.seriaBuilder();
        private final MultiblockDefinition.SeriaBuilder outputPattern = MultiblockDefinition.seriaBuilder();
        private @Nullable ModifySpawnerAction postAction = null;

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

        public Builder symbol(char symbol, BlockStatePredicate.Builder predicate) {
            inputPattern.map(symbol, predicate);
            outputPattern.map(symbol, predicate);
            return this;
        }

        public Builder symbol(char symbol, Block block) {
            inputPattern.map(symbol, block);
            outputPattern.map(symbol, block);
            return this;
        }

        public Builder symbol(char symbol, Holder<Block> block) {
            return this.symbol(symbol, block.value());
        }

        public Builder symbol(char symbol, String blockName) {
            return this.symbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName)));
        }

        public Builder symbol(char symbol, TagKey<Block> tag) {
            inputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            outputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            return this;
        }

        public Builder inputSymbol(char symbol, BlockStatePredicate.Builder predicate) {
            inputPattern.map(symbol, predicate);
            return this;
        }

        public Builder inputSymbol(char symbol, Block block) {
            inputPattern.map(symbol, block);
            return this;
        }

        public Builder inputSymbol(char symbol, Holder<Block> block) {
            return this.inputSymbol(symbol, block.value());
        }

        public Builder inputSymbol(char symbol, String blockName) {
            return this.inputSymbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName)));
        }

        public Builder inputSymbol(char symbol, TagKey<Block> tag) {
            inputPattern.map(symbol, BlockStatePredicate.builder().of(tag));
            return this;
        }

        public Builder outputSymbol(char symbol, BlockStatePredicate.Builder predicate) {
            outputPattern.map(symbol, predicate);
            return this;
        }

        public Builder outputSymbol(char symbol, Block block) {
            outputPattern.map(symbol, block);
            return this;
        }

        public Builder outputSymbol(char symbol, Holder<Block> block) {
            return this.outputSymbol(symbol, block.value());
        }

        public Builder outputSymbol(char symbol, String blockName) {
            return this.outputSymbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName)));
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
