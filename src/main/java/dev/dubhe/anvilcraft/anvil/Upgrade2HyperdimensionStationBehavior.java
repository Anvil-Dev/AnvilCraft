package dev.dubhe.anvilcraft.anvil;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.util.AabbUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 将满级潜影集装箱升级为超维存储站：
 * 从顶部砸入 1x 奇点晶体 + 16x 超立方体，该过程不可逆。
 * 仅在潜影集装箱的空间大小已达到满级（4 次空间压缩器升级）时生效。
 */
public class Upgrade2HyperdimensionStationBehavior implements IAnvilBehavior {
    /** 满级潜影集装箱的空间大小上限（经 4 次空间压缩器翻倍后的最大值）。 */
    private static final int MAX_SPACE_SIZE = 1048576;
    private static final int REQUIRED_CRYSTALS = 1;
    private static final int REQUIRED_HYPERCUBES = 16;

    @Override
    public int priority() {
        // 高于 UpgradeShulkerContainerBehavior（空间压缩器升级），优先尝试超维存储站升级
        return 200;
    }

    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!hitBlockState.is(ModBlocks.SHULKER_CONTAINER)) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockPos mainPart = ModBlocks.SHULKER_CONTAINER.get().getMainPartPos(hitBlockPos, hitBlockState);
        BlockEntity blockEntity = serverLevel.getBlockEntity(mainPart);
        if (!(blockEntity instanceof ShulkerContainerBlockEntity shulker)) {
            return false;
        }
        UUID id = shulker.getId();
        if (id == null) {
            return false;
        }
        ShulkerContainerStorage oldStorage = Storages.get().get(id, ShulkerContainerStorage.class).orElse(null);
        if (oldStorage == null || oldStorage.getItems().getSpaceSize() < Upgrade2HyperdimensionStationBehavior.MAX_SPACE_SIZE) {
            // 未满级：交给空间压缩器升级 behavior 处理
            return false;
        }

        // 收集顶部实体：1x 奇点晶体 + 16x 超立方体
        List<ItemEntity> crystals = new ArrayList<>();
        List<ItemEntity> hypercubes = new ArrayList<>();
        int crystalCount = 0;
        int hypercubeCount = 0;
        for (ItemEntity entity : serverLevel.getEntitiesOfClass(
            ItemEntity.class,
            AabbUtil.createInclusive(hitBlockPos, hitBlockPos.above())
        )) {
            ItemStack stack = entity.getItem();
            if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
                crystals.add(entity);
                crystalCount += stack.getCount();
            } else if (stack.is(ModBlocks.HYPERCUBE.asItem())) {
                hypercubes.add(entity);
                hypercubeCount += stack.getCount();
            }
        }
        if (crystalCount < Upgrade2HyperdimensionStationBehavior.REQUIRED_CRYSTALS
            || hypercubeCount < Upgrade2HyperdimensionStationBehavior.REQUIRED_HYPERCUBES) {
            return false;
        }

        // 消耗 1x 奇点晶体
        int remaining = Upgrade2HyperdimensionStationBehavior.REQUIRED_CRYSTALS;
        for (ItemEntity entity : crystals) {
            ItemStack stack = entity.getItem();
            int shrink = Math.min(remaining, stack.getCount());
            remaining -= shrink;
            stack.shrink(shrink);
            if (stack.getCount() <= 0) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            if (remaining == 0) break;
        }
        // 消耗 16x 超立方体
        remaining = Upgrade2HyperdimensionStationBehavior.REQUIRED_HYPERCUBES;
        for (ItemEntity entity : hypercubes) {
            ItemStack stack = entity.getItem();
            int shrink = Math.min(remaining, stack.getCount());
            remaining -= shrink;
            stack.shrink(shrink);
            if (stack.getCount() <= 0) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            if (remaining == 0) break;
        }

        // 拆除潜影集装箱 3x3（与超维存储站同样以 BOTTOM_CENTER 为主方块）
        for (Cube3x3PartHalf half : Cube3x3PartHalf.values()) {
            serverLevel.setBlock(mainPart.offset(half.getOffset()), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }

        // 放置超维存储站主方块（setPlacedBy 会自动铺开 9 个 part）
        serverLevel.setBlock(
            mainPart,
            ModBlocks.HYPERDIMENSION_STORAGE_STATION.get().defaultBlockState()
                .setValue(HyperdimensionStorageStationBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER),
            Block.UPDATE_CLIENTS
        );
        BlockState placedState = serverLevel.getBlockState(mainPart);
        placedState.getBlock().setPlacedBy(serverLevel, mainPart, placedState, null, ItemStack.EMPTY);

        Storages.get().remove(id);

        BlockEntity newBe = serverLevel.getBlockEntity(mainPart);
        if (!(newBe instanceof HyperdimensionStorageStationBlockEntity station)) {
            return true;
        }
        id = station.getId();
        if (id == null) {
            id = UUID.randomUUID();
            station.setId(id);
        }
        HyperdimensionStorage newStorage = Storages.get().getOrCreate(id, HyperdimensionStorage.class);
        UnlimitedItemStacksResourceHandler newItems = newStorage.getItems();
        UnlimitedItemStacksResourceHandler oldItems = oldStorage.getItems();
        for (int i = 0; i < oldItems.size(); i++) {
            UnlimitedItemStack unlimitedStack = oldItems.getUnlimitedStackInSlot(i);
            if (unlimitedStack.isEmpty()) continue;
            newItems.insertItem(unlimitedStack.toStack(), false);
        }
        Storages.get().setDirty();

        return true;
    }
}
