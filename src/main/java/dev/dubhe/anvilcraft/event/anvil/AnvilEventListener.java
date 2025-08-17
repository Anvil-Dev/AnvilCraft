package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.init.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeManager;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

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
        handleNeoAnvilRecipe(event);
        for (IAnvilBehavior behavior : IAnvilBehavior.findMatching(hitBlockState)) {
            if (behavior.handle(level, hitBlockPos, hitBlockState, event.getFallDistance(), event)) {
                return;
            }
        }
    }

    public static void handleNeoAnvilRecipe(@NotNull AnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockPos pos = event.getPos();
        FallingBlockEntity entity = event.getEntity();
        InWorldRecipeManager manager = level.getRecipeManager().anc$getInWorldRecipeManager();
        InWorldRecipeContext context = new InWorldRecipeContext(serverLevel, pos.getCenter(), entity);
        manager.trigger(ModRecipeTriggers.ON_ANVIL_FALL_ON.get(), context);
        boolean damageAnvil = context.get(DamageAnvil.DAMAGE_ANVIL);
        if (!event.isAnvilDamage()) event.setAnvilDamage(damageAnvil);
        context.accept();
    }

    private static void brokeBlock(@NotNull Level level, BlockPos pos, AnvilFallOnLandEvent event) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockState state = level.getBlockState(pos);
        //noinspection deprecation
        if (state.getBlock().getExplosionResistance() >= 1200.0) event.setAnvilDamage(true);
        if (state.getDestroySpeed(level, pos) < 0) return;
        boolean smeltDrop = Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof EmberAnvilBlock)
            .orElse(false);
        boolean silkTouch = Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof RoyalAnvilBlock)
            .orElse(false);
        boolean fortune5 = Optional.of(event.getEntity())
            .map(FallingBlockEntity::getBlockState)
            .map(b -> b.getBlock() instanceof TranscendenceAnvilBlock)
            .orElse(false);
        ItemStack dummyTool = silkTouch ? BreakBlockUtil.getDummySilkTouchTool(serverLevel)
            : fortune5 ? BreakBlockUtil.getDummyFortune5Tool(serverLevel)
            : ItemStack.EMPTY;
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
        Vec3 pos = entity.position();
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, eventEntity)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, eventEntity)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, pos);
        Block anvil = eventEntity.getBlockState().getBlock();
        Optional<ServerPlayer> killerOp = Optional.empty();
        if (Util.instanceOfAny(anvil, EmberAnvilBlock.class, TranscendenceAnvilBlock.class)) {
            ServerPlayer killer = AnvilCraftFakePlayers.anvilCraftKiller.offerPlayer(serverLevel);
            builder.withParameter(LootContextParams.DAMAGE_SOURCE, entity.level().damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
            if (anvil instanceof TranscendenceAnvilBlock) {
                AnvilCraftFakePlayers.anvilCraftKiller.enableLooting5(serverLevel, killer);
            }
            killerOp = Optional.of(killer);
        }
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(entity.getLootTable());
        dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.6) dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        if (rate >= 0.8) dropItems(lootTable.getRandomItems(lootParams), serverLevel, pos);
        killerOp.ifPresent(killer -> AnvilCraftFakePlayers.anvilCraftKiller.disable(killer));
    }
}
