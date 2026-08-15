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
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@Getter
public class MultiblockRecipe implements IMultiblockRecipe, IDatagen {
    private final MultiblockDefinition pattern;
    private final ItemStack result;

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
        return MultiblockUtil.match(this.pattern, input, level).isPresent();
    }

    @Override
    public void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput ctx) {
        int size = ctx.size();
        MultiblockUtil.match(this.pattern, ctx, level).ifPresent(rotation -> {
            MultiblockUtil.consume(level, this.pattern, ctx, inputCorner, rotation);
            AnvilUtil.dropItems(
                List.of(this.result.copy()),
                level,
                landPos.relative(Direction.Axis.Y, -size / 2).getCenter()
            );
        });
    }

    @Override
    public boolean isValidCenterBlock(Level level, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.SPACE_OVERCOMPRESSOR);
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
        boolean named = predicate.getBlocks() instanceof HolderSet.Named<?>;
        int blockCount = predicate.getBlocks().size();

        if (predicate.getProperties().isEmpty()) {
            if (!named && blockCount == 1) {
                return "\"" + MultiblockRecipe.firstBlockId(predicate) + "\"";
            }
            if (named) {
                return "TagKey.create(Registries.BLOCK, ResourceLocation.parse(\""
                    + MultiblockRecipe.tagId(predicate) + "\"))";
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
                    .append(MultiblockRecipe.tagId(predicate)).append("\"))");
            } else {
                builder.append(predicate.getBlocks().stream()
                    .map(holder -> "BuiltInRegistries.BLOCK.get(ResourceLocation.parse(\""
                        + BuiltInRegistries.BLOCK.getKey(holder.value()) + "\"))")
                    .collect(Collectors.joining(", ")));
            }
            builder.append(")");
        }
        String blockId = MultiblockRecipe.firstBlockId(predicate);
        List<List<BlockStatePredicate.PropertyMatcher>> groups = predicate.getProperties();
        for (int i = 0; i < groups.size(); i++) {
            for (BlockStatePredicate.PropertyMatcher matcher : groups.get(i)) {
                builder.append(MultiblockRecipe.propertyCall(matcher, blockId));
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
