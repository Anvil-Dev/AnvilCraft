package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HealthPercentPressurePlateBlock extends PowerLevelPressurePlateBlock {
    private final boolean useMin;

    public HealthPercentPressurePlateBlock(Properties properties, boolean useMin) {
        super(BlockSetType.IRON, properties);
        this.useMin = useMin;
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(LivingEntity.class);
    }

    @Override
    protected int getSignalStrength(
        Level level, AABB box, Set<Class<? extends Entity>> entityClasses
    ) {
        FloatFloatPair minAndMax = getEntitiesHealthPercentMinAndMax(level, box, entityClasses);
        float value = this.useMin ? minAndMax.leftFloat() : minAndMax.rightFloat();
        return (int) (value * 15);
    }

    protected static FloatFloatPair getEntitiesHealthPercentMinAndMax(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        Set<Entity> entities = Sets.newHashSet();
        for (Class<? extends Entity> entityClass : entityClasses) {
            entities.addAll(level.getEntitiesOfClass(
                entityClass, box,
                EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
            ));
        }

        FloatList healthPercents = getHealthPercents(entities);
        try {
            return new FloatFloatImmutablePair(Math.max(healthPercents.getFirst(), 0), Math.min(healthPercents.getLast(), 1));
        } catch (NoSuchElementException ignored) {
            return new FloatFloatImmutablePair(0F, 0F);
        }
    }

    private static @NotNull FloatList getHealthPercents(Set<Entity> entities) {
        FloatList healthPercents = new FloatArrayList();
        for (Entity entity : entities) {
            float healthPercent;

            if (entity instanceof LivingEntity living) {
                healthPercent = living.getHealth() / living.getMaxHealth();
            } else if (entity instanceof EnderDragonPart part) {
                healthPercent = part.getParent().getHealth() / part.getParent().getHealth();
            } else {
                continue;
            }

            healthPercents.add(healthPercent);
        }
        Collections.sort(healthPercents);
        return healthPercents;
    }
}
