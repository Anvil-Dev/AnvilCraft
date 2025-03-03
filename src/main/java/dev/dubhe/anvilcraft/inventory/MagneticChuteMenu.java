package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
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
public class MagneticChuteMenu extends BaseChuteMenu<MagneticChuteBlockEntity> {
    public MagneticChuteMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData) {
        super(menuType, containerId, inventory, extraData);
    }

    public MagneticChuteMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId, inventory, blockEntity);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.MAGNETIC_CHUTE.get();
    }
}
