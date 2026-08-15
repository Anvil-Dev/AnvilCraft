package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.IDatagen;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class MultiblockRecipe implements IMultiblockRecipe, IDatagen {
    private final MultiblockDefinition pattern;
    private final ItemStack result;
    private Rotation matchedRotation = Rotation.NONE;

    public MultiblockRecipe(MultiblockDefinition pattern, ItemStack result) {
        this.pattern = pattern;
        this.result = result;
    }

    public static MultiblockBuilder builder() {
        return new MultiblockBuilder();
    }

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
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return this.result;
    }

    @Override
    public boolean matches(MultiblockInput input, Level level) {
        return MultiblockUtil.match(this.pattern, input)
            .map(rotation -> {
                this.matchedRotation = rotation;
                return true;
            })
            .orElse(false);
    }

    @Override
    public void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput ctx) {
        int size = ctx.size();
        MultiblockUtil.consume(level, this.pattern, ctx, inputCorner, this.matchedRotation);
        AnvilUtil.dropItems(
            List.of(this.result.copy()),
            level,
            landPos.relative(Direction.Axis.Y, -size / 2).getCenter()
        );
    }

    @Override
    public boolean isValidCenterBlock(Level level, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.SPACE_OVERCOMPRESSOR);
    }

    public BlockPattern toBlockPattern() {
        return MultiblockUtil.toBlockPattern(this.pattern);
    }

    @Override
    public String toDatagen() {
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(this.pattern);
        StringBuilder codeBuilder = new StringBuilder("MultiblockRecipe.builder(\"%s\", %d)"
            .formatted(BuiltInRegistries.ITEM.getKey(this.result.getItem()), this.result.getCount()));
        codeBuilder.append("\n");

        for (String[] layer : serialization.grid()) {
            codeBuilder.append("    .layer(");
            codeBuilder.append(Arrays.stream(layer).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
            codeBuilder.append(")\n");
        }
        serialization.mapping().forEach((symbol, predicate) -> {
            codeBuilder.append("    .symbol('").append(symbol).append("', ");
            codeBuilder.append(MultiblockRecipe.toDatagenPredicate(predicate));
            codeBuilder.append(")\n");
        });
        codeBuilder.append("    .save(provider);");
        return codeBuilder.toString();
    }

    private static String toDatagenPredicate(BlockStatePredicate predicate) {
        BlockPredicateWithState converted = MultiblockUtil.toBlockPredicate(predicate);
        if (converted.getTag() != null) {
            return "BlockPredicateWithState.ofTag(\"" + converted.getTag().location() + "\")";
        }
        String block = BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(converted.getBlock())).toString();
        if (converted.getProperties().isEmpty()) {
            return "\"" + block + "\"";
        }
        StringBuilder builder = new StringBuilder("BlockPredicateWithState.of(\"").append(block).append("\")");
        converted.getProperties().forEach((property, value) -> builder
            .append("\n        .hasState(\"")
            .append(property.getName())
            .append("\", \"")
            .append(BlockPredicateWithState.getNameOf(value))
            .append("\")"));
        return builder.toString();
    }

    @Override
    public String getSuggestedName() {
        return BuiltInRegistries.ITEM.getKey(this.result.getItem()).getPath();
    }

    public static class Serializer implements RecipeSerializer<MultiblockRecipe> {
        private static final MapCodec<MultiblockRecipe> CODEC = CodecUtil.mapCodec(
            MultiblockDefinition.CODEC
                .fieldOf("pattern")
                .forGetter(MultiblockRecipe::getPattern),
            ItemStack.CODEC
                .fieldOf("result")
                .forGetter(MultiblockRecipe::getResult),
            MultiblockRecipe::new
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, MultiblockRecipe> STREAM_CODEC = StreamCodec.composite(
            MultiblockDefinition.STREAM_CODEC,
            MultiblockRecipe::getPattern,
            ItemStack.STREAM_CODEC,
            MultiblockRecipe::getResult,
            MultiblockRecipe::new
        );

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
