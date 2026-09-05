package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** 月球（Mun）维度的摔落伤害：超过 20 格才有 1 点伤害，之后每升高 6 格增加 1 点。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class MunFallDamageEventListener {
    /** 无伤害的安全下落高度（格）。 */
    private static final float SAFE_FALL_DISTANCE = 20.0f;
    /** 超出安全高度后，每升高该格数增加 1 点伤害。 */
    private static final float BLOCKS_PER_DAMAGE = 6.0f;

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!event.getEntity().level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        float fallDistance = event.getDistance();
        if (fallDistance <= SAFE_FALL_DISTANCE) {
            event.setCanceled(true);
            return;
        }
        int damage = 1 + (int) Math.floor((fallDistance - SAFE_FALL_DISTANCE - 1.0f) / BLOCKS_PER_DAMAGE);
        // 原版结算伤害为 (distance - 3) * multiplier，抬高 distance 使结算伤害恰为目标值
        event.setDistance(damage + 3.0f);
        event.setDamageMultiplier(1.0f);
    }
}
