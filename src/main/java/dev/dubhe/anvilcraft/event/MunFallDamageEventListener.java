package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** 保留源版月球（Mun）的 20 格安全区与每 6 格伤害档位。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class MunFallDamageEventListener {
    /** 无伤害的安全下落高度（格）。 */
    private static final float SAFE_FALL_DISTANCE = 20.0f;
    /** 超出安全高度后，每升高该格数增加 1 点伤害。 */
    private static final float BLOCKS_PER_DAMAGE = 6.0f;

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!event.getEntity().level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        float fallDistance = (float) event.getDistance();
        if (fallDistance <= SAFE_FALL_DISTANCE) {
            event.setCanceled(true);
            return;
        }
        int damage = 1 + (int) Math.floor((fallDistance - SAFE_FALL_DISTANCE - 1.0f) / BLOCKS_PER_DAMAGE);
        // 保留源版距离编码；MunFallDamageMixin 继续使用 1.21 的向上取整结算。
        // 原 multiplier 不变，后续安全距离和伤害倍率属性仍参与结算。
        float multiplier = event.getDamageMultiplier();
        if (multiplier <= 0.0f) {
            event.setCanceled(true);
            return;
        }
        event.setDistance(3.0f + (damage + 0.5f) / multiplier);
    }
}
