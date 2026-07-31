package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ActiveSilencerMenu extends AbstractContainerMenu {
    @Getter
    private final ActiveSilencerBlockEntity blockEntity;

    private final Level level;

    /// 主动消音器的ScreenHandler
    public ActiveSilencerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity machine) {
        super(menuType, containerId);
        this.blockEntity = (ActiveSilencerBlockEntity) machine;
        this.level = inventory.player.level();
    }

    public ActiveSilencerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
            ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
            player,
            ModBlocks.ACTIVE_SILENCER.get()
        );
    }

    public void addSound(Identifier soundId) {
        this.blockEntity.addSound(soundId);
    }

    public void removeSound(Identifier soundId) {
        this.blockEntity.removeSound(soundId);
    }

    public void handleSync(List<Identifier> sounds) {
        this.blockEntity.sync(sounds);
    }
}
