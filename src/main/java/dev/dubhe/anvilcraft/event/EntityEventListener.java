package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModEntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class EntityEventListener {
    @SubscribeEvent
    public static void onCheckEntityInvulnerability(EntityInvulnerabilityCheckEvent event) {
        DamageSource source = event.getSource();
        Entity entity = event.getEntity();
        if (source.is(ModDamageTypeTags.IS_FALLING_GIANT_ANVIL)
            && entity.getType().builtInRegistryHolder().is(ModEntityTypeTags.FALLING_GIANT_ANVIL_DAMAGE_IMMUNE)) {
            event.setInvulnerable(true);
        }
    }
}
