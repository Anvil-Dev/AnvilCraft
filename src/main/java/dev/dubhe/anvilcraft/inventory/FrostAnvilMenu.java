package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.util.anvil.AnvilMenuResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

import javax.annotation.Nullable;

public class FrostAnvilMenu extends AnvilMenu implements HammerOpenedAnvilMenu {
    public final AnvilMenuResult result = AnvilMenuResult.builder()
        .allowBeyondMaxLevel(AnvilCraft.CONFIG.frostAnvilBeyondMaxLevel)
        .allowUsingFrostMetalToRepair()
        .noCostInRenaming()
        .noTaxInRepairUsingItem()
        .useNewRepairCostAlgorithm()
        .create();
    private final Inventory playerInventory;
    private final DataSlot openedHammerSlot = DataSlot.standalone();
    private final @Nullable OpenedHammerSource openedHammerSource;
    private boolean closingForHammerMove;

    public FrostAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public FrostAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, access, HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT);
    }

    public FrostAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int openedHammerSlot) {
        this(containerId, playerInventory, access, OpenedHammerSource.fromInventory(playerInventory, openedHammerSlot));
    }

    public FrostAnvilMenu(
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access,
        @Nullable OpenedHammerSource source
    ) {
        super(containerId, playerInventory, access);
        this.playerInventory = playerInventory;
        this.openedHammerSource = source;
        this.openedHammerSlot.set(source == null ? HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT : source.clientInventorySlot());
        this.addDataSlot(this.openedHammerSlot);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.FROST_ANVIL.get();
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return super.mayPickup(player, hasStack) || this.result.noCostInRenaming && this.result.onlyRenaming;
    }

    @Override
    public void createResult() {
        ItemStack inputLeft = this.getSlot(0).getItem();
        ItemStack inputRight = this.getSlot(1).getItem();
        this.result.createResult(
            this.player,
            inputLeft,
            inputRight,
            this.itemName,
            tax -> CommonHooks.onAnvilChange(this, inputLeft, inputRight, this.resultSlots, this.itemName, tax, this.player)
        );
        this.resultSlots.setItem(0, this.result.result);
        this.cost.set(this.result.xpCost);
        this.repairItemCountCost = this.result.repairItemCountCost;
    }

    @Override
    public int anvilcraft$getOpenedHammerSlot() {
        return this.openedHammerSlot.get();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean touchedHammer = HammerOpenedAnvilMenuHelper.touchesOpenedHammerSlot(
            this,
            this.playerInventory,
            slotId,
            button,
            clickType,
            this.openedHammerSlot.get()
        );
        if (touchedHammer) {
            HammerOpenedAnvilMenuHelper.closeOnServer(player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
        this.closeIfOpenedHammerMoved();
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        this.closeIfOpenedHammerMoved();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.openedHammerSource != null) {
            this.clearContainer(player, this.inputSlots);
        }
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        if (this.openedHammerSource != null) {
            this.openedHammerSource.damage();
            HammerOpenedAnvilMenuHelper.playUseSound(player);
        }
        this.closeIfOpenedHammerMoved();
    }

    private void closeIfOpenedHammerMoved() {
        if (this.openedHammerSource == null || this.closingForHammerMove) return;
        if (this.openedHammerSource.stillInPlace()) {
            return;
        }
        this.closingForHammerMove = true;
        HammerOpenedAnvilMenuHelper.closeOnServer(this.player);
    }
}
