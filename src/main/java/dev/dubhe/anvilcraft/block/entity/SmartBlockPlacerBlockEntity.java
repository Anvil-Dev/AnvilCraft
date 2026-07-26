
package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
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
import dev.dubhe.anvilcraft.util.StructureBookUtil;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

@Getter
@Setter
public class SmartBlockPlacerBlockEntity extends BlockEntity implements IPowerConsumer, MenuProvider, IDiskCloneable, IItemHandlerHolder {
    private static final ThreadLocal<Boolean> BLOCK_BEING_MOVED = ThreadLocal.withInitial(() -> false);
    private static final int PLACEMENT_INTERVAL = 20;
    public static final int POSITION_GRID_SIZE = 5;
    public static final int POSITIONS_PER_LAYER = POSITION_GRID_SIZE * POSITION_GRID_SIZE;
    public static final int POSITION_COUNT = POSITION_GRID_SIZE * POSITIONS_PER_LAYER;
    private static final int POSITION_GRID_RADIUS = POSITION_GRID_SIZE / 2;
    private static final int POSITION_DISTANCE = 4;

    private @Nullable PowerGrid grid;

    private OperationMode operation = OperationMode.PICKUP;
    private TargetMode target = TargetMode.POSITION;
    private BlueprintPlacementMode blueprintPlacementMode = BlueprintPlacementMode.SKIP;
    private ExecutionPhase phase = ExecutionPhase.IDLE;
    /**
     * 当前阶段的执行进度；<br>
     * 仅在 {@link SmartBlockPlacerBlockEntity#phase} 不为 {@link ExecutionPhase#IDLE} 时可用
     */
    private float progress = 0.0F;
    private @Nullable ITargetPointer pointer;
    private int selectedLayer;
    private int currentPlacementIndex;
    private final boolean[] layerPositions = new boolean[POSITION_COUNT];
    private final BlockState[] blueprintStates = new BlockState[POSITION_COUNT];
    private String loadedStructureName = "";
    private @Nullable UUID loadedStructureUuid;
    private boolean invalidStructure;
    private ItemStack missingBlockItem = ItemStack.EMPTY;
    private ItemStack currentHeldBlock = ItemStack.EMPTY;
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

