package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.BaseMachineBlockEntity;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class BaseMachineMenu extends AbstractContainerMenu {
    protected final BlockEntity machine;

    protected BaseMachineMenu(@Nullable MenuType<?> menuType, int containerId, @NotNull BlockEntity machine) {
        super(menuType, containerId);
        this.machine = machine;
    }

    public void setDirection(Direction direction) {
        if (this.machine instanceof BaseMachineBlockEntity entity) entity.setDirection(direction);
    }
}
