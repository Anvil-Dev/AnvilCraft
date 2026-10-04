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
    /** 超出安全高度后，每升高该格数增加 1 点伤害。 */

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!event.getEntity().level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        float fallDistance = event.getDistance();
        float safeFallDistance = AnvilCraft.CONFIG.world.munSafeFallDistance;
        float blocksPerDamage = AnvilCraft.CONFIG.world.munBlocksPerDamage;
        if (fallDistance <= safeFallDistance) {
            event.setCanceled(true);
            return;
        }
        int damage = 1 + (int) Math.floor((fallDistance - safeFallDistance - 1.0f) / blocksPerDamage);
        // 原版结算伤害为 floor((distance - 3) * multiplier)；按原 multiplier 反推 distance，
        // 使结算伤害恰为目标值。保留原 multiplier 以免摔落保护等减伤系数失效
        float multiplier = event.getDamageMultiplier();
        if (multiplier <= 0.0f) {
            event.setCanceled(true);
            return;
        }
        event.setDistance(3.0f + (damage + 0.5f) / multiplier);
    }
}
