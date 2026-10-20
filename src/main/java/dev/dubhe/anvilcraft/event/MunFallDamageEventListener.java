package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** 保留源版月球（Mun）的 20 格安全区与每 6 格伤害档位。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class MunFallDamageEventListener {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!event.getEntity().level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) return;
        float fallDistance = (float) event.getDistance();

        float safeFallDistance = AnvilCraft.CONFIG.world.munSafeFallDistance;
        float blocksPerDamage = AnvilCraft.CONFIG.world.munBlocksPerDamage;
        if (fallDistance <= safeFallDistance) {
            event.setCanceled(true);
            return;
        }
        int damage = 1 + (int) Math.floor((fallDistance - safeFallDistance - 1.0f) / blocksPerDamage);
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
