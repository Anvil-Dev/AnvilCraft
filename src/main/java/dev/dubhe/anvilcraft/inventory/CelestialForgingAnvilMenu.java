package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

@Getter
@SuppressWarnings({"checkstyle:all"})
public class CelestialForgingAnvilMenu extends AbstractContainerMenu {
    static final int ANVIL_SLOTS = 4;
    private static final int SEED_SLOT = 4;
    private final CelestialForgingAnvilBlockEntity blockEntity;

    // Slot indices: 0=time, 1=space, 2=mass, 3=energy, 4=seed

    public CelestialForgingAnvilMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory,
        CelestialForgingAnvilBlockEntity blockEntity
    ) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;

        // 4 confined anvil slots
        for (int i = 0; i < ANVIL_SLOTS; i++) {
            this.addSlot(new CFAAnvilSlot(blockEntity.getAnvilInventory(), i, 9, 38 + i * 18));
        }

        // Seed slot (暂无功能限制)
        this.addSlot(new Slot(blockEntity.getAnvilInventory(), SEED_SLOT, 9, 121));

        // Player inventory (3 rows x 9 columns)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 92 + col * 18, 125 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 92 + col * 18, 183));
        }
    }

    public CelestialForgingAnvilMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData
    ) {
        this(menuType, containerId, inventory,
            (CelestialForgingAnvilBlockEntity) Objects.requireNonNull(
                inventory.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index <= SEED_SLOT) {
            // From anvil/seed slot to player inventory
            if (!this.moveItemStackTo(stack, SEED_SLOT + 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From player inventory: try anvil slots first, then seed slot
            boolean moved = false;
            for (int i = 0; i < ANVIL_SLOTS; i++) {
                Slot anvilSlot = this.slots.get(i);
                if (anvilSlot.mayPlace(stack) && anvilSlot.getItem().isEmpty()) {
                    if (this.moveItemStackTo(stack, i, i + 1, false)) {
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) {
                this.moveItemStackTo(stack, SEED_SLOT, SEED_SLOT + 1, false);
            }
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            blockEntity.startSearch();
            return true;
        }
        // Scroll wheel anvil transfer: id 1-4 = add, 5-8 = remove
        if (id >= 1 && id <= 8) {
            int slot = (id - 1) % 4;
            boolean add = id <= 4;
            handleAnvilTransfer(slot, add);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private static final Item[] ANVIL_ITEMS = {
        ModBlocks.CONFINED_TIME_ANVILON.asItem(),
        ModBlocks.CONFINED_SPACE_ANVILON.asItem(),
        ModBlocks.CONFINED_MASS_ANVILON.asItem(),
        ModBlocks.CONFINED_ENERGY_ANVILON.asItem(),
    };

    private void handleAnvilTransfer(int slot, boolean add) {
        Slot targetSlot = this.slots.get(slot);
        Item targetItem = ANVIL_ITEMS[slot];
        if (add) {
            // Add from player inventory to anvil slot
            if (targetSlot.getItem().getCount() >= targetSlot.getMaxStackSize()) return;
            for (int i = ANVIL_SLOTS + 1; i < this.slots.size(); i++) {
                Slot invSlot = this.slots.get(i);
                if (invSlot.getItem().is(targetItem)) {
                    invSlot.remove(1);
                    if (targetSlot.getItem().isEmpty()) {
                        targetSlot.set(new ItemStack(targetItem));
                    } else {
                        targetSlot.getItem().grow(1);
                    }
                    targetSlot.setChanged();
                    return;
                }
            }
        } else {
            // Remove from anvil slot to player inventory
            if (targetSlot.getItem().isEmpty()) return;
            ItemStack toMove = targetSlot.getItem().copyWithCount(1);
            for (int i = this.slots.size() - 1; i >= ANVIL_SLOTS + 1; i--) {
                Slot invSlot = this.slots.get(i);
                ItemStack invStack = invSlot.getItem();
                if (invStack.isEmpty()) {
                    invSlot.set(toMove);
                    targetSlot.remove(1);
                    invSlot.setChanged();
                    targetSlot.setChanged();
                    return;
                }
                if (ItemStack.isSameItemSameComponents(invStack, toMove)
                    && invStack.getCount() < invSlot.getMaxStackSize()) {
                    invStack.grow(1);
                    targetSlot.remove(1);
                    invSlot.setChanged();
                    targetSlot.setChanged();
                    return;
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
            ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
            player,
            ModBlocks.CELESTIAL_FORGING_ANVIL.get()
        );
    }

    // === Parameter calculation methods ===

    public static String formatAge(int count) {
        if (count == 0) return "---";
        double my = 2.0 * Math.pow(2.0, (count - 1) / 3.0);
        if (my >= 1024.0 * 1024.0) {
            return format3SigFig(my / (1024.0 * 1024.0)) + " Ty";
        } else if (my >= 1024.0) {
            return format3SigFig(my / 1024.0) + " By";
        } else {
            return format3SigFig(my) + " My";
        }
    }

    public static String formatRadius(int count) {
        if (count == 0) return "---";
        double rEarth = 0.125 * Math.pow(2.0, (count - 1) / 3.0);
        if (rEarth >= 12.7) {
            return format3SigFig(rEarth * 0.125 / 12.7) + " R☉";
        } else {
            return format3SigFig(rEarth) + " R⊕";
        }
    }

    public static String formatMass(int count) {
        if (count == 0) return "---";
        double mEarth = 0.022 * Math.pow(2.0, (count - 1) / 2.0);
        if (mEarth >= 22600.0) {
            return format3SigFig(mEarth * 0.063 / 22600.0) + " M☉";
        } else {
            return format3SigFig(mEarth) + " M⊕";
        }
    }

    public static String formatTemperature(int count) {
        if (count == 0) return "---";
        double kelvin = 50.0 * Math.pow(2.0, (count - 1) / 6.0);
        if (kelvin < 800.0) {
            return format3SigFig(kelvin - 273.0) + " ℃";
        } else {
            return format3SigFig(kelvin) + " K";
        }
    }

    static String format3SigFig(double value) {
        if (Math.abs(value) < 1e-9) return "0";
        int pow = (int) Math.floor(Math.log10(Math.abs(value)));
        // For values >= 1000, round to nearest 10^(pow-2) for true 3 sig figs
        if (pow >= 3) {
            double scale = Math.pow(10, pow - 2);
            double rounded = Math.round(value / scale) * scale;
            return String.format(Locale.US, "%.0f", rounded);
        }
        int digits = Math.max(0, 2 - pow);
        if (digits > 6) digits = 6;
        return String.format(Locale.US, "%." + digits + "f", value);
    }

    // === Custom slot for confined anvils ===

    public static class CFAAnvilSlot extends Slot {

        public CFAAnvilSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return switch (this.getSlotIndex()) {
                case 0 -> stack.is(ModBlocks.CONFINED_TIME_ANVILON.asItem());
                case 1 -> stack.is(ModBlocks.CONFINED_SPACE_ANVILON.asItem());
                case 2 -> stack.is(ModBlocks.CONFINED_MASS_ANVILON.asItem());
                case 3 -> stack.is(ModBlocks.CONFINED_ENERGY_ANVILON.asItem());
                default -> false;
            };
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }
}
