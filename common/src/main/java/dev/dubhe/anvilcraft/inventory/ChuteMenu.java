package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
@Getter
public class ChuteMenu extends BaseMachineMenu implements IFilterMenu {
    public static ChuteMenu clientOf(MenuType<ChuteMenu> type, int containerId, Inventory inventory) {
        return new Client(type, containerId, inventory);
    }

    public static ChuteMenu serverOf(int containerId, @NotNull Inventory inventory, ChuteBlockEntity blockEntity) {
        return new Server(containerId, inventory, blockEntity);
    }

    private final Inventory inventory;
    public ChuteMenu(int containerId, @NotNull Inventory inventory, Container machine) {
        this(ModMenuTypes.CHUTE.get(), containerId, inventory, machine);
    }
    public ChuteBlockEntity getBlockEntity() {
        return (ChuteBlockEntity) getMachine();
    }

    public ChuteMenu(MenuType<ChuteMenu> type, int containerId, @NotNull Inventory inventory, Container machine) {
        super(type, containerId, machine);
        this.inventory = inventory;
        this.machine.startOpen(inventory.player);
        int i, j;
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlot(new FilterSlot(this.machine, j + i * 3, 62 + j * 18, 18 + i * 18, this));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }
    }

    private static class Server extends ChuteMenu implements IFilterMenuSever {
        public Server(int containerId, @NotNull Inventory inventory, ChuteBlockEntity blockEntity) {
            super(containerId, inventory, blockEntity);
        }
        @Override
        public ChuteBlockEntity getBlockEntity() {
            return super.getBlockEntity();
        }
        @Override
        public AbstractContainerMenu getMenu() {
            return this;
        }
        @Override
        public ServerPlayer getPlayer() {
            return (ServerPlayer) getInventory().player;
        }
        @Override
        public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player){
            IFilterMenuSever.super.clicked(slotId, button, clickType, player);
            super.clicked(slotId, button, clickType, player);
        }
        @Override
        public void setRecord(boolean record){
            super.setRecord(record);
            IFilterMenuSever.super.setRecord(record);
        }
    }

    private static class Client extends ChuteMenu {
        public Client(MenuType<ChuteMenu> type, int containerId, Inventory inventory) {
            super(type, containerId, inventory, new AutoCrafterContainer(NonNullList.withSize(9, ItemStack.EMPTY)));
        }
    }
    public @Nullable IFilterBlockEntity getEntity() {
        return this.machine instanceof IFilterBlockEntity entity ? entity : null;
    }
}
