package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.StructureScannerBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StructureScannerBlockEntity extends BaseMachineBlockEntity implements MenuProvider {
    /**
     * -- GETTER --
     *  获取Disk物品栏
     */
    // Disk物品栏
    @Getter
    private final SimpleContainer diskInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            StructureScannerBlockEntity.this.setChanged();
        }
    };
    
    /**
     * -- GETTER --
     *  获取输出物品栏
     */
    // 输出物品栏
    @Getter
    private final SimpleContainer outputInventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            StructureScannerBlockEntity.this.setChanged();
        }
    };
    
    // Disk物品栏的ItemHandler包装器,带物品验证
    private final IItemHandlerModifiable diskItemHandler = new InvWrapper(diskInventory) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 只允许放入结构磁盘
            if (!stack.is(ModItems.STRUCTURE_DISK.get())) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 只允许结构磁盘
            return stack.is(ModItems.STRUCTURE_DISK.get());
        }
        
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 禁止漏斗等外部设备取出物品
            return ItemStack.EMPTY;
        }
    };
    
    // 输出物品栏的ItemHandler包装器
    private final IItemHandlerModifiable outputItemHandler = new InvWrapper(outputInventory) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 允许漏斗等外部设备取出物品
            return super.extractItem(slot, amount, simulate);
        }
        
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 禁止漏斗等外部设备插入物品
            return stack;
        }
        
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 禁止外部设备插入任何物品
            return false;
        }
    };
    
    // 组合的ItemHandler
    private final IItemHandler combinedItemHandler = new CombinedInvWrapper(diskItemHandler, outputItemHandler);
    
    // 扫描范围 - X轴
    @Getter
    private final WatchableCyclingValue<Integer> rangeX = new WatchableCyclingValue<>(
        "rangeX",
        thiz -> this.setChanged(),
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    );
    
    // 扫描范围 - Y轴
    @Getter
    private final WatchableCyclingValue<Integer> rangeY = new WatchableCyclingValue<>(
        "rangeY",
        thiz -> this.setChanged(),
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    );
    
    // 扫描范围 - Z轴
    @Getter
    private final WatchableCyclingValue<Integer> rangeZ = new WatchableCyclingValue<>(
        "rangeZ",
        thiz -> this.setChanged(),
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    );

    /**
     * -- GETTER --
     *  是否正在扫描
     */
    // 扫描结果缓存（逐层扫描）
    @Getter
    private boolean isScanning = false;
    
    /**
     * -- GETTER --
     *  当前扫描的层
     */
    @Getter
    private int currentScanLayer = 0;
    
    /**
     * -- GETTER --
     *  获取扫描到的方块列表
     */
    @Getter
    private final List<CachedBlockData> scannedBlocks = new ArrayList<>();
    
    private long lastScanTick = 0;  // 上次扫描的tick
    
    // 自动保存相关
    private boolean pendingAutoSave = false;  // 是否有待执行的自动保存
    private String autoSaveStructureName = "";  // 自动保存的结构名称
    
    /**
     * 缓存的方块数据
     */
    public record CachedBlockData(int x, int y, int z, net.minecraft.world.level.block.state.BlockState state) {}
    
    /**
     * 是否正在扫描或已完成扫描
     */
    public boolean hasStartedScanning() {
        return this.isScanning || !this.scannedBlocks.isEmpty();
    }
    
    /**
     * 是否完成所有扫描
     */
    public boolean isScanComplete() {
        return !this.isScanning && !this.scannedBlocks.isEmpty();
    }
    
    /**
     * 信息栏状态枚举
     */
    public enum InfoStatus {
        READY,
        LARGE_STRUCTURE,
        UNKNOWN_BLOCKS,
        TOO_LARGE,
        MULTIBLOCK_BLOCKS
    }
    
    /**
     * 获取信息栏状态
     */
    public InfoStatus getInfoStatus() {
        // 检查是否超过16x16x16
        if (this.rangeX.get() > 16 || this.rangeY.get() > 16 || this.rangeZ.get() > 16) {
            return InfoStatus.TOO_LARGE;
        }
        
        // 检查是否大于5x5x5
        if (this.rangeX.get() > 5 || this.rangeY.get() > 5 || this.rangeZ.get() > 5) {
            return InfoStatus.LARGE_STRUCTURE;
        }
        
        // 检查是否有无法保存的方块
        if (this.hasUnknownBlocks()) {
            return InfoStatus.UNKNOWN_BLOCKS;
        }
        
        // 检查是否有多部分方块
        if (this.hasMultiblockBlocks()) {
            return InfoStatus.MULTIBLOCK_BLOCKS;
        }
        
        return InfoStatus.READY;
    }
    
    /**
     * 检查是否有无法保存的方块
     */
    private boolean hasUnknownBlocks() {
        // TODO: 实现检测逻辑，检查是否有无法序列化的方块
        return false;
    }
    
    /**
     * 检查是否有多部分方块
     */
    private boolean hasMultiblockBlocks() {
        if (this.level == null) return false;
        
        int rangeX = this.rangeX.get();
        int rangeY = this.rangeY.get();
        int rangeZ = this.rangeZ.get();
        int halfRangeX = rangeX / 2;

        // 使用与实际扫描相同的坐标计算方式，确保警告区域与保存/扫描区域一致
        for (int x = 0; x < rangeX; x++) {
            for (int y = 0; y < rangeY; y++) {
                for (int z = 0; z < rangeZ; z++) {
                    BlockPos checkPos = this.calculateWorldPos(x, y, z, halfRangeX);
                    net.minecraft.world.level.block.state.BlockState blockState = this.level.getBlockState(checkPos);
                    
                    // 检查是否为多方块方块
                    if (isMultiblockBlock(blockState)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查一个方块是否为多方块方块
     * 
     * @param blockState 方块状态
     * @return 如果是多方块方块返回true
     */
    private boolean isMultiblockBlock(net.minecraft.world.level.block.state.BlockState blockState) {
        net.minecraft.world.level.block.Block block = blockState.getBlock();
        
        // 使用switch表达式检查是否实现了多方块方块相关接口
        if (switch (block) {
                case dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity<?, ?> ignored1 -> true;
                case dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock<?> ignored2 -> true;
                case dev.dubhe.anvilcraft.api.IHasMultiBlock ignored3 -> true;
                default -> false;
            }) {
            return true;
        }
        
        // 检查原版多方块方块
        // 床（BED）：由两个方块组成
        if (block instanceof net.minecraft.world.level.block.BedBlock) {
            return true;
        }
        
        // 门（DOOR）：由上下两个方块组成
        return block instanceof net.minecraft.world.level.block.DoorBlock;
    }

    public StructureScannerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        // 设置默认值为 5 (index = 4)
        this.rangeX.fromIndex(4);
        this.rangeY.fromIndex(4);
        this.rangeZ.fromIndex(4);
    }
    
    @SuppressWarnings("unused")
    public void tickServer(net.minecraft.world.level.Level level, BlockPos pos) {
        // 每2 tick扫描一层
        if (this.isScanning && level.getGameTime() - this.lastScanTick >= 2) {
            this.scanNextLayer();
        }
        
        // 检查是否有待执行的自动保存
        if (this.pendingAutoSave && !this.isScanning && !this.scannedBlocks.isEmpty()) {
            // 扫描已完成，执行保存
            this.performAutoSave();
        }
    }
    
    /**
     * 开始扫描流程
     */
    public void startScanning() {
        if (this.level == null) {
            return;
        }
        
        // 如果已经在扫描中，重置扫描
        this.isScanning = true;
        this.currentScanLayer = 0;
        this.scannedBlocks.clear();
        this.lastScanTick = this.level.getGameTime();
        this.setChanged();
        
        // 同步到客户端（包括范围数据）
        if (!this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * 停止扫描
     */
    public void stopScanning() {
        this.isScanning = false;
        this.setChanged();
        
        // 同步到客户端
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }
    
    /**
     * 调度自动保存（在扫描完成后执行）
     */
    public void scheduleAutoSave(String structureName) {
        this.pendingAutoSave = true;
        this.autoSaveStructureName = structureName;
    }
    
    /**
     * 执行自动保存
     */
    private void performAutoSave() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        
        // 重置标志
        this.pendingAutoSave = false;
        
        // 检查是否放入了结构磁盘
        if (this.getDiskInventory().getItem(0).isEmpty()) {
            return;
        }
        
        // 检查输出槽位是否为空
        if (!this.getOutputInventory().getItem(0).isEmpty()) {
            return;
        }
        
        // 保存结构到磁盘
        dev.dubhe.anvilcraft.util.StructureSaveUtil.saveStructureToDisk(
            this.level, this, this.autoSaveStructureName
        );
        
        // 清空结构名称
        this.autoSaveStructureName = "";
    }
    
    /**
     * 扫描下一层
     */
    private void scanNextLayer() {
        if (this.level == null) {
            return;
        }
        
        final int rangeX = this.rangeX.get();
        final int rangeY = this.rangeY.get();
        final int rangeZ = this.rangeZ.get();
        final int halfRangeX = rangeX / 2;
        
        // 扫描当前层的所有方块
        for (int x = 0; x < rangeX; x++) {
            for (int z = 1; z < rangeZ + 1; z++) {
                BlockPos worldPos = calculateWorldPos(x, this.currentScanLayer, z - 1, halfRangeX);
                net.minecraft.world.level.block.state.BlockState blockState = this.level.getBlockState(worldPos);
                
                if (!blockState.isAir()) {
                    this.scannedBlocks.add(new CachedBlockData(x, this.currentScanLayer, z, blockState));
                }
            }
        }
        
        this.lastScanTick = this.level.getGameTime();
        this.setChanged();
        
        // 每扫描一层就同步到客户端
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
        
        // 移动到下一层
        this.currentScanLayer++;
        
        // 检查是否完成所有层
        if (this.currentScanLayer >= rangeY) {
            this.isScanning = false;
        }
    }
    
    /**
     * 计算世界坐标
     */
    private BlockPos calculateWorldPos(int previewX, int previewY, int previewZ, int halfRangeX) {
        BlockPos scannerPos = this.getBlockPos();
        if (this.level == null) {
            return scannerPos;
        }
        var blockState = this.level.getBlockState(scannerPos);
        Direction scannerFacing = blockState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        
        int localX = previewX - halfRangeX;
        
        return switch (scannerFacing) {
            case NORTH -> scannerPos.offset(localX, previewY, previewZ + 1);
            case SOUTH -> scannerPos.offset(-localX, previewY, -(previewZ + 1));
            case WEST -> scannerPos.offset(previewZ + 1, previewY, -localX);
            case EAST -> scannerPos.offset(-(previewZ + 1), previewY, localX);
            case DOWN, UP -> scannerPos.offset(localX, previewY, previewZ);
        };
    }

    @Override
    public Direction getDirection() {
        if (this.level != null) {
            return this.level.getBlockState(this.getBlockPos()).getValue(StructureScannerBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public void setDirection(Direction direction) {
        if (this.level != null) {
            this.level.setBlock(this.getBlockPos(), 
                this.getBlockState().setValue(StructureScannerBlock.FACING, direction), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.structure_scanner");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new StructureScannerMenu(ModMenuTypes.STRUCTURE_SCANNER.get(), containerId, inventory, this);
    }

    @Override
    public IItemHandler getItemHandler() {
        return combinedItemHandler;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, provider);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // 保存Disk物品栏
        tag.put("diskInventory", this.diskInventory.createTag(provider));
        // 保存输出物品栏
        tag.put("outputInventory", this.outputInventory.createTag(provider));
        // 保存扫描范围
        tag.putInt("rangeX", this.rangeX.index());
        tag.putInt("rangeY", this.rangeY.index());
        tag.putInt("rangeZ", this.rangeZ.index());
        // 保存扫描状态
        tag.putBoolean("isScanning", this.isScanning);
        tag.putInt("currentScanLayer", this.currentScanLayer);
        // 保存扫描结果
        if (!this.scannedBlocks.isEmpty()) {
            ListTag blocksTag = new ListTag();
            for (CachedBlockData data : this.scannedBlocks) {
                CompoundTag blockTag = new CompoundTag();
                blockTag.putInt("x", data.x());
                blockTag.putInt("y", data.y());
                blockTag.putInt("z", data.z());
                blockTag.put("state", net.minecraft.nbt.NbtUtils.writeBlockState(data.state()));
                blocksTag.add(blockTag);
            }
            tag.put("scannedBlocks", blocksTag);
        }
        // 保存自动保存状态
        tag.putBoolean("pendingAutoSave", this.pendingAutoSave);
        tag.putString("autoSaveStructureName", this.autoSaveStructureName);
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // 加载Disk物品栏
        this.diskInventory.fromTag(tag.getList("diskInventory", Tag.TAG_COMPOUND), provider);
        // 加载输出物品栏
        this.outputInventory.fromTag(tag.getList("outputInventory", Tag.TAG_COMPOUND), provider);
        // 加载扫描范围
        this.rangeX.fromIndex(tag.getInt("rangeX"));
        this.rangeY.fromIndex(tag.getInt("rangeY"));
        this.rangeZ.fromIndex(tag.getInt("rangeZ"));
        // 加载扫描状态
        this.isScanning = tag.getBoolean("isScanning");
        this.currentScanLayer = tag.getInt("currentScanLayer");
        // 加载扫描结果
        this.scannedBlocks.clear();
        if (tag.contains("scannedBlocks", Tag.TAG_LIST) && this.level != null) {
            ListTag blocksTag = tag.getList("scannedBlocks", Tag.TAG_COMPOUND);
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompound(i);
                int x = blockTag.getInt("x");
                int y = blockTag.getInt("y");
                int z = blockTag.getInt("z");
                net.minecraft.world.level.block.state.BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(
                    this.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),
                    blockTag.getCompound("state")
                );
                this.scannedBlocks.add(new CachedBlockData(x, y, z, state));
            }
        }
        // 加载自动保存状态
        this.pendingAutoSave = tag.getBoolean("pendingAutoSave");
        this.autoSaveStructureName = tag.getString("autoSaveStructureName");
    }
}
