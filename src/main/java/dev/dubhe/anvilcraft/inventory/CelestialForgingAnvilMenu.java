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
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@Getter
public class CelestialForgingAnvilMenu extends AbstractContainerMenu {
    static final int ANVIL_SLOTS = 4;
    private static final int SEED_SLOT = 4;
    static final int MATERIAL_SLOT = 5;
    private final CelestialForgingAnvilBlockEntity blockEntity;

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

        // Seed slot (single item, consumed on search)
        this.addSlot(new SeedSlot(blockEntity.getAnvilInventory(), SEED_SLOT, 9, 121));

        // Material slot
        this.addSlot(new CFAMaterialSlot(blockEntity, 267, 121));

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
            (CelestialForgingAnvilBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index <= SEED_SLOT || index == MATERIAL_SLOT) {
            if (!this.moveItemStackTo(stack, MATERIAL_SLOT + 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < ANVIL_SLOTS; i++) {
                Slot anvilSlot = this.slots.get(i);
                if (anvilSlot.mayPlace(stack)) {
                    if (this.moveItemStackTo(stack, i, i + 1, false)) {
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) {
                Slot matSlot = this.slots.get(MATERIAL_SLOT);
                if (matSlot.mayPlace(stack)) {
                    this.moveItemStackTo(stack, MATERIAL_SLOT, MATERIAL_SLOT + 1, false);
                }
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
            this.blockEntity.startSearch();
            return true;
        }
        if (id >= 1 && id <= 8) {
            int slot = (id - 1) % 4;
            boolean add = id <= 4;
            this.handleAnvilTransfer(slot, add);
            return true;
        }
        if (id >= 9 && id < 100) {
            int optionIndex = id - 9;
            this.blockEntity.configureMaterialSlot(optionIndex);
            return true;
        }
        if (id >= 100 && id < 200) {
            int optionIndex = id - 100;
            this.blockEntity.buildMegastructure(optionIndex);
            return true;
        }
        if (id == 200) {
            this.blockEntity.toggleLocked();
            return true;
        }
        if (id == 201) {
            this.blockEntity.browseHistoryPrev();
            return true;
        }
        if (id == 202) {
            this.blockEntity.browseHistoryNext();
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
            if (targetSlot.getItem().getCount() >= targetSlot.getItem().getMaxStackSize()) return;
            for (int i = MATERIAL_SLOT + 1; i < this.slots.size(); i++) {
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
            if (targetSlot.getItem().isEmpty()) return;
            ItemStack toMove = targetSlot.getItem().copyWithCount(1);
            for (int i = this.slots.size() - 1; i >= MATERIAL_SLOT + 1; i--) {
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
                    && invStack.getCount() < invStack.getMaxStackSize()) {
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
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            this.blockEntity.setMaterialFilter(new ItemStack(Items.BARRIER));
            this.blockEntity.setMaterialLimit(0);
            this.blockEntity.setChanged();
            var level = this.blockEntity.getLevel();
            if (level != null) {
                var state = this.blockEntity.getBlockState();
                level.sendBlockUpdated(this.blockEntity.getBlockPos(), state, state, 3);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
            ContainerLevelAccess.create(this.blockEntity.getLevel(), this.blockEntity.getBlockPos()),
            player,
            ModBlocks.CELESTIAL_FORGING_ANVIL.get()
        );
    }

    // === Precomputed display tables ===

    private static final String[] AGE_TABLE = {
        "2 My", "2.52 My", "3.18 My", "4 My", "5.04 My", "6.35 My", "8 My", "10.1 My",
        "12.7 My", "16 My", "20.2 My", "25.4 My", "32 My", "40.3 My", "50.8 My", "64 My",
        "80.6 My", "102 My", "128 My", "161 My", "203 My", "256 My", "323 My", "406 My",
        "512 My", "645 My", "813 My", "1 By", "1.26 By", "1.59 By", "2 By", "2.52 By",
        "3.18 By", "4 By", "5.04 By", "6.35 By", "8 By", "10.1 By", "12.7 By", "16 By",
        "20.2 By", "25.4 By", "32 By", "40.3 By", "50.8 By", "64 By", "80.6 By", "102 By",
        "128 By", "161 By", "203 By", "256 By", "323 By", "406 By", "512 By", "645 By",
        "813 By", "1 Ty", "1.26 Ty", "1.59 Ty", "2 Ty", "2.52 Ty", "3.18 Ty", "4 Ty"
    };

    private static final String[] RADIUS_TABLE = {
        "0.125 R⊕", "0.158 R⊕", "0.198 R⊕", "0.25 R⊕",
        "0.32 R⊕", "0.4 R⊕", "0.5 R⊕", "0.63 R⊕",
        "0.79 R⊕", "1 R⊕", "1.26 R⊕", "1.59 R⊕",
        "2 R⊕", "2.52 R⊕", "3.18 R⊕", "4 R⊕",
        "5.04 R⊕", "6.35 R⊕", "8 R⊕", "10.1 R⊕",
        "0.125 R☉", "0.158 R☉", "0.198 R☉", "0.25 R☉",
        "0.32 R☉", "0.4 R☉", "0.5 R☉", "0.63 R☉",
        "0.79 R☉", "1 R☉", "1.26 R☉", "1.59 R☉",
        "2 R☉", "2.52 R☉", "3.18 R☉", "4 R☉",
        "5.04 R☉", "6.35 R☉", "8 R☉", "10.1 R☉",
        "12.7 R☉", "16 R☉", "20.2 R☉", "25.4 R☉",
        "32 R☉", "40.3 R☉", "50.8 R☉", "64 R☉",
        "80.6 R☉", "102 R☉", "128 R☉", "161 R☉",
        "203 R☉", "256 R☉", "323 R☉", "406 R☉",
        "512 R☉", "645 R☉", "813 R☉", "1k R☉",
        "1.26k R☉", "1.59k R☉", "2k R☉", "2.52k R☉"
    };

    private static final String[] MASS_TABLE = {
        "0.022 M⊕", "0.031 M⊕", "0.044 M⊕", "0.063 M⊕",
        "0.088 M⊕", "0.125 M⊕", "0.177 M⊕", "0.25 M⊕",
        "0.35 M⊕", "0.5 M⊕", "0.7 M⊕", "1 M⊕",
        "1.41 M⊕", "2 M⊕", "2.82 M⊕", "4 M⊕",
        "5.66 M⊕", "8 M⊕", "11.3 M⊕", "16 M⊕",
        "22.6 M⊕", "32 M⊕", "45.3 M⊕", "64 M⊕",
        "90.5 M⊕", "128 M⊕", "181 M⊕", "256 M⊕",
        "362 M⊕", "512 M⊕", "724 M⊕", "1k M⊕",
        "1.41k M⊕", "2k M⊕", "2.82k M⊕", "4k M⊕",
        "5.66k M⊕", "8k M⊕", "11.3k M⊕", "16k M⊕",
        "0.063 M☉", "0.088 M☉", "0.125 M☉", "0.177 M☉",
        "0.25 M☉", "0.35 M☉", "0.5 M☉", "0.7 M☉",
        "1 M☉", "1.41 M☉", "2 M☉", "2.82 M☉",
        "4 M☉", "5.66 M☉", "8 M☉", "11.3 M☉",
        "16 M☉", "22.6 M☉", "32 M☉", "45.3 M☉",
        "64 M☉", "90.5 M☉", "128 M☉", "181 M☉"
    };

    private static final String[] TEMPERATURE_TABLE = {
        "-223 ℃", "-217 ℃", "-210 ℃", "-202 ℃",
        "-194 ℃", "-184 ℃", "-173 ℃", "-161 ℃",
        "-147 ℃", "-132 ℃", "-114 ℃", "-95 ℃",
        "-73 ℃", "-49 ℃", "-21 ℃", "10 ℃",
        "44 ℃", "83 ℃", "127 ℃", "176 ℃",
        "231 ℃", "293 ℃", "362 ℃", "440 ℃",
        "800 K", "898 K", "1010 K", "1130 K",
        "1270 K", "1430 K", "1600 K", "1800 K",
        "2020 K", "2260 K", "2540 K", "2850 K",
        "3200 K", "3590 K", "4030 K", "4530 K",
        "5080 K", "5700 K", "6400 K", "7180 K",
        "8060 K", "9050 K", "10200 K", "11400 K",
        "12800 K", "14400 K", "16100 K", "18100 K",
        "20300 K", "22800 K", "25600 K", "28700 K",
        "32300 K", "36200 K", "40600 K", "45600 K",
        "51200 K", "57500 K", "64500 K", "72400 K"
    };

    public static String formatAge(int count) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return AGE_TABLE[count - 1];
        return "---";
    }

    public static String formatRadius(int count) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return RADIUS_TABLE[count - 1];
        return "---";
    }

    public static String formatMass(int count) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return MASS_TABLE[count - 1];
        return "---";
    }

    public static String formatTemperature(int count) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return TEMPERATURE_TABLE[count - 1];
        return "---";
    }

    public static String formatAgeOffset(int count, float offset) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return applyOffset(AGE_TABLE[count - 1], offset);
        return "---";
    }

    public static String formatRadiusOffset(int count, float offset) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return applyOffset(RADIUS_TABLE[count - 1], offset);
        return "---";
    }

