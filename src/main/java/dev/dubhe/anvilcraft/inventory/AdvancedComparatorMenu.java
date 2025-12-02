package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import lombok.Getter;
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

public class AdvancedComparatorMenu extends AbstractContainerMenu {
    @Getter
    private final AdvancedComparatorBlockEntity blockEntity;
    private final Level level;

    public AdvancedComparatorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity machine) {
        super(menuType, containerId);
        this.blockEntity = (AdvancedComparatorBlockEntity) machine;
        this.level = inventory.player.level();
    }

    public AdvancedComparatorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(
            menuType, containerId, inventory,
            Objects.requireNonNull(
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof AdvancedComparatorBlockEntity comparator
                    ? comparator.readDataNbt(Objects.requireNonNull(extraData.readNbt())) : null
            ));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
            ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
            player,
            ModBlocks.ADVANCED_COMPARATOR.get()
        );
    }

    public void setCompareMode(byte mode) {
        this.blockEntity.setCompareMode(AdvancedComparatorBlockEntity.Mode.fromIndex(mode));
    }

    public void setOutputInvert(boolean isInvert) {
        this.blockEntity.setOutputInvert(isInvert);
    }

    public void setRedstoneControl(boolean redstoneControl) {
        this.blockEntity.setRedstoneControl(redstoneControl);
    }
}
