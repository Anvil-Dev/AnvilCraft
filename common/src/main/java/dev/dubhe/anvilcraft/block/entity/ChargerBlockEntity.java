package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.StateListener;
import dev.dubhe.anvilcraft.util.WatchablePropertyDelegate;
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

import java.io.PrintWriter;
import java.io.StringWriter;

public class ChargerBlockEntity
        extends BlockEntity
        implements IPowerConsumer, IPowerProducer, IFilterBlockEntity, StateListener<Boolean> {

    @Setter
    private boolean isCharger;
    private int cd;
    private final WatchablePropertyDelegate<Boolean> locked = new WatchablePropertyDelegate<>(false) {
        @Override
        protected void onChanged(Boolean oldValue, Boolean newValue) {
            StringWriter sw = new StringWriter();
            PrintWriter writer = new PrintWriter(sw);
            new Exception().printStackTrace(writer);
            System.out.printf("StateChanged %s -> %s\n%s", oldValue, newValue, sw);
        }
    };

    private final FilteredItemDepository depository = new FilteredItemDepository(1) {

        @Override
        public ItemStack insert(
                int slot,
                @NotNull ItemStack stack,
                boolean simulate,
                boolean notifyChanges,
                boolean isServer
        ) {
            if (!locked.get()) {
                ItemStack original = stack.copy();
                original.shrink(1);
                if (original.isEmpty()) {
                    ItemStack left = super.insert(slot, stack.copyWithCount(1), simulate, notifyChanges, isServer);
                    if (left.isEmpty()) {
                        locked.set(true);
                    }
                    return left;
                } else {
                    ItemStack left = super.insert(slot, stack.copyWithCount(1), simulate, notifyChanges, isServer);
                    if (left.isEmpty()) {
                        locked.set(true);
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
            return !locked.get() ? super.extract(slot, amount, simulate, notifyChanges) : ItemStack.EMPTY;
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

    @Override
    public void gridTick() {
        if (depository.getStack(0).isEmpty()) {
            locked.set(false);
            return;
        }
        if (cd == 0 && containsValidItem(depository.getStack(0))) {
            locked.set(true);
            cd = 7;
            processItemTransform();
            return;
        }
        if (cd > 0) {
            cd--;
            locked.set(true);
        } else {
            cd = 0;
            locked.set(false);
        }
    }

    private boolean containsValidItem(ItemStack stack) {
        if (stack.is(ModItems.MAGNET.asItem())) {
            if (isCharger) {
                return stack.getDamageValue() != 0;
            }
            return false;
        }
        if (isCharger) {
            return stack.is(ModItems.CAPACITOR_EMPTY.asItem())
                    || stack.is(Items.IRON_INGOT);
        } else {
            return stack.is(ModItems.CAPACITOR.asItem())
                    || stack.is(ModItems.MAGNET_INGOT.asItem());
        }
    }

    private void processItemTransform() {
        ItemStack stack = depository.getStack(0).copy();
        if (stack.isEmpty() || !containsValidItem(stack)) return;
        if (isCharger) {
            if (stack.is(ModItems.CAPACITOR_EMPTY.asItem())) {
                depository.setStack(0, ModItems.CAPACITOR.asStack(1));
                return;
            }
            if (stack.is(Items.IRON_INGOT.asItem())) {
                depository.setStack(0, ModItems.MAGNET_INGOT.asStack(1));
                return;
            }
            if (stack.is(ModItems.MAGNET.asItem())) {
                depository.setStack(0, ModItems.MAGNET.asStack(1));
            }
        } else {
            if (stack.is(ModItems.MAGNET_INGOT.get())) {
                depository.setStack(0, Items.IRON_INGOT.getDefaultInstance().copyWithCount(1));
                return;
            }
            if (stack.is(Items.IRON_INGOT.asItem())) {
                depository.setStack(0, ModItems.MAGNET_INGOT.asStack(1));
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
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        cd = tag.getInt("Cooldown");
        depository.deserializeNbt(tag.getCompound("Depository"));
        isCharger = tag.getBoolean("Mode");
    }

    @Override
    public int getInputPower() {
        return locked.get() && isCharger ? 32 : 0;
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        return isCharger ? PowerComponentType.CONSUMER : PowerComponentType.PRODUCER;
    }

    @Override
    public int getOutputPower() {
        return locked.get() && !isCharger ? 24 : 0;
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
        locked.set(false);
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
}