    public static String formatMassOffset(int count, float offset) {
        if (count == 0) return "---";
        if (count >= 1 && count <= 64) return applyOffset(MASS_TABLE[count - 1], offset);
        return "---";
    }

    private static String applyOffset(String entry, float offset) {
        int spaceIdx = entry.indexOf(' ');
        String numStr = entry.substring(0, spaceIdx);
        double multiplier = 1.0;
        if (numStr.endsWith("k")) {
            numStr = numStr.substring(0, numStr.length() - 1);
            multiplier = 1000.0;
        }
        double value = Double.parseDouble(numStr) * multiplier;
        double offsetValue = value * (1.0 + offset);
        String unit = entry.substring(spaceIdx + 1);
        return format3SigFig(offsetValue) + " " + unit;
    }

    @SuppressWarnings("MalformedFormatString")
    private static String format3SigFig(double value) {
        if (Math.abs(value) < 1e-9) return "0";
        int pow = (int) Math.floor(Math.log10(Math.abs(value)));
        if (pow >= 3) {
            double scale = Math.pow(10, pow - 2);
            double rounded = Math.round(value / scale) * scale;
            return String.format(Locale.US, "%.0f", rounded);
        }
        int digits = Math.max(0, 2 - pow);
        if (digits > 6) digits = 6;
        String formatted = String.format(Locale.US, "%." + digits + "f", value);
        if (formatted.contains(".")) {
            formatted = formatted.replaceAll("0+$", "");
            formatted = formatted.replaceAll("\\.$", "");
        }
        return formatted;
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

    // === Custom slot for building material ===

    public static class CFAMaterialSlot extends Slot {

        private final CelestialForgingAnvilBlockEntity blockEntity;

        public CFAMaterialSlot(CelestialForgingAnvilBlockEntity blockEntity, int x, int y) {
            super(blockEntity.getMaterialContainer(), 0, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            ItemStack filter = this.blockEntity.getMaterialFilter();
            if (filter.isEmpty() || filter.is(Items.BARRIER)) return false;
            return ItemStack.isSameItemSameComponents(filter, stack);
        }

        @Override
        public int getMaxStackSize() {
            int limit = this.blockEntity.getMaterialLimit();
            return limit > 0 ? limit : 1;
        }
    }

    // === Custom slot for seed items ===

    public static class SeedSlot extends Slot {

        public SeedSlot(net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
