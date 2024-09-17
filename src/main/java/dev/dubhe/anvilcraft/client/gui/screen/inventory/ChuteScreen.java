package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ChuteScreen extends BaseChuteScreen<ChuteBlockEntity, ChuteMenu> {

    public ChuteScreen(ChuteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    boolean shouldSkipDirection(Direction direction) {
        return Direction.UP == direction;
    }
}
