
package dev.dubhe.anvilcraft.block.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.api.event.SmartBlockPlacerFindPointerEvent;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

@Getter
@Setter
public class SmartBlockPlacerBlockEntity extends BlockEntity implements IPowerConsumer, MenuProvider, IDiskCloneable, IItemHandlerHolder {
    public static final int POSITION_GRID_SIZE = 5;
    public static final int POSITIONS_PER_LAYER = POSITION_GRID_SIZE * POSITION_GRID_SIZE;
    public static final int POSITION_COUNT = POSITION_GRID_SIZE * POSITIONS_PER_LAYER;
    private static final int POSITION_GRID_RADIUS = POSITION_GRID_SIZE / 2;
    private static final int POSITION_DISTANCE = 4;

    private @Nullable PowerGrid grid;

    private OperationMode operation = OperationMode.PICKUP;
    private TargetMode target = TargetMode.POSITION;
    private BlueprintPlacementMode placement = BlueprintPlacementMode.SKIP;
    private ExecutionPhase phase = ExecutionPhase.IDLE;
    /**
     * 当前阶段的执行进度；<br>
     * 取值范围为 0 到 1
     */
    private float phaseProgress = 0.0F;
    private @Nullable ITargetPointer pointer;
    private int selectedLayer;
    private int currentPlacementIndex;
    private final boolean[] layerPositions = new boolean[POSITION_COUNT];
    private StructureBlueprint blueprint = StructureBlueprint.empty();
    private @Nullable Either<ItemStack, BlockState> missingBlock;
    private @Nullable Either<ItemStack, BlockState> currentHeldBlock;
    private boolean loadingBlueprintInventory;

