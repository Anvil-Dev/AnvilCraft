package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.ItemResourceHandlerCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheOutput;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.IEntityCauldron;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public record RoyalPreferenceOutcome(ChanceItemStack result) implements IRecipeOutcome<RoyalPreferenceOutcome> {
    public static final InWorldRecipeData<Boolean> IS_ROYAL_STEEL_RECIPE = InWorldRecipeData.of(
        AnvilCraft.of("is_royal_steel_recipe"),
        false
    );
    private static final Vec3 INPUT_RANGE = new Vec3(0.75, 0.75, 0.75);
    private static final Vec3 INPUT_OFFSET = new Vec3(0.0, -0.375, 0.0);
    private static final Vec3 OUTPUT_OFFSET = new Vec3(0.0, -0.75, 0.0);

    @Override
    public IRecipeOutcome.Type<RoyalPreferenceOutcome> getType() {
        return ModRecipeOutcomeTypes.ROYAL_PREFERENCE.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        ServerLevel level = context.getLevel();
        Vec3 pos = context.getPos();
        if (!context.get(RoyalPreferenceOutcome.IS_ROYAL_STEEL_RECIPE)) return;

        BlockPos belowPos = BlockPos.containing(pos.x, pos.y - 1, pos.z);

        BlockEntity blockEntity = level.getBlockEntity(belowPos);
        boolean hasRecipeCache = false;
        if (blockEntity instanceof ItemResourceHandlerCache cache) {
            hasRecipeCache = true;
            if (RoyalPreferenceOutcome.hasRoyalPreferred(level, cache.getInput())) {
                this.addBonus(context);
                return;
            }
        }

        ItemResourceHandlerCache entityCauldron = RoyalPreferenceOutcome.findEntityCauldron(level, belowPos);
        if (entityCauldron != null) {
            hasRecipeCache = true;
            if (RoyalPreferenceOutcome.hasRoyalPreferred(level, entityCauldron.getInput())) {
                this.addBonus(context);
                return;
            }
        }

        // 没有配方缓存的容器（如原版容器）退回到直接读能力
        if (!hasRecipeCache) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, belowPos, null);
            if (handler != null && RoyalPreferenceOutcome.hasRoyalPreferred(level, handler)) {
                this.addBonus(context);
                return;
            }
        }

        // 炼药锅场景：扫描掉落物
        Vec3 inputCenter = pos.add(RoyalPreferenceOutcome.INPUT_OFFSET);
        AABB inputBox = new AABB(inputCenter, inputCenter).inflate(
            RoyalPreferenceOutcome.INPUT_RANGE.x, RoyalPreferenceOutcome.INPUT_RANGE.y, RoyalPreferenceOutcome.INPUT_RANGE.z);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, inputBox)) {
            if (RoyalPreference.isRoyalPreferred(level, itemEntity.getItem())) {
                this.addBonus(context);
                return;
            }
        }
    }

    /// 把加倍产物交给配方物品缓存，由缓存决定放进容器还是掉落
    private void addBonus(InWorldRecipeContext context) {
        ItemStack bonus = this.result.stack().create().copyWithCount(context.getInt(this.result.count()));
        if (bonus.isEmpty()) return;
        ItemCache cache = context.computeIfAbsent(ItemCache.ITEM_CACHE);
        ICacheOutput output = cache.getOutput(bonus, context.getPos().add(RoyalPreferenceOutcome.OUTPUT_OFFSET));
        output.grow(bonus, true);
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
    }

    private static boolean hasRoyalPreferred(ServerLevel level, ResourceHandler<ItemResource> handler) {
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
            if (!stack.isEmpty() && RoyalPreference.isRoyalPreferred(level, stack)) return true;
        }
        return false;
    }

    /// 查找占据该位置、可作为配方容器的实体炼药锅
    private static @Nullable ItemResourceHandlerCache findEntityCauldron(ServerLevel level, BlockPos pos) {
        Vec3 center = pos.getCenter();
        Entity closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Entity entity : level.getEntitiesOfClass(
            Entity.class,
            new AABB(pos).inflate(0.0625),
            entity -> entity.isAlive()
                      && entity instanceof IEntityCauldron
                      && entity instanceof ItemResourceHandlerCache
        )) {
            double distance = entity.getBoundingBox().getCenter().distanceToSqr(center);
            if (distance >= closestDistance) continue;
            closest = entity;
            closestDistance = distance;
        }
        return closest instanceof ItemResourceHandlerCache cache ? cache : null;
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
    public static class RoyalPreference {
        static @Nullable Optional<Item> preferredGem;
        static @Nullable Optional<Item> preferredGemBlock;

        public static boolean isRoyalPreferred(ServerLevel level, ItemStack stack) {
            if (RoyalPreference.preferredGem == null) RoyalPreference.initRoyalPreferredGem(() -> new Random(level.getSeed()));
            if (RoyalPreference.preferredGemBlock == null) RoyalPreference.initRoyalPreferredGemBlock(() -> new Random(level.getSeed()));
            return stack.is(RoyalPreference.preferredGem.orElseThrow()) || stack.is(RoyalPreference.preferredGemBlock.orElseThrow());
        }

        public static void initRoyalPreference(long seed) {
            Supplier<Random> randomFactory = () -> new Random(seed);
            RoyalPreference.initRoyalPreferredGem(randomFactory);
            RoyalPreference.initRoyalPreferredGemBlock(randomFactory);
        }

        private static void initRoyalPreferredGem(Supplier<Random> randomFactory) {
            List<Item> gems = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.GEMS)) {
                if (holder.value() != Items.EMERALD) {
                    gems.add(holder.value());
                }
            }
            if (gems.isEmpty()) return;
            Random random = randomFactory.get();
            RoyalPreference.preferredGem = Optional.of(gems.get(random.nextInt(gems.size())));
        }

        private static void initRoyalPreferredGemBlock(Supplier<Random> randomFactory) {
            List<Item> gemBlocks = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.GEM_BLOCKS)) {
                if (holder.value() != Items.EMERALD_BLOCK) {
                    gemBlocks.add(holder.value());
                }
            }
            if (gemBlocks.isEmpty()) return;
            Random random = randomFactory.get();
            RoyalPreference.preferredGemBlock = Optional.of(gemBlocks.get(random.nextInt(gemBlocks.size())));
        }
    }

    public static class Type implements IRecipeOutcome.Type<RoyalPreferenceOutcome> {
        public static final MapCodec<RoyalPreferenceOutcome> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChanceItemStack.CODEC.fieldOf("result").forGetter(RoyalPreferenceOutcome::result)
        ).apply(instance, RoyalPreferenceOutcome::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RoyalPreferenceOutcome> STREAM_CODEC = StreamCodec.composite(
            ChanceItemStack.STREAM_CODEC,
            RoyalPreferenceOutcome::result,
            RoyalPreferenceOutcome::new
        );

        @Override
        public MapCodec<RoyalPreferenceOutcome> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RoyalPreferenceOutcome> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
