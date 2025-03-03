package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ChuteMenu extends BaseChuteMenu<ChuteBlockEntity> {
    public ChuteMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData) {
        super(menuType, containerId, inventory, extraData);
    }

    public ChuteMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId, inventory, blockEntity);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.CHUTE.get();
    }
}
