package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public class CrateBlock extends Block implements EntityBlock, IHammerRemovable {
    public CrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CRATE.create(pos, state);
    }

    public static List<CrateBlockEntity> getNearbyCrates(Level level, BlockPos sourcePos) {
        List<CrateBlockEntity> crates = new ArrayList<>();
        CrateBlockEntity source = null;
        for (BlockPos pos : BlockPos.betweenClosed(sourcePos.offset(-1, -1, -1), sourcePos.offset(1, 1, 1))) {
            if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) {
                continue;
            }
            if (pos.equals(sourcePos)) {
                source = crate;
            } else {
                crates.add(crate);
            }
        }
        if (source != null) {
            crates.add(source);
        }
        return crates;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity be) {
            be.dropContents(level, pos);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity entity) {
            if (player.isSpectator()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (player.isShiftKeyDown() && itemStack.is(ModBlocks.LARGE_CRATE.asItem())) {
                if (level.isClientSide()) return ItemInteractionResult.sidedSuccess(true);
                return CrateBlock.mergeIntoLargeCrate(level, pos, itemStack, player);
            }
            if (player instanceof ServerPlayer) {
                return ItemInteractionResult.sidedSuccess(false);
            } else if (level.isClientSide()) {
                level.playSound(player, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos()));
                return ItemInteractionResult.sidedSuccess(true);
            }
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level instanceof Level realLevel) {
            StorageBlockEntity.applyPickStorageId(stack, realLevel, pos, state, ModStorageTypes.CRATE);
        }
        return stack;
    }

    private static ItemInteractionResult mergeIntoLargeCrate(
        Level level,
        BlockPos center,
        ItemStack largeCrateStack,
        Player player
    ) {
        BlockPos origin = CrateBlock.findLargeCrateOrigin(level, center);
        if (origin == null) return ItemInteractionResult.FAIL;

        List<CrateBlockEntity> crates = new ArrayList<>();
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = origin.offset(part.getOffset());
            if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate)) return ItemInteractionResult.FAIL;
            crates.add(crate);
        }

        StorageRef ref = largeCrateStack.get(ModComponents.STORAGE);
        UUID targetId = ref != null && ref.type() == ModStorageTypes.LARGE_CRATE
            ? ref.id().orElseGet(UUID::randomUUID)
            : UUID.randomUUID();
        LargeCrateStorage target = Storages.get().getOrCreate(targetId, LargeCrateStorage.class);
        SpaceSizeItemStacksResourceHandler targetItems = target.getItems();

        Set<UUID> sourceIds = new HashSet<>();
        List<UnlimitedItemStack> toTransfer = new ArrayList<>();
        for (CrateBlockEntity crate : crates) {
            UUID sourceId = crate.getId();
            if (sourceId == null || !sourceIds.add(sourceId)) continue;
            Optional<BaseStorage<?>> sourceOp = Storages.get().get(sourceId);
            if (sourceOp.isEmpty()) continue;
            BaseStorage<?> source = sourceOp.get();
            UnlimitedItemStacksResourceHandler items = source.getItems();
            for (int i = 0; i < items.size(); i++) {
                UnlimitedItemStack stack = items.getUnlimitedStackInSlot(i);
                if (stack.isEmpty()) continue;
                if (!targetItems.insertItem(stack.toStack(), true).isEmpty()) {
                    return ItemInteractionResult.FAIL;
                }
                toTransfer.add(stack);
            }
        }
        for (UnlimitedItemStack stack : toTransfer) {
            targetItems.insertItem(stack.toStack(), false);
        }
        targetItems.insertItem(ModBlocks.CRATE.asStack(27), false);

        Storages.get().put(target);
        for (UUID sourceId : sourceIds) {
            Storages.get().remove(sourceId);
        }

        // 先移除 27 个旧箱子：逐 part 放置会被多方块 updateShape 判定为结构不完整而破坏（未放置的邻居还是普通箱子）。
        // 参照多方块升级做法：全部置为 AIR 后放置主方块（不带邻居更新），再通过 setPlacedBy 铺开其余 part（此时邻居为已放置的同类 part）。
        // 被替换的 27 个普通箱子已塞入新板条箱内。
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = origin.offset(part.getOffset());
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
        level.setBlock(
            origin,
            ModBlocks.LARGE_CRATE.getDefaultState().setValue(LargeCrateBlock.HALF, Cube3x3PartHalf.BOTTOM_CENTER),
            Block.UPDATE_CLIENTS
        );
        BlockState placedState = level.getBlockState(origin);
        placedState.getBlock().setPlacedBy(level, origin, placedState, player, ItemStack.EMPTY);
        if (level.getBlockEntity(origin) instanceof StorageBlockEntity storage) {
            storage.setId(target.getId());
        }
        if (!player.hasInfiniteMaterials()) {
            largeCrateStack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    /**
     * 从被点击的箱子出发，扫描其周围可能的 3x3x3 大箱子区域，返回能使全部
     * 27 个 part 都是箱子的底层中心位置；找不到则返回 null。
     */
    private static @Nullable BlockPos findLargeCrateOrigin(Level level, BlockPos center) {
        Cube3x3PartHalf[] parts = Cube3x3PartHalf.values();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oy = 0; oy <= 2; oy++) {
                for (int oz = -1; oz <= 1; oz++) {
                    BlockPos candidate = center.offset(ox, oy - 2, oz);
                    boolean matched = true;
                    for (Cube3x3PartHalf part : parts) {
                        BlockPos pos = candidate.offset(part.getOffset());
                        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity)) {
                            matched = false;
                            break;
                        }
                    }
                    if (matched) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }
}
