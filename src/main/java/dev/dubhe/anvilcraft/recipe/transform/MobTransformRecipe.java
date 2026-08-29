package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

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
    public static final Codec<MobTransformRecipe> CODEC = MobTransformRecipe.MAP_CODEC.codec();
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
    public static final RecipeSerializer<MobTransformRecipe> SERIALIZER = new RecipeSerializer<>(
        MobTransformRecipe.MAP_CODEC,
        MobTransformRecipe.STREAM_CODEC
    );

    @Override
    public boolean matches(MobTransformInput in, Level level) {
        boolean typeMatches = in.inputEntity().getType() == this.input;
        if (!typeMatches) return false;
        return this.predicates().stream().allMatch(it -> it.test(new EntityDataAccessor(in.inputEntity()).getData()));
    }

    @Override
    public ItemStack assemble(MobTransformInput input) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public RecipeType<MobTransformRecipe> getType() {
        return ModRecipeTypes.MOB_TRANSFORM.get();
    }

    @Override
    public RecipeSerializer<MobTransformRecipe> getSerializer() {
        return MobTransformRecipe.SERIALIZER;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
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
        EntityType<?> entityType = this.getResult(rand);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        Entity newEntity = EntityType.loadEntityRecursive(
            tag,
            level,
            EntitySpawnReason.CONVERSION,
            e -> {
                e.copyPosition(livingEntity);
                return e;
            }
        );
        if (newEntity == null) return null;
        if (newEntity instanceof Mob mob) {
            EventHooks.finalizeMobSpawn(
                mob,
                level,
                level.getCurrentDifficultyAt(newEntity.blockPosition()),
                EntitySpawnReason.NATURAL,
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
        RegistryAccess registries = level.registryAccess();
        ProblemReporter.Collector reporter = new ProblemReporter.Collector(newEntity.problemPath());
        TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
        newEntity.saveWithoutId(output);
        tag = new CompoundTag();
        for (TagModification tagModification : this.tagModifications()) {
            tagModification.accept(tag);
        }
        output.store(tag);
        UUID uuid = newEntity.getUUID();
        newEntity.load(TagValueInput.create(reporter, registries, output.buildResult()));
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

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "mob_transform";
    }
}
