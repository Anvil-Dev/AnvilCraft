package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;

public record ScarePhantomAmuletEffect() implements IAmuletEffect {
    public static final ScarePhantomAmuletEffect INSTANCE = new ScarePhantomAmuletEffect();

    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        if (!(ctx.get(ModAmuletEffectContextKeys.TARGETING_MOB).orElse(null) instanceof Phantom)) {
            return;
        }
        ctx.set(ModAmuletEffectContextKeys.IGNORE_MOB, true);
    }
}
