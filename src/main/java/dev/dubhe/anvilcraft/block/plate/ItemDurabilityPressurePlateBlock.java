package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.util.MathUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

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
        TreeSet<Float> set = Sets.newTreeSet();
        for (ItemEntity item : level.getEntitiesOfClass(
            ItemEntity.class, box,
            EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
        )) {
            ItemStack stack = item.getItem();
            set.add(MathUtil.safeDivide(stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()));
        }

        try {
            return new Pair<>(Math.max(set.getFirst(), 0), Math.min(set.getLast(), 1));
        } catch (NoSuchElementException ignored) {
            return new Pair<>(0F, 0F);
        }
    }
}
