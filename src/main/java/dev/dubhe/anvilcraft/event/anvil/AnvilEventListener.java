package dev.dubhe.anvilcraft.event.anvil;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.workstation.NeoforgeBlock;
import dev.dubhe.anvilcraft.block.workstation.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.ember.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.frost.FrostAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.royal.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilEventListener {

    private static boolean behaviorRegistered = false;

    /// 侦听铁砧落地事件
    ///
    /// @param event 铁砧落地事件
    @SubscribeEvent
    public static void onLand(AnvilEvent.OnLand event) {
        if (!behaviorRegistered) {
            IAnvilBehavior.register();
            behaviorRegistered = true;
        }
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        final BlockState blockState = level.getBlockState(pos);
        TriggerUtil.anvilOnGround(level, pos);
        final BlockPos hitBlockPos = pos.below();
        final BlockState hitBlockState = level.getBlockState(hitBlockPos);
        BlockPos belowPos = hitBlockPos.below();
        BlockState hitBelowState = level.getBlockState(belowPos);
        if (hitBelowState.is(Blocks.STONECUTTER)) {
            brokeBlock(level, hitBlockPos, event);
            return;
        }
        handleNeoAnvilRecipe(event);
        for (IAnvilBehavior behavior : IAnvilBehavior.findMatching(hitBlockState)) {
            if (behavior.handle(level, hitBlockPos, hitBlockState, event.getFallDistance(), event)) {
                return;
            }
        }
        if (blockState.is(ModBlocks.NEOFORGE)) {
            if (event.getFallDistance() > 1) {
                if (level.getRandom().nextDouble() < 0.01) {
                    NeoforgeBlock.damage(level, pos);
                }
            }
        }
    }

    public static void handleNeoAnvilRecipe(AnvilEvent.OnLand event) {
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        FallingBlockEntity entity = event.getEntity();
        InWorldRecipeManager manager = level.recipeAccess().anvillib$getInWorldRecipeManager();
        InWorldRecipeContext context = new InWorldRecipeContext(level, pos.getCenter().subtract(0.0, 0.5, 0.0), entity);
        manager.trigger(ModRecipeTriggers.ON_ANVIL_FALL_ON, context);
        boolean damageAnvil = context.get(DamageAnvil.DAMAGE_ANVIL);
        if (!event.isAnvilDamage()) event.setAnvilDamage(damageAnvil);
        context.accept();
    }

    private static void brokeBlock(Level level, BlockPos pos, AnvilEvent.OnLand event) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockState state = level.getBlockState(pos);
        // noinspection deprecation
        if (state.getBlock().getExplosionResistance() >= 1200.0) event.setAnvilDamage(true);
        if (state.getDestroySpeed(level, pos) < 0) return;

        if (// noDropsButExp
            Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b1 -> b1.getBlock() instanceof FrostAnvilBlock)
            .orElse(false)
        ) {
            ServerPlayer destroyer = AnvilCraftFakePlayers.getDestroyer().offerPlayer(serverLevel);
            ItemStack dummyTool = BreakBlockUtil.getDummyDisintegrationTool(serverLevel);
            AnvilCraftFakePlayers.getDestroyer().enabledDestroy(destroyer, dummyTool);
            ExperienceOrb.award(
                serverLevel,
                pos.getCenter(),
                EnchantmentHelper.processBlockExperience(
                    serverLevel,
                    dummyTool,
                    state.getExpDrop(level, pos, level.getBlockEntity(pos), destroyer, dummyTool)
                )
            );
            state.spawnAfterBreak(serverLevel, pos, dummyTool, true);
            if (state.getBlock() instanceof IHasMultiBlock multiBlock) {
                multiBlock.onRemove(level, pos, state);
            }
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            AnvilCraftFakePlayers.getDestroyer().disable(destroyer);
            return;
        }
        final boolean smeltDrop = Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof EmberAnvilBlock)
            .orElse(false);
        final boolean silkTouch = Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof RoyalAnvilBlock)
            .orElse(false);
        final boolean fortune5 = Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof TranscendenceAnvilBlock)
            .orElse(false);

        ItemStack dummyTool;
        if (silkTouch) {
            dummyTool = BreakBlockUtil.getDummySilkTouchTool(serverLevel);
        } else if (fortune5) {
            dummyTool = BreakBlockUtil.getDummyFortune5Tool(serverLevel);
        } else {
            dummyTool = ItemStack.EMPTY;
        }
        state.spawnAfterBreak(serverLevel, pos, dummyTool, false);
        if (state.getBlock() instanceof IHasMultiBlock multiBlock) {
            multiBlock.onRemove(level, pos, state);
        }

        List<ItemStack> drops;
        if (smeltDrop) {
            drops = BreakBlockUtil.dropSmelt(serverLevel, pos);
        } else if (silkTouch) {
            drops = BreakBlockUtil.dropSilkTouch(serverLevel, pos);
        } else if (fortune5) {
            drops = BreakBlockUtil.dropFortune5(serverLevel, pos);
        } else {
            drops = BreakBlockUtil.drop(serverLevel, pos);
        }
        AnvilUtil.dropItems(drops, level, pos.getCenter());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    /// 侦听铁砧伤害实体事件
    ///
    /// @param event 铁砧伤害实体事件
    @SubscribeEvent
    public static void onAnvilHurtEntity(AnvilEvent.HurtEntity event) {
        Entity hurtedEntity = event.getHurtedEntity();
        if (!(hurtedEntity instanceof LivingEntity entity) || entity.isBaby()) return;
        if (!(hurtedEntity.level() instanceof ServerLevel serverLevel)) return;
        if (!entity.isAlive()) return;
        if (entity.isDeadOrDying()) return;
        if (entity.hurtTime > 0) return;
        float damage = event.getDamage();
        float maxHealth = entity.getMaxHealth();
        double rate = damage / maxHealth;
        if (rate < 0.4) return;
        FallingBlockEntity eventEntity = event.getEntity();
        DamageSource source = entity.level().damageSources().anvil(eventEntity);
        if (entity.isInvulnerableTo(event.getLevel(), source)) return;
        Vec3 pos = entity.position();
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, eventEntity)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, eventEntity)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, pos);
        Block anvil = eventEntity.getBlockState().getBlock();
        switch (anvil) {
            case FrostAnvilBlock ignored -> AnvilEventListener.dropExps(
                serverLevel,
                entity,
                pos,
                rate
            );
            case EmberAnvilBlock ignored -> AnvilEventListener.dropItems(
                serverLevel,
                entity,
                pos,
                rate,
                builder,
                true,
                false
            );
            case TranscendenceAnvilBlock ignored -> AnvilEventListener.dropItems(
                serverLevel,
                entity,
                pos,
                rate,
                builder,
                true,
                true
            );
            default -> AnvilEventListener.dropItems(
                serverLevel,
                entity,
                pos,
                rate,
                builder,
                false,
                false
            );
        }
        TriggerUtil.anvilLooting(serverLevel, BlockPos.containing(pos), entity);
    }

    private static void dropItems(
        ServerLevel level,
        LivingEntity entity,
        Vec3 pos,
        double rate,
        LootParams.Builder builder,
        boolean enableKiller,
        boolean enableLooting5
    ) {
        Optional<ServerPlayer> killerOp = Optional.empty();
        if (enableKiller) {
            ServerPlayer killer = AnvilCraftFakePlayers.getKiller().offerPlayer(level);
            builder.withParameter(LootContextParams.DAMAGE_SOURCE, entity.level().damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
            if (enableLooting5) AnvilCraftFakePlayers.getKiller().enableLooting5(level, killer);
            killerOp = Optional.of(killer);
        }
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        LootTable lootTable = entity.getLootTable()
            .map(key -> level.getServer().reloadableRegistries().getLootTable(key))
            .orElse(LootTable.EMPTY);
        AnvilUtil.dropItems(lootTable.getRandomItems(lootParams), level, pos);
        if (rate >= 0.6) AnvilUtil.dropItems(lootTable.getRandomItems(lootParams), level, pos);
        if (rate >= 0.8) AnvilUtil.dropItems(lootTable.getRandomItems(lootParams), level, pos);
        killerOp.ifPresent(killer -> AnvilCraftFakePlayers.getKiller().disable(killer));
    }

    private static void dropExps(
        ServerLevel level,
        LivingEntity entity,
        Vec3 pos,
        double rate
    ) {
        ServerPlayer killer = AnvilCraftFakePlayers.getKiller().offerPlayer(level);
        AnvilCraftFakePlayers.getKiller().enableDisintegration(level, killer);

        ExperienceOrb.award(level, pos, entity.getExperienceReward(level, killer));
        if (rate >= 0.6) ExperienceOrb.award(level, pos, entity.getExperienceReward(level, killer));
        if (rate >= 0.8) ExperienceOrb.award(level, pos, entity.getExperienceReward(level, killer));
        AnvilCraftFakePlayers.getKiller().disable(killer);
    }
}