    private final SimpleContainer bookInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmartBlockPlacerBlockEntity.this.setChanged();
            Level level = SmartBlockPlacerBlockEntity.this.getLevel();
            if (!SmartBlockPlacerBlockEntity.this.loadingBlueprintInventory
                && level != null && !level.isClientSide() && !this.getItem(0).isEmpty()) {
                StructureBookUtil.generateMaterialListBookToOutput(
                    level,
                    SmartBlockPlacerBlockEntity.this.getBlockPos(),
                    SmartBlockPlacerBlockEntity.this
                );
            }
        }
    };
    private final SimpleContainer outputBookInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmartBlockPlacerBlockEntity.this.setChanged();
        }
    };

    private long clientAnimationStartTime;
    private @Nullable BlockPos clientLastTargetPos;
    private boolean clientIsRetracting;
    private long clientRetractStartTime;
    private float[] clientRetractStartAngles = new float[4];
    private float clientRetractStartProgress;
    private boolean retractSoundPlayed;

    public SmartBlockPlacerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SMART_BLOCK_PLACER.get(), pos, blockState);
    }

    private SmartBlockPlacerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        Arrays.fill(this.blueprintStates, Blocks.AIR.defaultBlockState());
    }

    public static SmartBlockPlacerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new SmartBlockPlacerBlockEntity(type, pos, blockState);
    }

    public static boolean isBlockBeingMovedByPlacer() {
        return BLOCK_BEING_MOVED.get();
    }

    public static void setBlockBeingMovedByPlacer(boolean moving) {
        BLOCK_BEING_MOVED.set(moving);
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
        ITargetPointer found = this.findPointer(level);
        boolean changed = found != this.pointer || previousPlacementIndex != this.currentPlacementIndex;
        this.pointer = found;
        if (found == null && !this.currentHeldBlock.isEmpty()) {
            this.currentHeldBlock = ItemStack.EMPTY;
            changed = true;
        }
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
            if (!this.hasAvailablePosition(level)) {
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
                this.updateMissingBlockItem(level, ItemStack.EMPTY);
                this.currentHeldBlock = blueprintTarget.state().getBlock().asItem().getDefaultInstance();
                return found;
            }
            this.updateMissingBlockItem(
                level,
                blueprintTarget.state().getBlock().asItem().getDefaultInstance()
            );
            if (this.blueprintPlacementMode == BlueprintPlacementMode.WAIT) {
                return null;
            }
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
        }
        return null;
    }

    private @Nullable ITargetPointer findPointer(ServerLevel level, @Nullable BlockState requiredState) {
        if (this.pointer != null
            && this.pointer.isStillValid(level)
            && (requiredState == null || this.pointer.matches(requiredState))) {
            return this.pointer;
        }
        SmartBlockPlacerFindPointerEvent event = new SmartBlockPlacerFindPointerEvent(
            level,
            this.getSourcePos(),
            this.getFacing(),
            this.operation,
            this.target,
            this.phase,
            requiredState
        );
        SmartBlockPlacerFindPointerEvent postedEvent = NeoForge.EVENT_BUS.post(event);
        if (postedEvent.isCanceled()) {
            return null;
        }
        return postedEvent.getPointer();
    }

    public void tickClient() {
        this.advancePhaseProgress();
    }

    public void tickServer(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (Objects.requireNonNull(this.phase) == ExecutionPhase.IDLE) {
            if (this.refreshPointer(serverLevel) != null) {
                this.changePhase(serverLevel, pos, ExecutionPhase.PREPARE);
            }
        } else if (this.phase == ExecutionPhase.PREPARE) {
            if (this.advancePhaseProgress()) {
                this.changePhase(serverLevel, pos, ExecutionPhase.EXTEND);
            }
        } else if (this.phase == ExecutionPhase.EXTEND) {
            if (this.advancePhaseProgress()) {
                this.applyToTargetPosition(serverLevel);
                this.changePhase(serverLevel, pos, ExecutionPhase.RESET);
            }
        } else if (this.phase == ExecutionPhase.RESET) {
            if (this.advancePhaseProgress()) {
                ExecutionPhase nextPhase = ExecutionPhase.IDLE;
                if (this.refreshPointer(serverLevel) != null) {
                    nextPhase = ExecutionPhase.PREPARE;
                }
                this.changePhase(serverLevel, pos, nextPhase);
            }
        }
    }

    private boolean advancePhaseProgress() {
        if (this.phase == ExecutionPhase.IDLE) {
            return false;
        }
        float phaseInterval = PLACEMENT_INTERVAL * this.phase.getIntervalPercent();
        this.progress = Math.min(this.progress + 1.0F / phaseInterval, 1.0F);
        return this.progress >= 1.0F;
    }

    private void changePhase(ServerLevel level, BlockPos pos, ExecutionPhase phase) {
        this.phase = phase;
        this.progress = 0.0F;
        this.setChanged();
        level.sendBlockUpdated(pos, this.getBlockState(), this.getBlockState(), 3);
    }

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
        if (this.currentPlacementIndex >= positions.size()) {
            this.currentPlacementIndex = 0;
        }
        for (int offset = 0; offset < positions.size(); offset++) {
            int index = (this.currentPlacementIndex + offset) % positions.size();
            BlockPos targetPos = positions.get(index);
            if (!level.getBlockState(targetPos).canBeReplaced()) {
                continue;
            }
            if (pointer.applyToPos(level, targetPos)) {
                this.currentPlacementIndex = (index + 1) % positions.size();
                return;
            }
            if (!pointer.isStillValid(level)) {
                return;
            }
        }
    }

    private void applyToBlueprintPosition(ServerLevel level) {
        BlueprintTarget blueprintTarget = this.findNextBlueprintTarget(level);
        ITargetPointer pointer = this.pointer;
        if (blueprintTarget == null || pointer == null || !pointer.isStillValid(level)) {
            return;
        }
        if (!pointer.matches(blueprintTarget.state())) {
            this.pointer = null;
            return;
        }
        if (pointer.applyToPos(level, blueprintTarget.pos(), blueprintTarget.state())) {
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
            this.updateMissingBlockItem(level, ItemStack.EMPTY);
            this.pointer = null;
            this.setChanged();
            return;
        }
        this.pointer = null;
        this.updateMissingBlockItem(
            level,
            blueprintTarget.state().getBlock().asItem().getDefaultInstance()
        );
        if (this.blueprintPlacementMode == BlueprintPlacementMode.SKIP) {
            this.advanceBlueprintIndex(blueprintTarget.orderIndex());
        }
        this.setChanged();
    }

    private @Nullable BlueprintTarget findNextBlueprintTarget(ServerLevel level) {
        for (int checked = 0; checked < POSITION_COUNT; checked++) {
            int orderIndex = Math.floorMod(this.currentPlacementIndex, POSITION_COUNT);
            int storageIndex = getStorageIndexForOrder(orderIndex);
            BlockState storedState = this.blueprintStates[storageIndex];
            if (storedState.isAir()) {
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }

            BlockPos targetPos = this.getBlueprintPosition(storageIndex);
            BlockState requiredState = this.getBlueprintStateForPlacement(storageIndex);
            BlockState worldState = level.getBlockState(targetPos);
            if (worldState.equals(requiredState)) {
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            if (worldState.is(requiredState.getBlock())) {
                level.setBlock(targetPos, requiredState, 3);
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            if (!worldState.canBeReplaced()) {
                this.updateMissingBlockItem(
                    level,
                    requiredState.getBlock().asItem().getDefaultInstance()
                );
                if (this.blueprintPlacementMode == BlueprintPlacementMode.WAIT) {
                    return null;
                }
                this.advanceBlueprintIndex(orderIndex);
                continue;
            }
            return new BlueprintTarget(orderIndex, targetPos, requiredState);
        }
        return null;
    }

    private void advanceBlueprintIndex(int orderIndex) {
        this.currentPlacementIndex = (orderIndex + 1) % POSITION_COUNT;
        this.setChanged();
    }

    private void updateMissingBlockItem(ServerLevel level, ItemStack stack) {
        if (ItemStack.matches(this.missingBlockItem, stack)) {
            return;
        }
        this.missingBlockItem = stack;
        this.setChanged();
        level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    private static int getStorageIndexForOrder(int orderIndex) {
        int layer = orderIndex / POSITIONS_PER_LAYER;
        int inLayer = orderIndex % POSITIONS_PER_LAYER;
        int column = inLayer / POSITION_GRID_SIZE;
        int row = inLayer % POSITION_GRID_SIZE;
        return getPositionIndex(layer, row * POSITION_GRID_SIZE + column);
    }

    public BlockPos getBlueprintPosition(int storageIndex) {
        int layer = storageIndex / POSITIONS_PER_LAYER;
        int position = storageIndex % POSITIONS_PER_LAYER;
        int row = position / POSITION_GRID_SIZE;
        int column = position % POSITION_GRID_SIZE;
        Direction targetFacing = this.getFacing().getOpposite();
        Direction right = targetFacing.getClockWise();
        boolean upsideDown = this.isUpsideDown();
        int verticalOffset = upsideDown ? layer - POSITION_GRID_SIZE + 1 : layer;
        return this.getBlockPos().relative(targetFacing, POSITION_DISTANCE)
            .above(verticalOffset)
            .relative(right, column - POSITION_GRID_RADIUS)
            .relative(targetFacing, POSITION_GRID_RADIUS - row);
    }

    public BlockState getBlueprintStateForPlacement(int storageIndex) {
        return this.getBlueprintStateForPlacement(
            storageIndex,
            this.getFacing().getOpposite(),
            this.isUpsideDown()
        );
    }

    @SuppressWarnings("deprecation")
    public BlockState getBlueprintStateForPlacement(
        int storageIndex,
        Direction targetFacing,
        boolean upsideDown
    ) {
        if (storageIndex < 0 || storageIndex >= POSITION_COUNT) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state = this.blueprintStates[storageIndex].rotate(this.getBlueprintRotation(targetFacing));
        return upsideDown ? flipHalfPropertyStatic(state) : state;
    }

    private Rotation getBlueprintRotation(Direction targetFacing) {
        ItemStack blueprint = this.blueprintItemHandler.getStackInSlot(0);
        StructureDiskData data = blueprint.get(ModComponents.STRUCTURE_DISK_DATA);
        Direction scannerFacing = data == null ? Direction.NORTH : data.direction();
        int placerRotation = switch (targetFacing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        int scannerCorrection = switch (scannerFacing) {
            case NORTH -> 2;
            case WEST -> 3;
            case EAST -> 1;
            default -> 0;
        };
        return switch ((placerRotation + scannerCorrection) % 4) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private boolean isUpsideDown() {
        BlockState state = this.getBlockState();
        return state.hasProperty(SmartBlockPlacerBlock.UPSIDE_DOWN)
            && state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
    }

    private record BlueprintTarget(int orderIndex, BlockPos pos, BlockState state) {
    }

    private boolean hasAvailablePosition(ServerLevel level) {
        for (BlockPos pos : this.getOrderedPositionTargets()) {
            if (level.getBlockState(pos).canBeReplaced()) {
                return true;
            }
        }
        return false;
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
            byte[] storedPositions = tag.getByteArray("layerPositions");
            for (int index = 0; index < Math.min(POSITION_COUNT, storedPositions.length); index++) {
                this.layerPositions[index] = storedPositions[index] != 0;
            }
            return;
        }
        if (tag.contains("positionMarks", Tag.TAG_BYTE_ARRAY)) {
            byte[] storedPositions = tag.getByteArray("positionMarks");
            for (int index = 0; index < Math.min(POSITION_COUNT, storedPositions.length); index++) {
                this.layerPositions[index] = storedPositions[index] != 0;
            }
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

    private void onBlueprintItemChanged() {
        ItemStack blueprint = this.blueprintItemHandler.getStackInSlot(0);
        this.target = blueprint.isEmpty() ? TargetMode.POSITION : TargetMode.BLUEPRINT;
        this.currentPlacementIndex = 0;
        this.pointer = null;
        this.missingBlockItem = ItemStack.EMPTY;
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
            this.invalidStructure = !blueprint.isEmpty();
            return;
        }

        this.loadedStructureName = diskData.name();
        this.loadedStructureUuid = diskData.uuid();
        StructureLoadUtil.StructureData structure = StructureLoadUtil.loadStructureFromDisk(this.level, blueprint);
        if (structure == null || structure.isEmpty()) {
            this.invalidStructure = true;
            return;
        }

        int columnOffset = (POSITION_GRID_SIZE - diskData.sizeX()) / 2;
        int rowOffset = (POSITION_GRID_SIZE - diskData.sizeZ()) / 2;
        for (StructureLoadUtil.BlockPosition block : structure.blocks) {
            int column = columnOffset + block.x();
            int row = rowOffset + diskData.sizeZ() - block.z() - 1;
            int layer = block.y();
            if (!isValidLayer(layer)
                || column < 0 || column >= POSITION_GRID_SIZE
                || row < 0 || row >= POSITION_GRID_SIZE
                || block.state().isAir()) {
                continue;
            }
            int position = row * POSITION_GRID_SIZE + column;
            this.blueprintStates[getPositionIndex(layer, position)] = block.state();
        }
    }

    private void clearBlueprint() {
        Arrays.fill(this.blueprintStates, Blocks.AIR.defaultBlockState());
        this.loadedStructureName = "";
        this.loadedStructureUuid = null;
        this.invalidStructure = false;
    }

    private void saveBlueprintStates(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag states = new ListTag();
        for (int index = 0; index < POSITION_COUNT; index++) {
            BlockState state = this.blueprintStates[index];
            if (state.isAir()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("index", index);
            entry.put("state", NbtUtils.writeBlockState(state));
            states.add(entry);
        }
        tag.put("blueprintStates", states);
        tag.putString("loadedStructureName", this.loadedStructureName);
        if (this.loadedStructureUuid != null) {
            tag.putUUID("loadedStructureUuid", this.loadedStructureUuid);
        }
        tag.putBoolean("invalidStructure", this.invalidStructure);
        if (!this.missingBlockItem.isEmpty()) {
            tag.put("missingBlockItem", this.missingBlockItem.save(registries));
        }
    }

    private void loadBlueprintStates(CompoundTag tag, HolderLookup.Provider registries) {
        Arrays.fill(this.blueprintStates, Blocks.AIR.defaultBlockState());
        ListTag states = tag.getList("blueprintStates", Tag.TAG_COMPOUND);
        for (int listIndex = 0; listIndex < states.size(); listIndex++) {
            CompoundTag entry = states.getCompound(listIndex);
            int index = entry.getInt("index");
            if (index < 0 || index >= POSITION_COUNT || !entry.contains("state", Tag.TAG_COMPOUND)) {
                continue;
            }
            this.blueprintStates[index] = NbtUtils.readBlockState(
                registries.lookupOrThrow(Registries.BLOCK),
                entry.getCompound("state")
            );
        }
        this.loadedStructureName = tag.getString("loadedStructureName");
        this.loadedStructureUuid = tag.hasUUID("loadedStructureUuid") ? tag.getUUID("loadedStructureUuid") : null;
        this.invalidStructure = tag.getBoolean("invalidStructure");
        this.missingBlockItem = tag.contains("missingBlockItem", Tag.TAG_COMPOUND)
            ? ItemStack.parseOptional(registries, tag.getCompound("missingBlockItem"))
            : ItemStack.EMPTY;
    }

    public boolean hasBlueprint() {
        for (BlockState state : this.blueprintStates) {
            if (!state.isAir()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasInvalidStructure() {
        return this.invalidStructure;
    }

    public boolean isPickupMode() {
        return this.operation == OperationMode.PICKUP;
    }

    public void setPickupMode(boolean pickupMode) {
        this.operation = pickupMode ? OperationMode.PICKUP : OperationMode.MOVE;
        this.pointer = null;
        this.syncPositionSelection();
    }

    public boolean isSkipMissingMode() {
        return this.blueprintPlacementMode == BlueprintPlacementMode.SKIP;
    }

    public void setSkipMissingMode(boolean skipMissingMode) {
        this.blueprintPlacementMode = skipMissingMode
            ? BlueprintPlacementMode.SKIP
            : BlueprintPlacementMode.WAIT;
        this.pointer = null;
        this.syncPositionSelection();
    }

    public @Nullable BlockPos getCurrentBlueprintTargetPosition() {
        for (int checked = 0; checked < POSITION_COUNT; checked++) {
            int orderIndex = Math.floorMod(this.currentPlacementIndex + checked, POSITION_COUNT);
            int storageIndex = getStorageIndexForOrder(orderIndex);
            if (!this.blueprintStates[storageIndex].isAir()) {
                return this.getBlueprintPosition(storageIndex);
            }
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
            for (int index = 0; index < POSITION_COUNT; index++) {
                if (this.blueprintStates[index].isAir()) {
                    continue;
                }
                total++;
                if (level.getBlockState(this.getBlueprintPosition(index)).equals(this.getBlueprintStateForPlacement(index))) {
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

    public int getPlaceCooldown() {
        return this.phase == ExecutionPhase.IDLE ? 0 : 1;
    }

    public boolean isPowered() {
        return this.getBlockState().hasProperty(SmartBlockPlacerBlock.POWERED)
            && this.getBlockState().getValue(SmartBlockPlacerBlock.POWERED);
    }

    public boolean isHasRedstoneSignal() {
        return this.level != null && this.level.hasNeighborSignal(this.getBlockPos());
    }

    public void updateClientAnimationState(boolean powered, boolean hasRedstoneSignal) {
        if (!powered || hasRedstoneSignal) {
            this.clientAnimationStartTime = 0;
        }
    }

    public static BlockState flipHalfPropertyStatic(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HALF)) {
            Half half = state.getValue(BlockStateProperties.HALF);
            state = state.setValue(BlockStateProperties.HALF, half == Half.TOP ? Half.BOTTOM : Half.TOP);
        }
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            SlabType slabType = state.getValue(BlockStateProperties.SLAB_TYPE);
            state = state.setValue(BlockStateProperties.SLAB_TYPE, switch (slabType) {
                case BOTTOM -> SlabType.TOP;
                case TOP -> SlabType.BOTTOM;
                case DOUBLE -> SlabType.DOUBLE;
            });
        }
        if (state.hasProperty(BlockStateProperties.VERTICAL_DIRECTION)) {
            Direction direction = state.getValue(BlockStateProperties.VERTICAL_DIRECTION);
            state = state.setValue(
                BlockStateProperties.VERTICAL_DIRECTION,
                direction == Direction.UP ? Direction.DOWN : Direction.UP
            );
        }
        return state;
    }

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
        tag.put(
            "blueprintPlacementMode",
            BlueprintPlacementMode.CODEC.encodeStart(ops, this.blueprintPlacementMode).getOrThrow()
        );
        tag.put("phase", ExecutionPhase.CODEC.encodeStart(ops, this.phase).getOrThrow());
        tag.putFloat("progress", this.progress);
        tag.put("blueprintInventory", this.blueprintItemHandler.serializeNBT(registries));
        tag.put("bookInventory", this.bookInventory.createTag(registries));
        tag.put("outputBookInventory", this.outputBookInventory.createTag(registries));
        this.savePositionSelection(tag);
        this.saveBlueprintStates(tag, registries);
        if (this.pointer != null) {
            tag.put("pointer", ITargetPointer.CODEC.encodeStart(ops, this.pointer).getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        this.operation = tag.contains("operation")
            ? OperationMode.CODEC.parse(ops, tag.get("operation")).result().orElse(OperationMode.PICKUP)
            : OperationMode.PICKUP;
        this.target = tag.contains("target")
            ? TargetMode.CODEC.parse(ops, tag.get("target")).result().orElse(TargetMode.POSITION)
            : TargetMode.POSITION;
        this.blueprintPlacementMode = tag.contains("blueprintPlacementMode")
            ? BlueprintPlacementMode.CODEC.parse(ops, tag.get("blueprintPlacementMode"))
                .result().orElse(BlueprintPlacementMode.SKIP)
            : BlueprintPlacementMode.SKIP;
        this.phase = tag.contains("phase")
            ? ExecutionPhase.CODEC.parse(ops, tag.get("phase")).result().orElse(ExecutionPhase.IDLE)
            : ExecutionPhase.IDLE;
        this.progress = tag.getFloat("progress");
        this.loadingBlueprintInventory = true;
        try {
            if (tag.contains("blueprintInventory", Tag.TAG_COMPOUND)) {
                this.blueprintItemHandler.deserializeNBT(registries, tag.getCompound("blueprintInventory"));
            }
            this.bookInventory.fromTag(tag.getList("bookInventory", Tag.TAG_COMPOUND), registries);
            this.outputBookInventory.fromTag(tag.getList("outputBookInventory", Tag.TAG_COMPOUND), registries);
        } finally {
            this.loadingBlueprintInventory = false;
        }
        this.loadPositionSelection(tag);
        this.loadBlueprintStates(tag, registries);
        this.target = this.blueprintItemHandler.getStackInSlot(0).isEmpty()
            ? TargetMode.POSITION
            : TargetMode.BLUEPRINT;
        if (tag.contains("pointer")) {
            this.pointer = ITargetPointer.CODEC.parse(ops, tag.get("pointer")).result().orElse(null);
        }
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
        tag.put(
            "blueprintPlacementMode",
            BlueprintPlacementMode.CODEC.encodeStart(ops, this.blueprintPlacementMode).getOrThrow()
        );
        tag.put("phase", ExecutionPhase.CODEC.encodeStart(ops, this.phase).getOrThrow());
        tag.putFloat("progress", this.progress);
        this.savePositionSelection(tag);
        if (this.pointer != null) {
            tag.put("pointer", ITargetPointer.CODEC.encodeStart(ops, this.pointer).getOrThrow());
        }
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        if (this.getLevel() == null) {
            return;
        }
        RegistryOps<Tag> ops = this.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        this.operation = data.contains("operation")
            ? OperationMode.CODEC.parse(ops, data.get("operation")).result().orElse(this.operation)
            : this.operation;
        this.target = data.contains("target")
            ? TargetMode.CODEC.parse(ops, data.get("target")).result().orElse(this.target)
            : this.target;
        this.blueprintPlacementMode = data.contains("blueprintPlacementMode")
            ? BlueprintPlacementMode.CODEC.parse(ops, data.get("blueprintPlacementMode"))
                .result().orElse(this.blueprintPlacementMode)
            : this.blueprintPlacementMode;
        this.phase = data.contains("phase")
            ? ExecutionPhase.CODEC.parse(ops, data.get("phase")).result().orElse(this.phase)
            : this.phase;
        this.progress = data.getFloat("progress");
        this.loadPositionSelection(data);
        if (data.contains("pointer")) {
            this.pointer = ITargetPointer.CODEC.parse(ops, data.get("pointer")).result().orElse(null);
        }
        this.syncPositionSelection();
    }
    // endregion

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
        IDLE(0.0F),
        PREPARE(0.3F),
        EXTEND(0.4F),
        RESET(0.3F),
        ;

        public static final Codec<ExecutionPhase> CODEC = StringRepresentable.fromEnum(ExecutionPhase::values);
        private final float intervalPercent;

        ExecutionPhase(float intervalPercent) {
            this.intervalPercent = intervalPercent;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
