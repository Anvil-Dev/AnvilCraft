package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    /** 抵达 Mun 的高度。 */
    private static final double ARRIVAL_HEIGHT = 32.0;
    /** Mun 与主世界的坐标比例（参考下界 1:8）。 */
    private static final double COORDINATE_SCALE = 16.0;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ThrownEnderpearl pearl)) return;
        if (!(pearl.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (pearl.getY() <= ESCAPE_HEIGHT) return;
        if (pearl.getDeltaMovement().length() < ESCAPE_SPEED) return;
        if (!(pearl.getOwner() instanceof ServerPlayer player)) return;
        ServerLevel mun = level.getServer().getLevel(CelestialTravelManager.MUN_LEVEL);
        if (mun == null) return;
        double x = pearl.getX() / COORDINATE_SCALE;
        double z = pearl.getZ() / COORDINATE_SCALE;
        // 弃置珍珠以免落地后再次触发传送
        pearl.discard();
        player.teleportTo(mun, x, ARRIVAL_HEIGHT, z, player.getYRot(), player.getXRot());
    }
}
