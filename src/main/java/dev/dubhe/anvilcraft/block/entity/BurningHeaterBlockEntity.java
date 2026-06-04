package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

public class BurningHeaterBlockEntity extends BlockEntity implements IItemHandlerHolder {
    /**
     * 最大燃烧时间：1200秒 = 24000tick
     */
    public static final int MAX_BURN_TIME = 1200 * 20;

    private int burnTime = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return FurnaceBlockEntity.isFuel(stack) || stack.is(Items.BUCKET);
        }
    };

    public BurningHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    /**
     * 服务端tick：倒计时燃烧时间并自动消耗燃料，更新方块状态
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (this.burnTime > 0) {
            this.burnTime--;
            if (this.burnTime % 20 == 0) {
                setChanged();
            }
        }
        if (this.burnTime <= 0) {
            tryConsumeFuel();
        }
        updateBurningState(level, pos, state);
    }

    /**
     * 根据燃烧时间更新方块状态
     */
    private void updateBurningState(Level level, BlockPos pos, BlockState state) {
        int targetLevel;
        if (this.burnTime >= 300 * 20) {
            targetLevel = 2; // 点燃
        } else if (this.burnTime > 0) {
            targetLevel = 1; // 阴燃
        } else {
            targetLevel = 0; // 熄灭
        }
        if (state.getValue(BurningHeaterBlock.LEVEL) != targetLevel) {
            level.setBlock(pos, state.setValue(BurningHeaterBlock.LEVEL, targetLevel), 3);
        }
    }

    /**
     * 尝试消耗燃料槽中的燃料来补充燃烧时间
     */
    private void tryConsumeFuel() {
        ItemStack fuel = this.itemHandler.getStackInSlot(0);
        if (fuel.isEmpty()) return;
        if (this.burnTime >= MAX_BURN_TIME) return;

        int burnTimePerItem = getItemBurnTime(fuel);
        if (burnTimePerItem <= 0) return;

        int remaining = MAX_BURN_TIME - this.burnTime;
        int maxConsumable = Math.max(1, remaining / burnTimePerItem);
        int itemsToConsume = Math.min(fuel.getCount(), maxConsumable);

        this.burnTime += itemsToConsume * burnTimePerItem;
        if (this.burnTime > MAX_BURN_TIME) {
            this.burnTime = MAX_BURN_TIME;
        }

        // 熔岩桶特殊处理：消耗后留下空桶
        if (fuel.is(Items.LAVA_BUCKET)) {
            int newCount = fuel.getCount() - itemsToConsume;
            if (newCount <= 0) {
                this.itemHandler.setStackInSlot(0, new ItemStack(Items.BUCKET));
            } else {
                fuel.shrink(itemsToConsume);
                // 尝试将空桶放入槽中（与剩余熔岩桶共存时无法堆叠，空桶直接丢弃）
            }
        } else {
            fuel.shrink(itemsToConsume);
        }
        setChanged();
    }

    /**
     * 获取单个物品的燃烧时间（tick）
     */
    private static int getItemBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        FurnaceFuel fuel = stack.getItem().builtInRegistryHolder()
            .getData(NeoForgeDataMaps.FURNACE_FUELS);
        return fuel != null ? fuel.burnTime() : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("BurnTime", this.burnTime);
        tag.put("Inventory", this.itemHandler.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.burnTime = tag.getInt("BurnTime");
        this.itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }
}
