package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record MobTransformRecipe(
    EntityType<?> input,
    List<TransformResult> results,
    List<NumericTagValuePredicate> predicates,
    List<TagModification> tagModifications,
    List<TransformOptions> options
) implements Recipe<MobTransformInput> {
    public static final Codec<MobTransformRecipe> CODEC = Serializer.MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTransformRecipe> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.ENTITY,
        MobTransformRecipe::input,
        TransformResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformRecipe::results,
        NumericTagValuePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformRecipe::predicates,
        TagModification.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformRecipe::tagModifications,
        TransformOptions.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformRecipe::options,
        MobTransformRecipe::new
    );

    @Override
    public boolean matches(MobTransformInput in, Level level) {
        boolean typeMatches = in.inputEntity().getType() == input;
        if (!typeMatches) return false;
        return this.predicates().stream().allMatch(it -> it.test(new EntityDataAccessor(in.inputEntity()).getData()));
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
        if (this.results().size() == 1) return this.results().getFirst().resultEntityType();
        List<TransformResult> sorted = new ArrayList<>(this.results()
                .stream()
                .sorted(Comparator.comparingDouble(TransformResult::probability))
                .toList()
        );
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

    @Nullable
    public Entity apply(RandomSource rand, LivingEntity livingEntity, ServerLevel level) {
        EntityType<?> entityType = getResult(rand);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        Entity newEntity = EntityType.loadEntityRecursive(
            tag, level, (e) -> {
                e.moveTo(
                    livingEntity.position().x,
                    livingEntity.position().y,
                    livingEntity.position().z,
                    e.getYRot(),
                    e.getXRot()
                );
                return e;
            }
        );
        if (newEntity == null) return null;
        if (newEntity instanceof Mob mob) {
            // noinspection deprecation,OverrideOnly
            mob.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(newEntity.blockPosition()),
                MobSpawnType.NATURAL,
                null
            );
        }
        for (TransformOptions option : this.options()
            .stream()
            .sorted(Comparator.comparingInt(TransformOptions::getPriority).reversed())
            .toList()
        ) {
            option.accept(livingEntity, newEntity);
        }
        CompoundTag compoundTag = newEntity.saveWithoutId(new CompoundTag());
        for (TagModification tagModification : this.tagModifications()) {
            tagModification.accept(compoundTag);
        }
        UUID uuid = newEntity.getUUID();
        newEntity.load(compoundTag);
        newEntity.setUUID(uuid);
        return newEntity;
    }

    public static TransformRecipeBuilder from(EntityType<?> type) {
        return new TransformRecipeBuilder(type);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<MobTransformRecipe> {
        public static final MapCodec<MobTransformRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            CodecUtil.ENTITY
                .fieldOf("input")
                .forGetter(MobTransformRecipe::input),
            TransformResult.CODEC
                .listOf()
                .fieldOf("results")
                .forGetter(MobTransformRecipe::results),
            NumericTagValuePredicate.CODEC
                .listOf()
                .optionalFieldOf("tag_predicates", List.of())
                .forGetter(MobTransformRecipe::predicates),
            TagModification.CODEC
                .listOf()
                .optionalFieldOf("tag_modifications", List.of())
                .forGetter(MobTransformRecipe::tagModifications),
            TransformOptions.CODEC
                .listOf()
                .optionalFieldOf("transform_options", List.of())
                .forGetter(MobTransformRecipe::options)
        ).apply(ins, MobTransformRecipe::new));

        @Override
        public MapCodec<MobTransformRecipe> codec() {
            return Serializer.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MobTransformRecipe> streamCodec() {
            return MobTransformRecipe.STREAM_CODEC;
        }
    }
}
