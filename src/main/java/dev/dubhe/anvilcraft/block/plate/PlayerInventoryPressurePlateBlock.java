package dev.dubhe.anvilcraft.block.plate;

import com.google.common.collect.ImmutableSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PlayerInventoryPressurePlateBlock extends PowerLevelPressurePlateBlock {
    public PlayerInventoryPressurePlateBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    protected static float getInventoryOccupiedCapacityMaxPercent(Level level, AABB box) {
        float result = 0F;

        for (Player player : level.getEntitiesOfClass(
            Player.class, box,
            EntitySelector.NO_SPECTATORS.and(entity -> !entity.isIgnoringBlockTriggers())
        )) {
            Inventory inventory = player.getInventory();

            int occupiedSlots = 0;
            for (ItemStack stack : inventory.items) {
                if (!stack.isEmpty()) {
                    occupiedSlots++;
                }
            }

            float occupiedPercent = (float) occupiedSlots / inventory.getContainerSize();
            result = Math.max(result, occupiedPercent);
        }

        return result;
    }

    @Override
    protected Set<Class<? extends Entity>> getEntityClasses() {
        return ImmutableSet.of(Player.class);
    }

    @Override
    protected int getSignalStrength(Level level, net.minecraft.world.phys.AABB box, Set<Class<? extends Entity>> entityClasses) {
        return (int) Math.clamp(getInventoryOccupiedCapacityMaxPercent(level, box) * 15, 0, 15);
    }
}
