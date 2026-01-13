package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.util.anvil.AnvilMenuResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;

public class FrostAnvilMenu extends AnvilMenu {
    public final AnvilMenuResult result = AnvilMenuResult.builder()
        .allowBeyondMaxLevel(AnvilCraft.CONFIG.frostAnvilBeyondMaxLevel)
        .allowUsingFrostMetalToRepair()
        .noCostInRenaming()
        .noTaxInRepairUsingItem()
        .useNewRepairCostAlgorithm()
        .create();

    public FrostAnvilMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public FrostAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
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
}
