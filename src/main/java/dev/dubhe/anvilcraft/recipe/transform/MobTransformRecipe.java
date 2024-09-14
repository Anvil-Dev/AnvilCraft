package dev.dubhe.anvilcraft.recipe.transform;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Utils;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MobTransformRecipe implements Recipe<MobTransformInput> {

    public static final Codec<MobTransformRecipe> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    CodecUtil.ENTITY_CODEC.fieldOf("input").forGetter(o -> o.input),
                    TransformResult.CODEC.listOf().fieldOf("results").forGetter(o -> o.results),
                    NumericTagValuePredicate.CODEC
                            .listOf()
                            .optionalFieldOf("tagPredicates")
                            .forGetter(o -> Utils.intoOptional(o.predicates)),
                    TagModification.CODEC
                            .listOf()
                            .optionalFieldOf("tagModifications")
                            .forGetter(o -> Utils.intoOptional(o.tagModifications)),
                    TransformOptions.CODEC
                            .listOf()
                            .optionalFieldOf("transformOptions")
                            .forGetter(o -> Utils.intoOptional(o.options)))
            .apply(ins, MobTransformRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTransformRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> buf.writeNbt(intoTag(recipe)), friendlyByteBuf -> fromTag(friendlyByteBuf.readNbt()));

    private final EntityType<?> input;
    private final List<TransformResult> results;
    private final List<NumericTagValuePredicate> predicates;
    private final List<TagModification> tagModifications;
    private final List<TransformOptions> options;

    /**
     * 生物转化配方
     */
    public MobTransformRecipe(
            EntityType<?> input,
            List<TransformResult> results,
            Optional<List<NumericTagValuePredicate>> tagPredicates,
            Optional<List<TagModification>> tagModifications,
            Optional<List<TransformOptions>> options) {
        this(
                input,
                results,
                tagPredicates.orElseGet(List::of),
                tagModifications.orElseGet(List::of),
                options.orElseGet(List::of));
    }

    public MobTransformRecipe(
            EntityType<?> input,
            List<TransformResult> results,
            List<NumericTagValuePredicate> predicates,
            List<TagModification> tagModifications,
            List<TransformOptions> options) {
        this.input = input;
        this.results = results;
        this.predicates = predicates;
        this.tagModifications = tagModifications;
        this.options = options;
    }

    @Override
    public boolean matches(MobTransformInput in, Level level) {
        boolean typeMatches = in.getInputEntity().getType() == input;
        if (!typeMatches) return false;
        return predicates.stream().allMatch(it -> it.test(new EntityDataAccessor(in.getInputEntity()).getData()));
    }

    @Override
    public ItemStack assemble(MobTransformInput mobTransformInput, HolderLookup.Provider provider) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MOB_TRANSFORM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MOB_TRANSFORM_TYPE.get();
    }

    private EntityType<?> getResult(RandomSource rand) {
        if (results.size() == 1) return results.getFirst().resultEntityType();
        List<TransformResult> sorted = new ArrayList<>(results.stream()
                .sorted(Comparator.comparingDouble(TransformResult::probability))
                .toList());
        List<Double> probList = new ArrayList<>(List.of(0d));
        for (TransformResult transformResult : sorted) {
            probList.add(probList.getLast() + transformResult.probability());
        }
        double p = rand.nextDouble();
        for (int i = 1; i < probList.size(); i++) {
            double end = probList.get(i);
            double start = probList.get(i - 1);
            if (p >= start && p < end) {
                return sorted.get(i - 1).resultEntityType();
            }
        }
        return sorted.getLast().resultEntityType();
    }

    @Nullable public Entity apply(RandomSource rand, LivingEntity livingEntity, ServerLevel level) {
        EntityType<?> entityType = getResult(rand);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        Entity newEntity = EntityType.loadEntityRecursive(tag, level, (e) -> {
            e.moveTo(
                    livingEntity.position().x,
                    livingEntity.position().y,
                    livingEntity.position().z,
                    e.getYRot(),
                    e.getXRot());
            return e;
        });
        if (newEntity == null) return null;
        if (newEntity instanceof Mob mob) {
            mob.finalizeSpawn(
                    level, level.getCurrentDifficultyAt(newEntity.blockPosition()), MobSpawnType.NATURAL, null);
        }
        for (TransformOptions option : options.stream()
                .sorted(Comparator.comparingInt(TransformOptions::getPriority).reversed())
                .toList()) {
            option.accept(livingEntity, newEntity);
        }
        CompoundTag compoundTag = newEntity.saveWithoutId(new CompoundTag());
        for (TagModification tagModification : tagModifications) {
            tagModification.accept(compoundTag);
        }
        UUID uuid = newEntity.getUUID();
        newEntity.load(compoundTag);
        newEntity.setUUID(uuid);
        return newEntity;
    }

    public static MobTransformRecipe fromTag(Tag tag) {
        return CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow().getFirst();
    }

    public static Tag intoTag(MobTransformRecipe recipe) {
        return CODEC.encodeStart(NbtOps.INSTANCE, recipe).getOrThrow();
    }

    public static TransformRecipeBuilder from(EntityType<?> type) {
        return new TransformRecipeBuilder(type);
    }

    public static final class Serializer implements RecipeSerializer<MobTransformRecipe> {
        public static final MapCodec<MobTransformRecipe> MAP_CODEC =
                RecordCodecBuilder.<MobTransformRecipe>mapCodec(ins -> ins.group(
                                CodecUtil.ENTITY_CODEC.fieldOf("input").forGetter(o -> o.input),
                                TransformResult.CODEC
                                        .listOf()
                                        .fieldOf("results")
                                        .forGetter(o -> o.results),
                                NumericTagValuePredicate.CODEC
                                        .listOf()
                                        .optionalFieldOf("tagPredicates")
                                        .forGetter(o -> Utils.intoOptional(o.predicates)),
                                TagModification.CODEC
                                        .listOf()
                                        .optionalFieldOf("tagModifications")
                                        .forGetter(o -> Utils.intoOptional(o.tagModifications)),
                                TransformOptions.CODEC
                                        .listOf()
                                        .optionalFieldOf("transformOptions")
                                        .forGetter(o -> Utils.intoOptional(o.options)))
                        .apply(ins, MobTransformRecipe::new));

        @Override
        public MapCodec<MobTransformRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MobTransformRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
