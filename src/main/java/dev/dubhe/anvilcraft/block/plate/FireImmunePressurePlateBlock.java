package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import java.util.Set;

public class FireImmunePressurePlateBlock extends PowerLevelPressurePlateBlock {
    public FireImmunePressurePlateBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(LivingEntity.class, ItemEntity.class);
    }

    @Override
    protected int getSignalStrength(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        return Math.clamp(getFireImmuneEntityCount(level, box, entityClasses), 0, 15);
    }

    protected static int getFireImmuneEntityCount(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        Set<Entity> entities = Sets.newHashSet();
        for (Class<? extends Entity> entityClass : entityClasses) {
            entities.addAll(level.getEntitiesOfClass(
                entityClass, box,
                EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
            ));
        }

        int result = 0;
        for (Entity entity : entities) {
            if (entity.fireImmune()) {
                result++;
            } else if (
                entity instanceof ItemEntity item && item.fireImmune()
                    && item.getItem().getCount() >= item.getItem().getMaxStackSize()
            ) {
                result++;
            }
        }

        return result;
    }
}
