package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IChargerChargeable;
import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.StateListener;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class ChargerBlockEntity extends BlockEntity
        implements IPowerConsumer, IPowerProducer, IFilterBlockEntity, StateListener<Boolean>, ItemHandlerHolder
{

    @Setter
    private boolean isCharger;

    private boolean previousDischargeFailed = false;
    private int cd;
    private boolean locked = false;
    private boolean powered = false;
    private boolean jumpOver = false;

    @Getter
    private final FilteredItemStackHandler itemHandler = new FilteredItemStackHandler(1) {

        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!locked && !previousDischargeFailed) {
                ItemStack original = stack.copy();
                original.shrink(1);
                if (original.isEmpty()) {
                    ItemStack left = super.insertItem(slot, stack.copyWithCount(1), simulate);
                    if (left.isEmpty() && !simulate) {
                        locked = true;
                    }
                    return left;
                } else {
                    ItemStack left = super.insertItem(slot, stack.copyWithCount(1), simulate);
                    if (left.isEmpty() && !simulate) {
                        locked = true;
                    }
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
            return !locked ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
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
        if (isCharger) {
            return stack.getItem() instanceof IChargerChargeable || stack.is(Items.IRON_INGOT);
        }
        return stack.getItem() instanceof IChargerDischargeable;
    }

    private void processItemTransform() {
        ItemStack stack = itemHandler.getStackInSlot(0).copy();
        if (stack.isEmpty() || !containsValidItem(stack)) return;
        if (isCharger) {
            if (stack.getItem() instanceof IChargerChargeable chargeable) {
                itemHandler.setStackInSlot(0, chargeable.charge(stack));
                return;
            }
            if (stack.is(Items.IRON_INGOT.asItem())) {
                itemHandler.setStackInSlot(0, ModItems.MAGNET_INGOT.asStack(1));
            }
        } else {
            if (stack.getItem() instanceof IChargerDischargeable dischargeable) {
                itemHandler.setStackInSlot(0, dischargeable.discharge(stack));
            }
        }
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
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Cooldown", cd);
        tag.put("Depository", itemHandler.serializeNBT(provider));
        tag.putBoolean("Mode", isCharger);
        tag.putBoolean("PreviousDischargeFailed", previousDischargeFailed);
        tag.putBoolean("Locked", locked);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cd = tag.getInt("Cooldown");
        itemHandler.deserializeNBT(provider, tag.getCompound("Depository"));
        isCharger = tag.getBoolean("Mode");
        locked = tag.getBoolean("Locked");
        previousDischargeFailed = tag.getBoolean("PreviousDischargeFailed");
    }

    @Override
    public int getInputPower() {
        return locked && isCharger && !powered ? 32 : 0;
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        return isCharger ? PowerComponentType.CONSUMER : PowerComponentType.PRODUCER;
    }

    @Override
    public int getOutputPower() {
        return locked && !isCharger && !powered ? 24 : 0;
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
        return cd != 0;
    }

    @Override
    public Boolean getState() {
        return isCharger;
    }

    @Override
    public void notifyStateChanged(Boolean newState) {
        isCharger = newState;
        if (isCharger) {
            previousDischargeFailed = true;
        }
        locked = false;
        cd = 0;
    }

    /**
     * 充放电器逻辑
     */
    public void tick(Level level1, BlockPos blockPos) {
        this.flushState(level1, blockPos);
        BlockState state = level1.getBlockState(blockPos);
        powered = state.getValue(ChargerBlock.POWERED);
        if (grid == null) return;
        if (level1.getGameTime() % 21 != 0) return;
        if (itemHandler.getStackInSlot(0).isEmpty()) {
            locked = false;
            return;
        }
        if (grid.isWorking() && !isCharger) {
            previousDischargeFailed = false;
        }
        if (powered) return;
        if (cd == 0 && containsValidItem(itemHandler.getStackInSlot(0)) && !jumpOver) {
            locked = true;
            cd = 7;
            if (!isCharger) processItemTransform();
            return;
        }
        if (previousDischargeFailed) {
            if (cd <= 0) {
                locked = false;
            }
            return;
        }
        if (isCharger) {
            if (grid.isWorking()) {
                if (cd > 0) {
                    cd--;
                    locked = true;
                    jumpOver = true;
                } else {
                    processItemTransform();
                    cd = 0;
                    locked = false;
                    jumpOver = false;
                }
            }
        } else {
            if (cd > 0) {
                cd--;
                locked = true;
            } else {
                if (!grid.isWorking() && !previousDischargeFailed) {
                    previousDischargeFailed = true;
                }
                cd = 0;
                locked = false;
            }
        }
    }
}
