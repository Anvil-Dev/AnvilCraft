package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import java.util.Set;

public class PlayerHungerPressurePlateBlock extends PowerLevelPressurePlateBlock {
    public PlayerHungerPressurePlateBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(Player.class);
    }

    @Override
    protected int getSignalStrength(Level level, net.minecraft.world.phys.AABB box, Set<Class<? extends Entity>> entityClasses) {
        return (int) Math.clamp(getMaxHungerPercent(level, box) * 15, 0, 15);
    }

    protected static float getMaxHungerPercent(Level level, AABB box) {
        float result = 0;

        for (Player player : level.getEntitiesOfClass(
            Player.class, box,
            EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
        )) {
            FoodData foodData = player.getFoodData();
            result = Math.max(result, (float) foodData.getFoodLevel() / 20);
        }

        return result;
    }
}
