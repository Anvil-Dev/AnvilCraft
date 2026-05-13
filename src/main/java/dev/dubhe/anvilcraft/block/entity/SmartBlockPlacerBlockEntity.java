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
    private static final int PLACEMENT_INTERVAL = 20; // 放置间隔（tick），1秒
    private static final int PLACEMENT_DELAY = 6; // 放置延迟时间（tick），0.7秒（动画进行到“往前戳”时放置）
    private PowerGrid grid = null;
    private boolean isPowered = false;
    private boolean hasRedstoneSignal = false;
    private int selectedLayer = 0;
    private int placeCooldown = 0; // 放置冷却计时器（0=可以放置，>0=冷却中）
    private ItemStack currentHeldBlock = ItemStack.EMPTY; // 当前钳子中持有的方块（用于客户端渲染）
    private int currentPlacementIndex = 0; // 当前放置进度索引
    /**
     * -- GETTER --
     *  获取所有layer的位置配置
     *
     */
    private final Map<Integer, Set<Integer>> layerPositions = new HashMap<>(); // 每个layer对应的位置集合
    
    // 客户端动画状态
    private long clientAnimationStartTime = 0; // 动画开始时间
    private BlockPos clientLastTargetPos = null; // 上一个目标位置
    private int lastPlaceCooldown = 0; // 上一次的placeCooldown值，用于检测新周期
    
    /**
     * 更新客户端动画状态（供渲染器调用）
     */
    public void updateClientAnimationState(boolean isPowered, boolean hasRedstoneSignal) {
        // 如果断电或有红石信号，重置动画状态
        if (!isPowered || hasRedstoneSignal) {
            clientAnimationStartTime = 0;
            clientLastTargetPos = null;
        }
    }
    
    /**
     * 获取动画开始时间
     */
    public long getClientAnimationStartTime() {
        return clientAnimationStartTime;
    }
    
    /**
     * 设置动画开始时间
     */
    public void setClientAnimationStartTime(long time) {
        this.clientAnimationStartTime = time;
    }
    
    /**
     * 获取上一个目标位置
     */
    public BlockPos getClientLastTargetPos() {
        return clientLastTargetPos;
    }
    
    /**
     * 设置上一个目标位置
     */
    public void setClientLastTargetPos(BlockPos pos) {
        this.clientLastTargetPos = pos;
    }
    
    /**
     * 获取上一次的placeCooldown值
     */
    public int getLastPlaceCooldown() {
        return lastPlaceCooldown;
    }
    
    /**
     * 设置上一次的placeCooldown值
     */
    public void setLastPlaceCooldown(int cooldown) {
        this.lastPlaceCooldown = cooldown;
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
        // 保存当前持有的方块（用于客户端渲染）
        if (!currentHeldBlock.isEmpty()) {
            tag.put("currentHeldBlock", currentHeldBlock.save(provider));
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
        this.placeCooldown = tag.getInt("placeCooldown");
        // 加载当前持有的方块
        if (tag.contains("currentHeldBlock", Tag.TAG_COMPOUND)) {
            this.currentHeldBlock = ItemStack.parse(provider, tag.getCompound("currentHeldBlock")).orElse(ItemStack.EMPTY);
        } else {
            this.currentHeldBlock = ItemStack.EMPTY;
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
                
            // 如果从“不能工作”变为“可以工作”，重置放置索引
            // 不能工作的情况：断电 或 有红石信号
            // 可以工作的情况：通电 且 无红石信号
            boolean wasAbleToWork = wasPowered && !wasRedstoneSignal;
            boolean isAbleToWork = this.isPowered && !this.hasRedstoneSignal;
                
            if (!wasAbleToWork && isAbleToWork) {
                currentPlacementIndex = 0;
            }
                
            // 方块放置逻辑 - 简化版
            // 逻辑说明：
            // 1. placeCooldown = 0: 可以开始新周期
            // 2. placeCooldown = 20→1: 冷却倒计时，在4时放置方块（动画进行到0.8秒，“往前戳”动作）
            // 3. 每20tick一个完整周期
            // 4. 只有当前周期结束后才能开始新周期，中途出现空位不影响正在进行的周期
            // 5. 即使没有空位，也要让placeCooldown完整倒计时，确保动画播放完成
            // 动画时序对应：
            //   0-0.2s (tick 0-4, placeCooldown 20-16): 底盘旋转
            //   0.2-0.4s (tick 4-8, placeCooldown 16-12): 机械臂移动到位（旋转完成后立即开始）
            //   0.4-0.8s (tick 8-16, placeCooldown 12-4): 停顿
            //   0.8s (tick 16, placeCooldown 4): ★ 放置方块（往前戳开始）
            //   0.8-0.9s (tick 16-18, placeCooldown 4-2): 往前戳动作
            //   0.9-1.0s (tick 18-20, placeCooldown 2-0): 收回动画
            if (isPowered && !hasRedstoneSignal) {
                boolean needsPlacement = hasEmptyPositions(level, pos);
                boolean hasBlocksInContainer = hasBlockItemsInContainer(level, pos);
                
                // 冷却倒计时（先倒计时，再判断是否开始新周期）
                if (placeCooldown > 0) {
                    placeCooldown--;
                    
                    // 在倒计时到4时（即经过了16tick，0.8秒），执行放置（“往前戳”动作开始）
                    if (placeCooldown == PLACEMENT_DELAY && needsPlacement && hasBlocksInContainer) {
                        placeBlocks(level, pos);
                    }
                } else if (needsPlacement && hasBlocksInContainer) {
                    // 只有在冷却完全结束后，且有空位，且容器中有方块时，才能开始新周期
                    placeCooldown = PLACEMENT_INTERVAL; // 设置为20，下一个tick开始倒计时
                    // 预览要放置的方块，用于客户端渲染（不真正提取）
                    currentHeldBlock = peekBlockItemFromNearbyContainers(level, pos);
                    onChanged();
                }
            } else {
                // 断电或有红石信号时，重置
                placeCooldown = 0;
                currentHeldBlock = ItemStack.EMPTY;
            }
                
            onChanged();
        } else {
            // 客户端tick：更新工作状态动画
            tickClient(level, pos);
        }
    }
        
    /**
     * 客户端tick，用于更新动画状态
     */
    private void tickClient(Level level, BlockPos pos) {
        // 检测新周期的开始：placeCooldown从0变为20
        if (lastPlaceCooldown == 0 && placeCooldown == PLACEMENT_INTERVAL) {
            // 新周期开始，重置动画状态
            clientAnimationStartTime = 0;
            clientLastTargetPos = null;
        }
        
        // 更新lastPlaceCooldown
        lastPlaceCooldown = placeCooldown;
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
     * 检查容器中是否有方块物品
     */
    private boolean hasBlockItemsInContainer(Level level, BlockPos placerPos) {
        BlockState placerState = level.getBlockState(placerPos);
        Direction facing = placerState.getValue(HorizontalDirectionalBlock.FACING);
            
        // 背面一格的位置（facing的反方向）
        BlockPos containerPos = placerPos.relative(facing.getOpposite());
            
        // 尝试从容器检查物品
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
                    return true; // 找到方块物品
                }
            }
        }
            
        return false; // 没有找到方块物品
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
                // 从背面容器提取匹配的方块物品
                ItemStack blockItem = extractBlockItemFromNearbyContainers(level, placerPos);
                if (blockItem.isEmpty()) {
                    // 没有提取到方块，清空持有的方块
                    currentHeldBlock = ItemStack.EMPTY;
                    onChanged();
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
                    // 放置完成后清空持有的方块
                    currentHeldBlock = ItemStack.EMPTY;
                    onChanged();
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
     * 从背面一格的容器中预览方块物品（不真正提取）
     */
    private ItemStack peekBlockItemFromNearbyContainers(Level level, BlockPos placerPos) {
        BlockState placerState = level.getBlockState(placerPos);
        Direction facing = placerState.getValue(HorizontalDirectionalBlock.FACING);
            
        // 背面一格的位置（facing的反方向）
        BlockPos containerPos = placerPos.relative(facing.getOpposite());
            
        // 尝试从容器预览物品
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
                    // 返回副本，不真正提取
                    return stack.copy();
                }
            }
        }
            
        return ItemStack.EMPTY;
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
        tag.putInt("placeCooldown", placeCooldown);
        // 同步当前持有的方块（用于客户端渲染）
        if (!currentHeldBlock.isEmpty()) {
            tag.put("currentHeldBlock", currentHeldBlock.save(registries));
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
