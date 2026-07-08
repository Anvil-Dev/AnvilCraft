package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class ControlValveMenu extends AbstractContainerMenu {
    public static final int FILTER_X = 80;
    public static final int FILTER_Y = 18;

    @Nullable
    private final ControlValveBlockEntity blockEntity;
    private final Level level;

    public ControlValveMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        BlockEntity machine
    ) {
        this(
            menuType,
            containerId,
            inventory,
            machine instanceof ControlValveBlockEntity valve ? valve : null
        );
    }

    public ControlValveMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        FriendlyByteBuf extraData
    ) {
        this(menuType, containerId, inventory, getBlockEntity(inventory, extraData.readBlockPos()));
    }

    private ControlValveMenu(
        @Nullable MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        @Nullable ControlValveBlockEntity blockEntity
    ) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        this.level = inventory.player.level();
        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);
    }

    @Nullable
    private static ControlValveBlockEntity getBlockEntity(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof ControlValveBlockEntity valve ? valve : null;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }
    }

    public @Nullable ControlValveBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.blockEntity == null) return false;
        return stillValid(
            ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
            player,
            ModBlocks.CONTROL_VALVE.get()
        );
    }
}
