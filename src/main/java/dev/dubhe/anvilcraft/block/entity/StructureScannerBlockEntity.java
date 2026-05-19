package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

public class StructureScannerBlockEntity extends BaseMachineBlockEntity implements MenuProvider {
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(0);

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
    
    // Disk物品栏的ItemHandler包装器,带物品验证
    private final IItemHandler diskItemHandler = new InvWrapper(diskInventory) {
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

    public StructureScannerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        // 设置默认值为 5 (index = 4)
        this.rangeX.fromIndex(4);
        this.rangeY.fromIndex(4);
        this.rangeZ.fromIndex(4);
    }

    @Override
    public Direction getDirection() {
        return Direction.NORTH;
    }

    @Override
    public void setDirection(Direction direction) {
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
        return diskItemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // 保存Disk物品栏
        tag.put("diskInventory", this.diskInventory.createTag(provider));
        // 保存扫描范围
        tag.putInt("rangeX", this.rangeX.index());
        tag.putInt("rangeY", this.rangeY.index());
        tag.putInt("rangeZ", this.rangeZ.index());
    }
    
    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // 加载Disk物品栏
        this.diskInventory.fromTag(tag.getList("diskInventory", Tag.TAG_COMPOUND), provider);
        // 加载扫描范围
        this.rangeX.fromIndex(tag.getInt("rangeX"));
        this.rangeY.fromIndex(tag.getInt("rangeY"));
        this.rangeZ.fromIndex(tag.getInt("rangeZ"));
    }
}
