package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public class MobTransformWithItemRecipe implements Recipe<MobTransformWithItemRecipe.Input> {

    public static final Codec<MobTransformWithItemRecipe> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            CodecUtil.ENTITY_CODEC.fieldOf("input").forGetter(o -> o.input),
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(MobTransformWithItemRecipe::getItemIngredients),
            TransformResult.CODEC.fieldOf("specialResult").forGetter(o -> o.specialResult),
            ItemStack.CODEC.fieldOf("itemResult").forGetter(o -> o.itemResult),
            Codec.INT.fieldOf("chancePercentPerItem").forGetter(o -> o.chancePercentPerItem),
            NumericTagValuePredicate.CODEC
                .listOf()
                .optionalFieldOf("tagPredicates")
                .forGetter(o -> Util.intoOptional(o.predicates)),
            TagModification.CODEC
                .listOf()
                .optionalFieldOf("tagModifications")
                .forGetter(o -> Util.intoOptional(o.tagModifications)),
            TransformOptions.CODEC
                .listOf()
                .optionalFieldOf("transformOptions")
                .forGetter(o -> Util.intoOptional(o.options)))
        .apply(ins, MobTransformWithItemRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTransformWithItemRecipe> STREAM_CODEC = StreamCodec.of(
        MobTransformWithItemRecipe::intoTag, MobTransformWithItemRecipe::fromTag);

    public final EntityType<?> input;
    public final List<ItemIngredientPredicate> itemIngredients;
    public final TransformResult specialResult;
    public final ItemStack itemResult;
    public final int chancePercentPerItem;
    private final List<NumericTagValuePredicate> predicates;
    private final List<TagModification> tagModifications;
    private final List<TransformOptions> options;

    public MobTransformWithItemRecipe(
        EntityType<?> input,
        List<ItemIngredientPredicate> itemIngredients,
        TransformResult specialResult,
        ItemStack itemResult,
        int chancePercentPerItem,
        Optional<List<NumericTagValuePredicate>> tagPredicates,
        Optional<List<TagModification>> tagModifications,
        Optional<List<TransformOptions>> options) {
        this(
            input,
            itemIngredients,
            specialResult,
            itemResult,
            chancePercentPerItem,
            tagPredicates.orElseGet(List::of),
            tagModifications.orElseGet(List::of),
            options.orElseGet(List::of));
    }

    public MobTransformWithItemRecipe(
        EntityType<?> input,
        List<ItemIngredientPredicate> itemIngredients,
        TransformResult specialResult,
        ItemStack itemResult,
        int chancePercentPerItem,
        List<NumericTagValuePredicate> predicates,
        List<TagModification> tagModifications,
        List<TransformOptions> options) {
        this.input = input;
        this.itemIngredients = itemIngredients;
        this.specialResult = specialResult;
        this.itemResult = itemResult;
        this.chancePercentPerItem = chancePercentPerItem;
        this.predicates = predicates;
        this.tagModifications = tagModifications;
        this.options = options;
    }

    @Override
    public boolean matches(Input in, @NotNull Level level) {
        boolean typeMatches = in.getInputEntity().getType() == input;
        if (!typeMatches) return false;
        if (!testItem(in.getItem(0))) return false;
        return predicates.stream().allMatch(it -> it.test(new EntityDataAccessor(in.getInputEntity()).getData()));
    }

    public boolean testEntity(LivingEntity livingEntity) {
        return livingEntity.getType() == this.input;
    }

    public boolean testItem(ItemStack item) {
        // TODO: 迁移
        return itemIngredients.getFirst().test(item);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Input input, HolderLookup.@NotNull Provider provider) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider provider) {
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get();
    }

    @Nullable
    private EntityType<?> getResult(RandomSource rand, LivingEntity livingEntity) {
        boolean hasTransformItem = this.testItem(livingEntity.getMainHandItem());
        float probability = 0;
        if (hasTransformItem) {
            probability = chancePercentPerItem * 0.01f * livingEntity.getMainHandItem().getCount();
            probability = Math.min(probability, 1f);
        }
        float r = rand.nextFloat();
        if (hasTransformItem && r <= probability) {
            return specialResult.resultEntityType();
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
                level,
                level.getCurrentDifficultyAt(newEntity.blockPosition()),
                MobSpawnType.NATURAL,
                null
            );
        }
        for (TransformOptions option : options.stream()
            .sorted(Comparator.comparingInt(TransformOptions::getPriority).reversed())
            .toList()
        ) {
            if (option == TransformOptions.REPLACE_ANVIL
                || option == TransformOptions.KEEP_INVENTORY)
                continue;
            option.accept(livingEntity, newEntity);
        }
        setTransformedItem(livingEntity, newEntity);
        CompoundTag compoundTag = newEntity.saveWithoutId(new CompoundTag());
        for (TagModification tagModification : tagModifications) {
            tagModification.accept(compoundTag);
        }
        UUID uuid = newEntity.getUUID();
        newEntity.load(compoundTag);
        newEntity.setUUID(uuid);
        return newEntity;
    }

    public void setTransformedItem(Entity oldEntity, Entity newEntity) {
        if (newEntity instanceof LivingEntity n && oldEntity instanceof LivingEntity) {
            n.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(itemResult.getItem(), itemResult.getCount()));
            if (n instanceof Mob mob) {
                mob.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
            }
        }
    }

    public static MobTransformWithItemRecipe fromTag(RegistryFriendlyByteBuf buf) {
        return CODEC.decode(buf.registryAccess().createSerializationContext(NbtOps.INSTANCE), buf.readNbt()).getOrThrow().getFirst();
    }

    public static void intoTag(RegistryFriendlyByteBuf buf, MobTransformWithItemRecipe recipe) {
        buf.writeNbt(CODEC.encodeStart(buf.registryAccess().createSerializationContext(NbtOps.INSTANCE), recipe).getOrThrow());
    }

    public static TransformWithItemRecipeBuilder from(
        EntityType<?> type,
        ItemLike itemInput,
        EntityType<?> specialResult,
        ItemStack itemResult) {
        ItemIngredientPredicate item = ItemIngredientPredicate.Builder.item().of(itemInput).build();
        return new TransformWithItemRecipeBuilder(type, Collections.singletonList(item), specialResult, itemResult);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(LivingEntity inputEntity) implements RecipeInput {
        @Override
        public @NotNull ItemStack getItem(int i) {
            return inputEntity.getMainHandItem();
        }

        public LivingEntity getInputEntity() {
            return this.inputEntity;
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
        public static final MapCodec<MobTransformWithItemRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(ins -> ins.group(
                    CodecUtil.ENTITY_CODEC.fieldOf("input").forGetter(o -> o.input),
                    ItemIngredientPredicate.CODEC.listOf()
                        .optionalFieldOf("ingredients", List.of())
                        .forGetter(MobTransformWithItemRecipe::getItemIngredients),
                    TransformResult.CODEC.fieldOf("specialResult").forGetter(o -> o.specialResult),
                    ItemStack.CODEC.fieldOf("itemResult").forGetter(o -> o.itemResult),
                    Codec.INT.fieldOf("chancePercentPerItem").forGetter(o -> o.chancePercentPerItem),
                    NumericTagValuePredicate.CODEC
                        .listOf()
                        .optionalFieldOf("tagPredicates")
                        .forGetter(o -> Util.intoOptional(o.predicates)),
                    TagModification.CODEC
                        .listOf()
                        .optionalFieldOf("tagModifications")
                        .forGetter(o -> Util.intoOptional(o.tagModifications)),
                    TransformOptions.CODEC
                        .listOf()
                        .optionalFieldOf("transformOptions")
                        .forGetter(o -> Util.intoOptional(o.options)))
                .apply(ins, MobTransformWithItemRecipe::new));

        @Override
        public @NotNull MapCodec<MobTransformWithItemRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, MobTransformWithItemRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }


}
