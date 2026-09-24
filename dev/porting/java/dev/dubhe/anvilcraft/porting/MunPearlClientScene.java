package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.event.MunTravelEventListener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MunPearlClientScene {
    private static int stage;
    private static long deadline;
    private static volatile boolean ready;
    private static volatile @Nullable RuntimeException failure;
    private static @Nullable UUID actor;
    private static @Nullable MunLightingQuality quality;
    private static volatile int damageCount;
    private static volatile float damageTotal;
    private static boolean cancelLanding;
    private static boolean returning;
    private static @Nullable Vec3 expectedLanding;
    private static float expectedYaw;
    private static float expectedPitch;
    private static @Nullable ThrownEnderpearl launched;

    public static void frame(Minecraft client) {
        if (failure != null) throw failure;
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        check(System.currentTimeMillis() < deadline, "Moon pearl travel timed out at " + stage);
        client.options.pauseOnLostFocus = false;
        if (stage == 0) {
            stage = 1;
            client.setScreen(null);
            quality = AnvilCraft.CLIENT_CONFIG.munLightingQuality;
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.OFF;
            server(client, () -> {
                var server = Objects.requireNonNull(client.getSingleplayerServer());
                var player = server.getPlayerList().getPlayers().getFirst();
                actor = player.getUUID();
                var moon = Objects.requireNonNull(server.getLevel(CelestialTravelManager.MUN_LEVEL));
                platform(moon, moon.getWorldBorderAdjustedRespawnData(moon.getRespawnData()).pos());
                platform(server.overworld(), BlockPos.ZERO);
                player.setGameMode(GameType.SURVIVAL);
                player.setNoGravity(true);
                player.setHealth(20);
                player.teleportTo(server.overworld(), 0.5, 302, 0.5, Set.<Relative>of(), 37, -22, false);
            });
            return;
        }
        if (!ready || client.level == null || client.player == null || client.screen != null || client.getOverlay() != null) return;
        if (stage == 1 && client.level.dimension().equals(Level.OVERWORLD)) {
            stage = 2;
            server(client, () -> {
                var player = Objects.requireNonNull(client.getSingleplayerServer()).getPlayerList().getPlayers().getFirst();
                boundaries(player);
                time(player.level(), 18000);
                returning = false;
                launched = pearl(player, 510, new Vec3(0, 40, 0));
                player.level().addFreshEntity(launched);
            });
        } else if (stage == 2 && client.level.dimension().equals(CelestialTravelManager.MUN_LEVEL) && damageCount == 1) {
            stage = 3;
            server(client, () -> {
                var server = Objects.requireNonNull(client.getSingleplayerServer());
                var player = server.getPlayerList().getPlayers().getFirst();
                arrival(player, 1);
                returnEligibility(player);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "setworldspawn 512 80 512");
                player.setHealth(20);
                player.invulnerableTime = 0;
                player.teleportTo(player.level(), 0.5, 302, 0.5, Set.<Relative>of(), 37, -22, false);
            });
        } else if (stage == 3) {
            stage = 4;
            server(client, () -> {
                var player = Objects.requireNonNull(client.getSingleplayerServer()).getPlayerList().getPlayers().getFirst();
                returning = true;
                time(player.level(), 6000);
                launched = pearl(player, 510, new Vec3(0, 40, 0));
                player.level().addFreshEntity(launched);
            });
        } else if (stage == 4 && client.level.dimension().equals(Level.OVERWORLD) && damageCount == 2) {
            stage = 5;
            server(client, () -> {
                var player = Objects.requireNonNull(client.getSingleplayerServer()).getPlayerList().getPlayers().getFirst();
                arrival(player, 4);
                check(Math.abs(player.getX()) < 24 && Math.abs(player.getZ()) < 24, "Return used the moved world spawn");
                AnvilCraft.LOGGER.info("PORT_MUN_PEARL_PASSED: boundaries, launch eligibility, round trip, "
                    + "event overrides and acknowledged damage");
            });
        } else if (stage == 5) {
            stage = 6;
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = Objects.requireNonNull(quality);
            client.stop();
        }
    }

    @SubscribeEvent
    public static void landing(EntityTeleportEvent.EnderPearl event) {
        if (!Boolean.getBoolean("anvilcraft.portMunTravelScene") || !event.getPlayer().getUUID().equals(actor)) return;
        if (cancelLanding) {
            event.setCanceled(true);
            return;
        }
        if (returning) {
            event.setAttackDamage(3);
            event.setTargetX(2.5);
            event.setTargetZ(3.5);
        }
        expectedLanding = event.getTarget();
        expectedYaw = event.getPlayer().getYRot();
        expectedPitch = event.getPlayer().getXRot();
    }

    @SubscribeEvent
    public static void damage(LivingDamageEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portMunTravelScene") || !event.getEntity().getUUID().equals(actor)
            || !event.getSource().is(DamageTypes.FALL)) return;
        try {
            var player = (ServerPlayer) event.getEntity();
            check(!player.isChangingDimension() && player.connection.hasClientLoaded(), "Damage preceded client arrival acknowledgement");
            check(event.getHealthDamage() == (returning ? 3 : 1), "Arrival damage was changed or swallowed");
            damageTotal += event.getHealthDamage();
            damageCount++;
            AnvilCraft.LOGGER.info("PORT_MUN_PEARL_DAMAGE: count={}, amount={}, dimension={}",
                damageCount, event.getHealthDamage(), player.level().dimension());
        } catch (RuntimeException exception) {
            failure = exception;
        }
    }

    private static void boundaries(ServerPlayer player) {
        cancelLanding = true;
        try {
            time(player.level(), 18000);
            rejected(player, 512, new Vec3(0, 40, 0));
            rejected(player, 513, new Vec3(0, -40, 0));
            rejected(player, 513, new Vec3(40, 0, 0));
            rejected(player, 513, new Vec3(0, 31.999, 0));
            for (long time : new long[]{16999, 19001, 0}) {
                time(player.level(), time);
                rejected(player, 513, new Vec3(0, 40, 0));
            }
            for (long time : new long[]{17000, 19000, -7000, -5000}) {
                time(player.level(), time);
                var pearl = pearl(player, 513, new Vec3(0, 32, 0));
                MunTravelEventListener.onEntityTick(new EntityTickEvent.Post(pearl));
                check(pearl.isRemoved() && player.level().dimension().equals(Level.OVERWORLD),
                    "Boundary launch did not reach the cancellable landing event");
            }
            time(player.level(), 18000);
            var diagonal = pearl(player, 513, new Vec3(32, 0.001, 0));
            MunTravelEventListener.onEntityTick(new EntityTickEvent.Post(diagonal));
            check(diagonal.isRemoved(), "Launch threshold used vertical speed instead of total speed");
        } finally {
            cancelLanding = false;
        }
    }

    private static void rejected(ServerPlayer player, double height, Vec3 velocity) {
        var pearl = pearl(player, height, velocity);
        MunTravelEventListener.onEntityTick(new EntityTickEvent.Post(pearl));
        check(!pearl.isRemoved() && player.level().dimension().equals(Level.OVERWORLD), "Ineligible launch was accepted");
        pearl.discard();
    }

    private static void returnEligibility(ServerPlayer player) {
        player.setPos(128, 302, 0);
        var eligible = pearl(player, 513, new Vec3(0, 40, 0));
        MunTravelEventListener.onEntityJoin(new EntityJoinLevelEvent(eligible, player.level()));
        player.setPos(129, 302, 0);
        check(eligible.getPersistentData().getBooleanOr("anvilcraft:mun_return_launch", false), "Launch radius boundary was excluded");
        cancelLanding = true;
        try {
            MunTravelEventListener.onEntityTick(new EntityTickEvent.Post(eligible));
            check(eligible.isRemoved(), "Leaving the return radius revoked launch eligibility");
        } finally {
            cancelLanding = false;
        }
        var denied = pearl(player, 513, new Vec3(0, 40, 0));
        MunTravelEventListener.onEntityJoin(new EntityJoinLevelEvent(denied, player.level()));
        player.setPos(0, 302, 0);
        MunTravelEventListener.onEntityTick(new EntityTickEvent.Post(denied));
        check(!denied.isRemoved(), "Moving into the return radius changed launch eligibility");
        eligible.discard();
        denied.discard();
    }

    private static ThrownEnderpearl pearl(ServerPlayer player, double height, Vec3 velocity) {
        var pearl = new ThrownEnderpearl(player.level(), player, Items.ENDER_PEARL.getDefaultInstance());
        pearl.setPos(player.getX(), height, player.getZ());
        pearl.setDeltaMovement(velocity);
        return pearl;
    }

    private static void arrival(ServerPlayer player, float total) {
        check(Objects.requireNonNull(launched).isRemoved(), "Travel pearl survived arrival");
        check(player.position().distanceToSqr(Objects.requireNonNull(expectedLanding)) < 0.01, "Landing override was lost");
        check(player.getDeltaMovement().lengthSqr() < 0.001 && player.fallDistance == 0, "Arrival retained speed or fall distance");
        check(player.getYRot() == expectedYaw && player.getXRot() == expectedPitch, "Travel changed player facing");
        check(damageTotal == total, "Arrival damage was repeated or missing");
        AnvilCraft.LOGGER.info("PORT_MUN_PEARL_ARRIVAL: {}, {}", player.level().dimension(), player.position());
    }

    private static void time(ServerLevel level, long time) {
        var clock = level.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        level.clockManager().setTotalTicks(clock, time);
    }

    private static void platform(ServerLevel level, BlockPos center) {
        for (int x = -24; x <= 24; x++) {
            for (int z = -24; z <= 24; z++) {
                level.setBlock(new BlockPos(center.getX() + x, 300, center.getZ() + z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 301; y < 306; y++) {
                    level.setBlock(new BlockPos(center.getX() + x, y, center.getZ() + z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void server(Minecraft client, Runnable work) {
        ready = false;
        Objects.requireNonNull(client.getSingleplayerServer()).execute(() -> {
            try {
                work.run();
                ready = true;
            } catch (RuntimeException exception) {
                failure = exception;
            }
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
