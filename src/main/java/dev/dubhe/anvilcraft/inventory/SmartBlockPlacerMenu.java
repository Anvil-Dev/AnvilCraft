package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class SmartBlockPlacerMenu extends AbstractContainerMenu {
    @Nullable
    private final SmartBlockPlacerBlockEntity blockEntity;
    private final Level level;

    public SmartBlockPlacerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity machine) {
        super(menuType, containerId);
        this.blockEntity = (SmartBlockPlacerBlockEntity) machine;
        this.level = inventory.player.level();
    }

    public SmartBlockPlacerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, Objects.requireNonNull(
            inventory.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    @Nullable
    public SmartBlockPlacerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return false;
        return stillValid(
            ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
            player,
            ModBlocks.SMART_BLOCK_PLACER.get()
        );
    }
}
