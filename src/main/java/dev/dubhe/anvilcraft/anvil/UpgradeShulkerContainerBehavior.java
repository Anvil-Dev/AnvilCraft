package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.util.AabbUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpgradeShulkerContainerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        if (!hitBlockState.is(ModBlocks.SHULKER_CONTAINER)) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockPos mainPart = ModBlocks.SHULKER_CONTAINER.get().getMainPartPos(hitBlockPos, hitBlockState);
        List<ItemEntity> spaceOvercompressor = new ArrayList<>();
        int count = 0;
        for (ItemEntity entity : serverLevel.getEntitiesOfClass(
            ItemEntity.class,
            AabbUtil.createInclusive(hitBlockPos, hitBlockPos.above())
        )) {
            ItemStack stack = entity.getItem();
            if (stack.is(ModBlocks.SPACE_OVERCOMPRESSOR.asItem())) {
                spaceOvercompressor.add(entity);
                count += stack.getCount();
            }
        }
        if (count <= 0) {
            return false;
        }

        BlockEntity blockEntity = serverLevel.getBlockEntity(mainPart);
        if (!(blockEntity instanceof ShulkerContainerBlockEntity be)) {
            return false;
        }
        UUID id = be.getId();
        if (id == null) {
            id = UUID.randomUUID();
            be.setId(id);
        }
        ShulkerContainerStorage sc = Storages.get().getOrCreate(id, ShulkerContainerStorage.class);
        TypeLimitItemStacksResourceHandler scItems = sc.getItems();

        count = Math.min(count, 4);
        for (int i = 0; i < count; i++) {
            int old = scItems.getSpaceSize();
            scItems.addSpaceSize(size -> Math.min(size * 2, 1048576)); // 65536 * 2^4
            if (old == scItems.getSpaceSize()) {
                count = i;
                break;
            } else {
                Storages.get().setDirty();
            }
        }

        for (ItemEntity entity : spaceOvercompressor) {
            ItemStack stack = entity.getItem();
            int shrink = Math.min(count, stack.getCount());
            count -= shrink;
            stack.shrink(shrink);
            if (stack.getCount() <= 0) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            if (count == 0) break;
        }

        return true;
    }
}
