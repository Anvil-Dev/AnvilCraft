package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.network.ChargerSyncPacket;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.IStateListener;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DischargerBlockEntity extends BlockEntity
    implements IPowerProducer, IFilterBlockEntity, IStateListener<Boolean>, IItemHandlerHolder, IHasDisplayItem {

    /** 放电器每tick从物品抽取的FE量（与FE收集器一致） */
    static final int FE_EXTRACT_PER_TICK = 10_000;

    @Getter
    @Setter
    private int timeLeft = 0;
    @Getter
    @Setter
    private int timeTotalCache = 0;
    private int powerValue = 0;
    @Getter
    @Setter
    private boolean isFeDischarging = false;
    private int signalCache = 0;

    @Getter
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(3) {

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && itemHandler.getStackInSlot(0).isEmpty()) {
                ItemStack original = stack.copy();
                original.shrink(1);
                if (original.isEmpty()) {
                    return super.insertItem(slot, stack.copyWithCount(1), simulate);
                } else {
                    ItemStack left = super.insertItem(slot, stack.copyWithCount(1), simulate);
                    return stack.copyWithCount(stack.getCount() - 1 + left.getCount());
                }
            } else {
                return stack;
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return containsValidItem(stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 2 ? super.extractItem(2, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (level != null && !level.isClientSide) {
                setChanged();
                updateDisplayItemStack();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    @Getter
    private ItemStack displayItemStack = ItemStack.EMPTY;

    @Getter
    @Setter
    private PowerGrid grid;

    public DischargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean containsValidItem(ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        if (level != null) {
            Optional<RecipeHolder<ChargerChargingRecipe>> x = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get(), input, level);
            if (x.isPresent()) {
                if (x.get().value().power == 0) return false;
                return x.get().value().power > 0; // 放电器使用power > 0的配方
            }
        }
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage == null) return false;
        return energyStorage.canExtract() && energyStorage.getEnergyStored() > 0;
    }

    @Nullable
    private ChargerChargingRecipe getItemRecipe(ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        if (level != null) {
            Optional<RecipeHolder<ChargerChargingRecipe>> x = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get(), input, level);
            if (x.isPresent()) {
                return x.get().value();
            }
        }
        return null;
    }

    private boolean checkRecipeItemNotValid(@Nullable ChargerChargingRecipe recipe, @SuppressWarnings("unused") ItemStack stack) {
        if (recipe != null) {
            if (recipe.power == 0) return true;
            return recipe.power <= 0; // 放电器只接受power > 0的配方
        }
        return true;
    }

    private void moveItemToTransformingSlot() {
        ItemStack stack = itemHandler.getStackInSlot(0).copy();
        if (stack.isEmpty()) return;
        if (!itemHandler.getStackInSlot(1).isEmpty()) return;

        ChargerChargingRecipe recipe = getItemRecipe(stack);
        if (!checkRecipeItemNotValid(recipe, stack)) {
            isFeDischarging = false;
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            itemHandler.setStackInSlot(1, recipe.getResult().copy());
            timeLeft = recipe.time + 1;
            timeTotalCache = recipe.time;
            powerValue = recipe.power;
            syncPacket();
            return;
        }

        // FE放电：物品有可抽取的FE时开始放电
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null && energyStorage.canExtract()) {
            int currentEnergy = energyStorage.getEnergyStored();
            if (currentEnergy > 0) {
                isFeDischarging = true;
                itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                itemHandler.setStackInSlot(1, stack);
                timeLeft = currentEnergy;
                timeTotalCache = energyStorage.getMaxEnergyStored();
                powerValue = 64;
                syncPacket();
            }
        }
    }

    private void syncPacket() {
        if (this.getCurrentLevel() == null || !(this.getCurrentLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel, serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeDischarging));
    }

    private void moveItemToTransformedOverSlot() {
        ItemStack stack = itemHandler.getStackInSlot(1).copy();
        if (stack.isEmpty()) return;
        if (!itemHandler.getStackInSlot(2).isEmpty()) {
            powerValue = 0;
            return;
        }
        itemHandler.setStackInSlot(2, stack);
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        powerValue = 0;
        isFeDischarging = false;
    }

    private void updateDisplayItemStack() {
        ItemStack newDisplayStack = getDisplayItemStackForRender();
        displayItemStack = newDisplayStack.copy();
        PacketDistributor.sendToPlayersTrackingChunk(
            (ServerLevel) level,
            level.getChunk(getBlockPos()).getPos(),
            new UpdateDisplayItemPacket(displayItemStack, getPos())
        );
    }

    private ItemStack getDisplayItemStackForRender() {
        for (int i = 2; i >= 0; i--) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return itemHandler.getStackInSlot(i);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void updateDisplayItem(ItemStack stack) {
        this.displayItemStack = stack;
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("TimeLeft", timeLeft);
        tag.putInt("TimeTotalCache", timeTotalCache);
        tag.put("Depository", itemHandler.serializeNBT(provider));
        tag.putBoolean("FeDischarging", isFeDischarging);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        timeLeft = tag.getInt("TimeLeft");
        timeTotalCache = tag.getInt("TimeTotalCache");
        itemHandler.deserializeNBT(provider, tag.getCompound("Depository"));
        isFeDischarging = tag.getBoolean("FeDischarging");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        if (level != null && level.isClientSide) {
            this.displayItemStack = getDisplayItemStackForRender().copy();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection,
                             ClientboundBlockEntityDataPacket packet,
                             HolderLookup.Provider provider) {
        super.onDataPacket(connection, packet, provider);
        if (level != null && level.isClientSide) {
            this.displayItemStack = getDisplayItemStackForRender().copy();
        }
    }

    private int getFeDischargingPowerLevel() {
        if (grid == null) return 0;
        int consume = grid.getConsume();
        int count = 0;
        for (IPowerComponent component : grid.getComponents()) {
            if (component instanceof DischargerBlockEntity other && other.isFeDischarging) {
                count++;
            }
        }
        int perDevice = Math.max(1, consume / Math.max(1, count));
        if (perDevice >= 512) return 512;
        if (perDevice >= 256) return 256;
        if (perDevice >= 128) return 128;
        if (perDevice >= 64) return 64;
        return 0;
    }

    @Nullable
    public ItemStack tryExtractItemFromSlot1() {
        ItemStack stack = itemHandler.getStackInSlot(1);
        if (stack.isEmpty()) return null;
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        isFeDischarging = false;
        timeLeft = 0;
        powerValue = 0;
        setChanged();
        return stack;
    }

    @Override
    public int getOutputPower() {
        return !this.getBlockState().getValue(ChargerBlock.POWERED) ? powerValue : 0;
    }

    public double getProgress() {
        if (this.timeTotalCache == 0) return 0;
        // 放电器：进度从满衰减到空 (remaining / total)
        return Math.max(0, Math.min(1, (double) timeLeft / timeTotalCache));
    }

    public int getAnalogRedstoneSignal() {
        double progress = this.getProgress();
        if (itemHandler.getStackInSlot(0).isEmpty() && itemHandler.getStackInSlot(1).isEmpty()) return 0;
        return (int) Math.round(progress * 15);
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return itemHandler;
    }

    @Override
    public boolean isFilterEnabled() {
        return true;
    }

    @Override
    public boolean isSlotDisabled(int slot) {
        return timeLeft > 0;
    }

    @Override
    public Boolean getState() {
        return Boolean.FALSE;
    }

    @Override
    public void notifyStateChanged(Boolean newState) {
        ItemStack stack0 = itemHandler.getStackInSlot(0).copy();
        ItemStack stack1 = itemHandler.getStackInSlot(1).copy();
        ItemStack stack2 = itemHandler.getStackInSlot(2).copy();
        dropItemStack(stack0);
        dropItemStack(stack1);
        dropItemStack(stack2);
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        itemHandler.setStackInSlot(2, ItemStack.EMPTY);
        timeLeft = 0;
        isFeDischarging = false;
        powerValue = 0;
    }

    private void dropItemStack(ItemStack stack1) {
        if (!stack1.isEmpty()) {
            if (level != null) {
                Vec3 dropPos = getBlockPos().above().getBottomCenter();
                ItemEntity itemEntity = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z,
                    stack1, 0, 0, 0);
                itemEntity.setDefaultPickUpDelay();
            }
        }
    }

    /**
     * 放电器逻辑
     */
    public void tick(Level level1, BlockPos blockPos) {
        this.flushState(level1, blockPos);
        BlockState state = level1.getBlockState(blockPos);
        boolean powered = state.getValue(ChargerBlock.POWERED);
        if (grid == null) return;
        if (powered) return;
        if (timeLeft == 0) {
            moveItemToTransformingSlot();
        }
        if (timeLeft > 0) {
            if (isFeDischarging) {
                powerValue = getFeDischargingPowerLevel();
            }
            if (isFeDischarging) {
                ItemStack processingStack = itemHandler.getStackInSlot(1);
                if (!processingStack.isEmpty()) {
                    IEnergyStorage storage = processingStack.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (storage != null) {
                        int currentEnergy = storage.getEnergyStored();
                        if (currentEnergy <= 0) {
                            isFeDischarging = false;
                            timeLeft = 0;
                            timeTotalCache = 0;
                        } else {
                            int extracted = storage.extractEnergy(
                                Math.min(FE_EXTRACT_PER_TICK, currentEnergy), false);
                            powerValue = (int) (extracted
                                * (1 - AnvilCraft.CONFIG.powerConverter.powerConverterLoss)
                                / AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency);
                            timeLeft = currentEnergy - extracted;
                            timeTotalCache = storage.getMaxEnergyStored();
                        }
                    }
                }
            } else {
                timeLeft--;
            }
        }
        if (timeLeft == 0) {
            moveItemToTransformedOverSlot();
            if (timeLeft == 0) {
                this.timeTotalCache = 0;
            }
        }

        int signal = this.getAnalogRedstoneSignal();
        if (this.signalCache != signal) {
            this.signalCache = signal;
            level1.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        }

        if (!(level1 instanceof ServerLevel level2)) return;
        if (level2.getGameTime() % 10 != 0) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            level2, level2.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeDischarging));
    }
}
