package dev.dubhe.anvilcraft.util.mixin.ref;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ProvidenceRef {
    private static final List<ProvidenceData> SHOULD_TRIGGER = new ArrayList<>();

    public static void shouldTrigger(
        ResourceKey<Level> dimension, int enchantmentLevel, EnchantedItemInUse item, int entityId
    ) {
        SHOULD_TRIGGER.add(new ProvidenceData(dimension, enchantmentLevel, item, entityId));
    }

    public static boolean shouldItTrigger(
        ResourceKey<Level> dimension, int enchantmentLevel, EnchantedItemInUse item, int entityId
    ) {
        int index = SHOULD_TRIGGER.indexOf(new ProvidenceData(dimension, enchantmentLevel, item, entityId));
        if (index != -1) {
            SHOULD_TRIGGER.remove(index);
            return true;
        }
        return false;
    }

    record ProvidenceData(ResourceKey<Level> dimension, int enchantmentLevel, EnchantedItemInUse item, int entityId) {
    }
}
