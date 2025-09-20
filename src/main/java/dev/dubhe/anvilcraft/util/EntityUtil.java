package dev.dubhe.anvilcraft.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtil {
    public static void setShulkerOpen(Shulker shulker) {
        try {
            shulker.getAttribute(Attributes.ARMOR).removeModifier(Shulker.COVERED_ARMOR_MODIFIER_ID);
        } catch (Exception ignored) {
        }

        shulker.getEntityData().set(Shulker.DATA_PEEK_ID, (byte) 100);
    }
    
    @Nullable
    public static <T extends Entity> T getAnyEntityOfClass(Level level, Class<T> clazz, AABB bounds, Predicate<? super T> predicate) {
        List<T> entities = new ArrayList<>(1);
        level.getEntities(EntityTypeTest.forClass(clazz), bounds, predicate, entities, 1);
        return entities.isEmpty() ? null : entities.getFirst();
    }
}
