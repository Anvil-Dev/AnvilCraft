package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.VoidMatterBlock;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
    /** 溢出销毁状态：相邻存在虚空物质块时置为 true，超出上限的输入被直接销毁 */
    public static final BooleanProperty DISPOSE = BooleanProperty.create("dispose");

    public CrateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CrateBlock.DISPOSE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CrateBlock.DISPOSE);
    }

    /**
     * 判断该板条箱位置是否相邻任意虚空物质块。
     *
     * <p>仅普通虚空物质块（{@link VoidMatterBlock}）触发销毁模式；激发态虚空物质
     * （{@code ExcitedStateVoidMatterBlock}）不触发，属有意设计（激发态不稳定，
     * 会随机衰变并连带使相邻普通虚空物质衰变，无法作为稳定的销毁源）。</p>
     *
     * <p>按 6 向（上下左右前后，不含斜角）判定，与虚空物质自身的邻接判定
     * （{@code VoidMatterBlock} 随机刻衰变、激发态衰变链均遍历 6 向）保持一致。</p>
     *
     * @param level 世界
     * @param pos   板条箱位置
     * @return 相邻存在普通虚空物质块时为 true
     */
    public static boolean hasAdjacentVoidMatter(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).getBlock() instanceof VoidMatterBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把该位置的板条箱状态按相邻虚空物质块同步为 dispose 开 / 关（值未变化时不动作）。
     */
    public static void updateDisposeState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CrateBlock) || level.isClientSide()) {
            return;
        }
        boolean dispose = CrateBlock.hasAdjacentVoidMatter(level, pos);
        if (state.getValue(CrateBlock.DISPOSE) != dispose) {
            level.setBlock(pos, state.setValue(CrateBlock.DISPOSE, dispose), Block.UPDATE_CLIENTS);
            // 方块状态变化不触发方块实体回调，这里直接同步存储的销毁标记
            if (level.getBlockEntity(pos) instanceof CrateBlockEntity crate) {
                crate.refreshDispose();
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CRATE.create(pos, state);
    }

    /**
     * 按 dispose 状态返回板条箱显示名：溢出销毁模式显示「溢出销毁板条箱」。
     */
    public static Component displayName(BlockState state) {
        return state.getValue(CrateBlock.DISPOSE)
            ? Component.translatable("block.anvilcraft.overflow_disposal_crate")
            : state.getBlock().getName();
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CrateBlockEntity be) {
            be.dropContents(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide() && !(newState.getBlock() instanceof CrateBlock)) {
            // 板条箱被移除后，相邻板条箱可能不再紧邻虚空物质，需要重新计算 dispose 状态
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                if (level.getBlockState(neighborPos).getBlock() instanceof CrateBlock) {
                    CrateBlock.updateDisposeState(level, neighborPos);
                }
            }
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        CrateBlock.updateDisposeState(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            CrateBlock.updateDisposeState(level, pos);
        }
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
                Component title = CrateBlock.displayName(state);
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos(), title));
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
        UUID targetId = ref != null && ref.type().is(ModStorageTypes.LARGE_CRATE.getKey())
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
