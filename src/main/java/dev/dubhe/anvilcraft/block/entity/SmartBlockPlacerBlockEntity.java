package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import lombok.Getter;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
public class SmartBlockPlacerBlockEntity extends BlockEntity implements IPowerConsumer, MenuProvider {
    private static final int POWER = 16;
    private static final int PLACE_COOLDOWN = 10; // 放置冷却时间（tick），0.5秒
    private static final int INITIAL_DELAY = 40; // 初始延迟时间（tick），约2秒
    private PowerGrid grid = null;
    private boolean isPowered = false;
    private boolean hasRedstoneSignal = false;
    private int selectedLayer = 0;
    private int placeCooldown = 0;
    private boolean isWaitingForPlacement = false; // 是否在等待放置（2秒延迟中）
    /**
     * -- GETTER --
     *  获取过滤的方块类型
     */
    private ItemStack filterBlock = ItemStack.EMPTY; // 过滤的方块类型（用于GUI配置）
    private int currentPlacementIndex = 0; // 当前放置进度索引
    /**
     * -- GETTER --
     *  获取所有layer的位置配置
     *
     */
    private final Map<Integer, Set<Integer>> layerPositions = new HashMap<>(); // 每个layer对应的位置集合
    
    // 客户端动画状态（每个BlockEntity独立计算）
    private float clientAnimationTicks = 0f;
    private long clientLastGameTime = 0;
    private boolean clientWasPowered = false;
    private boolean clientWasRedstoneSignal = false;
    
    // 每个方块独立的动画偏移量，防止多个方块动画同步
    private long clientAnimationOffset = 0;
    private boolean clientAnimationOffsetInitialized = false;

    // Getter方法供渲染器使用
    // 客户端待机动画状态（每个BlockEntity独立）
    private boolean clientIdleSwinging = false;
    private float clientIdleSwingProgress = 0f;
    private float clientIdleSwingDirection = 0f;
    private long clientIdleLastTriggerTime = 0;
    private long clientIdleNextTriggerDelay = 0;
    
    // 客户端工作状态动画目标
    private BlockPos clientWorkingTargetPos = null;
    private BlockPos clientPreviousTargetPos = null; // 前一个目标位置，用于过渡
    private float clientWorkingAnimationProgress = 0f; // 0-1 表示动画进度
    private float clientTransitionProgress = 0f; // 过渡进度 0-1
    
    // 当前显示的角度（用于平滑过渡）
    private float clientCurrentBaseAngle = 0f;
    private float clientCurrentUpperArmAngle = 0f;
    private float clientCurrentForearmAngle = 0f;
    private float clientCurrentClawAngle = 0f;
    
    /**
     * 更新客户端动画状态（供渲染器调用）
     */
    public void updateClientAnimationState(float animationTicks, long lastGameTime, boolean wasPowered, boolean wasRedstoneSignal) {
        this.clientAnimationTicks = animationTicks;
        this.clientLastGameTime = lastGameTime;
        this.clientWasPowered = wasPowered;
        this.clientWasRedstoneSignal = wasRedstoneSignal;
    }
    
    /**
     * 初始化动画偏移量（在方块首次创建或重新通电时调用）
     */
    public void initAnimationOffset() {
        if (this.level == null) return;
        
        long currentTime = this.level.getGameTime();
        // 为每个方块设置独立的随机偏移量（0-100tick）
        // 使用位置哈希和时间组合作为随机种子，确保每个方块都有独特的偏移
        long seed = this.getBlockPos().asLong() ^ currentTime;
        this.clientAnimationOffset = Math.abs(seed % 201); // 0-100tick
        this.clientAnimationOffsetInitialized = true;
        
        // 同时重置待机动画的触发时间，使用偏移量
        this.clientIdleLastTriggerTime = currentTime + this.clientAnimationOffset;
        this.clientIdleNextTriggerDelay = 120; // 固定120tick (6秒)
    }
    
