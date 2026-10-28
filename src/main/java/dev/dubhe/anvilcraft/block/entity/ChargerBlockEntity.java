package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
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

public class ChargerBlockEntity extends BlockEntity
    implements IPowerConsumer, IFilterBlockEntity, IStateListener<Boolean>, IItemHandlerHolder, IHasDisplayItem {

    @Getter
    @Setter
    private int timeLeft = 0;
    @Getter
    @Setter
    private int startupCoolDown = 0;
    @Getter
    @Setter
    private int timeTotalCache = 0;
    private int powerValue = 0;
    @Getter
    @Setter
    private boolean isFeCharging = false;
    private int feCooldown = 0;
    private int signalCache = 0;

    @Getter
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(3) {

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 0 && ChargerBlockEntity.this.itemHandler.getStackInSlot(0).isEmpty()) {
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
            return ChargerBlockEntity.this.containsValidItem(stack);
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
                ChargerBlockEntity.this.updateDisplayItemStack();
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
    }

    public boolean containsValidItem(ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        if (level != null) {
            Optional<RecipeHolder<ChargerChargingRecipe>> x = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get(), input, level);
            if (x.isPresent()) {
                if (x.get().value().power == 0) return false;
                return x.get().value().power < 0; // 充电器使用power < 0的配方
            }
        }
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage == null) return false;
        return energyStorage.canReceive();
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

    private boolean checkRecipeItemNotValid(
        @Nullable ChargerChargingRecipe recipe,
        @SuppressWarnings("unused") ItemStack stack
    ) {
        if (recipe != null) {
            if (recipe.power == 0) return true;
            return recipe.power >= 0; // 充电器只接受power < 0的配方
        }
        return true;
    }

    private void moveItemToTransformingSlot() {
        ItemStack stack = this.itemHandler.getStackInSlot(0).copy();
        if (stack.isEmpty()) return;
        if (!this.itemHandler.getStackInSlot(1).isEmpty()) return;

        ChargerChargingRecipe recipe = this.getItemRecipe(stack);
        if (!this.checkRecipeItemNotValid(recipe, stack)) {
            this.isFeCharging = false;
            this.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            this.itemHandler.setStackInSlot(1, stack);
            this.timeLeft = recipe.time + 1;
            this.timeTotalCache = recipe.time;
            this.powerValue = recipe.power;
            this.syncPacket();
            return;
        }

        // 充电器：物品可接收FE时开始充电
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null && energyStorage.canReceive()) {
            int maxEnergy = energyStorage.getMaxEnergyStored();
            int currentEnergy = energyStorage.getEnergyStored();
            int remainingFE = maxEnergy - currentEnergy;
            if (remainingFE > 0) {
                this.isFeCharging = true;
                this.feCooldown = 0;
                this.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                this.itemHandler.setStackInSlot(1, stack);
                this.timeLeft = remainingFE;
                this.timeTotalCache = maxEnergy;
                this.powerValue = 0;
                this.setStartupCoolDown(20);
                this.syncPacket();
            }
        }
    }

    private void syncPacket() {
        if (this.getCurrentLevel() == null || !(this.getCurrentLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel, serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeCharging)
        );
    }

    private void moveItemToTransformedOverSlot() {
        ItemStack stack = this.itemHandler.getStackInSlot(1).copy();
        if (stack.isEmpty()) return;
        if (!this.itemHandler.getStackInSlot(2).isEmpty()) {
            this.powerValue = 0;
            return;
        }
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage != null) {
            this.isFeCharging = false;
            this.itemHandler.setStackInSlot(2, stack);
        } else {
            ChargerChargingRecipe recipe = this.getItemRecipe(stack);
            if (this.checkRecipeItemNotValid(recipe, stack)) return;
            if (level == null) return;
            this.itemHandler.setStackInSlot(2, recipe.assemble(new SingleRecipeInput(stack), level.registryAccess()));
        }
        this.itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        this.powerValue = 0;
        this.isFeCharging = false;
    }

    private void updateDisplayItemStack() {
        ItemStack newDisplayStack = this.getDisplayItemStackForRender();
        this.displayItemStack = newDisplayStack.copy();
        PacketDistributor.sendToPlayersTrackingChunk(
            (ServerLevel) level,
            level.getChunk(getBlockPos()).getPos(),
            new UpdateDisplayItemPacket(this.displayItemStack, this.getPos())
        );
    }

    private ItemStack getDisplayItemStackForRender() {
        for (int i = 2; i >= 0; i--) {
            if (!this.itemHandler.getStackInSlot(i).isEmpty()) {
                return this.itemHandler.getStackInSlot(i);
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
        tag.putInt("TimeLeft", this.timeLeft);
        tag.putInt("TimeTotalCache", this.timeTotalCache);
        tag.putInt("PowerValue", this.powerValue);
        tag.putInt("StartupCoolDown", this.startupCoolDown);
        tag.put("Depository", this.itemHandler.serializeNBT(provider));
        tag.putBoolean("FeCharging", this.isFeCharging);
        tag.putInt("FeCooldown", this.feCooldown);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.timeLeft = tag.getInt("TimeLeft");
        this.timeTotalCache = tag.getInt("TimeTotalCache");
        this.powerValue = tag.getInt("PowerValue");
        this.startupCoolDown = tag.getInt("StartupCoolDown");
        this.itemHandler.deserializeNBT(provider, tag.getCompound("Depository"));
        this.isFeCharging = tag.getBoolean("FeCharging");
        this.feCooldown = tag.getInt("FeCooldown");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        // 客户端加载后重新计算显示物品
        if (level != null && level.isClientSide) {
            this.displayItemStack = this.getDisplayItemStackForRender().copy();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
        Connection connection,
        ClientboundBlockEntityDataPacket packet,
        HolderLookup.Provider provider
    ) {
        super.onDataPacket(connection, packet, provider);
        // 收到方块更新包后重新计算显示物品
        if (level != null && level.isClientSide) {
            this.displayItemStack = this.getDisplayItemStackForRender().copy();
        }
    }

    @Override
    public int getInputPower() {
        if (this.getBlockState().getValue(ChargerBlock.POWERED)) return 0;
        return -this.powerValue;
    }

    /** 按电网统一分配的余量更新装备充电负载，避免重复占用其他设备的功率。 */
    public int refreshGridDemand(long available) {
        if (!this.isFeCharging) {
            return this.getInputPower();
        }
        int power = 0;
        if (this.timeLeft > 0 && !this.getBlockState().getValue(ChargerBlock.POWERED)) {
            if (available >= 512) {
                power = 512;
            } else if (available >= 256) {
                power = 256;
            } else if (available >= 128) {
                power = 128;
            } else if (available >= 64) {
                power = 64;
            }
        }
        this.powerValue = -power;
        return power;
    }

    @Nullable
    public ItemStack tryExtractItemFromSlot1() {
        ItemStack stack = this.itemHandler.getStackInSlot(1);
        if (stack.isEmpty()) return null;
        this.itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        this.isFeCharging = false;
        this.feCooldown = 0;
        this.timeLeft = 0;
        this.powerValue = 0;
        setChanged();
        return stack;
    }

    public int getOutputPower() {
        return 0;
    }

    public double getProgress() {
        if (this.timeTotalCache == 0) return 0;
        return Math.max(0, Math.min(1, 1 - (double) this.timeLeft / this.timeTotalCache));
    }

    public int getAnalogRedstoneSignal() {
        double progress = this.getProgress();
        if (this.itemHandler.getStackInSlot(0).isEmpty() && this.itemHandler.getStackInSlot(1).isEmpty()) return 0;
        return (int) Math.round(progress * 15);
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return this.itemHandler;
    }

    @Override
    public boolean isFilterEnabled() {
        return true;
    }

    @Override
    public boolean isSlotDisabled(int slot) {
        return this.timeLeft > 0;
    }

    @Override
    public Boolean getState() {
        return Boolean.TRUE;
    }

    @Override
    public void notifyStateChanged(Boolean newState) {
        ItemStack stack0 = this.itemHandler.getStackInSlot(0).copy();
        ItemStack stack1 = this.itemHandler.getStackInSlot(1).copy();
        ItemStack stack2 = this.itemHandler.getStackInSlot(2).copy();
        this.dropItemStack(stack0);
        this.dropItemStack(stack1);
        this.dropItemStack(stack2);
        this.itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        this.itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        this.itemHandler.setStackInSlot(2, ItemStack.EMPTY);
        this.timeLeft = 0;
        this.isFeCharging = false;
        this.feCooldown = 0;
        this.powerValue = 0;
    }

    private void dropItemStack(ItemStack stack1) {
        if (!stack1.isEmpty()) {
            if (level != null) {
                Vec3 dropPos = getBlockPos().above().getBottomCenter();
                ItemEntity itemEntity = new ItemEntity(
                    level, dropPos.x, dropPos.y, dropPos.z,
                    stack1, 0, 0, 0
                );
                itemEntity.setDefaultPickUpDelay();
            }
        }
    }

    /**
     * 充电器逻辑
     */
    public void tick(Level level1, BlockPos blockPos) {
        this.flushState(level1, blockPos);
        BlockState state = level1.getBlockState(blockPos);
        boolean powered = state.getValue(ChargerBlock.POWERED);
        if (this.grid == null) return;
        if (powered) {
            this.setStartupCoolDown(20);
        }
        int powerValueCache = this.powerValue;
        if (this.timeLeft == 0) {
            this.moveItemToTransformingSlot();
        }
        if (this.powerValue < powerValueCache) {
            this.setStartupCoolDown(20);
        }
        if (this.startupCoolDown > 0) {
            this.startupCoolDown--;
            return;
        }
        if (this.timeLeft > 0 && isGridWorking()) {
            if (this.isFeCharging) {
                ItemStack processingStack = this.itemHandler.getStackInSlot(1);
                if (!processingStack.isEmpty()) {
                    IEnergyStorage storage = processingStack.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (storage != null) {
                        int powerLevel = this.getInputPower();
                        if (powerLevel > 0) {
                            int countdown = AnvilCraft.CONFIG.machines.powerConverter.workInterval;
                            int efficiency = AnvilCraft.CONFIG.machines.powerConverter.efficiency;
                            int remainingFE = storage.getMaxEnergyStored() - storage.getEnergyStored();
                            if (remainingFE <= 0) {
                                this.isFeCharging = false;
                                this.timeLeft = 0;
                                this.timeTotalCache = 0;
                            } else {
                                int feChargeRate = powerLevel * efficiency;
                                if (this.feCooldown <= 0) {
                                    this.feCooldown = countdown;
                                    storage.receiveEnergy(feChargeRate * countdown, false);
                                } else {
                                    this.feCooldown--;
                                }
                                this.timeLeft = remainingFE;
                                this.timeTotalCache = storage.getMaxEnergyStored();
                            }
                        }
                    }
                }
            } else {
                this.timeLeft--;
            }
        }
        if (this.timeLeft == 0) {
            this.moveItemToTransformedOverSlot();
            this.timeTotalCache = 0;
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
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeCharging)
        );
    }
}
