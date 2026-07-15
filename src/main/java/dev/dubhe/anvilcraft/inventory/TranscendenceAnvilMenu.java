package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.util.anvil.AnvilMenuResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class TranscendenceAnvilMenu extends AnvilMenu implements HammerOpenedAnvilMenu {
    public final AnvilMenuResult result = AnvilMenuResult.builder()
        .allowBeyondMaxLevel(AnvilCraft.CONFIG.transcendenceAnvilBeyondMaxLevel)
        .allowEnchantingMultipleItems()
        .ignoreEnchantmentCompatible()
        .noCostInRenaming()
        .noTaxInRepairUsingItem()
        .useNewRepairCostAlgorithm()
        .create();
    private final Inventory playerInventory;
    private final DataSlot openedHammerSlot = DataSlot.standalone();
    private final @Nullable OpenedHammerSource openedHammerSource;
    private boolean closingForHammerMove;

    public TranscendenceAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public TranscendenceAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, access, HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT);
    }

    public TranscendenceAnvilMenu(
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access,
        int openedHammerSlot
    ) {
        this(containerId, playerInventory, access, OpenedHammerSource.fromInventory(playerInventory, openedHammerSlot));
    }

    public TranscendenceAnvilMenu(
        int containerId,
        Inventory playerInventory,
        ContainerLevelAccess access,
        @Nullable OpenedHammerSource source
    ) {
        super(containerId, playerInventory, access);
        this.playerInventory = playerInventory;
        this.openedHammerSource = source;
        this.openedHammerSlot.set(
            source == null ? HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT : source.clientInventorySlot()
        );
        this.addDataSlot(this.openedHammerSlot);
    }

    @Override
    public MenuType<?> getType() {
        return ModMenuTypes.TRANSCENDENCE_ANVIL.get();
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return super.mayPickup(player, hasStack) || this.result.noCostInRenaming && this.result.onlyRenaming;
    }

    @Override
    public void createResultInternal() {
        ItemStack inputLeft = this.getSlot(0).getItem();
        ItemStack inputRight = this.getSlot(1).getItem();
        this.result.createResult(
            this.player,
            inputLeft,
            inputRight,
            this.itemName
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
    public void clicked(int slotId, int button, ContainerInput containerInput, Player player) {
        boolean touchedHammer = HammerOpenedAnvilMenuHelper.touchesOpenedHammerSlot(
            this,
            this.playerInventory,
            slotId,
            button,
            containerInput,
            this.openedHammerSlot.get()
        );
        if (touchedHammer) {
            HammerOpenedAnvilMenuHelper.closeOnServer(player);
            return;
        }
        super.clicked(slotId, button, containerInput, player);
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

    private void closeIfOpenedHammerMoved() {
        if (this.openedHammerSource == null || this.closingForHammerMove) return;
        if (this.openedHammerSource.stillInPlace()) return;
        this.closingForHammerMove = true;
        HammerOpenedAnvilMenuHelper.closeOnServer(this.player);
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        final int costCache = this.cost.get();
        super.onTake(player, stack);
        if (this.openedHammerSource != null) {
            this.openedHammerSource.damage();
            HammerOpenedAnvilMenuHelper.playUseSound(player);
        }
        this.closeIfOpenedHammerMoved();
        if (costCache >= 5 && costCache < 15) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 6000, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 6000, 1));
        } else if (costCache >= 15) {
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 12000, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 12000, 2));
        }
    }
}
