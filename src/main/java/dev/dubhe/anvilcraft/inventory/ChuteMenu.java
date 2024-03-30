package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChuteMenu extends BaseMachineMenu implements IFilterMenu {
    private Inventory inventory;

    public ChuteMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(9));
        this.inventory = inventory;
    }

    public ChuteMenu(int containerId, @NotNull Inventory inventory, Container machine) {
        super(ModMenuTypes.CHUTE, containerId, machine);
        this.inventory = inventory;
        this.machine.startOpen(this.inventory.player);
        int i, j;
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlot(new FilterSlot(this.machine, j + i * 3, 62 + j * 18, 18 + i * 18, this));
            }
        }
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlot(new Slot(this.inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(this.inventory, i, 8 + i * 18, 142));
        }
    }

    public @Nullable IFilterBlockEntity getEntity() {
        return this.machine instanceof IFilterBlockEntity entity ? entity : null;
    }
}
