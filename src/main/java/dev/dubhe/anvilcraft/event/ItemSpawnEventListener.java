package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber
public class ItemSpawnEventListener {
    @SubscribeEvent
    public static void onItemSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            Level level = itemEntity.level();
            if (level.isClientSide) return;
            int x = (int) Math.floor(itemEntity.getX());
            int y = (int) Math.floor(itemEntity.getY());
            int z = (int) Math.floor(itemEntity.getZ());
            for (int ix = x - 8; ix <= x + 8; ix++) {
                for (int iy = y - 8; iy <= y + 8; iy++) {
                    for (int iz = z - 8; iz <= z + 8; iz++) {
                        BlockPos pos = new BlockPos(ix, iy, iz);
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof ItemCollectorBlockEntity i) {
                            int cooldown = i.getCooldown().get();
                            int r = i.getRangeRadius().get();
                            if (cooldown == 0 && isValidDistance(pos, x, y, z, r)) {
                                ItemStack itemStack = itemEntity.getItem();
                                int slotIndex = 0;
                                while (itemStack != ItemStack.EMPTY && slotIndex < 9) {
                                    itemStack = i.getItemHandler().insertItem(slotIndex++, itemStack, false);
                                }
                                if (itemStack != ItemStack.EMPTY) {
                                    itemEntity.setItem(itemStack);
                                } else {
                                    event.setCanceled(true);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isValidDistance(BlockPos pos, int x, int y, int z, int r) {
        boolean flag = true;
        if (Math.abs(pos.getX() - x) > r || Math.abs(pos.getY() - y) > r || Math.abs(pos.getZ() - z) > r) flag = false;
        return flag;
    }
}
