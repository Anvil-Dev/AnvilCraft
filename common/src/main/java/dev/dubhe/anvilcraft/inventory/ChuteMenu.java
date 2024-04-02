package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChuteMenu extends BaseMachineMenu implements IFilterMenu {

    public ChuteMenu(MenuType<ChuteMenu> type, int containerId, Inventory inventory) {
        this(type, containerId, inventory, new SimpleContainer(9));
    }

    public ChuteMenu(int containerId, @NotNull Inventory inventory, Container machine) {
        this(ModMenuTypes.CHUTE.get(), containerId, inventory, machine);
    }

    public ChuteMenu(MenuType<ChuteMenu> type, int containerId, @NotNull Inventory inventory, Container machine) {
        super(type, containerId, machine);
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

    public @Nullable IFilterBlockEntity getEntity() {
        return this.machine instanceof IFilterBlockEntity entity ? entity : null;
    }
}
