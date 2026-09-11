package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/** Upward high-speed pearls reach Mun at midnight or return from its origin area. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class MunTravelEventListener {
    private static final double ESCAPE_HEIGHT = 512.0;
    private static final double ESCAPE_SPEED = 32.0;
    private static final int MIDNIGHT = 18000;
    private static final int MIDNIGHT_WINDOW = 1000;
    private static final int ARRIVAL_MIN_DISTANCE = 8;
    private static final int ARRIVAL_MAX_DISTANCE = 16;
    private static final int RETURN_RADIUS = 128;
    private static final String RETURN_LAUNCH_KEY = "anvilcraft:mun_return_launch";
    private static final Map<ServerPlayer, ArrivalDamage> ARRIVAL_DAMAGE = new WeakHashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        if (event.getEntity() instanceof ThrownEnderpearl pearl) isReturnLaunch(pearl);
    }

    private static boolean isReturnLaunch(ThrownEnderpearl pearl) {
        var data = pearl.getPersistentData();
        if (!data.contains(RETURN_LAUNCH_KEY)) {
            boolean eligible = pearl.getOwner() instanceof ServerPlayer player && player.level() == pearl.level()
                && player.getX() * player.getX() + player.getZ() * player.getZ() <= RETURN_RADIUS * RETURN_RADIUS;
            data.putBoolean(RETURN_LAUNCH_KEY, eligible);
        }
        return data.getBoolean(RETURN_LAUNCH_KEY);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ThrownEnderpearl pearl) || pearl.isRemoved()) return;
        if (!(pearl.level() instanceof ServerLevel level)) return;
        boolean returning = level.dimension().equals(CelestialTravelManager.MUN_LEVEL);
        if (returning) {
            if (!isReturnLaunch(pearl)) return;
        } else if (!level.dimension().equals(Level.OVERWORLD)
            || Math.abs(Math.floorMod(level.getDayTime(), 24000L) - MIDNIGHT) > MIDNIGHT_WINDOW) {
            return;
        }
        if (pearl.getY() <= ESCAPE_HEIGHT || pearl.getDeltaMovement().y <= 0) return;
        if (pearl.getDeltaMovement().lengthSqr() < ESCAPE_SPEED * ESCAPE_SPEED) return;
        if (!(pearl.getOwner() instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSleeping() || player.level() != level || !player.connection.isAcceptingMessages()) return;
        ServerLevel destination = level.getServer().getLevel(returning ? Level.OVERWORLD : CelestialTravelManager.MUN_LEVEL);
        if (destination == null) return;
        RandomSource random = destination.getRandom();
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = ARRIVAL_MIN_DISTANCE + random.nextDouble() * (ARRIVAL_MAX_DISTANCE - ARRIVAL_MIN_DISTANCE);
        BlockPos spawn = returning ? BlockPos.ZERO : destination.getSharedSpawnPos();
        int x = spawn.getX() + Mth.floor(Math.cos(angle) * distance);
        int z = spawn.getZ() + Mth.floor(Math.sin(angle) * distance);
        BlockPos landing = CelestialTravelManager.findSafeSurfaceLandingPos(destination, new BlockPos(x, 0, z));
        if (landing == null) return;
        Vec3 target = Vec3.atBottomCenterOf(landing);
        var teleport = EventHooks.onEnderPearlLand(
            player, target.x, target.y, target.z, pearl, 1.0F,
            new BlockHitResult(target, Direction.UP, landing.below(), false)
        );
        if (teleport.isCanceled()) {
            pearl.discard();
            return;
        }
        player.unRide();
        if (player.changeDimension(new DimensionTransition(
            destination, teleport.getTarget(), Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING
        )) == null) return;
        pearl.discard();
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.resetCurrentImpulseContext();
        player.setOnGround(true);
        ARRIVAL_DAMAGE.put(player, new ArrivalDamage(destination.dimension(), teleport.getAttackDamage()));
        destination.playSound(null, player.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArrivalDamage damage = ARRIVAL_DAMAGE.get(player);
        if (damage == null) return;
        if (!player.isAlive() || !player.level().dimension().equals(damage.dimension())) {
            ARRIVAL_DAMAGE.remove(player);
            return;
        }
        // Cross-dimension travel is invulnerable until the client acknowledges arrival.
        if (player.isChangingDimension()) return;
        ARRIVAL_DAMAGE.remove(player);
        player.hurt(player.damageSources().fall(), damage.amount());
    }

    private record ArrivalDamage(ResourceKey<Level> dimension, float amount) {
    }
}
