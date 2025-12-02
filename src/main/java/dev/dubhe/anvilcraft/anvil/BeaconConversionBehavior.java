package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BeaconConversionBehavior implements IAnvilBehavior {
    private static final Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap() {
        {
            put(1, 0.02f);
            put(2, 0.05f);
            put(3, 0.2f);
            put(4, 1f);
        }
    };

    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        BlockPos above = hitBlockPos.above();
        int beaconLevel = getBeaconLevel(level, hitBlockPos);
        final List<ItemEntity> itemEntities = level.getEntitiesOfClass(ItemEntity.class, new AABB(above));
        BlockEntity blockEntity = level.getBlockEntity(hitBlockPos);
        if (!(blockEntity instanceof BeaconBlockEntity beaconBlockEntity)) {
            return false;
        }
        if (beaconBlockEntity.getBeamSections().isEmpty()) {
            return false;
        }
        if (beaconLevel <= 0) {
            return false;
        }
        for (ItemEntity itemEntity : itemEntities) {
            ItemStack item = itemEntity.getItem();
            if (item.is(ModItems.CURSED_GOLD_INGOT)) {
                ItemStack stack = item.copy();
                stack.shrink(1);
                itemEntity.setItem(stack);
                for (int i = 1; i <= 4; i++) {
                    if (beaconLevel == i) {
                        if (level.random.nextDouble() < map.get(i)) {
                            level.setBlockAndUpdate(hitBlockPos, ModBlocks.CORRUPTED_BEACON.getDefaultState());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int getBeaconLevel(Level level, BlockPos beaconPos) {
        int result = 0;
        for (int i = 1; i <= 4; i++) {
            for (BlockPos blockPos : BlockPos.betweenClosed(beaconPos.offset(-i, -i, -i), beaconPos.offset(i, -i, i))) {
                if (!level.getBlockState(blockPos).is(ModBlocks.CURSED_GOLD_BLOCK)) {
                    return result;
                }
            }
            result = i;
        }
        return result;
    }
}
