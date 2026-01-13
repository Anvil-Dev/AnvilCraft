package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.ListUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public record MobTransformWithItemRecipe(
    EntityType<?> input,
    List<ItemIngredientPredicate> itemIngredients,
    TransformResult specialResult,
    ItemStack itemResult,
    int chancePercentPerItem,
    List<NumericTagValuePredicate> predicates,
    List<TagModification> tagModifications,
    List<TransformOptions> options
) implements Recipe<MobTransformWithItemRecipe.Input> {
    public static final Codec<MobTransformWithItemRecipe> CODEC = Serializer.MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTransformWithItemRecipe> STREAM_CODEC = CodecUtil.composite(
        CodecUtil.ENTITY_STREAM_CODEC,
        MobTransformWithItemRecipe::input,
        ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
        MobTransformWithItemRecipe::itemIngredients,
        TransformResult.STREAM_CODEC,
        MobTransformWithItemRecipe::specialResult,
        ItemStack.STREAM_CODEC,
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

    @Override
    public boolean matches(Input in, Level level) {
        boolean typeMatches = in.getInputEntity().getType() == this.input();
        if (!typeMatches) return false;
        if (!testItem(in.getItem(0))) return false;
        return this.predicates()
            .stream()
            .allMatch(it -> it.test(new EntityDataAccessor(in.getInputEntity()).getData()));
    }

    public boolean testEntity(LivingEntity livingEntity) {
        return livingEntity.getType() == this.input();
    }

    public boolean testItem(ItemStack item) {
        // TODO: 迁移
        AtomicBoolean result = new AtomicBoolean(false);
        ListUtil.safelyGet(this.itemIngredients(), 0).ifPresent(ingredient -> result.set(ingredient.test(item)));
        return result.get();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
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
        return ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get();
    }

    @Nullable
    private EntityType<?> getResult(RandomSource rand, LivingEntity livingEntity) {
        boolean hasTransformItem = this.testItem(livingEntity.getMainHandItem());
        float probability = 0;
        if (hasTransformItem) {
            probability = this.chancePercentPerItem() * 0.01f * livingEntity.getMainHandItem().getCount();
            probability = Math.min(probability, 1f);
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
        EntityType<?> entityType = getResult(rand, livingEntity);
        if (entityType == null) return null;
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
            if (
                option == TransformOptions.REPLACE_ANVIL
                || option == TransformOptions.KEEP_INVENTORY
            ) {
                continue;
            }
            option.accept(livingEntity, newEntity);
        }
        this.setTransformedItem(livingEntity, newEntity);
        CompoundTag compoundTag = newEntity.saveWithoutId(new CompoundTag());
        for (TagModification tagModification : this.tagModifications()) {
            tagModification.accept(compoundTag);
        }
        UUID uuid = newEntity.getUUID();
        newEntity.load(compoundTag);
        newEntity.setUUID(uuid);
        return newEntity;
    }

    public void setTransformedItem(Entity oldEntity, Entity newEntity) {
        if (newEntity instanceof LivingEntity entity && oldEntity instanceof LivingEntity) {
            entity.setItemInHand(
                InteractionHand.MAIN_HAND,
                new ItemStack(itemResult.getItem(), itemResult.getCount())
            );
            if (entity instanceof Mob mob) {
                mob.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
            }
        }
    }

    public static TransformWithItemRecipeBuilder from(
        EntityType<?> type,
        ItemLike itemInput,
        EntityType<?> specialResult,
        ItemStack itemResult
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

    public static final class Serializer implements RecipeSerializer<MobTransformWithItemRecipe> {
        public static final MapCodec<MobTransformWithItemRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            CodecUtil.ENTITY_CODEC.fieldOf("input").forGetter(MobTransformWithItemRecipe::input),
            ItemIngredientPredicate.CODEC.listOf()
                    .optionalFieldOf("ingredients", List.of()).forGetter(MobTransformWithItemRecipe::itemIngredients),
            TransformResult.CODEC.fieldOf("special_result").forGetter(MobTransformWithItemRecipe::specialResult),
            ItemStack.CODEC.fieldOf("item_result").forGetter(MobTransformWithItemRecipe::itemResult),
            Codec.INT.fieldOf("chance_percent_per_item").forGetter(MobTransformWithItemRecipe::chancePercentPerItem),
            NumericTagValuePredicate.CODEC.listOf()
                .optionalFieldOf("tag_predicates", List.of())
                .forGetter(MobTransformWithItemRecipe::predicates),
            TagModification.CODEC.listOf()
                .optionalFieldOf("tag_modifications", List.of())
                .forGetter(MobTransformWithItemRecipe::tagModifications),
            TransformOptions.CODEC.listOf().optionalFieldOf("transform_options", List.of()).forGetter(MobTransformWithItemRecipe::options)
        ).apply(ins, MobTransformWithItemRecipe::new));

        @Override
        public MapCodec<MobTransformWithItemRecipe> codec() {
            return Serializer.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MobTransformWithItemRecipe> streamCodec() {
            return MobTransformWithItemRecipe.STREAM_CODEC;
        }
    }
}
