package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class SmartBlockPlacerBlockEntity extends BlockEntity implements IPowerConsumer, MenuProvider, IDiskCloneable {
    private static final int POWER = 16;
    private static final int PLACEMENT_INTERVAL = 20;
    private static final int PLACEMENT_DELAY = 6;
    
    private PowerGrid grid = null;
    private boolean isPowered = false;
    private boolean hasRedstoneSignal = false;
    private int selectedLayer = 0;
    private int placeCooldown = 0;
    private ItemStack currentHeldBlock = ItemStack.EMPTY;
    private int currentPlacementIndex = 0;
    private final Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean isPickupMode = true;

    // 客户端动画状态
    private long clientAnimationStartTime = 0;
    private BlockPos clientLastTargetPos = null;
    private int lastPlaceCooldown = 0;

    public void updateClientAnimationState(boolean isPowered, boolean hasRedstoneSignal) {
        if (!isPowered || hasRedstoneSignal) {
            this.clientAnimationStartTime = 0;
            this.clientLastTargetPos = null;
        }
    }

    public SmartBlockPlacerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SMART_BLOCK_PLACER.get(), pos, blockState);
    }

    private SmartBlockPlacerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static SmartBlockPlacerBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new SmartBlockPlacerBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("isPowered", isPowered);
        tag.putBoolean("hasRedstoneSignal", hasRedstoneSignal);
        tag.putInt("selectedLayer", selectedLayer);
        tag.putInt("currentPlacementIndex", currentPlacementIndex);
        tag.putInt("placeCooldown", placeCooldown);
        tag.putBoolean("isPickupMode", isPickupMode);
        if (!currentHeldBlock.isEmpty()) {
            tag.put("currentHeldBlock", currentHeldBlock.save(provider));
        }
        saveLayerPositions(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.isPowered = tag.getBoolean("isPowered");
        this.hasRedstoneSignal = tag.getBoolean("hasRedstoneSignal");
        this.selectedLayer = tag.getInt("selectedLayer");
        this.currentPlacementIndex = tag.getInt("currentPlacementIndex");
        this.placeCooldown = tag.getInt("placeCooldown");
        this.isPickupMode = tag.getBoolean("isPickupMode");
        this.currentHeldBlock = tag.contains("currentHeldBlock", Tag.TAG_COMPOUND)
            ? ItemStack.parse(provider, tag.getCompound("currentHeldBlock")).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
        loadLayerPositions(tag);
    }

    public void tickServer(Level level, BlockPos pos) {
        boolean previousPowered = this.isPowered;
        boolean previousRedstoneSignal = this.hasRedstoneSignal;
        this.isPowered = this.grid != null && this.grid.isWorking();
        this.hasRedstoneSignal = level.hasNeighborSignal(pos);

        boolean stateChanged = this.isPowered != previousPowered || this.hasRedstoneSignal != previousRedstoneSignal;

        boolean previousAbleToWork = previousPowered && !previousRedstoneSignal;
        boolean currentAbleToWork = this.isPowered && !this.hasRedstoneSignal;

        boolean indexReset = !previousAbleToWork && currentAbleToWork && this.currentPlacementIndex != 0;
        if (indexReset) {
            this.currentPlacementIndex = 0;
        }

        if (this.isPowered && !this.hasRedstoneSignal) {
            if (this.isPickupMode) {
                this.tickPickupMode(level, pos);
            } else {
                this.tickMoveMode(level, pos);
            }
        } else {
            boolean cooldownReset = this.placeCooldown != 0;
            if (cooldownReset) {
                this.placeCooldown = 0;
            }
            boolean heldItemCleared = !this.currentHeldBlock.isEmpty();
            if (heldItemCleared) {
                this.currentHeldBlock = ItemStack.EMPTY;
            }
            
            if (stateChanged || cooldownReset || heldItemCleared) {
                this.onChanged();
            }
        }
    }

    public void tickClient() {
        // 检测新的工作周期开始：placeCooldown 从低值变为高值（表示新的放置周期）
        // 使用阈值判断，避免依赖具体的 lastPlaceCooldown 值
        boolean isNewCycle = this.placeCooldown > this.lastPlaceCooldown 
            && this.placeCooldown >= PLACEMENT_INTERVAL;
        
        if (isNewCycle) {
            this.clientAnimationStartTime = 0;
            this.clientLastTargetPos = null;
        }
        this.lastPlaceCooldown = this.placeCooldown;
    }
    
    private void tickPickupMode(Level level, BlockPos pos) {
        boolean needsPlacement = this.hasEmptyPositions(level, pos);
        boolean hasBlocksInContainer = this.hasBlockItemsInContainer(level, pos);
    
        if (this.placeCooldown > 0) {
            this.placeCooldown--;
            if (this.placeCooldown == PLACEMENT_DELAY && needsPlacement && hasBlocksInContainer) {
                this.placeBlocks(level, pos);
            }
        } else if (needsPlacement && hasBlocksInContainer) {
            if (this.currentHeldBlock.isEmpty()) {
                this.currentPlacementIndex = 0;
            }
            this.placeCooldown = PLACEMENT_INTERVAL;
            this.currentHeldBlock = this.peekBlockItemFromContainer(level, pos);
            this.onChanged();
        }
    }
    
    private void tickMoveMode(Level level, BlockPos pos) {
        boolean needsMove = this.hasTargetPositions(level, pos);
    
        if (this.placeCooldown > 0) {
            this.placeCooldown--;
            if (this.placeCooldown == PLACEMENT_DELAY && needsMove) {
                this.moveBlocks(level, pos);
            }
        } else if (needsMove) {
            this.placeCooldown = PLACEMENT_INTERVAL;
            this.onChanged();
        }
    }

    private boolean hasEmptyPositions(Level level, BlockPos placerPos) {
        Direction facing = level.getBlockState(placerPos).getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);

        for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
            int layer = entry.getKey();
            for (int position : entry.getValue()) {
                BlockPos targetPos = SmartBlockPlacerBlockEntity
                    .calculateTargetPosition(basePos, facing, position / 5, position % 5, layer, upsideDown);
                if (level.isEmptyBlock(targetPos)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTargetPositions(Level level, BlockPos placerPos) {
        if (this.layerPositions.isEmpty()) {
            return false;
        }
        
        Direction facing = level.getBlockState(placerPos).getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());

        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return false;
        }

        for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
            int layer = entry.getKey();
            for (int position : entry.getValue()) {
                BlockPos targetPos = SmartBlockPlacerBlockEntity
                    .calculateTargetPosition(basePos, facing, position / 5, position % 5, layer, upsideDown);
                if (level.isEmptyBlock(targetPos)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean isBlockNotPushable(BlockState state, Level level, BlockPos pos, Direction facing) {
        return !net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(
            state, level, pos, facing, false, facing
        );
    }

    private boolean hasBlockItemsInContainer(Level level, BlockPos placerPos) {
        return !getBlockItemFromContainer(level, placerPos, false).isEmpty();
    }

    private Direction getFacing(BlockPos pos, Level level) {
        return level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
    }

    private void placeBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        List<BlockPos> allPositions = this.buildOrderedPositions(basePos, facing, upsideDown);

        if (allPositions.isEmpty()) {
            return;
        }

        if (this.currentPlacementIndex >= allPositions.size()) {
            this.currentPlacementIndex = 0;
        }

        for (int i = 0; i < allPositions.size(); i++) {
            int index = (this.currentPlacementIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);

            if (level.isEmptyBlock(targetPos)) {
                ItemStack blockItem = this.extractBlockItemFromContainer(level, placerPos);
                if (blockItem.isEmpty()) {
                    this.currentPlacementIndex = 0;
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return;
                }

                if (blockItem.getItem() instanceof BlockItem blockItemObj) {
                    Orientation orientation = this.calculatePlacementOrientation(facing, upsideDown);

                    if (AnvilCraftFakePlayers.anvilcraftBlockPlacer.placeBlock(
                        level, targetPos, orientation, blockItemObj, blockItem) == net.minecraft.world.InteractionResult.FAIL) {
                        return;
                    }

                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return;
                }
            }
        }
    }

    private void moveBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());
        List<BlockPos> allPositions = this.buildOrderedPositions(basePos, facing, upsideDown);

        if (allPositions.isEmpty()) {
            return;
        }

        if (this.currentPlacementIndex >= allPositions.size()) {
            this.currentPlacementIndex = 0;
        }

        BlockState sourceState = level.getBlockState(sourcePos);
        if (isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return;
        }

        for (int i = 0; i < allPositions.size(); i++) {
            int index = (this.currentPlacementIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);

            if (level.isEmptyBlock(targetPos)) {
                BlockState sourceState2 = level.getBlockState(sourcePos);
                
                ItemStack blockItem = sourceState2.getBlock().asItem().getDefaultInstance();
                if (!(blockItem.getItem() instanceof BlockItem)) {
                    // Do not remove the source block if it's not a BlockItem to avoid voiding blocks
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
                
                level.removeBlock(sourcePos, false);
                
                if (blockItem.getItem() instanceof BlockItem blockItemObj) {
                    Orientation orientation = this.calculatePlacementOrientation(facing, upsideDown);
                    
                    if (AnvilCraftFakePlayers.anvilcraftBlockPlacer.placeBlock(
                        level, targetPos, orientation, blockItemObj, blockItem) == net.minecraft.world.InteractionResult.FAIL) {
                        level.setBlock(sourcePos, sourceState2, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                        return;
                    }
                    
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return;
                }
            }
        }
    }

    /**
     * 构建有序的放置位置列表
     * 顺序：从最下面一层开始，每一层从最远离放置器的位置开始，从左到右，然后逐渐向下
     * 
     * @param basePos 基准位置
     * @param facing 朝向
     * @param upsideDown 是否倒挂
     * @return 有序的位置列表
     */
    public List<BlockPos> buildOrderedPositions(BlockPos basePos, Direction facing, boolean upsideDown) {
        return SmartBlockPlacerBlockEntity.buildOrderedPositions(basePos, facing, this.layerPositions, upsideDown);
    }
    
    /**
     * 构建有序的放置位置列表（静态方法，供渲染器调用）
     * 
     * @param basePos 基准位置
     * @param facing 朝向
     * @param layerPositions 层位置映射
     * @param upsideDown 是否倒挂
     * @return 有序的位置列表
     */
    public static List<BlockPos> buildOrderedPositions(
        BlockPos basePos, 
        Direction facing, 
        Map<Integer, Set<Integer>> layerPositions, 
        boolean upsideDown
    ) {
        if (layerPositions.isEmpty()) {
            return List.of();
        }
        
        List<BlockPos> positions = new ArrayList<>();
        List<Integer> sortedLayers = new ArrayList<>(layerPositions.keySet());
        sortedLayers.sort(Integer::compareTo);

        for (int layer : sortedLayers) {
            Set<Integer> layerPosSet = layerPositions.get(layer);
            if (layerPosSet == null || layerPosSet.isEmpty()) {
                continue;
            }

            List<int[]> rowColList = new ArrayList<>(layerPosSet.size());
            for (int position : layerPosSet) {
                rowColList.add(new int[]{position / 5, position % 5});
            }

            rowColList.sort((a, b) -> {
                if (a[0] != b[0]) {
                    return Integer.compare(a[0], b[0]);
                }
                return Integer.compare(a[1], b[1]);
            });

            for (int[] rowCol : rowColList) {
                positions.add(calculateTargetPosition(basePos, facing, rowCol[0], rowCol[1], layer, upsideDown));
            }
        }
        return positions;
    }

    private ItemStack peekBlockItemFromContainer(Level level, BlockPos placerPos) {
        return this.getBlockItemFromContainer(level, placerPos, false);
    }

    private ItemStack extractBlockItemFromContainer(Level level, BlockPos placerPos) {
        return this.getBlockItemFromContainer(level, placerPos, true);
    }

    private ItemStack getBlockItemFromContainer(Level level, BlockPos placerPos, boolean extract) {
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());

        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        int slot;
        for (slot = 0; itemHandler != null && slot < itemHandler.getSlots(); slot++) {
            ItemStack blockItemStack = itemHandler.extractItem(slot, 1, true);
            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                return extract ? itemHandler.extractItem(slot, 1, false) : blockItemStack.copy();
            }
        }

        if (itemHandler == null) {
            AABB aabb = new AABB(inputPos);
            List<Entity> rawEntities = level.getEntitiesOfClass(
                Entity.class, aabb, e -> e instanceof ContainerEntity && !((ContainerEntity) e).isEmpty()
            );
            
            for (Entity rawEntity : rawEntities) {
                if (rawEntity instanceof ContainerEntity containerEntity) {
                    IItemHandler entityHandler = ((Entity) containerEntity).getCapability(
                        Capabilities.ItemHandler.ENTITY, null
                    );
                    if (entityHandler != null) {
                        for (slot = 0; slot < entityHandler.getSlots(); slot++) {
                            ItemStack blockItemStack = entityHandler.extractItem(slot, 1, true);
                            if (!blockItemStack.isEmpty() && blockItemStack.getItem() instanceof BlockItem) {
                                if (!extract) {
                                    return blockItemStack.copy();
                                } else {
                                    return entityHandler.extractItem(slot, 1, false);
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }

        AABB aabb = new AABB(inputPos);
        List<ItemEntity> entities = level.getEntities(
            EntityTypeTest.forClass(ItemEntity.class),
            aabb,
            Entity::isAlive
        );
        if (entities.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemEntity itemEntity = null;
        for (ItemEntity entity : entities) {
            if (entity.getItem().getItem() instanceof BlockItem) {
                itemEntity = entity;
                break;
            }
        }

        if (itemEntity == null) {
            return ItemStack.EMPTY;
        }

        // 提取时先保存物品副本，再修改实体数量
        ItemStack extracted = itemEntity.getItem().copyWithCount(1);
        if (extract) {
            int count = itemEntity.getItem().getCount();
            if (count > 1) {
                itemEntity.getItem().setCount(count - 1);
            } else {
                itemEntity.discard();
            }
            return extracted;
        }
        return itemEntity.getItem().copy();
    }
    
    private Orientation calculatePlacementOrientation(Direction facing, boolean upsideDown) {
        return switch (facing) {
            case NORTH -> upsideDown ? Orientation.SOUTH_UP : Orientation.NORTH_UP;
            case SOUTH -> upsideDown ? Orientation.NORTH_UP : Orientation.SOUTH_UP;
            case WEST -> upsideDown ? Orientation.EAST_UP : Orientation.WEST_UP;
            case EAST -> upsideDown ? Orientation.WEST_UP : Orientation.EAST_UP;
            default -> Orientation.NORTH_UP;
        };
    }
    
    /**
     * 计算目标位置
     * 
     * @param basePos 基准位置
     * @param facing 朝向
     * @param row 行索引 (0-4)
     * @param col 列索引 (0-4)
     * @param layer 层索引
     * @param upsideDown 是否倒挂
     * @return 目标方块位置
     */
    @SuppressWarnings("checkstyle:LocalVariableName")
    public static BlockPos calculateTargetPosition(BlockPos basePos, Direction facing, int row, int col, int layer, boolean upsideDown) {
        Direction right = facing.getClockWise();
        int yOffset = upsideDown ? layer - 4 : layer;
        return basePos.atY(basePos.getY() + yOffset)
            .relative(right, col - 2)
            .relative(right.getClockWise(), row - 2);
    }

    public void onChanged() {
        this.setChanged();
        Level level = this.getLevel();
        if (level != null) {
            level.sendBlockUpdated(
                this.getBlockPos(),
                this.getBlockState(),
                this.getBlockState(),
                Block.UPDATE_CLIENTS
            );
        }
    }

    public void setSelectedLayer(int layer) {
        this.selectedLayer = layer;
        this.onChanged();
    }

    public void setPickupMode(boolean pickupMode) {
        this.isPickupMode = pickupMode;
        this.onChanged();
    }

    public void togglePosition(int layer, int position, boolean selected) {
        Set<Integer> positions = layerPositions.computeIfAbsent(layer, k -> new HashSet<>());
        if (selected) {
            positions.add(position);
        } else {
            positions.remove(position);
            if (positions.isEmpty()) {
                layerPositions.remove(layer);
            }
        }
        this.onChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    
    private void saveLayerPositions(CompoundTag tag) {
        CompoundTag layerTag = new CompoundTag();
        for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
            layerTag.putIntArray("layer_" + entry.getKey(),
                entry.getValue().stream().mapToInt(Integer::intValue).toArray());
        }
        tag.put("layerPositions", layerTag);
    }
    
    private void loadLayerPositions(CompoundTag tag) {
        this.layerPositions.clear();
        if (tag.contains("layerPositions", Tag.TAG_COMPOUND)) {
            CompoundTag layerTag = tag.getCompound("layerPositions");
            for (String key : layerTag.getAllKeys()) {
                if (key.startsWith("layer_")) {
                    int layer = Integer.parseInt(key.substring(6));
                    Set<Integer> positions = new HashSet<>();
                    for (int pos : layerTag.getIntArray(key)) {
                        positions.add(pos);
                    }
                    this.layerPositions.put(layer, positions);
                }
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getInputPower() {
        return SmartBlockPlacerBlockEntity.POWER;
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
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
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.smart_block_placer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) {
            return null;
        }
        return new SmartBlockPlacerMenu(ModMenuTypes.SMART_BLOCK_PLACER.get(), containerId, inventory, this);
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.putInt("selectedLayer", this.selectedLayer);
        tag.putInt("currentPlacementIndex", this.currentPlacementIndex);
        tag.putBoolean("isPickupMode", this.isPickupMode);
        this.saveLayerPositions(tag);
    }

    @Override
    public void applyDiskData(CompoundTag tag) {
        this.selectedLayer = tag.getInt("selectedLayer");
        this.currentPlacementIndex = tag.getInt("currentPlacementIndex");
        this.isPickupMode = tag.getBoolean("isPickupMode");
        this.loadLayerPositions(tag);
        this.onChanged();
    }

}