    private final ItemStackHandler blueprintItemHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot != 0 || !stack.is(ModItems.STRUCTURE_DISK.get())) {
                return false;
            }
            StructureDiskData data = stack.get(ModComponents.STRUCTURE_DISK_DATA);
            return data != null
                && data.sizeX() > 0 && data.sizeX() <= POSITION_GRID_SIZE
                && data.sizeY() > 0 && data.sizeY() <= POSITION_GRID_SIZE
                && data.sizeZ() > 0 && data.sizeZ() <= POSITION_GRID_SIZE;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (!SmartBlockPlacerBlockEntity.this.loadingBlueprintInventory) {
                SmartBlockPlacerBlockEntity.this.onBlueprintItemChanged();
            }
        }
    };

    private @Nullable BlockPos clientAnimationTargetPos;
    private boolean clientRetractSoundPlayed;

    // region Lifecycle and Target
    public SmartBlockPlacerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SMART_BLOCK_PLACER.get(), pos, blockState);
    }

    private SmartBlockPlacerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.resetExecutionState();
        if (!this.blueprintItemHandler.getStackInSlot(0).isEmpty()) {
            this.onBlueprintItemChanged();
        }
    }

    private void resetExecutionState() {
        this.phase = ExecutionPhase.IDLE;
        this.phaseProgress = 0.0F;
        this.pointer = null;
        this.currentHeldBlock = null;
        this.clientAnimationTargetPos = null;
        this.clientRetractSoundPlayed = false;
    }

    public static SmartBlockPlacerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new SmartBlockPlacerBlockEntity(type, pos, blockState);
    }

    /**
     * 放置器指向取料位置的方向，即放置器朝向的反向
     */
    public Direction getFacing() {
        BlockState state = this.getBlockState();
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
            ? state.getValue(HorizontalDirectionalBlock.FACING)
            : Direction.NORTH;
        return facing.getOpposite();
    }

    /**
     * 取料位置：放置器背面相邻的方块
     */
    public BlockPos getSourcePos() {
        return this.getBlockPos().relative(this.getFacing());
    }

    /**
     * 重新在取料位置寻找目标；<br>
     * 当前目标仍然有效时保持不变
     *
     * @return 当前目标，取料位置没有可用目标时为 {@code null}
     */
    public @Nullable ITargetPointer refreshPointer(ServerLevel level) {
        int previousPlacementIndex = this.currentPlacementIndex;
        Either<ItemStack, BlockState> previousHeldBlock = this.currentHeldBlock;
        ITargetPointer found = this.findPointer(level);
        boolean changed = found != this.pointer || previousPlacementIndex != this.currentPlacementIndex;
        this.pointer = found;
        if (found == null) {
            this.currentHeldBlock = null;
        } else if (this.target == TargetMode.POSITION) {
            this.currentHeldBlock = found.getDisplayedBlock();
        }
        changed |= !displayedBlocksMatch(previousHeldBlock, this.currentHeldBlock);
        if (changed) {
            this.setChanged();
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
        return found;
    }

    /**
     * 在取料位置寻找可用的目标：<br>
     * {@link OperationMode#PICKUP} 搬运物品，优先取容器中的方块物品，其次取掉落物；<br>
     * {@link OperationMode#MOVE} 搬运方块本身
     */
    private @Nullable ITargetPointer findPointer(ServerLevel level) {
        if (this.target == TargetMode.POSITION) {
            if (this.selectNextPositionTarget(level) == null) {
                return null;
            }
            return this.findPointer(level, null);
        }

        for (int checked = 0; checked < POSITION_COUNT; checked++) {
            BlueprintTarget blueprintTarget = this.findNextBlueprintTarget(level);
            if (blueprintTarget == null) {
                return null;
            }
            ITargetPointer found = this.findPointer(level, blueprintTarget.state());
            if (found != null) {
                this.updateMissingBlock(level, null);
                this.currentHeldBlock = this.createDisplayedBlock(level, blueprintTarget.state());
                return found;
            }
            this.updateMissingBlock(level, this.createDisplayedBlock(level, blueprintTarget.state()));
            if (this.placement == BlueprintPlacementMode.WAIT) {
                return null;
            }
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
        }
        return null;
    }

    private @Nullable ITargetPointer findPointer(ServerLevel level, @Nullable BlockState requiredState) {
        if (
            this.pointer != null
            && this.pointer.isStillValid(level)
            && (requiredState == null || this.pointer.matches(level, requiredState))
        ) {
            return this.pointer;
        }
        SmartBlockPlacerFindPointerEvent event = NeoForge.EVENT_BUS.post(new SmartBlockPlacerFindPointerEvent(
            level,
            this.getSourcePos(),
            this.getFacing(),
            this.operation,
            this.target,
            this.phase,
            requiredState
        ));
        if (event.isCanceled()) {
            return null;
        }
        return event.getPointer();
    }
    // endregion

    // region Execution
    public void tickClient() {
        if (this.phase == ExecutionPhase.IDLE) {
            this.clientAnimationTargetPos = null;
            this.clientRetractSoundPlayed = false;
        }
        if (this.canOperate()) {
            this.advancePhaseProgress();
        }
    }

    public void tickServer(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        this.flushState(serverLevel, pos);
        if (!this.canOperate()) {
            return;
        }
        if (this.phase == ExecutionPhase.IDLE) {
            if (this.advancePhaseProgress() && this.refreshPointer(serverLevel) != null) {
                this.transitionToPhase(serverLevel, pos, ExecutionPhase.PREPARE);
            }
        } else if (this.phase == ExecutionPhase.PREPARE) {
            if (this.advancePhaseProgress()) {
                if (!this.isCurrentTargetAvailable(serverLevel)) {
                    this.skipCurrentTarget();
                    this.pointer = null;
                    this.currentHeldBlock = null;
                    this.transitionToPhase(serverLevel, pos, ExecutionPhase.IDLE);
                } else if (this.refreshPointer(serverLevel) != null) {
                    this.transitionToPhase(serverLevel, pos, ExecutionPhase.EXTEND);
                } else {
                    this.transitionToPhase(serverLevel, pos, ExecutionPhase.IDLE);
                }
            }
        } else if (this.phase == ExecutionPhase.EXTEND) {
            if (this.advancePhaseProgress()) {
                this.applyToTargetPosition(serverLevel);
                this.transitionToPhase(serverLevel, pos, ExecutionPhase.RESET);
            }
        } else if (this.phase == ExecutionPhase.RESET) {
            if (this.advancePhaseProgress()) {
                this.transitionToPhase(serverLevel, pos, ExecutionPhase.IDLE);
            }
        }
    }

    private boolean advancePhaseProgress() {
        int phaseDuration = this.phase.getDurationTicks();
        if (phaseDuration <= 0) {
            this.phaseProgress = 1.0F;
            return true;
        }
        this.phaseProgress = Math.min(this.phaseProgress + 1.0F / phaseDuration, 1.0F);
        return this.phaseProgress >= 1.0F;
    }

    private void transitionToPhase(ServerLevel level, BlockPos pos, ExecutionPhase nextPhase) {
        this.phase = nextPhase;
        this.phaseProgress = 0.0F;
        this.setChanged();
        level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), 3);
    }
    // endregion

    // region Placement
    private void applyToTargetPosition(ServerLevel level) {
        if (this.target == TargetMode.BLUEPRINT) {
            this.applyToBlueprintPosition(level);
            return;
        }
        ITargetPointer pointer = this.pointer;
        if (pointer == null || !pointer.isStillValid(level)) {
            return;
        }
        List<BlockPos> positions = this.getOrderedPositionTargets();
        if (positions.isEmpty()) {
            return;
        }
        int index = Math.floorMod(this.currentPlacementIndex, positions.size());
        BlockPos targetPos = positions.get(index);
        if (!BlockPlacementUtil.isTargetAvailable(level, targetPos)) {
            this.advancePositionIndex(index, positions.size());
            return;
        }
        if (pointer.applyToPos(level, targetPos)) {
            this.advancePositionIndex(index, positions.size());
        } else if (pointer.isStillValid(level)) {
            this.advancePositionIndex(index, positions.size());
        }
    }

    private void applyToBlueprintPosition(ServerLevel level) {
        int expectedOrderIndex = Math.floorMod(this.currentPlacementIndex, POSITION_COUNT);
        BlueprintTarget blueprintTarget = this.findNextBlueprintTarget(level);
        ITargetPointer pointer = this.pointer;
        if (blueprintTarget == null || pointer == null || !pointer.isStillValid(level)) {
            return;
        }
        if (blueprintTarget.orderIndex() != expectedOrderIndex) {
            this.pointer = null;
            return;
        }
        if (!pointer.matches(level, blueprintTarget.state())) {
            this.pointer = null;
            return;
        }
        if (!BlockPlacementUtil.isTargetAvailable(level, blueprintTarget.pos())) {
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
            this.pointer = null;
            return;
        }
        BlockPlacementUtil.BlueprintLayout blueprintLayout = this.getBlueprintLayout();
        List<BlockPlacementUtil.BlueprintPartSnapshot> partSnapshots = blueprintLayout.capturePartSnapshots(
            level,
            blueprintTarget.pos(),
            blueprintTarget.state()
        );
        if (!BlockPlacementUtil.areTargetsAvailable(level, partSnapshots)) {
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
            this.pointer = null;
            return;
        }
        if (pointer.applyToPos(level, blueprintTarget.pos(), blueprintTarget.state())) {
            if (BlockPlacementUtil.applyBlueprintStates(level, partSnapshots)) {
                this.advanceBlueprintIndex(blueprintTarget.orderIndex());
                this.updateMissingBlock(level, null);
            } else {
                this.updateMissingBlock(level, this.createDisplayedBlock(level, blueprintTarget.state()));
            }
            this.pointer = null;
            this.setChanged();
            return;
        }
        this.pointer = null;
        this.updateMissingBlock(level, this.createDisplayedBlock(level, blueprintTarget.state()));
        if (this.placement == BlueprintPlacementMode.SKIP) {
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
        }
        this.setChanged();
    }

    private @Nullable BlueprintTarget findNextBlueprintTarget(ServerLevel level) {
        if (this.currentPlacementIndex >= POSITION_COUNT) {
            this.currentPlacementIndex = 0;
            this.setChanged();
            return null;
        }
        BlockPlacementUtil.BlueprintLayout blueprintLayout = this.getBlueprintLayout();
        while (this.currentPlacementIndex < POSITION_COUNT) {
            int orderIndex = this.currentPlacementIndex;
            int storageIndex = blueprintLayout.getStorageIndexForOrder(orderIndex);
            BlockState storedState = this.blueprint.states()[storageIndex];
            if (storedState.isAir()) {
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }

            BlockPos targetPos = blueprintLayout.getPosition(storageIndex);
            BlockState requiredState = blueprintLayout.getState(storageIndex);
            if (BlockPlacementUtil.isSecondaryMultiblockPart(requiredState)) {
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            BlockState worldState = level.getBlockState(targetPos);
            if (BlockPlacementUtil.isBlueprintStatePresent(level, targetPos, requiredState)) {
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            if (!worldState.canBeReplaced()) {
                this.updateMissingBlock(level, this.createDisplayedBlock(level, requiredState));
                if (this.placement == BlueprintPlacementMode.WAIT) {
                    return null;
                }
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            if (!BlockPlacementUtil.isTargetUnobstructed(level, targetPos)) {
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            return new BlueprintTarget(orderIndex, targetPos, requiredState);
        }
        this.currentPlacementIndex = 0;
        this.setChanged();
        return null;
    }

    private boolean isCurrentTargetAvailable(ServerLevel level) {
        if (this.target == TargetMode.BLUEPRINT) {
            BlockPlacementUtil.BlueprintLayout blueprintLayout = this.getBlueprintLayout();
            int orderIndex = Math.floorMod(this.currentPlacementIndex, POSITION_COUNT);
            int storageIndex = blueprintLayout.getStorageIndexForOrder(orderIndex);
            if (this.blueprint.states()[storageIndex].isAir()) {
                return false;
            }
            BlockPos targetPos = blueprintLayout.getPosition(storageIndex);
            BlockState requiredState = blueprintLayout.getState(storageIndex);
            return !BlockPlacementUtil.isSecondaryMultiblockPart(requiredState)
                && !BlockPlacementUtil.isBlueprintStatePresent(level, targetPos, requiredState)
                && BlockPlacementUtil.isTargetAvailable(level, targetPos);
        }

        List<BlockPos> positions = this.getOrderedPositionTargets();
        if (positions.isEmpty()) {
            return false;
        }
        BlockPos targetPos = positions.get(Math.floorMod(this.currentPlacementIndex, positions.size()));
        return BlockPlacementUtil.isTargetAvailable(level, targetPos);
    }

    private void skipCurrentTarget() {
        if (this.target == TargetMode.BLUEPRINT) {
            this.advanceBlueprintIndex(Math.floorMod(this.currentPlacementIndex, POSITION_COUNT));
            return;
        }
        List<BlockPos> positions = this.getOrderedPositionTargets();
        if (!positions.isEmpty()) {
            int index = Math.floorMod(this.currentPlacementIndex, positions.size());
            this.advancePositionIndex(index, positions.size());
        }
    }

    private void advancePositionIndex(int index, int positionCount) {
        this.currentPlacementIndex = (index + 1) % positionCount;
        this.setChanged();
    }

    private void advanceBlueprintIndex(int orderIndex) {
        this.currentPlacementIndex = orderIndex + 1;
        this.setChanged();
    }

    private Either<ItemStack, BlockState> createDisplayedBlock(Level level, BlockState state) {
        if (this.operation == OperationMode.PICKUP) {
            ItemStack stack = BlockPlacementRules.getPrimaryPlacementItem(
                level.registryAccess(),
                state.getBlock().defaultBlockState()
            );
            if (stack != null) {
                return Either.left(stack);
            }
        }
        return Either.right(state);
    }

    private void updateMissingBlock(ServerLevel level, @Nullable Either<ItemStack, BlockState> missingBlock) {
        if (this.placement == BlueprintPlacementMode.SKIP) {
            missingBlock = null;
        }
        if (displayedBlocksMatch(this.missingBlock, missingBlock)) {
            return;
        }
        this.missingBlock = missingBlock;
        this.setChanged();
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    private static boolean displayedBlocksMatch(
        @Nullable Either<ItemStack, BlockState> first,
        @Nullable Either<ItemStack, BlockState> second
    ) {
        if (first == null || second == null) {
            return first == second;
        }
        return Boolean.TRUE.equals(first.map(
            stack -> second.left().map(other -> ItemStack.matches(stack, other)).orElse(false),
            state -> second.right().map(state::equals).orElse(false)
        ));
    }
    // endregion

    // region Blueprint Placement
    public BlockPos getBlueprintPosition(int storageIndex) {
        return this.getBlueprintLayout().getPosition(storageIndex);
    }

    public BlockState getBlueprintStateForPlacement(int storageIndex) {
        return this.getBlueprintLayout().getState(storageIndex);
    }

    public BlockState getBlueprintStateForPlacement(
        int storageIndex,
        Direction targetFacing,
        boolean upsideDown
    ) {
        return this.getBlueprintLayout().getState(storageIndex, targetFacing, upsideDown);
    }

    private BlockPlacementUtil.BlueprintLayout getBlueprintLayout() {
        Level level = this.getLevel();
        if (level == null) {
            throw new IllegalStateException("Smart Block Placer is not attached to a level");
        }
        ItemStack blueprintItem = this.blueprintItemHandler.getStackInSlot(0);
        StructureDiskData diskData = blueprintItem.get(ModComponents.STRUCTURE_DISK_DATA);
        Direction scannerFacing = diskData == null ? Direction.NORTH : diskData.direction();
        return new BlockPlacementUtil.BlueprintLayout(
            level,
            this.getBlockPos(),
            this.getFacing().getOpposite(),
            scannerFacing,
            this.isUpsideDown(),
            POSITION_GRID_SIZE,
            POSITION_DISTANCE,
            this.blueprint.states()
        );
    }

    private boolean isUpsideDown() {
        BlockState state = this.getBlockState();
        return state.hasProperty(SmartBlockPlacerBlock.UPSIDE_DOWN)
            && state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
    }

    private record BlueprintTarget(int orderIndex, BlockPos pos, BlockState state) {
    }

    public record StructureBlueprint(String name, BlockState[] states, boolean invalid) {
        private static StructureBlueprint empty() {
            BlockState[] states = new BlockState[POSITION_COUNT];
            Arrays.fill(states, Blocks.AIR.defaultBlockState());
            return new StructureBlueprint("", states, false);
        }
    }
    // endregion

    // region Position Selection
    private @Nullable BlockPos selectNextPositionTarget(ServerLevel level) {
        List<BlockPos> positions = this.getOrderedPositionTargets();
        for (int offset = 0; offset < positions.size(); offset++) {
            int index = Math.floorMod(this.currentPlacementIndex + offset, positions.size());
            BlockPos targetPos = positions.get(index);
            if (BlockPlacementUtil.isTargetAvailable(level, targetPos)) {
                if (this.currentPlacementIndex != index) {
                    this.currentPlacementIndex = index;
                    this.setChanged();
                }
                return targetPos;
            }
        }
        return null;
    }

    private List<BlockPos> getOrderedPositionTargets() {
        Direction targetFacing = this.getFacing().getOpposite();
        BlockPos basePos = this.getBlockPos().relative(targetFacing, POSITION_DISTANCE);
        BlockState state = this.getBlockState();
        boolean upsideDown = state.hasProperty(SmartBlockPlacerBlock.UPSIDE_DOWN)
            && state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        return buildOrderedPositions(basePos, targetFacing, this.layerPositions, upsideDown);
    }

    /**
     * 构建 POSITION 模式的有序点位：从下到上、从左往右、从远到近。
     */
    public static List<BlockPos> buildOrderedPositions(
        BlockPos basePos,
        Direction facing,
        boolean[] layerPositions,
        boolean upsideDown
    ) {
        List<BlockPos> positions = new ArrayList<>();
        Direction right = facing.getClockWise();
        for (int layer = 0; layer < POSITION_GRID_SIZE; layer++) {
            int verticalOffset = upsideDown ? layer - POSITION_GRID_SIZE + 1 : layer;
            for (int column = 0; column < POSITION_GRID_SIZE; column++) {
                for (int row = 0; row < POSITION_GRID_SIZE; row++) {
                    int position = row * POSITION_GRID_SIZE + column;
                    if (!layerPositions[getPositionIndex(layer, position)]) {
                        continue;
                    }
                    positions.add(basePos.above(verticalOffset)
                        .relative(right, column - POSITION_GRID_RADIUS)
                        .relative(facing, POSITION_GRID_RADIUS - row));
                }
            }
        }
        return positions;
    }

    public void setSelectedLayer(int layer) {
        if (!isValidLayer(layer) || this.selectedLayer == layer) {
            return;
        }
        this.selectedLayer = layer;
        this.syncPositionSelection();
    }

    public void togglePosition(int layer, int position, boolean selected) {
        if (!isValidLayer(layer) || !isValidPosition(position)) {
            return;
        }
        int index = getPositionIndex(layer, position);
        if (this.layerPositions[index] == selected) {
            return;
        }
        this.layerPositions[index] = selected;
        this.currentPlacementIndex = 0;
        this.syncPositionSelection();
    }

    private static boolean isValidLayer(int layer) {
        return layer >= 0 && layer < POSITION_GRID_SIZE;
    }

    private static boolean isValidPosition(int position) {
        return position >= 0 && position < POSITIONS_PER_LAYER;
    }

    public static int getPositionIndex(int layer, int position) {
        return layer * POSITIONS_PER_LAYER + position;
    }

    private void syncPositionSelection() {
        this.setChanged();
        Level level = this.getLevel();
        if (level != null) {
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    private void savePositionSelection(CompoundTag tag) {
        tag.putInt("selectedLayer", this.selectedLayer);
        tag.putInt("currentPlacementIndex", this.currentPlacementIndex);
        byte[] storedPositions = new byte[POSITION_COUNT];
        for (int index = 0; index < POSITION_COUNT; index++) {
            storedPositions[index] = this.layerPositions[index] ? (byte) 1 : (byte) 0;
        }
        tag.putByteArray("layerPositions", storedPositions);
        tag.remove("positionMarks");
    }

    private void loadPositionSelection(CompoundTag tag) {
        int savedLayer = tag.getInt("selectedLayer");
        this.selectedLayer = isValidLayer(savedLayer) ? savedLayer : 0;
        this.currentPlacementIndex = Math.max(0, tag.getInt("currentPlacementIndex"));
        Arrays.fill(this.layerPositions, false);
        if (tag.contains("layerPositions", Tag.TAG_BYTE_ARRAY)) {
            this.loadPositionArray(tag.getByteArray("layerPositions"));
            return;
        }
        this.loadLegacyPositionSelection(tag);
    }

    private void loadPositionArray(byte[] storedPositions) {
        for (int index = 0; index < Math.min(POSITION_COUNT, storedPositions.length); index++) {
            this.layerPositions[index] = storedPositions[index] != 0;
        }
    }

    private void loadLegacyPositionSelection(CompoundTag tag) {
        if (tag.contains("positionMarks", Tag.TAG_BYTE_ARRAY)) {
            this.loadPositionArray(tag.getByteArray("positionMarks"));
            return;
        }
        if (!tag.contains("layerPositions", Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag layersTag = tag.getCompound("layerPositions");
        for (int layer = 0; layer < POSITION_GRID_SIZE; layer++) {
            for (int position : layersTag.getIntArray("layer_" + layer)) {
                if (isValidPosition(position)) {
                    this.layerPositions[getPositionIndex(layer, position)] = true;
                }
            }
        }
    }
    // endregion

    // region Blueprint Data
    private void onBlueprintItemChanged() {
        ItemStack blueprint = this.blueprintItemHandler.getStackInSlot(0);
        this.target = blueprint.isEmpty() ? TargetMode.POSITION : TargetMode.BLUEPRINT;
        this.currentPlacementIndex = 0;
        this.pointer = null;
        this.missingBlock = null;
        this.currentHeldBlock = null;
        if (blueprint.isEmpty()) {
            this.clearBlueprint();
        } else if (this.level != null && !this.level.isClientSide()) {
            this.reloadBlueprintFromItem();
        }
        this.syncPositionSelection();
    }

    private void reloadBlueprintFromItem() {
        this.clearBlueprint();
        ItemStack blueprint = this.blueprintItemHandler.getStackInSlot(0);
        StructureDiskData diskData = blueprint.get(ModComponents.STRUCTURE_DISK_DATA);
        if (blueprint.isEmpty() || diskData == null || this.level == null) {
            this.blueprint = new StructureBlueprint("", this.blueprint.states(), !blueprint.isEmpty());
            return;
        }

        StructureLoadUtil.StructureData structure = StructureLoadUtil.loadStructureFromDisk(this.level, blueprint);
        if (structure == null || structure.isEmpty()) {
            this.blueprint = new StructureBlueprint(diskData.name(), this.blueprint.states(), true);
            return;
        }

        int columnOffset = (POSITION_GRID_SIZE - diskData.sizeX()) / 2;
        int rowOffset = (POSITION_GRID_SIZE - diskData.sizeZ()) / 2;
        for (StructureLoadUtil.BlockPosition block : structure.blocks) {
            int column = columnOffset + diskData.sizeX() - block.x() - 1;
            int row = rowOffset + diskData.sizeZ() - block.z() - 1;
            int layer = block.y();
            if (!isValidLayer(layer)
                || column < 0 || column >= POSITION_GRID_SIZE
                || row < 0 || row >= POSITION_GRID_SIZE
                || block.state().isAir()) {
                continue;
            }
            int position = row * POSITION_GRID_SIZE + column;
            this.blueprint.states()[getPositionIndex(layer, position)] = block.state();
        }
        this.blueprint = new StructureBlueprint(diskData.name(), this.blueprint.states(), false);
    }

    private void clearBlueprint() {
        this.blueprint = StructureBlueprint.empty();
    }

    private void saveBlueprintData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag states = new ListTag();
        for (int index = 0; index < POSITION_COUNT; index++) {
            BlockState state = this.blueprint.states()[index];
            if (state.isAir()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("index", index);
            entry.put("state", NbtUtils.writeBlockState(state));
            states.add(entry);
        }
        tag.put("blueprintStates", states);
        tag.putString("loadedStructureName", this.blueprint.name());
        tag.putBoolean("invalidStructure", this.blueprint.invalid());
        saveDisplayedBlock(tag, "missingBlock", this.missingBlock, registries);
        saveDisplayedBlock(tag, "currentHeldBlock", this.currentHeldBlock, registries);
    }

    private void loadBlueprintData(CompoundTag tag, HolderLookup.Provider registries) {
        BlockState[] blueprintStates = new BlockState[POSITION_COUNT];
        Arrays.fill(blueprintStates, Blocks.AIR.defaultBlockState());
        ListTag states = tag.getList("blueprintStates", Tag.TAG_COMPOUND);
        for (int listIndex = 0; listIndex < states.size(); listIndex++) {
            CompoundTag entry = states.getCompound(listIndex);
            int index = entry.getInt("index");
            if (index < 0 || index >= POSITION_COUNT || !entry.contains("state", Tag.TAG_COMPOUND)) {
                continue;
            }
            blueprintStates[index] = NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK),
                entry.getCompound("state")
            );
        }
        this.blueprint = new StructureBlueprint(
            tag.getString("loadedStructureName"),
            blueprintStates,
            tag.getBoolean("invalidStructure")
        );
        this.missingBlock = loadDisplayedBlock(tag, "missingBlock", registries);
        if (this.missingBlock == null) {
            this.missingBlock = loadLegacyDisplayedBlock(tag, registries);
        }
        if (this.placement == BlueprintPlacementMode.SKIP) {
            this.missingBlock = null;
        }
        this.currentHeldBlock = loadDisplayedBlock(tag, "currentHeldBlock", registries);
    }

    private static void saveDisplayedBlock(
        CompoundTag tag,
        String key,
        @Nullable Either<ItemStack, BlockState> displayedBlock,
        HolderLookup.Provider registries
    ) {
        if (displayedBlock == null) {
            return;
        }
        CompoundTag displayedBlockTag = new CompoundTag();
        displayedBlock
            .ifLeft(stack -> displayedBlockTag.put("item", stack.save(registries)))
            .ifRight(state -> displayedBlockTag.put("state", NbtUtils.writeBlockState(state)));
        tag.put(key, displayedBlockTag);
    }

    private static @Nullable Either<ItemStack, BlockState> loadDisplayedBlock(
        CompoundTag tag,
        String key,
        HolderLookup.Provider registries
    ) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag displayedBlockTag = tag.getCompound(key);
        if (displayedBlockTag.contains("item", Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parseOptional(registries, displayedBlockTag.getCompound("item"));
            return stack.isEmpty() ? null : Either.left(stack);
        }
        if (displayedBlockTag.contains("state", Tag.TAG_COMPOUND)) {
            return Either.right(NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK),
                displayedBlockTag.getCompound("state")
            ));
        }
        return null;
    }

    private static @Nullable Either<ItemStack, BlockState> loadLegacyDisplayedBlock(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("missingBlockItem", Tag.TAG_COMPOUND)) {
            return null;
        }
        ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("missingBlockItem"));
        return stack.isEmpty() ? null : Either.left(stack);
    }
    // endregion

    // region State
    public boolean hasBlueprint() {
        for (BlockState state : this.blueprint.states()) {
            if (!state.isAir()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPickupMode() {
        return this.operation == OperationMode.PICKUP;
    }

    public void setPickupMode(boolean pickupMode) {
        this.operation = pickupMode ? OperationMode.PICKUP : OperationMode.MOVE;
        this.pointer = null;
        this.missingBlock = null;
        this.currentHeldBlock = null;
        this.syncPositionSelection();
    }

    public boolean isSkipMissingMode() {
        return this.placement == BlueprintPlacementMode.SKIP;
    }

    public void setSkipMissingMode(boolean skipMissingMode) {
        this.placement = skipMissingMode
            ? BlueprintPlacementMode.SKIP
            : BlueprintPlacementMode.WAIT;
        this.pointer = null;
        this.missingBlock = null;
        this.syncPositionSelection();
    }

    public @Nullable BlockPos getCurrentBlueprintTargetPosition() {
        Level level = this.getLevel();
        BlockPlacementUtil.BlueprintLayout blueprintLayout = this.getBlueprintLayout();
        for (int orderIndex = this.currentPlacementIndex; orderIndex < POSITION_COUNT; orderIndex++) {
            int storageIndex = blueprintLayout.getStorageIndexForOrder(orderIndex);
            if (this.blueprint.states()[storageIndex].isAir()) {
                continue;
            }
            BlockPos targetPos = blueprintLayout.getPosition(storageIndex);
            if (level != null) {
                BlockState requiredState = blueprintLayout.getState(storageIndex);
                if (BlockPlacementUtil.isSecondaryMultiblockPart(requiredState)) {
                    continue;
                }
                BlockState worldState = level.getBlockState(targetPos);
                if (BlockPlacementUtil.isBlueprintStatePresent(level, targetPos, requiredState)
                    || !worldState.canBeReplaced()
                    || !BlockPlacementUtil.isTargetUnobstructed(level, targetPos)) {
                    continue;
                }
            }
            return targetPos;
        }
        return null;
    }

    public int getComparatorOutput() {
        int total = 0;
        int placed = 0;
        Level level = this.getLevel();
        if (level == null) {
            return 0;
        }
        if (this.target == TargetMode.BLUEPRINT) {
            BlockPlacementUtil.BlueprintLayout blueprintLayout = this.getBlueprintLayout();
            for (int index = 0; index < POSITION_COUNT; index++) {
                if (this.blueprint.states()[index].isAir()) {
                    continue;
                }
                total++;
                BlockState requiredState = blueprintLayout.getState(index);
                if (BlockPlacementUtil.isBlueprintStatePresent(
                    level,
                    blueprintLayout.getPosition(index),
                    requiredState
                )) {
                    placed++;
                }
            }
        } else {
            for (BlockPos pos : this.getOrderedPositionTargets()) {
                total++;
                if (!level.getBlockState(pos).canBeReplaced()) {
                    placed++;
                }
            }
        }
        return total == 0 ? 0 : Math.min(15, placed * 15 / total);
    }

    public boolean isAnimationActive() {
        return this.phase != ExecutionPhase.IDLE;
    }

    public float getAnimationProgress(float partialTick) {
        if (!this.isAnimationActive()) {
            return 0.0F;
        }
        float interpolatedPhaseProgress = this.phaseProgress;
        if (this.canOperate()) {
            interpolatedPhaseProgress = Math.min(
                interpolatedPhaseProgress + partialTick / this.phase.getDurationTicks(),
                1.0F
            );
        }
        float elapsedTicks = this.phase.getAnimationStartTick()
            + interpolatedPhaseProgress * this.phase.getDurationTicks();
        return elapsedTicks / ExecutionPhase.getAnimationDurationTicks();
    }

    public boolean isOverloaded() {
        BlockState state = this.getCurrentBlockState();
        return state.hasProperty(SmartBlockPlacerBlock.OVERLOAD)
            && state.getValue(SmartBlockPlacerBlock.OVERLOAD);
    }

    public boolean isRedstoneLocked() {
        BlockState state = this.getCurrentBlockState();
        return state.hasProperty(SmartBlockPlacerBlock.POWERED)
            && state.getValue(SmartBlockPlacerBlock.POWERED);
    }

    public boolean canOperate() {
        return !this.isOverloaded() && !this.isRedstoneLocked();
    }

    private BlockState getCurrentBlockState() {
        return this.level == null ? this.getBlockState() : this.level.getBlockState(this.getBlockPos());
    }
    // endregion

    // region BlockEntity - Update
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    // endregion

    // region BlockEntity - Save
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        tag.put("operation", OperationMode.CODEC.encodeStart(ops, this.operation).getOrThrow());
        tag.put("target", TargetMode.CODEC.encodeStart(ops, this.target).getOrThrow());
        tag.put("placement", BlueprintPlacementMode.CODEC.encodeStart(ops, this.placement).getOrThrow());
        tag.put("phase", ExecutionPhase.CODEC.encodeStart(ops, this.phase).getOrThrow());
        tag.putFloat("progress", this.phaseProgress);
        tag.put("blueprintInventory", this.blueprintItemHandler.serializeNBT(registries));
        this.savePositionSelection(tag);
        this.saveBlueprintData(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (tag.contains("operation")) {
            this.operation = OperationMode.CODEC.parse(ops, tag.get("operation")).result().orElse(OperationMode.PICKUP);
        }
        if (tag.contains("target")) {
            this.target = TargetMode.CODEC.parse(ops, tag.get("target")).result().orElse(TargetMode.POSITION);
        }
        if (tag.contains("placement")) {
            this.placement = BlueprintPlacementMode.CODEC.parse(ops, tag.get("placement")).result().orElse(BlueprintPlacementMode.SKIP);
        }
        this.loadLegacyModeData(tag, ops);
        if (tag.contains("phase")) {
            this.phase = ExecutionPhase.CODEC.parse(ops, tag.get("phase")).result().orElse(ExecutionPhase.IDLE);
        }
        this.phaseProgress = tag.getFloat("progress");
        this.loadBlueprintItem(tag, registries);
        this.loadPositionSelection(tag);
        this.loadBlueprintData(tag, registries);
        this.target = this.blueprintItemHandler.getStackInSlot(0).isEmpty()
            ? TargetMode.POSITION
            : TargetMode.BLUEPRINT;
        this.pointer = null;
    }

    private void loadBlueprintItem(CompoundTag tag, HolderLookup.Provider registries) {
        this.loadingBlueprintInventory = true;
        try {
            if (tag.contains("blueprintInventory", Tag.TAG_COMPOUND)) {
                this.blueprintItemHandler.deserializeNBT(registries, tag.getCompound("blueprintInventory"));
            } else {
                this.loadLegacyBlueprintItem(tag, registries);
            }
        } finally {
            this.loadingBlueprintInventory = false;
        }
    }

    private void loadLegacyModeData(CompoundTag tag, RegistryOps<Tag> ops) {
        if (!tag.contains("operation") && tag.contains("isPickupMode", Tag.TAG_BYTE)) {
            this.operation = tag.getBoolean("isPickupMode") ? OperationMode.PICKUP : OperationMode.MOVE;
        }
        if (tag.contains("placement")) {
            return;
        }
        if (tag.contains("blueprintPlacementMode")) {
            this.placement = BlueprintPlacementMode.CODEC.parse(ops, tag.get("blueprintPlacementMode"))
                .result().orElse(BlueprintPlacementMode.SKIP);
        } else if (tag.contains("isSkipMissingMode", Tag.TAG_BYTE)) {
            this.placement = tag.getBoolean("isSkipMissingMode")
                ? BlueprintPlacementMode.SKIP
                : BlueprintPlacementMode.WAIT;
        }
    }

    private void loadLegacyBlueprintItem(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack blueprintItem = ItemStack.EMPTY;
        if (tag.contains("diskInventory", Tag.TAG_COMPOUND)) {
            blueprintItem = ItemStack.parseOptional(registries, tag.getCompound("diskInventory"));
        } else if (tag.contains("diskInventory", Tag.TAG_LIST)) {
            blueprintItem = loadLegacyBlueprintItem(tag.getList("diskInventory", Tag.TAG_COMPOUND), registries);
        }
        if (blueprintItem.isEmpty() && tag.contains("Items", Tag.TAG_LIST)) {
            blueprintItem = loadLegacyBlueprintItem(tag.getList("Items", Tag.TAG_COMPOUND), registries);
        }
        if (blueprintItem.is(ModItems.STRUCTURE_DISK.get())) {
            this.blueprintItemHandler.setStackInSlot(0, blueprintItem);
        }
    }

    private static ItemStack loadLegacyBlueprintItem(ListTag items, HolderLookup.Provider registries) {
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entry = items.getCompound(index);
            CompoundTag itemTag = entry.contains("Item", Tag.TAG_COMPOUND)
                ? entry.getCompound("Item")
                : entry;
            ItemStack stack = ItemStack.parseOptional(registries, itemTag);
            if (stack.is(ModItems.STRUCTURE_DISK.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    // endregion

    // region IPowerConsumer
    @Override
    public int getInputPower() {
        return this.target.getPower();
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.blueprintItemHandler;
    }
    // endregion

    // region MenuProvider
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.smart_block_placer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SmartBlockPlacerMenu(ModMenuTypes.SMART_BLOCK_PLACER.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }
    // endregion

    // region IDiskCloneable
    @Override
    public void storeDiskData(CompoundTag tag) {
        if (this.getLevel() == null) {
            return;
        }
        RegistryOps<Tag> ops = this.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        tag.put("operation", OperationMode.CODEC.encodeStart(ops, this.operation).getOrThrow());
        tag.put("target", TargetMode.CODEC.encodeStart(ops, this.target).getOrThrow());
        tag.put("placement", BlueprintPlacementMode.CODEC.encodeStart(ops, this.placement).getOrThrow());
        this.savePositionSelection(tag);
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        if (this.getLevel() == null) {
            return;
        }
        RegistryOps<Tag> ops = this.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (data.contains("operation")) {
            OperationMode.CODEC.parse(ops, data.get("operation")).result()
                .ifPresent(operation -> this.operation = operation);
        }
        if (data.contains("target")) {
            TargetMode.CODEC.parse(ops, data.get("target")).result()
                .ifPresent(target -> this.target = target);
        }
        if (data.contains("placement")) {
            BlueprintPlacementMode.CODEC.parse(ops, data.get("placement")).result()
                .ifPresent(placement -> this.placement = placement);
        }
        this.loadLegacyModeData(data, ops);
        if (this.placement == BlueprintPlacementMode.SKIP) {
            this.missingBlock = null;
        }
        this.loadPositionSelection(data);
        this.resetExecutionState();
        this.syncPositionSelection();
    }
    // endregion

    // region Modes
    public enum OperationMode implements StringRepresentable {
        PICKUP,
        MOVE,
        ;

        public static final Codec<OperationMode> CODEC = StringRepresentable.fromEnum(OperationMode::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Getter
    public enum TargetMode implements StringRepresentable {
        POSITION(16),
        BLUEPRINT(128),
        ;

        public static final Codec<TargetMode> CODEC = StringRepresentable.fromEnum(TargetMode::values);
        private final int power;

        TargetMode(int power) {
            this.power = power;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Getter
    public enum BlueprintPlacementMode implements StringRepresentable {
        SKIP,
        WAIT,
        ;

        public static final Codec<BlueprintPlacementMode> CODEC = StringRepresentable.fromEnum(BlueprintPlacementMode::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Getter
    public enum ExecutionPhase implements StringRepresentable {
        IDLE(0, 0),
        PREPARE(0, 6),
        EXTEND(6, 8),
        RESET(14, 6),
        ;

        public static final Codec<ExecutionPhase> CODEC = StringRepresentable.fromEnum(ExecutionPhase::values);
        private final int animationStartTick;
        private final int durationTicks;

        ExecutionPhase(int animationStartTick, int durationTicks) {
            this.animationStartTick = animationStartTick;
            this.durationTicks = durationTicks;
        }

        public static int getAnimationDurationTicks() {
            return RESET.animationStartTick + RESET.durationTicks;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
    // endregion
}
