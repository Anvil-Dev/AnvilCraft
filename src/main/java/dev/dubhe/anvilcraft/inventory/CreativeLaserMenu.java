package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public class CreativeLaserMenu extends AbstractContainerMenu {
    @Nullable
    private final CreativeLaserBlockEntity blockEntity;

    public CreativeLaserMenu(int containerId, CreativeLaserBlockEntity blockEntity) {
        super(ModMenuTypes.CREATIVE_LASER.get(), containerId);
        this.blockEntity = blockEntity;
    }

    @SuppressWarnings("resource")
    public CreativeLaserMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf buf) {
        super(menuType, containerId);
        this.blockEntity = inventory.player.level()
            .getBlockEntity(buf.readBlockPos()) instanceof CreativeLaserBlockEntity entity ? entity : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
