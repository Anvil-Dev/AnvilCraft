package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.power.generator.ChargerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.network.ChargerSyncPacket;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.IStateListener;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class ChargerBlockEntity extends BlockEntity
    implements IPowerConsumer, IPowerProducer, IFilterBlockEntity, IStateListener<Boolean>, IItemResourceHandlerHolder, IHasDisplayItem {

    @Setter
    private boolean isCharger;
    @Setter
    private int timeLeft = 0;
    @Setter
    private int timeTotalCache = 0;
    private int powerValue = 0;
    private int signalCache = 0;

    @Getter
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
            return super.extract(2, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != 2) return 0;
            return super.extract(2, resource, amount, transaction);
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
    private PowerGrid grid;

    public ChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.isCharger = blockState.is(ModBlocks.CHARGER.get());
    }

    public boolean containsValidItem(ItemResource resource) {
        SingleRecipeInput input = new SingleRecipeInput(resource.toStack());
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) return false;
        Optional<RecipeHolder<ChargerChargingRecipe>> recipe = serverLevel.recipeAccess().getRecipeFor(
            ModRecipeTypes.CHARGER_CHARGING.get(),
            input,
            serverLevel
        );
        if (recipe.isEmpty()) return false;
        if (recipe.get().value().power == 0) return false;
        return this.isCharger == recipe.get().value().power < 0;
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
        if (recipe.power == 0) return true;
        return this.isCharger != recipe.power < 0;
    }

    private void moveItemToTransformingSlot() {
        ItemResource resource = this.itemHandler.getResource(0);
        if (resource.isEmpty()) return;
        if (!this.itemHandler.getResource(1).isEmpty()) return;
        ChargerChargingRecipe recipe = this.getItemRecipe(resource);
        if (this.checkRecipeItemNotValid(recipe)) return;
        this.itemHandler.set(0, ItemResource.EMPTY, 0);
        if (this.isCharger) {
            this.itemHandler.set(1, resource, 1);
        } else {
            ItemStackTemplate transformed = recipe.getResult();
            this.itemHandler.set(1, ItemResource.of(transformed), transformed.count());
        }
        this.timeLeft = recipe.time + 1; // since there is a "timeLeft--" after this, here +1 to negate
        this.timeTotalCache = recipe.time; // make a total time cache for client display
        this.powerValue = recipe.power;
        if (this.getCurrentLevel() == null || !(this.getCurrentLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache)
        );
    }

    private void moveItemToTransformedOverSlot() {
        ItemResource resource = this.itemHandler.getResource(1);
        if (resource.isEmpty()) return;
        if (!this.itemHandler.getResource(2).isEmpty()) {
            this.powerValue = 0;
            return;
        }
        if (this.isCharger) {
            ChargerChargingRecipe recipe = this.getItemRecipe(resource);
            if (this.checkRecipeItemNotValid(recipe)) return;
            ItemStackTemplate transformed = recipe.getResult();
            this.itemHandler.set(2, ItemResource.of(transformed), transformed.count());
        } else {
            this.itemHandler.set(2, resource, 1);
        }
        this.itemHandler.set(1, ItemResource.EMPTY, 0);
        this.powerValue = 0;
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
        this.itemHandler.serialize(output.child("Depository"));
        output.putBoolean("Mode", this.isCharger);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.timeLeft = input.getIntOr("TimeLeft", 0);
        this.itemHandler.deserialize(input.childOrEmpty("Depository"));
        this.isCharger = input.getBooleanOr("Mode", false);
    }

    @Override
    public int getInputPower() {
        return this.isCharger && !this.getBlockState().getValue(ChargerBlock.POWERED) ? -this.powerValue : 0;
    }

    @Override
    public PowerComponentType getComponentType() {
        return this.isCharger ? PowerComponentType.CONSUMER : PowerComponentType.PRODUCER;
    }

    @Override
    public int getOutputPower() {
        return !this.isCharger && !this.getBlockState().getValue(ChargerBlock.POWERED) ? this.powerValue : 0;
    }

    public double getProgress() {
        if (this.timeTotalCache != 0) return 1 - (double) this.timeLeft / this.timeTotalCache;
        return 0;
    }

    public int getAnalogRedstoneSignal() {
        if (this.itemHandler.getResource(0).isEmpty() && this.itemHandler.getResource(1).isEmpty()) return 0;
        return (int) Math.round(this.getProgress() * 15);
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
        return this.isCharger;
    }

    @Override
    public void notifyStateChanged(Boolean newState) {
        this.isCharger = newState;
        ItemHandlerUtil.dropAllToPos(this.itemHandler, this.getCurrentLevel(), this.getPos().above().getBottomCenter());
        this.timeLeft = 0;
        this.powerValue = 0;
    }

    /**
     * 充放电器逻辑
     */
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
            if (!this.isCharger || this.isGridWorking()) {
                // if isDisCharger or (isCharger and isGridWorking)
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
            level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
        }

        if (!(level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.getGameTime() % 10 != 0) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            serverLevel.getChunk(this.getBlockPos()).getPos(),
            new ChargerSyncPacket(this.getPos(), this.timeLeft, this.timeTotalCache)
        );
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        if (this.isCharger) {
            return IPowerConsumer.super.toPowerComponentInfo();
        } else {
            return IPowerProducer.super.toPowerComponentInfo();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        FilteredItemStackHandler depository = this.getFilteredItemStackHandler();
        ItemHandlerUtil.dropAllToPos(depository, this.level, this.getPos().getCenter());
    }
}
