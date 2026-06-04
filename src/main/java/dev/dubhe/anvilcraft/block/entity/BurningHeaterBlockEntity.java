package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.jetbrains.annotations.Nullable;

public class BurningHeaterBlockEntity extends BlockEntity implements IItemHandlerHolder {
    /**
     * 最大燃烧时间：1200秒 = 24000tick
     */
    public static final int MAX_BURN_TIME = 1200 * 20;

    @Getter
    private int burnTime = 0;

    /**
     * 客户端上次同步到 burnTime 时的游戏时间（用于本地倒计时估算）
     */
    private long lastSyncGameTime = 0;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getItemBurnTime(stack) > 0 || stack.is(Items.BUCKET);
        }
    };

    public BurningHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        super.onDataPacket(net, pkt, provider);
        if (level != null) {
            this.lastSyncGameTime = level.getGameTime();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            this.lastSyncGameTime = level.getGameTime();
        }
    }

    /**
     * 获取用于显示的燃烧时间。
     * 在客户端上，根据上次同步时间进行本地倒计时估算，实现平滑刷新。
     */
    public int getDisplayBurnTime() {
        if (level == null || !level.isClientSide()) return burnTime;
        if (lastSyncGameTime <= 0) return burnTime;
        int elapsed = (int) (level.getGameTime() - this.lastSyncGameTime);
        return Math.max(0, this.burnTime - elapsed);
    }

    /**
     * 服务端tick：倒计时燃烧时间并自动补充燃料，更新方块状态
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (this.burnTime > 0) {
            this.burnTime--;
            if (this.burnTime % 20 == 0) {
                setChanged();
            }
        }
        tryConsumeFuel();
        updateBurningState(level, pos, state);
        HeaterManager.addProducer(pos, level, ModHeaterInfos.BURNING_HEATER);
    }

    /**
     * 消耗燃烧时间（用于铁砧合成）
     *
     * @param ticks 消耗的tick数
     */
    public void consumeBurnTime(int ticks) {
        this.burnTime = Math.max(0, this.burnTime - ticks);
        setChanged();
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
     * 尝试消耗燃料槽中的燃料来补充燃烧时间。
     * 仅当剩余空间足以完整消耗一个燃料时才消耗，不浪费燃料的燃烧时间。
     */
    private void tryConsumeFuel() {
        if (this.burnTime >= MAX_BURN_TIME) return;

        ItemStack fuel = this.itemHandler.getStackInSlot(0);
        int burnTimePerItem = getItemBurnTime(fuel);
        if (burnTimePerItem <= 0) return;

        int itemsToConsume = Math.min(fuel.getCount(), (MAX_BURN_TIME - this.burnTime) / burnTimePerItem);
        if (itemsToConsume <= 0) return;

        this.burnTime += itemsToConsume * burnTimePerItem;

        this.itemHandler.extractItem(0, itemsToConsume, false);
        // 熔岩桶等容器物品：消耗后槽位空时留下空桶
        if (fuel.hasCraftingRemainingItem() && this.itemHandler.getStackInSlot(0).isEmpty()) {
            this.itemHandler.setStackInSlot(0, fuel.getCraftingRemainingItem());
        }
    }

    /**
     * 获取单个物品的燃烧时间（tick）
     */
    public static int getItemBurnTime(ItemStack stack) {
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
