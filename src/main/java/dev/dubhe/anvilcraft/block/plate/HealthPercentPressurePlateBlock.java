package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HealthPercentPressurePlateBlock extends PowerLevelPressurePlateBlock {
    private final boolean useMin;

    public HealthPercentPressurePlateBlock(Properties properties, boolean useMin) {
        super(BlockSetType.IRON, properties);
        this.useMin = useMin;
    }

    protected static Pair<Float, Float> getEntitiesHealthPercentMinAndMax(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        Set<Entity> entities = Sets.newHashSet();
        for (Class<? extends Entity> entityClass : entityClasses) {
            entities.addAll(level.getEntitiesOfClass(
                entityClass, box,
                EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
            ));
        }

        TreeSet<Float> set = Sets.newTreeSet();
        for (Entity entity : entities) {
            float healthPercent;

            if (entity instanceof LivingEntity living) {
                healthPercent = living.getHealth() / living.getMaxHealth();
            } else if (entity instanceof EnderDragonPart part) {
                healthPercent = part.getParent().getHealth() / part.getParent().getHealth();
            } else {
                continue;
            }

            set.add(healthPercent);
        }

        try {
            return new Pair<>(Math.max(set.getFirst(), 0), Math.min(set.getLast(), 1));
        } catch (NoSuchElementException ignored) {
            return new Pair<>(0F, 0F);
        }
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(LivingEntity.class);
    }

    @Override
    protected int getSignalStrength(
        Level level, AABB box, Set<Class<? extends Entity>> entityClasses
    ) {
        Pair<Float, Float> minAndMax = getEntitiesHealthPercentMinAndMax(level, box, entityClasses);
        float value = this.useMin ? minAndMax.getFirst() : minAndMax.getSecond();
        return (int) (value * 15);
    }
}
