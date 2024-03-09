package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.component.LimitSlot;
import dev.dubhe.anvilcraft.init.ModItemTags;
import lombok.Getter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;

@Getter
public class InteractMachineMenu extends BaseMachineMenu {

    public InteractMachineMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(11));
    }

    public InteractMachineMenu(int containerId, @NotNull Inventory inventory, @NotNull Container interactMachine) {
        super(ModMenuTypes.INTERACT_MACHINE, containerId, interactMachine);
        this.machine.startOpen(inventory.player);
        int i, j;
        this.addSlot(new LimitSlot(this.machine, 0, 26, 18, item -> item.is(ModItemTags.PROTOCOL)));
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.machine, j + i * 3 + 1, 62 + j * 18, 18 + i * 18));
            }
        }
        this.addSlot(new LimitSlot(this.machine, 10, 134, 54));
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
}
