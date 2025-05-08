package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.SqueezingRecipe;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.dubhe.anvilcraft.util.AnvilUtil.dropItems;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilEventListener {

    private static boolean behaviorRegistered = false;

    /**
     * 侦听铁砧落地事件
     *
     * @param event 铁砧落地事件
     */
    @SubscribeEvent
    public static void onLand(@NotNull AnvilFallOnLandEvent event) {
        if (!behaviorRegistered) {
            IAnvilBehavior.register();
            behaviorRegistered = true;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        MinecraftServer server = level.getServer();
        if (null == server) return;
        final BlockPos hitBlockPos = pos.below();
        final BlockState hitBlockState = level.getBlockState(hitBlockPos);
        BlockPos belowPos = hitBlockPos.below();
        BlockState hitBelowState = level.getBlockState(belowPos);
        if (hitBelowState.is(Blocks.STONECUTTER)) {
            brokeBlock(level, hitBlockPos, event);
            return;
        }

        handleBlockCompressRecipe(level, hitBlockPos);
        handleBlockCrushRecipe(level, hitBlockPos);
        handleItemInjectRecipe(level, hitBlockPos, hitBlockState);
        handleSqueezingRecipe(level, hitBlockPos, hitBlockState);


        for (IAnvilBehavior behavior : IAnvilBehavior.findMatching(hitBlockState)) {
            if (behavior.handle(level, hitBlockPos, hitBlockState, event.getFallDistance(), event)) {
                return;
            }
        }
    }

    private static void handleBlockCrushRecipe(Level level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.getRecipeManager()
            .getRecipeFor(
                ModRecipeTypes.BLOCK_CRUSH_TYPE.get(), new BlockCrushRecipe.Input(state.getBlock()), level)
            .ifPresent(recipe ->
                level.setBlockAndUpdate(pos, recipe.value().result.defaultBlockState()));
    }

    private static void handleBlockCompressRecipe(Level level, final BlockPos pos) {
        List<Block> inputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            inputs.add(level.getBlockState(pos.below(i)).getBlock());
        }
        level.getRecipeManager()
            .getRecipeFor(ModRecipeTypes.BLOCK_COMPRESS_TYPE.get(), new BlockCompressRecipe.Input(inputs), level)
            .ifPresent(recipe -> {
                for (int i = 0; i < recipe.value().inputs.size(); i++) {
                    level.setBlockAndUpdate(pos.below(i), Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(
                    pos.below(recipe.value().inputs.size() - 1),
                    recipe.value().result.defaultBlockState());
            });
    }

    private static void handleItemInjectRecipe(Level level, final BlockPos pos, BlockState state) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.above())).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (items.isEmpty()) return;
        ItemInjectRecipe.Input input =
            new ItemInjectRecipe.Input(items.values().stream().toList(), state.getBlock());
        level.getRecipeManager()
            .getRecipeFor(ModRecipeTypes.ITEM_INJECT_TYPE.get(), input, level)
            .ifPresent(recipe -> {
                for (Ingredient ingredient : recipe.value().getIngredients()) {
                    for (ItemStack stack : input.items()) {
                        if (ingredient.test(stack)) {
                            stack.shrink(1);
                            break;
                        }
                    }
                }
                level.setBlockAndUpdate(pos, recipe.value().resultBlock.defaultBlockState());
                items.forEach((k, v) -> {
                    if (v.isEmpty()) {
                        k.discard();
                        return;
                    }
                    k.setItem(v.copy());
                });
            });
    }

    private static void handleSqueezingRecipe(Level level, final BlockPos pos, BlockState state) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (!(belowState.getBlock() instanceof AbstractCauldronBlock)) return;
        SqueezingRecipe.Input input = new SqueezingRecipe.Input(state.getBlock(), belowState);
        level.getRecipeManager()
            .getRecipeFor(ModRecipeTypes.SQUEEZING_TYPE.get(), input, level)
            .map(RecipeHolder::value)
            .ifPresent(recipe -> {
                CauldronUtil.fill(level, belowPos, recipe.getCauldron(), 1, false);
                level.setBlockAndUpdate(pos, recipe.resultBlock.defaultBlockState());
            });
    }

    private static void brokeBlock(@NotNull Level level, BlockPos pos, AnvilFallOnLandEvent event) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock().getExplosionResistance() >= 1200.0) event.setAnvilDamage(true);
        if (state.getDestroySpeed(level, pos) < 0) return;
        boolean smeltDrop = Optional.ofNullable(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof EmberAnvilBlock)
            .orElse(false);
        boolean silkTouch = Optional.ofNullable(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof RoyalAnvilBlock)
            .orElse(false);
        ItemStack dummyTool = silkTouch ? BreakBlockUtil.getDummySilkTouchTool(serverLevel) : ItemStack.EMPTY;
        state.spawnAfterBreak(serverLevel, pos, dummyTool, false);
        if (state.getBlock() instanceof IHasMultiBlock multiBlock) {
            multiBlock.onRemove(level, pos, state);
        }
        List<ItemStack> drops;
        if (smeltDrop) {
            drops = BreakBlockUtil.dropSmelt(serverLevel, pos);
        } else if (silkTouch) {
            drops = BreakBlockUtil.dropSilkTouch(serverLevel, pos);
        } else {
            drops = BreakBlockUtil.drop(serverLevel, pos);
        }
        dropItems(drops, level, pos.getCenter());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    /**
     * 侦听铁砧伤害实体事件
     *
     * @param event 铁砧伤害实体事件
     */
    @SubscribeEvent
    public static void onAnvilHurtEntity(@NotNull AnvilHurtEntityEvent event) {
        Entity hurtedEntity = event.getHurtedEntity();
        if (!(hurtedEntity instanceof LivingEntity entity)) return;
        if (!(hurtedEntity.level() instanceof ServerLevel serverLevel)) return;
        float damage = event.getDamage();
        float maxHealth = entity.getMaxHealth();
        double rate = damage / maxHealth;
        if (rate < 0.4) return;
        FallingBlockEntity eventEntity = event.getEntity();
        DamageSource source = entity.level().damageSources().anvil(eventEntity);
        LootParams.Builder builder = new LootParams.Builder(serverLevel);
        builder.withParameter(LootContextParams.DAMAGE_SOURCE, source);
        builder.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, eventEntity);
        builder.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, eventEntity);
        builder.withParameter(LootContextParams.THIS_ENTITY, entity);
        Vec3 pos = entity.position();
        builder.withParameter(LootContextParams.ORIGIN, pos);
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(entity.getLootTable());
        dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.6) dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.8) dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
    }
}
