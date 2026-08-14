package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.NbtPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.IDatagen;
import dev.dubhe.anvilcraft.util.NbtUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@Getter
public class Multiblock4DRecipe implements IMultiblockRecipe, IDatagen {
    public final List<MultiblockDefinition> definitions;
    public final ItemStack result;
    private Rotation matchedRotation = Rotation.NONE;

    public Multiblock4DRecipe(List<MultiblockDefinition> definitions, ItemStack result) {
        this.definitions = definitions;
        this.result = result;
    }

    public static Multiblock4DBuilder builder() {
        return new Multiblock4DBuilder();
    }

    public static Multiblock4DBuilder builder(ItemLike item, int count) {
        return new Multiblock4DBuilder(item, count);
    }

    public static Multiblock4DBuilder builder(String item, int count) {
        return Multiblock4DRecipe.builder(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)), count);
    }

    public static Multiblock4DBuilder builder(ItemLike item) {
        return Multiblock4DRecipe.builder(item, 1);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MULTIBLOCK_4D_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MULTIBLOCK_4D_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public boolean matches(MultiblockInput ctx, Level level) {
        if (this.definitions.isEmpty()) {
            return false;
        }
        return MultiblockUtil.match(this.definitions.getFirst(), ctx, level)
            .map(rotation -> {
                this.matchedRotation = rotation;
                return true;
            })
            .orElse(false);
    }

    @Override
    public ItemStack assemble(MultiblockInput ctx, HolderLookup.@NotNull Provider provider) {
        return this.result.copy();
    }

    @Override
    public void assemble(Level level, BlockPos landPos, BlockPos inputCorner, MultiblockInput ctx) {
    }

    @Override
    public boolean isValidCenterBlock(Level level, BlockPos pos, BlockState state) {
        return state.is(ModBlocks.SPACETIME_SUPERCOMPUTER);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String toDatagen() {
        NbtUtil.State state = new NbtUtil.State();
        List<String> nbtDeclarations = new ArrayList<>();
        List<String> nbtVariables = new ArrayList<>();

        StringBuilder codeBuilder = new StringBuilder(
            "Multiblock4DRecipe.builder(\"%s\", %d)"
                .formatted(BuiltInRegistries.ITEM.getKey(this.result.getItem()), this.result.getCount())
        );
        codeBuilder.append("\n");

        int definitionIndex = 0;
        for (MultiblockDefinition definition : this.definitions) {
            if (definitionIndex > 0) {
                codeBuilder.append("    .next()\n");
            }
            definitionIndex++;
            var seria = DefinitionSerialization.fromDefinition(definition);
            for (String[] layer : seria.grid()) {
                codeBuilder.append("    .layer(");
                codeBuilder.append(Arrays.stream(layer).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
                codeBuilder.append(")\n");
            }
            seria.mapping().forEach((symbol, predicate) -> {
                nbtVariables.clear();
                for (NbtPredicate nbt : predicate.getNbts()) {
                    if (nbt.tag().isEmpty()) continue;
                    nbtVariables.add("nbt" + (nbtDeclarations.size() + 1));
                    nbtDeclarations.add(NbtUtil.toConstructString(nbt.tag(), state));
                }
                codeBuilder.append("    .symbol('").append(symbol).append("', ")
                    .append(Multiblock4DRecipe.toDatagenPredicate(predicate, nbtVariables))
                    .append(")\n");
            });
        }
        codeBuilder.append("    .save(provider);\n");

        StringBuilder result = new StringBuilder();
        nbtDeclarations.forEach(result::append);
        return result.append(codeBuilder).toString();
    }

    private static String toDatagenPredicate(BlockStatePredicate predicate, List<String> nbtVariables) {
        boolean named = predicate.getBlocks() instanceof HolderSet.Named<?>;
        int blockCount = predicate.getBlocks().size();

        if (predicate.getProperties().isEmpty() && nbtVariables.size() <= 1) {
            if (!named && blockCount == 1 && nbtVariables.isEmpty()) {
                return "\"" + Multiblock4DRecipe.firstBlockId(predicate) + "\"";
            }
            if (!named && blockCount == 1) {
                return "\"" + Multiblock4DRecipe.firstBlockId(predicate) + "\", " + nbtVariables.getFirst();
            }
            if (named && nbtVariables.isEmpty()) {
                return "TagKey.create(Registries.BLOCK, ResourceLocation.parse(\""
                    + Multiblock4DRecipe.tagId(predicate) + "\"))";
            }
            if (named) {
                return "TagKey.create(Registries.BLOCK, ResourceLocation.parse(\""
                    + Multiblock4DRecipe.tagId(predicate) + "\")), " + nbtVariables.getFirst();
            }
            if (blockCount == 0 && nbtVariables.size() == 1) {
                return nbtVariables.getFirst();
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
                    .append(Multiblock4DRecipe.tagId(predicate)).append("\"))");
            } else {
                builder.append(Multiblock4DRecipe.blockIds(predicate).stream()
                    .map(id -> "BuiltInRegistries.BLOCK.get(ResourceLocation.parse(\"" + id + "\"))")
                    .collect(Collectors.joining(", ")));
            }
            builder.append(")");
        }
        String blockId = Multiblock4DRecipe.firstBlockId(predicate);
        List<List<BlockStatePredicate.PropertyMatcher>> groups = predicate.getProperties();
        for (int i = 0; i < groups.size(); i++) {
            for (BlockStatePredicate.PropertyMatcher matcher : groups.get(i)) {
                builder.append(Multiblock4DRecipe.propertyCall(matcher, blockId));
            }
            if (i < groups.size() - 1) {
                builder.append(".or()");
            }
        }
        for (String variable : nbtVariables) {
            builder.append(".nbt(").append(variable).append(")");
        }
        return builder.toString();
    }

    private static String tagId(BlockStatePredicate predicate) {
        if (predicate.getBlocks() instanceof HolderSet.Named<?> named) {
            return named.key().location().toString();
        }
        return "";
    }

    @Nullable
    private static String firstBlockId(BlockStatePredicate predicate) {
        return predicate.getBlocks().stream().findFirst()
            .map(holder -> BuiltInRegistries.BLOCK.getKey(holder.value()).toString())
            .orElse(null);
    }

    private static List<String> blockIds(BlockStatePredicate predicate) {
        List<String> ids = new ArrayList<>();
        predicate.getBlocks().forEach(holder -> ids.add(BuiltInRegistries.BLOCK.getKey(holder.value()).toString()));
        return ids;
    }

    private static String propertyCall(BlockStatePredicate.PropertyMatcher matcher, @Nullable String blockId) {
        if (blockId == null) {
            return "";
        }
        String property = "BuiltInRegistries.BLOCK.get(ResourceLocation.parse(\""
            + blockId + "\")).getStateDefinition().getProperty(\"" + matcher.name() + "\")";
        if (matcher.valueMatcher() instanceof BlockStatePredicate.ExactMatcher(String value1)) {
            return ".with(" + property + ", \"" + value1 + "\")";
        }
        if (matcher.valueMatcher() instanceof BlockStatePredicate.RangedMatcher(Optional<String> minValue, Optional<String> maxValue)) {
            return ".with((Property) " + property + ", "
                   + minValue.map(value -> "\"" + value + "\"").orElse("null") + ", "
                   + maxValue.map(value -> "\"" + value + "\"").orElse("null") + ")";
        }
        return "";
    }

    public static class Serializer implements RecipeSerializer<Multiblock4DRecipe> {
        public static final MapCodec<Multiblock4DRecipe> CODEC = CodecUtil.mapCodec(
            MultiblockDefinition.CODEC.codec()
                .listOf()
                .fieldOf("definitions")
                .forGetter(Multiblock4DRecipe::getDefinitions),
            ItemStack.CODEC
                .fieldOf("result")
                .forGetter(Multiblock4DRecipe::getResult),
            Multiblock4DRecipe::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Multiblock4DRecipe> STREAM_CODEC = StreamCodec.composite(
            MultiblockDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()),
            Multiblock4DRecipe::getDefinitions,
            ItemStack.STREAM_CODEC,
            Multiblock4DRecipe::getResult,
            Multiblock4DRecipe::new
        );

        @Override
        public MapCodec<Multiblock4DRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Multiblock4DRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }
}
