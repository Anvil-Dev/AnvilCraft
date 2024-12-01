package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import it.unimi.dsi.fastutil.Pair;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class TeslaTowerMenu extends AbstractContainerMenu {

    @Getter
    private final TeslaTowerBlockEntity blockEntity;

    private final Level level;

    public TeslaTowerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull BlockEntity machine) {
        super(menuType, containerId);
        blockEntity = (TeslaTowerBlockEntity) machine;
        this.level = inventory.player.level();
    }

    public TeslaTowerMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
            ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
            player,
            ModBlocks.TESLA_TOWER.get()
        );
    }

    public void addFilter(String id, String arg) {
        blockEntity.addFilter(id, arg);
    }

    public void removeFilter(String id, String arg) {
        blockEntity.removeFilter(id, arg);
    }

    public void handleSync(List<Pair<TeslaFilter, String>> filters) {
        blockEntity.handleSync(filters);
    }
}
