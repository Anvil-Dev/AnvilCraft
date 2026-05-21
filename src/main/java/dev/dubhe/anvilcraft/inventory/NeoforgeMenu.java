package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.workstation.NeoforgeBlock;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public class NeoforgeMenu extends AnvilMenu {
    public NeoforgeMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        this.access.execute((level, pos) -> {
            if (level.getRandom().nextDouble() < 0.01) {
                NeoforgeBlock.damage(level, pos);
            }
        });
    }
}
