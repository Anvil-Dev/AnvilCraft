package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/// 免疫异常物品带来的负面效果的护符效果
public record ImmuneAbnormalItemAmuletEffect() implements IAmuletEffect {
    public static final ImmuneAbnormalItemAmuletEffect INSTANCE = new ImmuneAbnormalItemAmuletEffect();

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        ctx.set(ModAmuletEffectContextKeys.IMMUNE_ABNORMAL_ITEM, true);
    }
}
