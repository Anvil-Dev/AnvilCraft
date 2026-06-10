package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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

public class ChargerBlockEntity extends BlockEntity
    implements IPowerConsumer, IPowerProducer, IFilterBlockEntity, IStateListener<Boolean>, IItemHandlerHolder, IHasDisplayItem {

    @Setter
    private boolean isCharger;
    @Setter
    private int timeLeft = 0;
    @Setter
    private int timeTotalCache = 0;
    private int powerValue = 0;
    private boolean isFeCharging = false;
    private int feCooldown = 0;
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

    public ChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        isCharger = blockState.is(ModBlocks.CHARGER.get());
    }

    public boolean containsValidItem(ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        if (level != null) {
            Optional<RecipeHolder<ChargerChargingRecipe>> x = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get(), input, level);
            if (x.isPresent()) {
                if (x.get().value().power == 0) return false;
                return isCharger == x.get().value().power < 0;
            }
        }
        if (isCharger) {
            IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            return energyStorage != null && energyStorage.canReceive();
        }
        return false;
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
            return isCharger != recipe.power < 0;
        }
        return true;
    }

    private void moveItemToTransformingSlot() {
        ItemStack stack = itemHandler.getStackInSlot(0).copy();
        if (stack.isEmpty()) return;
        if (!itemHandler.getStackInSlot(1).isEmpty()) return;

        ChargerChargingRecipe recipe = getItemRecipe(stack);
        if (!checkRecipeItemNotValid(recipe, stack)) {
            isFeCharging = false;
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            if (isCharger) {
                itemHandler.setStackInSlot(1, stack);
            } else {
                itemHandler.setStackInSlot(1, recipe.getResult().copy());
            }
            timeLeft = recipe.time + 1;
            timeTotalCache = recipe.time;
            powerValue = recipe.power;
            syncPacket();
            return;
        }

        if (isCharger) {
            IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyStorage != null && energyStorage.canReceive()) {
                int maxEnergy = energyStorage.getMaxEnergyStored();
                int currentEnergy = energyStorage.getEnergyStored();
                int remainingFE = maxEnergy - currentEnergy;
                if (remainingFE > 0) {
                    isFeCharging = true;
                    feCooldown = 0;
                    int feChargeRate = 16 * AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency;
                    int ticks = Math.max(1, (remainingFE + feChargeRate - 1) / feChargeRate);
                    itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                    itemHandler.setStackInSlot(1, stack);
                    timeLeft = ticks + 1;
                    timeTotalCache = ticks;
                    powerValue = -16;
                    syncPacket();
                }
            }
        }
    }

    private void syncPacket() {
        if (this.getCurrentLevel() == null || !(this.getCurrentLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel, serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache));
    }

    private void moveItemToTransformedOverSlot() {
        ItemStack stack = itemHandler.getStackInSlot(1).copy();
        if (stack.isEmpty()) return;
        if (!itemHandler.getStackInSlot(2).isEmpty()) {
            if (isFeCharging) {
                powerValue = -16;
            } else {
                powerValue = 0;
            }
            return;
        }
        if (isCharger) {
            IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (energyStorage != null) {
                isFeCharging = false;
                itemHandler.setStackInSlot(2, stack);
            } else {
                ChargerChargingRecipe recipe = getItemRecipe(stack);
                if (checkRecipeItemNotValid(recipe, stack)) return;
                itemHandler.setStackInSlot(2, recipe.getResult().copy());
            }
        } else {
            itemHandler.setStackInSlot(2, stack);
        }
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        powerValue = 0;
        isFeCharging = false;
    }

    private void updateDisplayItemStack() {
        ItemStack newDisplayStack = getDisplayItemStackForRender();
        if (!ItemStack.matches(displayItemStack, newDisplayStack)) {
            displayItemStack = newDisplayStack.copy();
            PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) level, 
                level.getChunk(getBlockPos()).getPos(), 
                new UpdateDisplayItemPacket(displayItemStack, getPos())
            );
        }
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
        tag.put("Depository", itemHandler.serializeNBT(provider));
        tag.putBoolean("Mode", isCharger);
        tag.putBoolean("FeCharging", isFeCharging);
        tag.putInt("FeCooldown", feCooldown);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        timeLeft = tag.getInt("TimeLeft");
        itemHandler.deserializeNBT(provider, tag.getCompound("Depository"));
        isCharger = tag.getBoolean("Mode");
        isFeCharging = tag.getBoolean("FeCharging");
        feCooldown = tag.getInt("FeCooldown");
    }

    @Override
    public int getInputPower() {
        if (!isCharger || this.getBlockState().getValue(ChargerBlock.POWERED)) return 0;
        if (isFeCharging) return 16;
        return -powerValue;
    }

    @Nullable
    public ItemStack tryExtractItemFromSlot1() {
        ItemStack stack = itemHandler.getStackInSlot(1);
        if (stack.isEmpty()) return null;
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        isFeCharging = false;
        feCooldown = 0;
        timeLeft = 0;
        powerValue = 0;
        setChanged();
        return stack;
    }

    @Override
    public PowerComponentType getComponentType() {
        return isCharger ? PowerComponentType.CONSUMER : PowerComponentType.PRODUCER;
    }

    @Override
    public int getOutputPower() {
        return !isCharger && !this.getBlockState().getValue(ChargerBlock.POWERED) ? powerValue : 0;
    }

    public double getProgress() {
        if (this.timeTotalCache != 0) return 1 - (double) timeLeft / timeTotalCache;
        return 0;
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
        return isCharger;
    }

    @Override
    public void notifyStateChanged(Boolean newState) {
        isCharger = newState;
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
        isFeCharging = false;
        feCooldown = 0;
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
     * 充放电器逻辑
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
            if (isFeCharging) {
                powerValue = -16;
            }
            if (!isCharger || isGridWorking()) {
                if (isFeCharging) {
                    // FE物品：动态计算剩余时间
                    ItemStack processingStack = itemHandler.getStackInSlot(1);
                    if (!processingStack.isEmpty()) {
                        IEnergyStorage storage = processingStack.getCapability(Capabilities.EnergyStorage.ITEM);
                        if (storage != null) {
                            int remainingFE = storage.getMaxEnergyStored() - storage.getEnergyStored();
                            if (remainingFE <= 0) {
                                isFeCharging = false;
                                timeLeft = 0;
                            } else {
                                int feChargeRate = 16 * AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency;
                                int ticks = Math.max(1, (remainingFE + feChargeRate - 1) / feChargeRate);
                                timeLeft = ticks + 1;
                                int countdown = AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
                                if (feCooldown <= 0) {
                                    feCooldown = countdown;
                                    storage.receiveEnergy(feChargeRate * countdown, false);
                                } else {
                                    feCooldown--;
                                }
                            }
                        }
                    }
                } else {
                    timeLeft--;
                }
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
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache));
    }
}
