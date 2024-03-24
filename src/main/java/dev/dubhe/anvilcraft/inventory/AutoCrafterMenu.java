package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.component.AutoCrafterSlot;
import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

@Getter
public class AutoCrafterMenu extends BaseMachineMenu {
    private Inventory inventory;

    public AutoCrafterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(9));
        this.inventory = inventory;
    }

    public AutoCrafterMenu(int containerId, @NotNull Inventory inventory, @NotNull Container interactMachine) {
        super(ModMenuTypes.AUTO_CRAFTER, containerId, interactMachine);
        this.inventory = inventory;
        this.machine.startOpen(inventory.player);
        int i, j;
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlot(new AutoCrafterSlot(this.machine, j + i * 3, 26 + j * 18, 18 + i * 18, this));
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.machine.stopOpen(player);
    }

    public void setRecordMaterial(boolean record) {
        if (this.machine instanceof AutoCrafterBlockEntity entity) entity.setRecord(record);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSlotDisabled(int index) {
        if (!(this.machine instanceof AutoCrafterBlockEntity entity)) return false;
        return entity.getDisabled().get(index);
    }

    public void setSlotDisabled(int index, boolean state) {
        if (!(this.machine instanceof AutoCrafterBlockEntity entity)) return;
        entity.getDisabled().set(index, state);
    }
}
