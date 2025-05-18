package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;

public class EntityUtil {
    public static void setShulkerOpen(Shulker shulker) {
        AttributeInstance armor = shulker.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.removeModifier(Shulker.COVERED_ARMOR_MODIFIER_ID);
        }

        shulker.getEntityData().set(Shulker.DATA_PEEK_ID, (byte) 100);
    }
}
