package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;

public class EntityUtil {
    public static void setShulkerOpen(Shulker shulker) {
        try {
            shulker.getAttribute(Attributes.ARMOR).removeModifier(Shulker.COVERED_ARMOR_MODIFIER_ID);
        } catch (Exception ignored) {
        }

        shulker.getEntityData().set(Shulker.DATA_PEEK_ID, (byte) 100);
    }
}
