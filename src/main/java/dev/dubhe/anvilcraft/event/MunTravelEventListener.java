package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.TheMonolith;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** 玩家掷出的末影珍珠突破主世界逃逸高度时，把玩家送往 Mun。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class MunTravelEventListener {
    /** 触发传送的逃逸高度（格）。 */
    private static final double ESCAPE_HEIGHT = 512.0;
    /** 触发传送的最低速度（格/tick）。 */
    private static final double ESCAPE_SPEED = 32.0;
    /** 抵达 Mun 时高出地表的格数。 */
    private static final int ARRIVAL_HEIGHT_ABOVE_SURFACE = 32;
    /** 抵达 Mun 时距世界原点（石碑）的最小距离（格）。 */
    private static final int ARRIVAL_MIN_DISTANCE = 64;
    /** 抵达 Mun 时距世界原点（石碑）的最大距离（格）。 */
    private static final int ARRIVAL_MAX_DISTANCE = 256;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ThrownEnderpearl pearl)) return;
        if (!(pearl.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (pearl.getY() <= ESCAPE_HEIGHT) return;
        if (pearl.getDeltaMovement().length() < ESCAPE_SPEED) return;
        if (!(pearl.getOwner() instanceof ServerPlayer player)) return;
        ServerLevel mun = level.getServer().getLevel(CelestialTravelManager.MUN_LEVEL);
        if (mun == null) return;
        // 落点在距石碑 64~256 格的圆环内随机
        RandomSource random = mun.getRandom();
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = ARRIVAL_MIN_DISTANCE + random.nextDouble() * (ARRIVAL_MAX_DISTANCE - ARRIVAL_MIN_DISTANCE);
        int x = Mth.floor(Math.cos(angle) * distance);
        int z = Mth.floor(Math.sin(angle) * distance);
        int y = CelestialTravelManager.findSurfaceY(mun, x, z) + ARRIVAL_HEIGHT_ABOVE_SURFACE;
        // 弃置珍珠以免落地后再次触发传送
        pearl.discard();
        player.teleportTo(mun, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
        TheMonolith.ensureGenerated(mun);
    }
}