    /**
     * 更新待机动画状态
     */
    public void updateIdleAnimationState(float smoothTicks) {
        if (this.level == null) return;
        
        long currentTime = this.level.getGameTime();
        
        // 确保动画偏移量已初始化
        if (!clientAnimationOffsetInitialized) {
            initAnimationOffset();
        }
        
        // 使用偏移后的时间检查是否需要触发新的摆动
        // 关键：使用固定的offsetTime作为基准，而不是每次都用currentTime
        long offsetTime = clientIdleLastTriggerTime + clientIdleNextTriggerDelay;
        
        if (!clientIdleSwinging && currentTime >= offsetTime) {
            // 触发摆动
            clientIdleSwinging = true;
            clientIdleSwingDirection = (currentTime % 2 == 0) ? 1f : -1f; // 随机方向
            clientIdleSwingProgress = 0f; // 重置进度
            clientIdleLastTriggerTime = currentTime; // 记录本次触发时间
            // 计算下一次触发延迟（固定间隔）
            clientIdleNextTriggerDelay = 120; // 固定120tick (6秒)
        }
        
        // 如果正在摆动，计算进度（使用游戏时间差 + partialTick实现平滑）
        if (clientIdleSwinging) {
            long elapsedTicks = currentTime - clientIdleLastTriggerTime;
            // 估算partialTick（smoothTicks包含的帧时间）
            float partialTick = smoothTicks - (float)
                ((long)
                    smoothTicks);
            clientIdleSwingProgress = elapsedTicks + partialTick;
            
            // 摆动结束（总时长100tick = 5秒）
            if (clientIdleSwingProgress >= 100f) {
                clientIdleSwinging = false;
                clientIdleSwingProgress = 0f;
                // 计算下一次触发时间
                offsetTime = clientIdleLastTriggerTime + clientIdleNextTriggerDelay;
                clientIdleLastTriggerTime = offsetTime; // 设置下次触发的基准时间
            }
        }
    }
    
    /**
     * 重置待机动画状态
     */
    public void resetIdleAnimationState() {
        clientIdleSwinging = false;
        clientIdleSwingProgress = 0f;
        clientIdleSwingDirection = 0f;
        clientIdleLastTriggerTime = 0;
        clientIdleNextTriggerDelay = 0;
        clientAnimationOffset = 0;
        clientAnimationOffsetInitialized = false;
    }
    
    /**
     * 更新工作状态动画目标（供渲染器调用）
     */
    public void updateWorkingTarget(BlockPos targetPos, float animationProgress) {
        // 如果目标位置改变，保存前一个位置用于过渡
        if (this.clientWorkingTargetPos != null && !this.clientWorkingTargetPos.equals(targetPos)) {
            this.clientPreviousTargetPos = this.clientWorkingTargetPos;
            this.clientTransitionProgress = 0f; // 重置过渡进度
        }
        this.clientWorkingTargetPos = targetPos;
        this.clientWorkingAnimationProgress = animationProgress;
    }
    
    /**
     * 获取前一个目标位置
     */
    public BlockPos getClientPreviousTargetPos() {
        return clientPreviousTargetPos;
    }
    
    /**
     * 获取过渡进度
     */
    public float getClientTransitionProgress() {
        return clientTransitionProgress;
    }
    
    /**
     * 更新过渡进度
     */
    public void updateTransitionProgress(float progress) {
        this.clientTransitionProgress = progress;
    }
    
    /**
     * 获取当前显示角度
     */
    public float getClientCurrentAngle(int index) {
        return switch (index) {
            case 0 -> clientCurrentBaseAngle;
            case 1 -> clientCurrentUpperArmAngle;
            case 2 -> clientCurrentForearmAngle;
            case 3 -> clientCurrentClawAngle;
            default -> 0f;
        };
    }
    
    /**
     * 设置当前显示角度
     */
    public void setCurrentAngles(float baseAngle, float upperArmAngle, float forearmAngle, float clawAngle) {
        this.clientCurrentBaseAngle = baseAngle;
        this.clientCurrentUpperArmAngle = upperArmAngle;
        this.clientCurrentForearmAngle = forearmAngle;
        this.clientCurrentClawAngle = clawAngle;
    }
    
