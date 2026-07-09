package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.item.abnormal.IAbnormal;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import dev.dubhe.anvilcraft.util.anvil.AnvilMenuResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RoyalAnvilMenu extends AnvilMenu implements HammerOpenedAnvilMenu {
    public final AnvilMenuResult result = AnvilMenuResult.builder()
        .allowBeyondMaxLevel(AnvilCraft.CONFIG.royalAnvilBeyondMaxLevel)
        .create();
    private final Inventory playerInventory;
    private final DataSlot openedHammerSlot = DataSlot.standalone();
    private final @Nullable OpenedHammerSource openedHammerSource;
    private boolean closingForHammerMove;

    public RoyalAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public RoyalAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, access, HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT);
    }

    public RoyalAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, int openedHammerSlot) {
        this(containerId, playerInventory, access, OpenedHammerSource.fromInventory(playerInventory, openedHammerSlot));
    }

    public RoyalAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, @Nullable OpenedHammerSource source) {
        super(containerId, playerInventory, access);
        this.playerInventory = playerInventory;
        this.openedHammerSource = source;
        this.openedHammerSlot.set(source == null ? HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT : source.clientInventorySlot());
        this.addDataSlot(this.openedHammerSlot);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.ROYAL_ANVIL.get();
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
            tax -> CommonHooks.onAnvilChange(
                this,
                inputLeft,
                inputRight,
                this.resultSlots,
                Objects.requireNonNull(this.itemName),
                tax,
                this.player
            )
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

    private void closeIfOpenedHammerMoved() {
        if (this.openedHammerSource == null || this.closingForHammerMove) return;
        if (this.openedHammerSource.stillInPlace()) {
            return;
        }
        this.closingForHammerMove = true;
        HammerOpenedAnvilMenuHelper.closeOnServer(this.player);
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        if (this.openedHammerSource != null) {
            HammerOpenedAnvilMenuHelper.playUseSound(player);
        }
        this.closeIfOpenedHammerMoved();
        Level level = player.level();
        if (level.isClientSide()) return;
        int curedNumber = IAbnormal.getAbnormalCount(player, ICursed.class);
        if (curedNumber <= 0) return;
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(player.getX(), player.getY(), player.getZ());
        level.addFreshEntity(bolt);
    }
}
