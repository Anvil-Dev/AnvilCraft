package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import java.util.NoSuchElementException;
import java.util.Set;

public class ItemDurabilityPressurePlateBlock extends PowerLevelPressurePlateBlock {
    private final boolean useMin;

    public ItemDurabilityPressurePlateBlock(Properties properties, boolean useMin) {
        super(BlockSetType.IRON, properties);
        this.useMin = useMin;
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(ItemEntity.class);
    }

    @Override
    protected int getSignalStrength(Level level, AABB box, Set<Class<? extends Entity>> entityClasses) {
        Pair<Float, Float> minAndMax = getItemDurabilityPercentMinAndMax(level, box);
        float value = this.useMin ? minAndMax.getFirst() : minAndMax.getSecond();
        return (int) (value * 15);
    }

    protected static Pair<Float, Float> getItemDurabilityPercentMinAndMax(Level level, AABB box) {
        FloatAVLTreeSet set = new FloatAVLTreeSet();
        for (ItemEntity item : level.getEntitiesOfClass(
            ItemEntity.class, box,
            EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
        )) {
            ItemStack stack = item.getItem();
            if (stack.getMaxDamage() == 0) {
                set.add(1);
                continue;
            }
            set.add((stack.getMaxDamage() - stack.getDamageValue() - 0.0F) / stack.getMaxDamage());
        }

        try {
            return new Pair<>(Math.max(set.getFirst(), 0), Math.min(set.getLast(), 1));
        } catch (NoSuchElementException ignored) {
            return new Pair<>(0F, 0F);
        }
    }
}
