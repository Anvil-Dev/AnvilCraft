package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PortableAnvilMenu extends AnvilMenu implements HammerOpenedAnvilMenu {
    private final Inventory playerInventory;
    private final DataSlot openedHammerSlot = DataSlot.standalone();
    private final @Nullable OpenedHammerSource openedHammerSource;
    private boolean closingForHammerMove;

    public PortableAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT);
    }

    public PortableAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int openedHammerSlot) {
        this(containerId, playerInventory, access, OpenedHammerSource.fromInventory(playerInventory, openedHammerSlot));
    }

    public PortableAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, @Nullable OpenedHammerSource source) {
        super(containerId, playerInventory, access);
        this.playerInventory = playerInventory;
        this.openedHammerSource = source;
        this.openedHammerSlot.set(source == null ? HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT : source.clientInventorySlot());
        this.addDataSlot(this.openedHammerSlot);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.PORTABLE_ANVIL.get();
    }

    @Override
    public int anvilcraft$getOpenedHammerSlot() {
        return this.openedHammerSlot.get();
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

    private void closeIfOpenedHammerMoved() {
        if (this.openedHammerSource == null || this.closingForHammerMove) return;
        if (this.openedHammerSource.stillInPlace()) {
            return;
        }
        this.closingForHammerMove = true;
        HammerOpenedAnvilMenuHelper.closeOnServer(this.player);
    }
}
