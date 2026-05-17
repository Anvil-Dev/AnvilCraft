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
    
    private PowerGrid grid = null;
    private boolean isPowered = false;
    private boolean hasRedstoneSignal = false;
    private int selectedLayer = 0;
    private int placeCooldown = 0;
    private long lastTickGameTime = -1;  // 记录上次tick的游戏时间，防止同tick重复递减
    private ItemStack currentHeldBlock = ItemStack.EMPTY;
    private int currentPlacementIndex = 0;
    private final Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean isPickupMode = true;

    /**
     * -- GETTER --
     *  获取Disk物品栏
     */
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
    
    // 客户端收回动画状态（每个BlockEntity独立）
    private boolean clientIsRetracting = false;
    private long clientRetractStartTime = 0;
    private float[] clientRetractStartAngles = new float[4];
    private float clientRetractStartProgress = 0f; // 保存中断时的进度，用于计算收回时长


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

        // 更新方块的 OVERLOAD 状态
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
        
        // 额外检测：如果 placeCooldown 从 0 变为非 0，也视为新周期
        // 这样可以处理目标位置被玩家手动放置后又拆除的情况
        boolean wasIdle = this.lastPlaceCooldown == 0;
        boolean isNowWorking = this.placeCooldown > 0;
        boolean becameActive = wasIdle && isNowWorking;
        
        if (isNewCycle || becameActive) {
            this.clientAnimationStartTime = 0;
            this.clientLastTargetPos = null;
        }
        this.lastPlaceCooldown = this.placeCooldown;
    }
    
    /**
     * 更新客户端动画状态（由渲染器调用）
     * 
     * @param isCurrentlyPowered 当前是否通电
     * @param hasRedstoneSignal 当前是否有红石信号
     */
    @SuppressWarnings("unused")
    public void updateClientAnimationState(boolean isCurrentlyPowered, boolean hasRedstoneSignal) {
        // 这个方法主要用于触发tickClient中的逻辑
        // 实际的动画状态更新在tickClient中处理
        // 渲染器通过调用此方法来确保客户端动画状态是最新的
        // 参数用于记录状态，未来可能用于更复杂的动画逻辑
        this.tickClient();
    }
    
    private void tickPickupMode(Level level, BlockPos pos) {
        boolean needsPlacement = this.hasEmptyPositions(level, pos);
        boolean hasBlocksInContainer = this.hasBlockItemsInContainer(level, pos);
        
        tickCommonCooldownLogic(level, 
            needsPlacement && hasBlocksInContainer,
            () -> this.placeBlocks(level, pos),
            () -> {
                // 新周期开始，预览要放置的方块
                this.currentHeldBlock = this.peekBlockItemFromContainer(level, pos);
            }
        );
    }
    
    private void tickMoveMode(Level level, BlockPos pos) {
        boolean needsMove = this.hasTargetPositions(level, pos);
        
        tickCommonCooldownLogic(level,
            needsMove,
            () -> this.moveBlocks(level, pos),
            () -> {
                // 新周期开始，设置钳子中持有的方块（从源位置获取）
                BlockPos sourcePos = pos.relative(level.getBlockState(pos).getValue(HorizontalDirectionalBlock.FACING).getOpposite());
                BlockState sourceState = level.getBlockState(sourcePos);
                ItemStack sourceItem = sourceState.getBlock().asItem().getDefaultInstance();
                if (!sourceItem.isEmpty() && sourceItem.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    this.currentHeldBlock = sourceItem.copy();
                } else {
                    this.currentHeldBlock = ItemStack.EMPTY;
                }
            }
        );
    }
    
    /**
     * 通用的冷却控制逻辑（两种模式共享）
     * 
     * @param level 世界
     * @param shouldExecute 是否应该执行放置/移动
     * @param executeAction 执行放置/移动的回调
     * @param onCycleStart 新周期开始时的回调（用于设置 currentHeldBlock）
     */
    private void tickCommonCooldownLogic(Level level, boolean shouldExecute, 
        Runnable executeAction, Runnable onCycleStart) {
        // 使用游戏时间控制冷却，防止同tick内重复递减
        long currentGameTime = level.getGameTime();
        boolean shouldDecrementCooldown = currentGameTime != this.lastTickGameTime;
        
        if (this.placeCooldown > 0 && shouldDecrementCooldown) {
            // 在 cooldown 倒计时到 PLACEMENT_DELAY 时执行放置/移动（递减前检查）
            if (this.placeCooldown == PLACEMENT_DELAY && shouldExecute) {
                // 重置索引（如果需要）
                if (this.currentHeldBlock.isEmpty()) {
                    this.currentPlacementIndex = 0;
                }
                // 执行放置/移动（使用周期开始时已设置的 currentHeldBlock）
                executeAction.run();
            }
            
            // 递减冷却
            this.placeCooldown--;
        }
        
        // 更新上次tick的游戏时间
        if (shouldDecrementCooldown) {
            this.lastTickGameTime = currentGameTime;
        }
        
        // 在 cooldown 结束后立即开始新的周期（不浪费 tick）
        if (this.placeCooldown == 0 && shouldExecute) {
            // 调用周期开始回调
            onCycleStart.run();
            
            this.placeCooldown = PLACEMENT_INTERVAL;
            this.lastTickGameTime = currentGameTime;  // 重置游戏时间记录
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
                
                // 检查基本有效性
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
     * 判断当前位置是否不能放置方块（参考普通BlockPlacer的逻辑）
     *
     * @param level      放置世界
     * @param blockState 目标位置的方块状态
     * @param blockItem  要放置的方块物品（可选，用于检查类型匹配）
     * @return 当前位置是否不能放置方块
     */
    private boolean canNotBePlaced(Level level, BlockState blockState, @Nullable net.minecraft.world.item.BlockItem blockItem) {
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            // 流体（水、岩浆等）可以被直接替换
            if (!blockState.getFluidState().isEmpty()) {
                return false;
            }
            // 海龟蛋
            if (blockState.is(net.minecraft.world.level.block.Blocks.TURTLE_EGG) 
                && blockState.getValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS) < 4) {
                // 如果要放置的是海龟蛋，检查类型是否匹配
                return blockItem != null && blockState.getBlock() != blockItem.getBlock(); // 类型不匹配，不能放置
            }
            // 海泡菜
            if (blockState.is(net.minecraft.world.level.block.Blocks.SEA_PICKLE) 
                && blockState.getValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES) < 4) {
                // 如果要放置的是海泡菜，检查类型是否匹配
                return blockItem != null && blockState.getBlock() != blockItem.getBlock(); // 类型不匹配，不能放置
            }
            // 蜡烛
            if (blockState.getBlock() instanceof net.minecraft.world.level.block.CandleBlock) {
                if (blockState.getValue(net.minecraft.world.level.block.CandleBlock.CANDLES) >= 4) {
                    return true; // 已满，不能放置
                }
                // 如果要放置的是蜡烛，检查类型是否匹配
                return blockItem != null && blockState.getBlock() != blockItem.getBlock(); // 类型不匹配，不能放置
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
            () -> this.peekBlockItemFromContainer(level, placerPos),
            (blockItem, blockItemObj, targetPos) -> {
                // 放置成功后从容器中提取物品
                ItemStack extracted = this.extractBlockItemFromContainer(level, placerPos);
                if (extracted.isEmpty()) {
                    this.currentPlacementIndex = 0;
                    this.currentHeldBlock = ItemStack.EMPTY;
                    this.onChanged();
                    return false;
                }
                
                // 放置成功，清空钳子中的方块
                this.currentHeldBlock = ItemStack.EMPTY;
                
                // 检查是否可以继续堆叠
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
        
        executeBlockOperation(level, placerPos, facing, upsideDown,
            () -> sourceState.getBlock().asItem().getDefaultInstance(),
            (blockItem, blockItemObj, targetPos) -> {
                // 移动成功后删除源方块
                level.removeBlock(sourcePos, false);
                
                // 移动成功，清空钳子中的方块
                this.currentHeldBlock = ItemStack.EMPTY;
                
                // 移动模式不支持堆叠
                return false;
            }
        );
    }
    
    /**
     * 执行方块操作（放置或移动）的通用逻辑
     * 
     * @param level 世界
     * @param placerPos 放置器位置
     * @param facing 朝向
     * @param upsideDown 是否倒挂
     * @param itemSupplier 物品提供者（从容器或源位置获取）
     * @param onSuccess 成功回调，返回是否可以继续堆叠
     */
    private void executeBlockOperation(Level level, BlockPos placerPos, Direction facing, boolean upsideDown,
        java.util.function.Supplier<ItemStack> itemSupplier,
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
            // 检查是否是空位置或可以放置的位置
            if (targetState.isAir() || !this.canNotBePlaced(level, targetState, null)) {
                // 获取物品
                ItemStack blockItem = itemSupplier.get();
                if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem blockItemObj)) {
                    // 物品无效，跳过这个位置
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
                
                // 检查类型匹配
                if (!targetState.isAir() && this.canNotBePlaced(level, targetState, blockItemObj)) {
                    // 类型不匹配，跳过这个位置
                    this.currentPlacementIndex = (index + 1) % allPositions.size();
                    this.onChanged();
                    return;
                }
                
                Orientation orientation = this.calculatePlacementOrientation(facing, upsideDown);

                // 尝试放置方块
                if (AnvilCraftFakePlayers.anvilcraftBlockPlacer.placeBlock(
                    level, targetPos, orientation, blockItemObj, blockItem) == net.minecraft.world.InteractionResult.FAIL) {
                    // 放置失败（非法位置或实体阻挡），保持在当前位置重试
                    this.onChanged();
                    return;
                }
                
                // 放置成功，执行后续操作
                boolean canStack = onSuccess.handle(blockItem, blockItemObj, targetPos);
                
                if (canStack) {
                    // 可以继续堆叠，保持currentPlacementIndex不变
                    this.onChanged();
                    return;
                }
                
                // 移动到下一个位置
                this.currentPlacementIndex = (index + 1) % allPositions.size();
                this.onChanged();
                return;
            }
        }
    }
    
    /**
     * 方块操作成功回调接口
     */
    @FunctionalInterface
    private interface BlockOperationSuccessHandler {
        /**
         * 处理放置/移动成功后的逻辑
         * 
         * @param blockItem 方块物品
         * @param blockItemObj 方块物品对象
         * @param targetPos 目标位置
         * @return 是否可以继续堆叠
         */
        boolean handle(ItemStack blockItem, BlockItem blockItemObj, BlockPos targetPos);
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
                    // 处理细雪桶：替换为桶
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
            // 处理细雪桶：替换为桶
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
