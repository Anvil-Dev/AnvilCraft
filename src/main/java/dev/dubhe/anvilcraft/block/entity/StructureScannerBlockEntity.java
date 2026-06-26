package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.workstation.StructureScannerBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.StructureSaveUtil;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StructureScannerBlockEntity extends BaseMachineBlockEntity implements MenuProvider {
    // 物品栏处理器: 槽位0=磁盘输入, 槽位1=输出
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(2) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (index == 0) {
                // 只允许放入结构磁盘
                return resource.getItem() == ModItems.STRUCTURE_DISK.get();
            }
            // 输出槽位: 禁止外部设备插入
            return false;
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return super.insert(0, resource, amount, transaction);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != 0) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return super.extract(1, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != 1) return 0;
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            StructureScannerBlockEntity.this.setChanged();
        }
    };

    // 向后兼容的 SimpleContainer 适配器（供菜单槽位使用，共享 FilteredItemStackHandler 的 backing list）
    @Getter
    private final SimpleContainer diskInventory = new SimpleContainer(1) {
        @Override
        public ItemStack getItem(int index) {
            return StructureScannerBlockEntity.this.itemHandler.getStacks().getFirst();
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack stack = StructureScannerBlockEntity.this.itemHandler.getStacks().getFirst();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = stack.split(count);
            if (stack.isEmpty()) StructureScannerBlockEntity.this.itemHandler.getStacks().set(0, ItemStack.EMPTY);
            this.setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack stack = StructureScannerBlockEntity.this.itemHandler.getStacks().getFirst();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            StructureScannerBlockEntity.this.itemHandler.getStacks().set(0, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            StructureScannerBlockEntity.this.itemHandler.getStacks().set(0, stack);
            this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return StructureScannerBlockEntity.this.itemHandler.isValid(0, ItemResource.of(stack));
        }

        @Override
        public void setChanged() {
            super.setChanged();
            StructureScannerBlockEntity.this.setChanged();
        }

        @Override
        public boolean isEmpty() {
            return StructureScannerBlockEntity.this.itemHandler.getStacks().getFirst().isEmpty();
        }
    };

    @Getter
    private final SimpleContainer outputInventory = new SimpleContainer(1) {
        @Override
        public ItemStack getItem(int index) {
            return StructureScannerBlockEntity.this.itemHandler.getStacks().getLast();
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            ItemStack stack = StructureScannerBlockEntity.this.itemHandler.getStacks().getLast();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = stack.split(count);
            if (stack.isEmpty()) StructureScannerBlockEntity.this.itemHandler.getStacks().set(1, ItemStack.EMPTY);
            this.setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack stack = StructureScannerBlockEntity.this.itemHandler.getStacks().getLast();
            if (stack.isEmpty()) return ItemStack.EMPTY;
            StructureScannerBlockEntity.this.itemHandler.getStacks().set(1, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            StructureScannerBlockEntity.this.itemHandler.getStacks().set(1, stack);
            this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return false;  // 输出槽位禁止手动放入
        }

        @Override
        public void setChanged() {
            super.setChanged();
            StructureScannerBlockEntity.this.setChanged();
        }

        @Override
        public boolean isEmpty() {
            return StructureScannerBlockEntity.this.itemHandler.getStacks().getLast().isEmpty();
        }
    };

    // 扫描范围 - X轴
    @Getter
    private final WatchableCyclingValue<Integer> rangeX = new WatchableCyclingValue<>(
        "rangeX",
        ignored -> this.setChanged(),
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    );
    
    // 扫描范围 - Y轴
    @Getter
    private final WatchableCyclingValue<Integer> rangeY = new WatchableCyclingValue<>(
        "rangeY",
        ignored -> this.setChanged(),
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    );
    
    // 扫描范围 - Z轴
    @Getter
    private final WatchableCyclingValue<Integer> rangeZ = new WatchableCyclingValue<>(
        "rangeZ",
        ignored -> this.setChanged(),
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
    public record CachedBlockData(int x, int y, int z, BlockState state) {}
    
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
        TOO_LARGE
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

        return InfoStatus.READY;
    }
    
    /**
     * 检查是否有无法保存的方块
     */
    private boolean hasUnknownBlocks() {
        // TODO: 实现检测逻辑，检查是否有无法序列化的方块
        return false;
    }
    
    public StructureScannerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        // 设置默认值为 5 (index = 4)
        this.rangeX.fromIndex(4);
        this.rangeY.fromIndex(4);
        this.rangeZ.fromIndex(4);
    }
    
    @SuppressWarnings("unused")
    public void tickServer(Level level, BlockPos pos) {
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
        if (!this.level.isClientSide()) {
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
        if (this.level != null && !this.level.isClientSide()) {
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
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        
        // 重置标志
        this.pendingAutoSave = false;
        
        // 检查是否放入了结构磁盘
        if (this.itemHandler.getResource(0).isEmpty()) {
            return;
        }
        
        // 检查输出槽位是否为空
        if (!this.itemHandler.getResource(1).isEmpty()) {
            return;
        }
        
        // 保存结构到磁盘
        StructureSaveUtil.saveStructureToDisk(
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
                BlockPos worldPos = this.calculateWorldPos(x, this.currentScanLayer, z - 1, halfRangeX);
                BlockState blockState = this.level.getBlockState(worldPos);
                
                if (!blockState.isAir()) {
                    this.scannedBlocks.add(new CachedBlockData(x, this.currentScanLayer, z, blockState));
                }
            }
        }
        
        this.lastScanTick = this.level.getGameTime();
        this.setChanged();

        // 移动到下一层
        this.currentScanLayer++;

        // 检查是否完成所有层（在发送同步包之前更新状态）
        if (this.currentScanLayer >= rangeY) {
            this.isScanning = false;
        }

        // 每扫描一层就同步到客户端（此时 isScanning 已处于最终状态）
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
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
        Direction scannerFacing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = false;
        if (blockState.hasProperty(StructureScannerBlock.UPSIDE_DOWN)) {
            upsideDown = blockState.getValue(StructureScannerBlock.UPSIDE_DOWN);
        }
        
        int localX = previewX - halfRangeX;
        int localY = upsideDown ? -previewY : previewY;
        
        return switch (scannerFacing) {
            case NORTH -> scannerPos.offset(localX, localY, previewZ + 2);
            case SOUTH -> scannerPos.offset(-localX, localY, -(previewZ + 2));
            case WEST -> scannerPos.offset(previewZ + 2, localY, -localX);
            case EAST -> scannerPos.offset(-(previewZ + 2), localY, localX);
            case DOWN, UP -> scannerPos.offset(localX, localY, previewZ);
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
            this.level.setBlock(
                this.getBlockPos(),
                this.getBlockState()
                    .setValue(StructureScannerBlock.FACING, direction),
                3
            );
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
    public FilteredItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    // 便捷访问方法
    public ItemStack getDiskStack() {
        ItemResource resource = this.itemHandler.getResource(0);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(this.itemHandler.getAmountAsInt(0));
    }

    public void setDiskStack(ItemStack stack) {
        this.itemHandler.set(0, ItemResource.of(stack), stack.getCount());
    }

    public ItemStack getOutputStack() {
        ItemResource resource = this.itemHandler.getResource(1);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(this.itemHandler.getAmountAsInt(1));
    }

    public void setOutputStack(ItemStack stack) {
        this.itemHandler.set(1, ItemResource.of(stack), stack.getCount());
    }

    public boolean isDiskEmpty() {
        return this.itemHandler.getResource(0).isEmpty();
    }

    public boolean hasOutput() {
        return !this.itemHandler.getResource(1).isEmpty();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), provider);
        output.store(super.getUpdateTag(provider));
        this.saveAdditionalData(output);
        return output.buildResult();
    }

    private void saveAdditionalData(ValueOutput output) {
        ValueOutput child = output.child("Inventory");
        this.itemHandler.serialize(child);
        output.putInt("rangeX", this.rangeX.index());
        output.putInt("rangeY", this.rangeY.index());
        output.putInt("rangeZ", this.rangeZ.index());
        output.putBoolean("isScanning", this.isScanning);
        output.putInt("currentScanLayer", this.currentScanLayer);
        if (!this.scannedBlocks.isEmpty()) {
            ValueOutput.ValueOutputList blocks = output.childrenList("scannedBlocks");
            for (CachedBlockData data : this.scannedBlocks) {
                ValueOutput blockOutput = blocks.addChild();
                blockOutput.putInt("x", data.x());
                blockOutput.putInt("y", data.y());
                blockOutput.putInt("z", data.z());
                blockOutput.store("state", BlockState.CODEC, data.state());
            }
        }
        output.putBoolean("pendingAutoSave", this.pendingAutoSave);
        output.putString("autoSaveStructureName", this.autoSaveStructureName);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.saveAdditionalData(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.loadScannerData(input);
    }

    private void loadScannerData(ValueInput input) {
        // 反序列化到 handler
        input.child("Inventory").ifPresent(this.itemHandler::deserialize);
        this.rangeX.fromIndex(input.getIntOr("rangeX", 0));
        this.rangeY.fromIndex(input.getIntOr("rangeY", 0));
        this.rangeZ.fromIndex(input.getIntOr("rangeZ", 0));
        this.isScanning = input.getBooleanOr("isScanning", false);
        this.currentScanLayer = input.getIntOr("currentScanLayer", 0);
        this.scannedBlocks.clear();
        for (ValueInput blockInput : input.childrenListOrEmpty("scannedBlocks")) {
            int x = blockInput.getIntOr("x", 0);
            int y = blockInput.getIntOr("y", 0);
            int z = blockInput.getIntOr("z", 0);
            blockInput.read("state", BlockState.CODEC)
                .ifPresent(state -> this.scannedBlocks.add(new CachedBlockData(x, y, z, state)));
        }
        this.pendingAutoSave = input.getBooleanOr("pendingAutoSave", false);
        this.autoSaveStructureName = input.getStringOr("autoSaveStructureName", "");
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        super.handleUpdateTag(input);
    }
}
