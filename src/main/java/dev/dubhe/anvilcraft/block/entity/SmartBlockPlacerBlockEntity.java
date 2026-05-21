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
import net.minecraft.world.SimpleContainer;
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
    
    // 标记当前是否有方块正在被智能放置器移动
    private static final ThreadLocal<Boolean> IS_BEING_MOVED_BY_PLACER = ThreadLocal.withInitial(() -> false);
    
    private PowerGrid grid = null;
    private boolean isPowered = false;
    private boolean hasRedstoneSignal = false;
    private int selectedLayer = 0;
    private int placeCooldown = 0;
    private long lastTickGameTime = -1;
    private ItemStack currentHeldBlock = ItemStack.EMPTY;
    private int currentPlacementIndex = 0;
    private final Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean isPickupMode = true;

    // Disk物品栏
    private final SimpleContainer diskInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmartBlockPlacerBlockEntity.this.setChanged();
        }
    };

    // 客户端动画状态
    private long clientAnimationStartTime = 0;
    @Nullable
    private BlockPos clientLastTargetPos = null;
    private int lastPlaceCooldown = 0;
    
    // 客户端收回动画状态
    private boolean clientIsRetracting = false;
    private long clientRetractStartTime = 0;
    private float[] clientRetractStartAngles = new float[4];
    private float clientRetractStartProgress = 0f;


    @SuppressWarnings("checkstyle:EmptyLineSeparator")
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
        // 保存Disk物品栏
        tag.put("diskInventory", this.diskInventory.createTag(provider));
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
        // 加载Disk物品栏
        this.diskInventory.fromTag(tag.getList("diskInventory", Tag.TAG_COMPOUND), provider);
    }

    public void tickServer(Level level, BlockPos pos) {
        final boolean previousPowered = this.isPowered;
        final boolean previousRedstoneSignal = this.hasRedstoneSignal;
        
        this.isPowered = this.grid != null && this.grid.isWorking();
        this.hasRedstoneSignal = level.hasNeighborSignal(pos);

        this.flushState(level, pos);

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
            
            boolean shutdownIndexReset = this.currentPlacementIndex != 0;
            if (shutdownIndexReset) {
                this.currentPlacementIndex = 0;
            }
            
            if (stateChanged || cooldownReset || heldItemCleared || shutdownIndexReset) {
                this.onChanged();
            }
        }
    }

    public void tickClient() {
        boolean isNewCycle = this.placeCooldown > this.lastPlaceCooldown 
            && this.placeCooldown >= PLACEMENT_INTERVAL;
        
        boolean wasIdle = this.lastPlaceCooldown == 0;
        boolean isNowWorking = this.placeCooldown > 0;
        boolean becameActive = wasIdle && isNowWorking;
        
        if (isNewCycle || becameActive) {
            this.clientAnimationStartTime = 0;
            this.clientLastTargetPos = null;
            this.clientIsRetracting = false;
        }
        
        this.lastPlaceCooldown = this.placeCooldown;
    }
    
    /**
     * 更新客户端动画状态
     */
    @SuppressWarnings("unused")
    public void updateClientAnimationState(boolean isCurrentlyPowered, boolean hasRedstoneSignal) {
        this.tickClient();
    }
    
    /**
     * 工作模式枚举
     */
    private enum WorkMode {
        PICKUP,   // 拾取模式：从容器获取方块并放置
        MOVE      // 移动模式：从源位置移动方块到目标位置
    }
    
    private void tickPickupMode(Level level, BlockPos pos) {
        tickWorkMode(level, pos, WorkMode.PICKUP);
    }
    
    private void tickMoveMode(Level level, BlockPos pos) {
        tickWorkMode(level, pos, WorkMode.MOVE);
    }
    
    /**
     * 统一的工作模式tick逻辑
     */
    private void tickWorkMode(Level level, BlockPos pos, WorkMode mode) {
        boolean canWork = switch (mode) {
            case PICKUP -> this.hasEmptyPositions(level, pos) && this.hasBlockItemsInContainer(level, pos);
            case MOVE -> this.hasTargetPositions(level, pos);
        };
        
        boolean isResourceDepleted = switch (mode) {
            case PICKUP -> !this.hasBlockItemsInContainer(level, pos);
            case MOVE -> !this.hasTargetPositions(level, pos);
        };
        
        if (stopWorkCycleIfResourceDepleted(isResourceDepleted)) {
            return;
        }
        
        tickCommonCooldownLogic(level,
            canWork,
            () -> {
                switch (mode) {
                    case PICKUP -> this.placeBlocks(level, pos);
                    case MOVE -> this.moveBlocks(level, pos);
                    default -> {}
                }
            },
            () -> {
                switch (mode) {
                    case PICKUP -> this.currentHeldBlock = this.peekBlockItemFromContainer(level, pos);
                    case MOVE -> {
                        Direction facing = level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING);
                        BlockPos sourcePos = pos.relative(facing.getOpposite());
                        BlockState sourceState = level.getBlockState(sourcePos);
                        ItemStack sourceItem = sourceState.getBlock().asItem().getDefaultInstance();
                        if (!sourceItem.isEmpty() && sourceItem.getItem() instanceof net.minecraft.world.item.BlockItem) {
                            this.currentHeldBlock = sourceItem.copy();
                        } else {
                            this.currentHeldBlock = ItemStack.EMPTY;
                        }
                    }
                    default -> {}
                }
            }
        );
    }
    
    /**
     * 资源耗尽时停止工作周期
     */
    private boolean stopWorkCycleIfResourceDepleted(boolean isResourceDepleted) {
        if (!isResourceDepleted) {
            return false;
        }
        
        if (!this.currentHeldBlock.isEmpty()) {
            this.currentHeldBlock = ItemStack.EMPTY;
        }
        
        if (this.placeCooldown > 0) {
            this.placeCooldown = 0;
        }
        
        this.currentPlacementIndex = 0;
        
        this.onChanged();
        return true;
    }
    
    /**
     * 通用冷却控制逻辑
     */
    private void tickCommonCooldownLogic(Level level, boolean shouldExecute, 
        Runnable executeAction, Runnable onCycleStart) {
        long currentGameTime = level.getGameTime();
        boolean shouldDecrementCooldown = currentGameTime != this.lastTickGameTime;
        
        if (this.placeCooldown > 0 && shouldDecrementCooldown) {
            if (this.placeCooldown == PLACEMENT_DELAY && shouldExecute) {
                if (this.currentHeldBlock.isEmpty()) {
                    this.currentPlacementIndex = 0;
                }
                executeAction.run();
            }
            
            this.placeCooldown--;
        }
        
        if (shouldDecrementCooldown) {
            this.lastTickGameTime = currentGameTime;
        }
        
        if (this.placeCooldown == 0 && shouldExecute) {
            onCycleStart.run();
            
            this.placeCooldown = PLACEMENT_INTERVAL;
            this.lastTickGameTime = currentGameTime;
            this.onChanged();
        }
    }

    private boolean hasEmptyPositions(Level level, BlockPos placerPos) {
        Direction facing = level.getBlockState(placerPos).getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);

        return hasValidTargetPositions(level, basePos, facing, upsideDown);
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

        return hasValidTargetPositions(level, basePos, facing, upsideDown);
    }
    
    /**
     * 检查是否有有效的目标位置
     *
     * @param level      世界
     * @param basePos    基准位置
     * @param facing     朝向
     * @param upsideDown 是否倒挂
     * @return 是否存在有效位置
     */
    private boolean hasValidTargetPositions(Level level, BlockPos basePos, Direction facing, 
        boolean upsideDown
    ) {
        for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
            int layer = entry.getKey();
            for (int position : entry.getValue()) {
                BlockPos targetPos = SmartBlockPlacerBlockEntity
                    .calculateTargetPosition(basePos, facing, position / 5, position % 5, layer, upsideDown);
                BlockState targetState = level.getBlockState(targetPos);
                            
                if (targetState.isAir() || !this.canNotBePlaced(level, targetState, null)) {
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

    /**
     * 判断是否不能放置方块
     */
    private boolean canNotBePlaced(Level level, BlockState blockState, @Nullable net.minecraft.world.item.BlockItem blockItem) {
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            if (!blockState.getFluidState().isEmpty()) {
                return false;
            }
            if (blockState.is(net.minecraft.world.level.block.Blocks.TURTLE_EGG) 
                && blockState.getValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS) < 4) {
                return blockItem != null && blockState.getBlock() != blockItem.getBlock();
            }
            if (blockState.is(net.minecraft.world.level.block.Blocks.SEA_PICKLE) 
                && blockState.getValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES) < 4) {
                return blockItem != null && blockState.getBlock() != blockItem.getBlock();
            }
            if (blockState.getBlock() instanceof net.minecraft.world.level.block.CandleBlock) {
                if (blockState.getValue(net.minecraft.world.level.block.CandleBlock.CANDLES) >= 4) {
                    return true;
                }
                return blockItem != null && blockState.getBlock() != blockItem.getBlock();
            }
        }
        return true;
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
        
        executeBlockOperation(level, placerPos, facing, upsideDown,
            () -> this.extractBlockItemFromContainer(level, placerPos),
            () -> this.peekBlockItemFromContainer(level, placerPos),
            false, // pickup模式允许堆叠到非空气方块
            (blockItem, blockItemObj, targetPos) -> {
                this.currentHeldBlock = ItemStack.EMPTY;
                
                BlockState newState = level.getBlockState(targetPos);
                return !newState.isAir() && !this.canNotBePlaced(level, newState, blockItemObj);
            }
        );
    }

    private void moveBlocks(Level level, BlockPos placerPos) {
        Direction facing = this.getFacing(placerPos, level);
        boolean upsideDown = level.getBlockState(placerPos).getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos sourcePos = placerPos.relative(facing.getOpposite());
        
        // 检查源方块
        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir() || isBlockNotPushable(sourceState, level, sourcePos, facing)) {
            return;
        }
        
        // 保存源BlockEntity的NBT数据（如果有）
        final net.minecraft.nbt.CompoundTag sourceBlockEntityData;
        BlockEntity sourceBlockEntity = level.getBlockEntity(sourcePos);
        if (sourceBlockEntity != null) {
            sourceBlockEntityData = sourceBlockEntity.saveWithFullMetadata(level.registryAccess());
        } else {
            sourceBlockEntityData = null;
        }
        
        final BlockState finalSourceState = sourceState;
        final ItemStack sourceItem = finalSourceState.getBlock().asItem().getDefaultInstance();
        
        executeBlockOperation(level, placerPos, facing, upsideDown,
            () -> sourceItem,
            () -> sourceItem,
            true, // move模式严格要求目标位置为空
            (blockItem, blockItemObj, targetPos) -> {
                BlockState stateToPlace = finalSourceState;
                if (finalSourceState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                    stateToPlace = finalSourceState.setValue(
                        net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, 
                        false
                    );
                }
                
                level.setBlock(targetPos, stateToPlace, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                
                if (sourceBlockEntityData != null) {
                    BlockEntity targetBlockEntity = level.getBlockEntity(targetPos);
                    if (targetBlockEntity != null) {
                        targetBlockEntity.loadWithComponents(sourceBlockEntityData, level.registryAccess());
                        targetBlockEntity.setChanged();
                    }
                }
                
                // 设置标志：方块正在被智能放置器移动
                IS_BEING_MOVED_BY_PLACER.set(true);
                try {
                    level.removeBlock(sourcePos, false);
                } finally {
                    // 确保标志被重置
                    IS_BEING_MOVED_BY_PLACER.set(false);
                }
                
                this.currentHeldBlock = ItemStack.EMPTY;
                
                // 移动模式不支持堆叠
                return false;
            }
        );
    }
    
    /**
     * 执行方块操作的通用逻辑
     * 
     * @param level 世界
     * @param placerPos 放置器位置
     * @param facing 朝向
     * @param upsideDown 是否倒挂
     * @param itemExtractor 物品提取器
     * @param itemPeeker 物品预览器
     * @param requireEmptyTarget 是否要求目标位置必须为空（move模式为true，pickup模式为false）
     * @param onSuccess 成功回调
     */
    private void executeBlockOperation(Level level, BlockPos placerPos, Direction facing, boolean upsideDown,
        java.util.function.Supplier<ItemStack> itemExtractor,
        java.util.function.Supplier<ItemStack> itemPeeker,
        boolean requireEmptyTarget,
        BlockOperationSuccessHandler onSuccess) {
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        List<BlockPos> allPositions = SmartBlockPlacerBlockEntity.buildOrderedPositions(basePos, facing, this.layerPositions, upsideDown);

        if (allPositions.isEmpty()) {
            return;
        }

        if (this.currentPlacementIndex >= allPositions.size()) {
            this.currentPlacementIndex = 0;
        }

        for (int i = 0; i < allPositions.size(); i++) {
            int index = (this.currentPlacementIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);

            BlockState targetState = level.getBlockState(targetPos);
            
            // 根据模式选择目标检查逻辑
            boolean isValidTarget;
            if (requireEmptyTarget) {
                // move模式：只接受空气方块
                isValidTarget = targetState.isAir();
            } else {
                // pickup模式：接受空气或可堆叠方块
                isValidTarget = targetState.isAir() || !this.canNotBePlaced(level, targetState, null);
            }
            
            if (isValidTarget) {
                // 先预览物品进行检查，不实际提取
                ItemStack peekedBlockItem = itemPeeker.get();
                if (peekedBlockItem.isEmpty() || !(peekedBlockItem.getItem() instanceof BlockItem peekedBlockItemObj)) {
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
                
                // 使用预览的物品进行放置合法性检查
                if (!targetState.isAir() && this.canNotBePlaced(level, targetState, peekedBlockItemObj)) {
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
                
                // 所有检查通过，现在才真正提取物品
                ItemStack blockItem = itemExtractor.get();
                if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem blockItemObj)) {
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
                
                Orientation orientation = this.calculatePlacementOrientation(facing, upsideDown);

                if (AnvilCraftFakePlayers.anvilcraftBlockPlacer.placeBlock(
                    level, targetPos, orientation, blockItemObj, blockItem) == net.minecraft.world.InteractionResult.FAIL) {
                    // 放置失败，需要回滚物品
                    this.rollbackExtractedItem(level, placerPos, blockItem);
                    this.onChanged();
                    return;
                }
                
                boolean canStack = onSuccess.handle(blockItem, blockItemObj, targetPos);
                
                if (canStack) {
                    this.onChanged();
                    return;
                }
                
                this.currentPlacementIndex = (index + 1) % allPositions.size();
                this.onChanged();
                return;
            }
        }
    }
    
    /**
     * 方块操作成功回调
     */
    @FunctionalInterface
    private interface BlockOperationSuccessHandler {
        boolean handle(ItemStack blockItem, BlockItem blockItemObj, BlockPos targetPos);
    }

    /**
     * 检查方块是否正在被智能放置器移动
     * 
     * @return 是否正在被移动
     */
    public static boolean isBlockBeingMovedByPlacer() {
        return IS_BEING_MOVED_BY_PLACER.get();
    }
    
    /**
     * 构建有序的放置位置列表
     * 顺序：从最下面一层开始，每一层从最远离放置器的位置开始，从左到右，然后逐渐向下
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
                if (extract) {
                    ItemStack extracted = itemHandler.extractItem(slot, 1, false);
                    if (extracted.is(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET)) {
                        itemHandler.insertItem(slot, new ItemStack(net.minecraft.world.item.Items.BUCKET), false);
                    }
                    return extracted;
                }
                return blockItemStack.copy();
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
            if (extracted.is(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET)) {
                itemEntity.setItem(new ItemStack(net.minecraft.world.item.Items.BUCKET, count));
                itemEntity.setDeltaMovement(0, 0, 0);
            } else if (count > 1) {
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
     * 回滚已提取的物品到原容器
     * 
     * @param level 世界
     * @param placerPos 放置器位置
     * @param extractedItem 已提取的物品
     */
    private void rollbackExtractedItem(Level level, BlockPos placerPos, ItemStack extractedItem) {
        if (extractedItem.isEmpty()) {
            return;
        }
        
        Direction facing = this.getFacing(placerPos, level);
        BlockPos inputPos = placerPos.relative(facing.getOpposite());
        
        // 尝试将物品放回容器
        IItemHandler itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, inputPos, null);
        if (itemHandler != null) {
            ItemStack remaining = extractedItem.copy();
            for (int slot = 0; slot < itemHandler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = itemHandler.insertItem(slot, remaining, false);
            }
            // 如果还有剩余物品，生成ItemEntity
            if (!remaining.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(level,
                    inputPos.getX() + 0.5, inputPos.getY() + 0.5, inputPos.getZ() + 0.5, remaining);
                level.addFreshEntity(itemEntity);
            }
            return;
        }
        
        // 如果没有ItemHandler，直接生成ItemEntity
        ItemEntity itemEntity = new ItemEntity(level, inputPos.getX() + 0.5, inputPos.getY() + 0.5, inputPos.getZ() + 0.5, extractedItem);
        level.addFreshEntity(itemEntity);
    }
    
    /**
     * 计算目标位置
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
