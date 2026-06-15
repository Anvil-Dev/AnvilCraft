package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.power.generator.ChargerBlock;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.network.ChargerSyncPacket;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.IStateListener;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ChargerBlockEntity extends BlockEntity
    implements IPowerConsumer, IFilterBlockEntity, IStateListener<Boolean>, IItemResourceHandlerHolder, IHasDisplayItem {

    @Getter
    @Setter
    private int timeLeft = 0;
    @Getter
    @Setter
    private int timeTotalCache = 0;
    private int powerValue = 0;
    @Getter
    @Setter
    private boolean isFeCharging = false;
    private int feCooldown = 0;
    private int signalCache = 0;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(3) {

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            if (!this.getResource(0).isEmpty()) return 0;
            return super.insert(0, resource, 1, transaction);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != 0) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return ChargerBlockEntity.this.containsValidItem(resource);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return super.extract(resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (ChargerBlockEntity.this.isSlotDisabled(index)) return 0;
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            Level level = ChargerBlockEntity.this.getLevel();
            if (level == null || level.isClientSide()) return;
            ChargerBlockEntity.this.setChanged();
            ChargerBlockEntity.this.updateDisplayItemStack();
            ChargerBlockEntity.this.level.sendBlockUpdated(
                ChargerBlockEntity.this.getBlockPos(),
                ChargerBlockEntity.this.getBlockState(),
                ChargerBlockEntity.this.getBlockState(),
                Block.UPDATE_ALL
            );
        }
    };

    @Getter
    private ItemStack displayItemStack = ItemStack.EMPTY;

    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    public ChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean containsValidItem(ItemResource resource) {
        SingleRecipeInput input = new SingleRecipeInput(resource.toStack());
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) return false;
        Optional<RecipeHolder<ChargerChargingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(
            ModRecipeTypes.CHARGER_CHARGING.get(),
            input,
            serverLevel
        );
        if (recipe.isPresent()) {
            if (recipe.get().value().power() == 0) return false;
            return recipe.get().value().power() < 0; // 充电器使用power < 0的配方
        }
        // 检查FE充电能力
        ItemStack stack = resource.toStack();
        if (stack.isEmpty()) return false;
        EnergyHandler energyHandler = Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack));
        if (energyHandler == null) return false;
        return energyHandler.getAmountAsInt() < energyHandler.getCapacityAsInt();
    }

    @Nullable
    private ChargerChargingRecipe getItemRecipe(ItemResource resource) {
        SingleRecipeInput input = new SingleRecipeInput(resource.toStack());
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) return null;
        Optional<RecipeHolder<ChargerChargingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(
            ModRecipeTypes.CHARGER_CHARGING.get(),
            input,
            serverLevel
        );
        return recipe.map(RecipeHolder::value).orElse(null);
    }

    private boolean checkRecipeItemNotValid(@Nullable ChargerChargingRecipe recipe) {
        if (recipe == null) return true;
        if (recipe.power() == 0) return true;
        return recipe.power() >= 0; // 充电器只接受power < 0的配方
    }

    private void moveItemToTransformingSlot() {
        ItemResource resource = this.itemHandler.getResource(0);
        if (resource.isEmpty()) return;
        if (!this.itemHandler.getResource(1).isEmpty()) return;

        ChargerChargingRecipe recipe = this.getItemRecipe(resource);
        if (!this.checkRecipeItemNotValid(recipe)) {
            this.isFeCharging = false;
            this.itemHandler.set(0, ItemResource.EMPTY, 0);
            this.itemHandler.set(1, resource, 1);
            this.timeLeft = recipe.time() + 1; // since there is a "timeLeft--" after this, here +1 to negate
            this.timeTotalCache = recipe.time();
            this.powerValue = recipe.power();
            this.syncPacket();
            return;
        }

        // FE充电：物品可接收FE时开始充电
        ItemStack stack = this.itemHandler.getStacks().get(0).copy();
        EnergyHandler energyHandler = Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack));
        if (energyHandler != null
            && energyHandler.getAmountAsInt() < energyHandler.getCapacityAsInt()
        ) {
            this.isFeCharging = true;
            this.feCooldown = 0;
            this.itemHandler.set(0, ItemResource.EMPTY, 0);
            this.itemHandler.set(1, resource, 1);
            int remainingFE = energyHandler.getCapacityAsInt() - energyHandler.getAmountAsInt();
            this.timeLeft = remainingFE;
            this.timeTotalCache = energyHandler.getCapacityAsInt();
            this.powerValue = -64;
            this.syncPacket();
        }
    }

    private void syncPacket() {
        if (this.getCurrentLevel() == null || !(this.getCurrentLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel, serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeCharging));
    }

    private void moveItemToTransformedOverSlot() {
        ItemResource resource = this.itemHandler.getResource(1);
        if (resource.isEmpty()) return;
        if (!this.itemHandler.getResource(2).isEmpty()) {
            this.powerValue = 0;
            return;
        }

        if (this.isFeCharging) {
            this.itemHandler.set(2, resource, 1);
        } else {
            ChargerChargingRecipe recipe = this.getItemRecipe(resource);
            if (this.checkRecipeItemNotValid(recipe)) return;
            ItemStackTemplate transformed = recipe.result();
            this.itemHandler.set(2, ItemResource.of(transformed), transformed.count());
        }
        this.itemHandler.set(1, ItemResource.EMPTY, 0);
        this.powerValue = 0;
        this.isFeCharging = false;
    }

    private void updateDisplayItemStack() {
        ItemStack newDisplayStack = this.getDisplayItemStackForRender();
        if (ItemStack.matches(this.displayItemStack, newDisplayStack)) return;
        this.displayItemStack = newDisplayStack.copy();
        PacketDistributor.sendToPlayersTrackingChunk(
            (ServerLevel) this.level,
            this.level.getChunk(this.getBlockPos()).getPos(),
            new UpdateDisplayItemPacket(this.displayItemStack, this.getPos())
        );
    }

    private ItemStack getDisplayItemStackForRender() {
        for (int i = 2; i >= 0; i--) {
            if (!this.itemHandler.getResource(i).isEmpty()) {
                return this.itemHandler.getResource(i).toStack(this.itemHandler.getAmountAsInt(i));
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
        return this.getBlockPos();
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.getLevel();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TimeLeft", this.timeLeft);
        output.putInt("TimeTotalCache", this.timeTotalCache);
        this.itemHandler.serialize(output.child("Depository"));
        output.putBoolean("FeCharging", this.isFeCharging);
        output.putInt("FeCooldown", this.feCooldown);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.timeLeft = input.getIntOr("TimeLeft", 0);
        this.timeTotalCache = input.getIntOr("TimeTotalCache", 0);
        this.itemHandler.deserialize(input.childOrEmpty("Depository"));
        this.isFeCharging = input.getBooleanOr("FeCharging", false);
        this.feCooldown = input.getIntOr("FeCooldown", 0);
    }

    @Override
    public int getInputPower() {
        if (this.getBlockState().getValue(ChargerBlock.POWERED)) return 0;
        return -this.powerValue;
    }

    private int getFeChargingPowerLevel() {
        if (this.grid == null) return 0;
        int remaining = this.grid.getRemaining();
        if (remaining >= 512) return 512;
        if (remaining >= 256) return 256;
        if (remaining >= 128) return 128;
        if (remaining >= 64) return 64;
        return 0;
    }

    @Nullable
    public ItemStack tryExtractItemFromSlot1() {
        ItemStack stack = this.itemHandler.getStacks().get(1).copy();
        if (stack.isEmpty()) return null;
        this.itemHandler.set(1, ItemResource.EMPTY, 0);
        this.isFeCharging = false;
        this.feCooldown = 0;
        this.timeLeft = 0;
        this.powerValue = 0;
        this.setChanged();
        return stack;
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.CONSUMER;
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
        if (this.itemHandler.getResource(0).isEmpty() && this.itemHandler.getResource(1).isEmpty()) return 0;
        return (int) Math.round(progress * 15);
    }

    @Override
    public FilteredItemStackHandler getFilteredItemStackHandler() {
        return this.itemHandler;
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
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
        ItemStack stack0 = this.itemHandler.getStacks().get(0).copy();
        ItemStack stack1 = this.itemHandler.getStacks().get(1).copy();
        ItemStack stack2 = this.itemHandler.getStacks().get(2).copy();
        this.dropItemStack(stack0);
        this.dropItemStack(stack1);
        this.dropItemStack(stack2);
        this.itemHandler.set(0, ItemResource.EMPTY, 0);
        this.itemHandler.set(1, ItemResource.EMPTY, 0);
        this.itemHandler.set(2, ItemResource.EMPTY, 0);
        this.timeLeft = 0;
        this.isFeCharging = false;
        this.feCooldown = 0;
        this.powerValue = 0;
    }

    private void dropItemStack(ItemStack stack) {
        if (!stack.isEmpty() && this.level != null) {
            Containers.dropItemStack(this.level,
                this.getBlockPos().getX() + 0.5,
                this.getBlockPos().getY() + 1.0,
                this.getBlockPos().getZ() + 0.5,
                stack);
        }
    }

    /// 充电器逻辑
    public void tick(Level level, BlockPos blockPos) {
        flushState(level, blockPos);
        BlockState state = level.getBlockState(blockPos);
        boolean powered = state.getValue(ChargerBlock.POWERED);
        if (this.grid == null) return;
        if (powered) return;
        if (this.timeLeft == 0) {
            this.moveItemToTransformingSlot();
        }
        if (this.timeLeft > 0) {
            if (this.isFeCharging) {
                this.powerValue = -(this.getFeChargingPowerLevel());
            }
            if (this.isGridWorking()) {
                if (this.isFeCharging) {
                    ItemStack processingStack = this.itemHandler.getStacks().get(1);
                    if (!processingStack.isEmpty()) {
                        EnergyHandler storage = Capabilities.Energy.ITEM.getCapability(
                            processingStack, ItemAccess.forStack(processingStack));
                        if (storage != null) {
                            int powerLevel = this.getFeChargingPowerLevel();
                            if (powerLevel > 0) {
                                int countdown = AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
                                int efficiency = AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency;
                                int remainingFE = storage.getCapacityAsInt() - storage.getAmountAsInt();
                                if (remainingFE <= 0) {
                                    this.isFeCharging = false;
                                    this.timeLeft = 0;
                                    this.timeTotalCache = 0;
                                } else {
                                    int feChargeRate = powerLevel * efficiency;
                                    if (this.feCooldown <= 0) {
                                        this.feCooldown = countdown;
                                        try (var transaction = Transaction.openRoot()) {
                                            storage.insert(feChargeRate * countdown, transaction);
                                            transaction.commit();
                                        }
                                    } else {
                                        this.feCooldown--;
                                    }
                                    this.timeLeft = remainingFE;
                                    this.timeTotalCache = storage.getCapacityAsInt();
                                }
                            }
                        }
                    }
                } else {
                    this.timeLeft--;
                }
            }
        }
        if (this.timeLeft == 0) {
            this.moveItemToTransformedOverSlot();
            if (this.timeLeft == 0) {
                this.timeTotalCache = 0;
            }
        }

        int signal = this.getAnalogRedstoneSignal();
        if (this.signalCache != signal) {
            this.signalCache = signal;
            level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        }

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameTime() % 10 != 0) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel, serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeCharging));
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        return IPowerConsumer.super.toPowerComponentInfo();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        FilteredItemStackHandler depository = this.getFilteredItemStackHandler();
        for (int slot = 0; slot < depository.size(); slot++) {
            ItemStack stack = depository.getStacks().get(slot).copy();
            if (!stack.isEmpty()) {
                Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }
}
