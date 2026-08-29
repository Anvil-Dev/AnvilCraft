package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record MobTransformWithItemRecipe(
    EntityType<?> input,
    List<ItemIngredientPredicate> itemIngredients,
    TransformResult specialResult,
    ItemStackTemplate itemResult,
    int chancePercentPerItem,
    List<NumericTagValuePredicate> predicates,
    List<TagModification> tagModifications,
    List<TransformOptions> options
) implements Recipe<MobTransformWithItemRecipe.Input> {
    public static final MapCodec<MobTransformWithItemRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        CodecUtil.ENTITY
            .fieldOf("input")
            .forGetter(MobTransformWithItemRecipe::input),
        ItemIngredientPredicate.CODEC
            .listOf()
            .optionalFieldOf("ingredients", List.of())
            .forGetter(MobTransformWithItemRecipe::itemIngredients),
        TransformResult.CODEC
            .fieldOf("special_result")
            .forGetter(MobTransformWithItemRecipe::specialResult),
        ItemStackTemplate.CODEC
            .fieldOf("item_result")
            .forGetter(MobTransformWithItemRecipe::itemResult),
        Codec.INT
            .fieldOf("chance_percent_per_item")
            .forGetter(MobTransformWithItemRecipe::chancePercentPerItem),
        NumericTagValuePredicate.CODEC
            .listOf()
            .optionalFieldOf("tag_predicates", List.of())
            .forGetter(MobTransformWithItemRecipe::predicates),
        TagModification.CODEC
            .listOf()
            .optionalFieldOf("tag_modifications", List.of())
            .forGetter(MobTransformWithItemRecipe::tagModifications),
        TransformOptions.CODEC
            .listOf()
            .optionalFieldOf("transform_options", List.of())
            .forGetter(MobTransformWithItemRecipe::options)
    ).apply(ins, MobTransformWithItemRecipe::new));
    public static final Codec<MobTransformWithItemRecipe> CODEC = MobTransformWithItemRecipe.MAP_CODEC.codec();
    public static final StreamCodec<RegistryFriendlyByteBuf, MobTransformWithItemRecipe> STREAM_CODEC = StreamCodecUtil.composite(
        StreamCodecUtil.ENTITY,
        MobTransformWithItemRecipe::input,
        ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformWithItemRecipe::itemIngredients,
        TransformResult.STREAM_CODEC,
        MobTransformWithItemRecipe::specialResult,
        ItemStackTemplate.STREAM_CODEC,
        MobTransformWithItemRecipe::itemResult,
        ByteBufCodecs.INT,
        MobTransformWithItemRecipe::chancePercentPerItem,
        NumericTagValuePredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformWithItemRecipe::predicates,
        TagModification.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformWithItemRecipe::tagModifications,
        TransformOptions.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformWithItemRecipe::options,
        MobTransformWithItemRecipe::new
    );
    public static final RecipeSerializer<MobTransformWithItemRecipe> SERIALIZER = new RecipeSerializer<>(
        MobTransformWithItemRecipe.MAP_CODEC,
        MobTransformWithItemRecipe.STREAM_CODEC
    );

    @Override
    public boolean matches(Input in, Level level) {
        boolean typeMatches = in.getInputEntity().getType() == this.input();
        if (!typeMatches) return false;
        if (!this.testItem(in.getItem(0))) return false;
        return this.predicates()
            .stream()
            .allMatch(it -> it.test(new EntityDataAccessor(in.getInputEntity()).getData()));
    }

    public boolean testEntity(LivingEntity livingEntity) {
        return livingEntity.getType() == this.input();
    }

    public boolean testItem(ItemStack item) {
        return !this.itemIngredients().isEmpty() && this.itemIngredients().getFirst().test(item);
    }

    @Override
    public ItemStack assemble(Input input) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public RecipeType<MobTransformWithItemRecipe> getType() {
        return ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<MobTransformWithItemRecipe> getSerializer() {
        return MobTransformWithItemRecipe.SERIALIZER;
    }

    @Nullable
    private EntityType<?> getResult(RandomSource rand, LivingEntity livingEntity) {
        boolean hasTransformItem = this.testItem(livingEntity.getMainHandItem());
        float probability = 0;
        if (hasTransformItem) {
            probability = this.chancePercentPerItem() * 0.01F * livingEntity.getMainHandItem().getCount();
            probability = Math.min(probability, 1F);
        }
        float r = rand.nextFloat();
        if (hasTransformItem && r <= probability) {
            return this.specialResult().resultEntityType();
        } else {
            return null;
        }
    }

    @Nullable
    public Entity apply(RandomSource rand, LivingEntity livingEntity, ServerLevel level) {
        EntityType<?> entityType = this.getResult(rand, livingEntity);
        if (entityType == null) return null;
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
            if (
                option == TransformOptions.REPLACE_ANVIL
                || option == TransformOptions.KEEP_INVENTORY
            ) {
                continue;
            }
            option.accept(livingEntity, newEntity);
        }
        this.setTransformedItem(livingEntity, newEntity);
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

    public void setTransformedItem(Entity oldEntity, Entity newEntity) {
        if (newEntity instanceof LivingEntity entity && oldEntity instanceof LivingEntity) {
            entity.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(this.itemResult.item(), this.itemResult.count())
            );
            if (entity instanceof Mob mob) {
                mob.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
            }
        }
    }

    public static TransformWithItemRecipeBuilder from(
        EntityType<?> type,
        ItemLike itemInput,
        EntityType<?> specialResult,
        ItemStackTemplate itemResult
    ) {
        ItemIngredientPredicate item = ItemIngredientPredicate.Builder.item()
            .of(itemInput)
            .build();
        return new TransformWithItemRecipeBuilder(type, Collections.singletonList(item), specialResult, itemResult);
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
        return "mob_transform_with_item";
    }

    public record Input(LivingEntity inputEntity) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return this.inputEntity().getMainHandItem();
        }

        public LivingEntity getInputEntity() {
            return this.inputEntity();
        }

        @Override
        public int size() {
            return 1;
        }

        public static Input of(LivingEntity livingEntity) {
            return new Input(livingEntity);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}
