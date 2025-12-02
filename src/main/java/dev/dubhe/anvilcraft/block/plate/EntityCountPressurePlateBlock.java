package dev.dubhe.anvilcraft.block.plate;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.function.Predicate;

public class EntityCountPressurePlateBlock extends PowerLevelPressurePlateBlock {
    private final Predicate<Entity> filter;

    public EntityCountPressurePlateBlock(Properties properties, Predicate<Entity> filter) {
        super(BlockSetType.IRON, properties);
        this.filter = filter;
    }

    @Override
    protected int getSignalStrength(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        return Math.clamp(getEntityCountWithFilter(level, box, this.filter), 0, 15);
    }

    protected static int getEntityCountWithFilter(Level level, AABB box, Predicate<Entity> filter) {
        return level.getEntitiesOfClass(
            Entity.class, box,
            EntitySelector.NO_SPECTATORS.and(filter)
        ).size();
    }
}
