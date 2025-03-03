package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModDamageTypes;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TimeWarpBehavior implements IAnvilBehavior {
    public static final int SOUL_PARTICLE_COUNT = 3;

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event) {
        BlockState belowState = level.getBlockState(hitBlockPos.below());
        if (!belowState.is(ModBlocks.CORRUPTED_BEACON) || !belowState.getValue(CorruptedBeaconBlock.LIT)) return false;

        List<LivingEntity> damagedEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(hitBlockPos),
            LivingEntity::isAlive);
        if (!damagedEntities.isEmpty()) {
            damagedEntities.forEach(it -> it.hurt(ModDamageTypes.lostInTime(level), Float.MAX_VALUE));
            if (level instanceof ServerLevel serverLevel && damagedEntities.stream().anyMatch(LivingEntity::isDeadOrDying)) {
                Vec3 particleCenter = hitBlockPos.above().getCenter();
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    particleCenter.x,
                    particleCenter.y,
                    particleCenter.z,
                    SOUL_PARTICLE_COUNT,
                    0.4, 0.4, 0.4, 0.01);
            }
        }

        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos))
            .stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (items.values().stream().anyMatch(it -> it.is(ModBlocks.RESIN_BLOCK.asItem()))) {
            items.entrySet().stream()
                .filter(it -> it.getValue().is(ModBlocks.RESIN_BLOCK.asItem()))
                .map(it -> {
                    ItemStack itemStack = it.getValue();
                    Entity entity = HasMobBlockItem.getMobFromItem(level, itemStack);
                    if (entity == null) {
                        return Map.entry(it.getKey(), itemStack.transmuteCopy(ModBlocks.AMBER_BLOCK));
                    }
                    ItemLike amberBlock = entity.getType().getCategory() == MobCategory.MONSTER
                        && level.getRandom().nextFloat() <= 0.05 ? ModBlocks.RESENTFUL_AMBER_BLOCK
                        : ModBlocks.MOB_AMBER_BLOCK;
                    return Map.entry(it.getKey(), itemStack.transmuteCopy(amberBlock));
                })
                .forEach(it -> {
                    ItemEntity old = it.getKey();
                    old.discard();
                    AnvilUtil.dropItems(List.of(it.getValue()), level, old.blockPosition().getCenter());
                });
            return true;
        }
        TimeWarpRecipe.Input input = new TimeWarpRecipe.Input(items.values().stream().toList(), hitBlockState);
        Optional<TimeWarpRecipe> recipeOptional = level.getRecipeManager()
            .getRecipesFor(ModRecipeTypes.TIME_WARP_TYPE.get(), input, level)
            .stream()
            .map(RecipeHolder::value)
            .max(Comparator.comparingInt(recipe -> recipe.getIngredients().size()));
        if (recipeOptional.isEmpty()) return false;
        TimeWarpRecipe recipe = recipeOptional.get();
        int times = recipe.getMaxCraftTime(input);
        Object2IntMap<Item> results = new Object2IntOpenHashMap<>();
        LootContext context;
        if (level instanceof ServerLevel serverLevel) {
            context = RecipeUtil.emptyLootContext(serverLevel);
        } else {
            return false;
        }
        for (int i = 0; i < times; i++) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack stack : items.values()) {
                    if (ingredient.test(stack)) {
                        stack.shrink(1);
                        break;
                    }
                }
            }
            for (ChanceItemStack stack : recipe.getResults()) {
                int amount = stack.getStack().getCount() * stack.getAmount().getInt(context);
                results.mergeInt(stack.getStack().getItem(), amount, Integer::sum);
            }
        }
        AnvilUtil.dropItems(
            results.object2IntEntrySet().stream()
                .map(entry -> new ItemStack(entry.getKey(), entry.getIntValue()))
                .toList(),
            level,
            hitBlockPos.getCenter()
        );
        if (recipe.isConsumeFluid()) {
            CauldronUtil.drain(level, hitBlockPos, recipe.getCauldron(), recipe.getRequiredFluidLevel(), false);
        }
        if (recipe.isProduceFluid()) {
            CauldronUtil.fill(level, hitBlockPos, recipe.getCauldron(), 1, false);
        }
        items.forEach((k, v) -> {
            if (v.isEmpty()) {
                k.discard();
                return;
            }
            k.setItem(v.copy());
        });
        return true;
    }
}
