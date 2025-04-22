package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.util.StateListener;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ChargerBlockEntity extends BlockEntity
    implements IPowerConsumer, IPowerProducer, IFilterBlockEntity, StateListener<Boolean>, IItemHandlerHolder {

    @Setter
    private boolean isCharger;
    private int timeLeft = 0;
    private int powerValue = 0;
    private boolean powered = false;

    @Getter
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(3) {

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
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
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 2 ? super.extractItem(2, amount, simulate) : ItemStack.EMPTY;
        }
    };

    @Getter
    @Setter
    private PowerGrid grid;

    public ChargerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        isCharger = blockState.is(ModBlocks.CHARGER.get());
    }

    private boolean containsValidItem(ItemStack stack) {
        ChargerChargingRecipe.Input input =
            new ChargerChargingRecipe.Input(stack);
        if (level != null) {
            Optional<RecipeHolder<ChargerChargingRecipe>> x = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get(), input, level);
            if (x.isPresent()) {
                if (x.get().value().power == 0) return false;
                return isCharger == x.get().value().power < 0;
                // (isCharger && isRecipePowerCharging) || (isDisCharger && RecipePowerDischarging)
            }
        }
        return false;
    }

    @Nullable
    private ChargerChargingRecipe getItemRecipe(ItemStack stack) {
        ChargerChargingRecipe.Input input =
            new ChargerChargingRecipe.Input(stack);
        if (level != null) {
            Optional<RecipeHolder<ChargerChargingRecipe>> x = level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.CHARGER_CHARGING_TYPE.get(), input, level);
            if (x.isPresent())  {
                return x.get().value();
            }
        }
        return null;
    }

    private boolean checkRecipeItemNotValid(@Nullable ChargerChargingRecipe recipe, ItemStack stack) {
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
        if (checkRecipeItemNotValid(recipe, stack)) return;
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        if (isCharger) {
            itemHandler.setStackInSlot(1, stack);
        } else {
            ItemStack transformed = recipe.getResult().getDefaultInstance();
            transformed.setCount(1);
            itemHandler.setStackInSlot(1, transformed);
        }
        timeLeft = recipe.time + 1; //since there is a "timeLeft--" after this, here +1 to negate
        powerValue = recipe.power;
    }

    private void moveItemToTransformedOverSlot() {
        ItemStack stack = itemHandler.getStackInSlot(1).copy();
        if (stack.isEmpty()) return;
        if (!itemHandler.getStackInSlot(2).isEmpty()) {
            powerValue = 0;
            return;
        }
        if (isCharger) {
            ChargerChargingRecipe recipe = getItemRecipe(stack);
            if (checkRecipeItemNotValid(recipe, stack)) return;
            ItemStack transformed = recipe.getResult().getDefaultInstance();
            transformed.setCount(1);
            itemHandler.setStackInSlot(2, transformed);
        } else {
            itemHandler.setStackInSlot(2, stack);
        }
        itemHandler.setStackInSlot(1, ItemStack.EMPTY);
        powerValue = 0;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("TimeLeft", timeLeft);
        tag.put("Depository", itemHandler.serializeNBT(provider));
        tag.putBoolean("Mode", isCharger);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(tag, provider);
        timeLeft = tag.getInt("TimeLeft");
        itemHandler.deserializeNBT(provider, tag.getCompound("Depository"));
        isCharger = tag.getBoolean("Mode");
    }

    @Override
    public int getInputPower() {
        return isCharger ? -powerValue : 0;
        //return locked && isCharger && !powered ? 32 : 0;
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        return isCharger ? PowerComponentType.CONSUMER : PowerComponentType.PRODUCER;
    }

    @Override
    public int getOutputPower() {
        return !isCharger ? powerValue : 0;
        //return locked && !isCharger && !powered ? 24 : 0;
    }

    @Override
    public FilteredItemStackHandler getFilteredItemDepository() {
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
        powered = state.getValue(ChargerBlock.POWERED);
        if (grid == null) return;
        if (powered) return;
        if (timeLeft == 0) {
            moveItemToTransformingSlot();
        }
        if (timeLeft > 0) {
            if (!isCharger || isGridWorking()) {
                //if isDisCharger or (isCharger and isGridWorking)
                timeLeft--;
            }
        }
        if (timeLeft == 0) {
            moveItemToTransformedOverSlot();
        }
    }
}
