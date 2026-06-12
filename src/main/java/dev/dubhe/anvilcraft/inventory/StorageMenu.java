package dev.dubhe.anvilcraft.inventory;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.saved.storage.network.MenuState;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

@Getter
public class StorageMenu extends AbstractContainerMenu {
    private final StorageBlockEntity be;
    private final MenuState state;
    private final Player player;

    public StorageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inv, @Nullable RegistryFriendlyByteBuf buf) {
        this(menuType, containerId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public StorageMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inv, BlockEntity be) {
        super(menuType, containerId);
        this.be = Util.cast(be);
        this.state = MenuState.get(this.be.getId());
        this.player = inv.player;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
            ContainerLevelAccess.create(player.level(), this.be.getBlockPos()),
            player,
            this.be.getBlockState().getBlock()
        );
    }
}
