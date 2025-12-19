package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import it.unimi.dsi.fastutil.floats.FloatSortedSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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
        Pair<Float, Float> minAndMax = getEntitiesHealthPercentMinAndMax(level, box, entityClasses);
        float value = this.useMin ? minAndMax.getFirst() : minAndMax.getSecond();
        return (int) (value * 15);
    }

    protected static Pair<Float, Float> getEntitiesHealthPercentMinAndMax(
        Level level,
        AABB box,
        Set<Class<? extends Entity>> entityClasses
    ) {
        Set<Entity> entities = Sets.newHashSet();
        for (Class<? extends Entity> entityClass : entityClasses) {
            entities.addAll(level.getEntitiesOfClass(
                entityClass, box,
                EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
            ));
        }

        var healthPercents = HealthPercentPressurePlateBlock.getHealthPercents(entities);

        if (healthPercents == null) return new Pair<>(0F, 0F);
        return new Pair<>(Math.max(healthPercents.leftFloat(), 0), Math.min(healthPercents.rightFloat(), 1));
    }

    private static @Nullable FloatFloatPair getHealthPercents(Set<Entity> entities) {
        FloatSortedSet set = new FloatAVLTreeSet();
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
        if (set.isEmpty()) return null;
        return FloatFloatPair.of(set.firstFloat(), set.lastFloat());
    }
}
