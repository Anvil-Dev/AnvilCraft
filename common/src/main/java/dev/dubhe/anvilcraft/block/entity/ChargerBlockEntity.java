package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.item.IChargerChargeable;
import dev.dubhe.anvilcraft.api.item.IChargerDischargeable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.ChargerBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.StateListener;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ChargerBlockEntity
        extends BlockEntity
        implements IPowerConsumer, IPowerProducer, IFilterBlockEntity, StateListener<Boolean> {

    @Setter
    private boolean isCharger;
    private boolean previousDischargeFailed = false;
    private int cd;
    private boolean locked = false;
    private boolean powered = false;
    private boolean jumpOver = false;

    private final FilteredItemDepository depository = new FilteredItemDepository(1) {

        @Override
        public ItemStack insert(
                int slot,
                @NotNull ItemStack stack,
                boolean simulate,
                boolean notifyChanges,
                boolean isServer
        ) {
            if (!locked && !previousDischargeFailed) {
                ItemStack original = stack.copy();
                original.shrink(1);
                if (original.isEmpty()) {
                    ItemStack left = super.insert(slot, stack.copyWithCount(1), simulate, notifyChanges, isServer);
                    if (left.isEmpty() && !simulate) {
                        locked = true;
                    }
                    return left;
                } else {
                    ItemStack left = super.insert(slot, stack.copyWithCount(1), simulate, notifyChanges, isServer);
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
        public ItemStack extract(int slot, int amount, boolean simulate, boolean notifyChanges) {
            return !locked ? super.extract(slot, amount, simulate, notifyChanges) : ItemStack.EMPTY;
        }
    };

    @Getter
    @Setter
    private PowerGrid grid;

    public ChargerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState
    ) {
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
        ItemStack stack = depository.getStack(0).copy();
        if (stack.isEmpty() || !containsValidItem(stack)) return;
        if (isCharger) {
            if (stack.getItem() instanceof IChargerChargeable chargeable) {
                depository.setStack(0, chargeable.charge(stack));
                return;
            }
            if (stack.is(Items.IRON_INGOT.asItem())) {
                depository.setStack(0, ModItems.MAGNET_INGOT.asItemStack(1));
            }
        } else {
            if (stack.getItem() instanceof IChargerDischargeable dischargeable) {
                depository.setStack(0, dischargeable.discharge(stack));
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
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cd);
        tag.put("Depository", depository.serializeNbt());
        tag.putBoolean("Mode", isCharger);
        tag.putBoolean("PreviousDischargeFailed", previousDischargeFailed);
        tag.putBoolean("Locked", locked);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        cd = tag.getInt("Cooldown");
        depository.deserializeNbt(tag.getCompound("Depository"));
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
    public FilteredItemDepository getFilteredItemDepository() {
        return depository;
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

    @ExpectPlatform
    public static @NotNull ChargerBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return null;
    }

    @ExpectPlatform
    public static void onBlockEntityRegister(BlockEntityType<ChargerBlockEntity> type) {
    }

    /**
     * 充放电器逻辑
     */
    public void tick(Level level1, BlockPos blockPos) {
        this.flushState(level1, blockPos);
        BlockState state = level1.getBlockState(blockPos);
        powered = state.getValue(ChargerBlock.POWERED);

        if (level1.getGameTime() % 21 != 0) return;
        if (depository.getStack(0).isEmpty()) {
            locked = false;
            return;
        }
        if (grid.isWork() && !isCharger) {
            previousDischargeFailed = false;
        }
        if (powered) return;
        if (cd == 0 && containsValidItem(depository.getStack(0)) && !jumpOver) {
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
            if (grid.isWork()) {
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
                if (!grid.isWork() && !previousDischargeFailed) {
                    previousDischargeFailed = true;
                }
                cd = 0;
                locked = false;
            }
        }
    }
}