    /**
     * 重置工作状态动画
     */
    public void resetWorkingState() {
        this.clientPreviousTargetPos = null;
        this.clientWorkingTargetPos = null;
        this.clientWorkingAnimationProgress = 0f;
        this.clientTransitionProgress = 0f;
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
        tag.putBoolean("isWaitingForPlacement", isWaitingForPlacement);
        tag.putInt("placeCooldown", placeCooldown);
        // 保存过滤的方块（只在非空时保存）
        if (!filterBlock.isEmpty()) {
            tag.put("filterBlock", filterBlock.save(provider));
        }
        // 保存每个layer的位置集合
        CompoundTag layerTag = new CompoundTag();
        for (Map.Entry<Integer, Set<Integer>> entry : layerPositions.entrySet()) {
            int[] positions = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
            layerTag.putIntArray("layer_" + entry.getKey(), positions);
        }
        tag.put("layerPositions", layerTag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.isPowered = tag.getBoolean("isPowered");
        this.hasRedstoneSignal = tag.getBoolean("hasRedstoneSignal");
        this.selectedLayer = tag.getInt("selectedLayer");
        this.currentPlacementIndex = tag.getInt("currentPlacementIndex");
        this.isWaitingForPlacement = tag.getBoolean("isWaitingForPlacement");
        this.placeCooldown = tag.getInt("placeCooldown");
        // 加载过滤的方块（只在存在时加载）
        if (tag.contains("filterBlock", Tag.TAG_COMPOUND)) {
            this.filterBlock = ItemStack.parse(provider, tag.getCompound("filterBlock")).orElse(ItemStack.EMPTY);
        } else {
            this.filterBlock = ItemStack.EMPTY;
        }
        // 加载每个layer的位置集合
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

    public void tick(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            // 服务端检测电网通电状态
            boolean wasPowered = this.isPowered;
            boolean wasRedstoneSignal = this.hasRedstoneSignal;
            this.isPowered = grid != null && grid.isWorking();
            // 检测红石信号
            this.hasRedstoneSignal = level.hasNeighborSignal(pos);
            
            // 如果从"不能工作"变为"可以工作"，重置放置索引
            // 不能工作的情况：断电 或 有红石信号
            // 可以工作的情况：通电 且 无红石信号
            boolean wasAbleToWork = wasPowered && !wasRedstoneSignal;
            boolean isAbleToWork = this.isPowered && !this.hasRedstoneSignal;
            
            if (!wasAbleToWork && isAbleToWork) {
                currentPlacementIndex = 0;
                System.out.println("[重置] 恢复工作状态，重置放置索引为0");
            }
            
            // 方块放置逻辑
            if (isPowered && !hasRedstoneSignal) {
                // 检查是否有需要放置的位置
                boolean needsPlacement = hasEmptyPositions(level, pos);
                
                if (needsPlacement && !isWaitingForPlacement) {
                    // 需要放置且不在等待中，开始2秒延迟
                    isWaitingForPlacement = true;
                    placeCooldown = INITIAL_DELAY;
                }
                
                if (isWaitingForPlacement) {
                    if (placeCooldown > 0) {
                        // 等待中，倒计时
                        placeCooldown--;
                    } else {
                        // 延迟结束，执行放置
                        boolean placed = placeBlocks(level, pos);
                        if (placed) {
                            // 放置成功，设置0.5秒冷却继续放置下一个
                            placeCooldown = PLACE_COOLDOWN;
                        } else {
                            // 放置失败（没有物品或所有位置已满），重置等待状态
                            isWaitingForPlacement = false;
                            placeCooldown = 0;
                        }
                    }
                }
            } else {
                // 断电或有红石信号时，重置所有状态
                isWaitingForPlacement = false;
                placeCooldown = 0;
            }
            
            onChanged();
        }
    }
    
    /**
     * 检查是否有空位需要放置方块
     */
    private boolean hasEmptyPositions(Level level, BlockPos placerPos) {
        BlockState placerState = level.getBlockState(placerPos);
        Direction facing = placerState.getValue(HorizontalDirectionalBlock.FACING);
        
        // 计算基准位置（放置器前方4格，水平方向）
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        
        // 检查所有配置的位置
        for (Map.Entry<Integer, Set<Integer>> entry : layerPositions.entrySet()) {
            int layer = entry.getKey();
            Set<Integer> positions = entry.getValue();
            
            for (int position : positions) {
                int row = position / 5;
                int col = position % 5;
                BlockPos targetPos = calculateTargetPosition(basePos, facing, row, col, layer);
                
                // 如果有任何一个位置是空的，返回true
                if (level.isEmptyBlock(targetPos)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 放置方块（每次只放置一个）
     *
     * @return 是否成功放置了方块
     */
    private boolean placeBlocks(Level level, BlockPos placerPos) {
        BlockState placerState = level.getBlockState(placerPos);
        Direction facing = placerState.getValue(HorizontalDirectionalBlock.FACING);
        
        // 计算基准位置（放置器前方4格，水平方向）
        BlockPos basePos = placerPos.relative(facing.getOpposite(), -4);
        
        // 构建所有待放置位置的列表，按新顺序排序
        List<BlockPos> allPositions = buildOrderedPositions(basePos, facing);
        
        // 如果没有配置任何位置，返回false
        if (allPositions.isEmpty()) {
            return false;
        }
        
        // 如果索引超出范围，重置
        if (currentPlacementIndex >= allPositions.size()) {
            currentPlacementIndex = 0;
        }
        
        // 从当前索引开始查找可以放置的位置
        for (int i = 0; i < allPositions.size(); i++) {
            int index = (currentPlacementIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);
            
            // 如果目标位置为空，则尝试放置方块
            if (level.isEmptyBlock(targetPos)) {
                // 调试：输出当前放置的位置信息
                System.out.println("[实际放置] Index=" + index + ", Pos=" + targetPos + 
                    ", 相对basePos: dx=" + (targetPos.getX() - basePos.getX()) + 
                    ", dy=" + (targetPos.getY() - basePos.getY()) + 
                    ", dz=" + (targetPos.getZ() - basePos.getZ()));
                
                // 从背面容器提取匹配的方块物品
                ItemStack blockItem = extractBlockItemFromNearbyContainers(level, placerPos);
                if (blockItem.isEmpty()) {
                    return false; // 没有找到可用的方块物品
                }
                
                if (blockItem.getItem() instanceof BlockItem blockItemObj) {
                    // 计算放置方向
                    final boolean upsideDown = placerState.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
                    Orientation orientation = calculatePlacementOrientation(facing, upsideDown);
                    
                    // 使用FakePlayer放置方块（支持方块的朝向、特殊放置逻辑）
                    if (AnvilCraftFakePlayers.anvilcraftBlockPlacer.placeBlock(
                        level, targetPos, orientation, blockItemObj, blockItem) == net.minecraft.world.InteractionResult.FAIL) {
                        return false; // 放置失败
                    }
                    
                    // 更新进度索引
                    currentPlacementIndex = (index + 1) % allPositions.size();
                    System.out.println("[实际放置] 成功！下一个index=" + currentPlacementIndex);
                    return true; // 成功放置一个方块
                }
            }
        }
        
        // 所有位置都已有方块
        return false;
    }
    
    /**
     * 构建有序的放置位置列表
     * 顺序：从最下面一层开始，每一层从最远离放置器的位置开始，从左到右，然后逐渐向下
     */
    private List<BlockPos> buildOrderedPositions(BlockPos basePos, Direction facing) {
        List<BlockPos> positions = new ArrayList<>();
        
        // 获取所有layer，按layer升序排序（从最下面开始）
        List<Integer> sortedLayers = new ArrayList<>(layerPositions.keySet());
        sortedLayers.sort(Integer::compareTo);
        
        for (int layer : sortedLayers) {
            Set<Integer> layerPositionsSet = layerPositions.get(layer);
            if (layerPositionsSet == null || layerPositionsSet.isEmpty()) {
                continue;
            }
            
            // 将该层的所有位置收集起来
            List<int[]> rowColList = new ArrayList<>();
            for (int position : layerPositionsSet) {
                int row = position / 5;
                int col = position % 5;
                rowColList.add(new int[]{row, col});
            }
            
            // 排序：先按row升序（从远到近，0→4），再按col升序（从左到右，0→4）
            rowColList.sort((a, b) -> {
                if (a[0] != b[0]) {
                    return Integer.compare(a[0], b[0]); // row升序（0→4，远→近）
                }
                return Integer.compare(a[1], b[1]); // col升序（0→4，左→右）
            });
            
            // 按排序后的顺序添加位置
            for (int[] rowCol : rowColList) {
                int row = rowCol[0];
                int col = rowCol[1];
                BlockPos targetPos = calculateTargetPosition(basePos, facing, row, col, layer);
                positions.add(targetPos);
            }
        }
        
        return positions;
    }
    
    /**
     * 从背面一格的容器中提取物块物品
     */
    private ItemStack extractBlockItemFromNearbyContainers(Level level, BlockPos placerPos) {
        BlockState placerState = level.getBlockState(placerPos);
        Direction facing = placerState.getValue(HorizontalDirectionalBlock.FACING);
            
        // 背面一格的位置（facing的反方向）
        BlockPos containerPos = placerPos.relative(facing.getOpposite());
            
        // 尝试从容器提取物品
        IItemHandler handler = level.getCapability(
            Capabilities.ItemHandler.BLOCK,
            containerPos,
            null
        );
            
        if (handler != null) {
            // 查找方块物品
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                    // 如果设置了过滤方块，检查是否匹配
                    if (!filterBlock.isEmpty()) {
                        if (!ItemStack.isSameItemSameComponents(stack, filterBlock)) {
                            continue; // 不匹配，跳过
                        }
                    }
                    // 提取一个物品
                    ItemStack extracted = handler.extractItem(slot, 1, false);
                    if (!extracted.isEmpty()) {
                        return extracted;
                    }
                }
            }
        }
            
        return ItemStack.EMPTY;
    }
    
    /**
     * 计算放置方向
     */
    private Orientation calculatePlacementOrientation(Direction facing, boolean upsideDown) {
        if (upsideDown) {
            return switch (facing) {
                case NORTH -> Orientation.SOUTH_UP;
                case SOUTH -> Orientation.NORTH_UP;
                case WEST -> Orientation.EAST_UP;
                case EAST -> Orientation.WEST_UP;
                default -> Orientation.NORTH_UP;
            };
        } else {
            return switch (facing) {
                case NORTH -> Orientation.NORTH_UP;
                case SOUTH -> Orientation.SOUTH_UP;
                case WEST -> Orientation.WEST_UP;
                case EAST -> Orientation.EAST_UP;
                default -> Orientation.NORTH_UP;
            };
        }
    }
    
    /**
     * 计算目标位置
     */
    private BlockPos calculateTargetPosition(BlockPos basePos, Direction facing, int row, int col, int layer) {
        // 根据朝向计算水平偏移方向
        Direction right = facing.getClockWise();
        
        BlockPos pos = basePos;
        pos = pos.above(layer); // 层偏移（垂直向上）
        pos = pos.relative(right, col - 2); // 列偏移（-2到+2）
        pos = pos.relative(facing.getClockWise().getClockWise(), row - 2); // 行偏移（-2到+2）
        
        return pos;
    }
    
    /**
     * 设置过滤的方块类型
     */
    @SuppressWarnings("unused")
    public void setFilterBlock(ItemStack block) {
        this.filterBlock = block.copy();
        this.onChanged();
    }

    public void onChanged() {
        this.setChanged();
        Level level = this.getLevel();
        if (level == null) return;
        level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    public void setSelectedLayer(int layer) {
        this.selectedLayer = layer;
        this.onChanged();
    }

    /**
     * 切换位置的选中状态
     *
     * @param layer layer索引
     * @param position 位置索引 (0-24)
     * @param selected 是否选中
     */
    public void togglePosition(int layer, int position, boolean selected) {
        Set<Integer> positions = layerPositions.computeIfAbsent(layer, k -> new HashSet<>());
        if (selected) {
            positions.add(position);
        } else {
            positions.remove(position);
            // 如果该layer没有位置了，移除它
            if (positions.isEmpty()) {
                layerPositions.remove(layer);
            }
        }
        this.onChanged();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("isPowered", isPowered);
        tag.putBoolean("hasRedstoneSignal", hasRedstoneSignal);
        tag.putInt("selectedLayer", selectedLayer);
        tag.putInt("currentPlacementIndex", currentPlacementIndex);
        tag.putBoolean("isWaitingForPlacement", isWaitingForPlacement);
        tag.putInt("placeCooldown", placeCooldown);
        // 同步过滤的方块（只在非空时同步）
        if (!filterBlock.isEmpty()) {
            tag.put("filterBlock", filterBlock.save(registries));
        }
        // 同步所有layer的位置配置
        CompoundTag layerTag = new CompoundTag();
        for (Map.Entry<Integer, Set<Integer>> entry : layerPositions.entrySet()) {
            int[] positions = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
            layerTag.putIntArray("layer_" + entry.getKey(), positions);
        }
        tag.put("layerPositions", layerTag);
        return tag;
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
        if (player.isSpectator()) return null;
        return new SmartBlockPlacerMenu(ModMenuTypes.SMART_BLOCK_PLACER.get(), containerId, inventory, this);
    }

}
