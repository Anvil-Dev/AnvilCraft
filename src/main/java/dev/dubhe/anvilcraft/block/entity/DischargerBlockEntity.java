package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
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

public class DischargerBlockEntity extends BlockEntity
    implements IPowerProducer, IFilterBlockEntity, IStateListener<Boolean>, IItemResourceHandlerHolder, IHasDisplayItem {

    /** 放电器每tick从物品抽取的FE量 */
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
    private boolean isFeDischarged = false; // FE 放电已完成，等待移出
    private int signalCache = 0;

    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(3) {

        @Override
        public boolean isValid(int index, ItemResource resource) {
            // 只允许从输入槽(slot 0)放入物品
            if (index != 0) return false;
            return super.isValid(index, resource);
        }

        @Override
        public int extract(int index, ItemResource resource, int maxExtract, TransactionContext transaction) {
            // 漏斗只能从输出槽(slot 2)抽取物品
            if (index != 2) return 0;
            return super.extract(index, resource, maxExtract, transaction);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return this.getSlotLimit(index);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            Level level = DischargerBlockEntity.this.getLevel();
            if (level == null || level.isClientSide()) return;
            DischargerBlockEntity.this.setChanged();
            DischargerBlockEntity.this.updateDisplayItemStack();
            level.sendBlockUpdated(
                DischargerBlockEntity.this.getBlockPos(),
                DischargerBlockEntity.this.getBlockState(),
                DischargerBlockEntity.this.getBlockState(),
                Block.UPDATE_ALL
            );
        }
    };

    @Getter
    private ItemStack displayItemStack = ItemStack.EMPTY;

    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    public DischargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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
            return recipe.get().value().power() > 0; // 放电器使用power > 0的配方
        }
        // 检查FE放电能力
        ItemStack stack = resource.toStack();
        if (stack.isEmpty()) return false;
        EnergyHandler energyHandler = Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack));
        if (energyHandler == null) return false;
        return energyHandler.getAmountAsInt() > 0;
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
        return recipe.power() <= 0; // 放电器只接受power > 0的配方
    }

    private void moveItemToTransformingSlot() {
        ItemResource resource = this.itemHandler.getResource(0);
        if (resource.isEmpty()) return;
        if (!this.itemHandler.getResource(1).isEmpty()) return;

        ChargerChargingRecipe recipe = this.getItemRecipe(resource);
        if (!this.checkRecipeItemNotValid(recipe)) {
            this.isFeDischarging = false;
            this.itemHandler.set(0, ItemResource.EMPTY, 0);
            ItemStackTemplate transformed = recipe.result();
            this.itemHandler.set(1, ItemResource.of(transformed), transformed.count());
            this.timeLeft = recipe.time() + 1;
            this.timeTotalCache = recipe.time();
            this.powerValue = recipe.power();
            this.syncPacket();
            return;
        }

        // FE放电：物品有可抽取的FE时开始放电
        ItemStack stack = this.itemHandler.getStacks().get(0).copy();
        EnergyHandler energyHandler = Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack));
        if (energyHandler != null && energyHandler.getAmountAsInt() > 0) {
            this.isFeDischarging = true;
            this.itemHandler.set(0, ItemResource.EMPTY, 0);
            this.itemHandler.set(1, ItemResource.of(stack), stack.getCount());
            this.timeLeft = energyHandler.getAmountAsInt();
            this.timeTotalCache = energyHandler.getCapacityAsInt();
            this.powerValue = 64;
            this.syncPacket();
        }
    }

    private void syncPacket() {
        if (this.getCurrentLevel() == null || !(this.getCurrentLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel, serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeDischarging));
    }

    private void moveItemToTransformedOverSlot() {
        ItemResource resource = this.itemHandler.getResource(1);
        if (resource.isEmpty()) return;
        if (!this.itemHandler.getResource(2).isEmpty()) {
            this.powerValue = 0;
            return;
        }
        this.itemHandler.set(2, resource, 1);
        this.itemHandler.set(1, ItemResource.EMPTY, 0);
        this.powerValue = 0;
        this.isFeDischarging = false;
        this.isFeDischarged = false;
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
        output.putBoolean("FeDischarging", this.isFeDischarging);
        output.putBoolean("FeDischarged", this.isFeDischarged);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.timeLeft = input.getIntOr("TimeLeft", 0);
        this.timeTotalCache = input.getIntOr("TimeTotalCache", 0);
        this.itemHandler.deserialize(input.childOrEmpty("Depository"));
        this.isFeDischarging = input.getBooleanOr("FeDischarging", false);
        this.isFeDischarged = input.getBooleanOr("FeDischarged", false);
    }

    private int getFeDischargingPowerLevel() {
        if (this.grid == null) return 0;
        int consume = this.grid.getConsume();
        int count = 0;
        for (IPowerComponent component : this.grid.getComponents()) {
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
        ItemStack stack = this.itemHandler.getStacks().get(1).copy();
        if (stack.isEmpty()) return null;
        this.itemHandler.set(1, ItemResource.EMPTY, 0);
        this.isFeDischarging = false;
        this.timeLeft = 0;
        this.powerValue = 0;
        this.setChanged();
        return stack;
    }

    @Override
    public int getOutputPower() {
        return !this.getBlockState().getValue(ChargerBlock.POWERED) ? this.powerValue : 0;
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }

    public void stopProcessing() {
        this.timeLeft = 0;
        this.timeTotalCache = 0;
        this.isFeDischarging = false;
        this.powerValue = 0;
    }

    public double getProgress() {
        if (this.timeTotalCache == 0) return 0;
        // 放电器：进度从满衰减到空 (remaining / total)
        return Math.max(0, Math.min(1, (double) this.timeLeft / this.timeTotalCache));
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
        return Boolean.FALSE;
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
        this.isFeDischarging = false;
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

    /// 放电器逻辑
    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
        BlockState state = level.getBlockState(blockPos);
        boolean powered = state.getValue(ChargerBlock.POWERED);
        if (this.grid == null) return;
        if (powered) return;
        if (this.timeLeft == 0) {
            this.moveItemToTransformingSlot();
        }
        if (this.timeLeft > 0) {
            if (this. isFeDischarging) {
                this.powerValue = this.getFeDischargingPowerLevel();
            }
            if (this.isFeDischarging) {
                ItemStack processingStack = this.itemHandler.getStacks().get(1);
                if (!processingStack.isEmpty()) {
                    EnergyHandler storage = Capabilities.Energy.ITEM.getCapability(
                        processingStack, ItemAccess.forStack(processingStack));
                    if (storage != null) {
                        int currentEnergy = storage.getAmountAsInt();
                        if (currentEnergy <= 0) {
                            this.isFeDischarging = false;
                            this.isFeDischarged = true; // 标记放电完成，等待移入输出槽
                            this.timeLeft = 0;
                            this.timeTotalCache = 0;
                        } else {
                            try (var transaction = Transaction.openRoot()) {
                                int extracted = storage.extract(
                                    Math.min(FE_EXTRACT_PER_TICK, currentEnergy), transaction);
                                transaction.commit();
                                this.powerValue = (int) (extracted
                                    * (1 - AnvilCraft.CONFIG.powerConverter.powerConverterLoss)
                                    / AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency);
                                this.timeLeft = currentEnergy - extracted;
                                this.timeTotalCache = storage.getCapacityAsInt();
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
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache, this.isFeDischarging));
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        return IPowerProducer.super.toPowerComponentInfo();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        Containers.dropContents(this.level, pos, this.getFilteredItemStackHandler().getStacks());
    }
}
