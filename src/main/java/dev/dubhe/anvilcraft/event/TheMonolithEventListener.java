package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModVillagers;
import dev.dubhe.anvilcraft.worldgen.TheMonolith;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class TheMonolithEventListener {
    private static final int HINT_TICKS = 100;
    private static final int HINT_RANGE = 5;
    private static final Map<ServerPlayer, Map<BlockPos, Integer>> PROGRESS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> RETURN_TOUCHES = new WeakHashMap<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TheMonolith.ensureGenerated(event.getServer().overworld());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) TheMonolith.ensureGenerated(level);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) TheMonolith.ensureGenerated(level);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        Set<BlockPos> nearby = level.getPoiManager().getInRange(
                type -> type.is(ModVillagers.MONOLITH_CORE_POI.getKey()),
                player.blockPosition(), HINT_RANGE + 1, PoiManager.Occupancy.ANY
            )
            .map(PoiRecord::getPos)
            .filter(level::hasChunkAt)
            .filter(pos -> pos.distToCenterSqr(player.position()) <= HINT_RANGE * HINT_RANGE)
            .filter(pos -> level.getBlockState(pos).is(ModBlocks.MONOLITH_CORE.get())
                || level.getBlockState(pos).is(ModBlocks.GIANT_MONOLITH_CORE.get()))
            .collect(Collectors.toSet());
        if (nearby.isEmpty()) {
            PROGRESS.remove(player);
            return;
        }
        Map<BlockPos, Integer> progress = PROGRESS.computeIfAbsent(player, ignored -> new HashMap<>());
        progress.keySet().retainAll(nearby);
        for (BlockPos pos : nearby) {
            int ticks = progress.compute(pos, (ignored, previous) -> previous == null ? 1 : Math.min(previous + 1, HINT_TICKS + 1));
            if (ticks != HINT_TICKS) continue;
            String key = level.getBlockState(pos).is(ModBlocks.GIANT_MONOLITH_CORE.get())
                ? "message.anvilcraft.monolith.giant_offering" : "message.anvilcraft.monolith.offering";
            player.sendSystemMessage(Component.translatable(key));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(ModBlocks.MONOLITH.get()) && !state.is(ModBlocks.MONOLITH_LINE.get())
            && !state.is(ModBlocks.GIANT_MONOLITH_LINE.get())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        long now = player.serverLevel().getGameTime();
        Long firstTouch = RETURN_TOUCHES.putIfAbsent(player, now);
        if (firstTouch == null) {
            player.sendSystemMessage(Component.translatable("message.anvilcraft.monolith.return_confirmation"));
            return;
        }
        if (firstTouch == now) return;
        RETURN_TOUCHES.remove(player);
        DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
        player.unRide();
        if (player.changeDimension(transition) == null) return;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.resetCurrentImpulseContext();
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        PROGRESS.remove(event.getEntity());
        RETURN_TOUCHES.remove(event.getEntity());
    }
}
