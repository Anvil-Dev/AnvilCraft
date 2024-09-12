package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.inventory.MagneticChuteMenu;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MagneticChuteScreen
        extends BaseChuteScreen<MagneticChuteBlockEntity, MagneticChuteMenu> {

    public MagneticChuteScreen(MagneticChuteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    boolean shouldSkipDirection(Direction direction) {
        return false;
    }
}
