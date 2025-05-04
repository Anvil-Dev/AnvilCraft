package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PulseGeneratorMenu extends AbstractContainerMenu {
    @Getter
    private final PulseGeneratorBlockEntity blockEntity;
    private final Level level;

    public PulseGeneratorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull BlockEntity machine) {
        super(menuType, containerId);
        this.blockEntity = (PulseGeneratorBlockEntity) machine;
        this.level = inventory.player.level();
    }

    public PulseGeneratorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData) {
        this(
            menuType, containerId, inventory,
            Objects.requireNonNull(
                inventory.player.level().getBlockEntity(extraData.readBlockPos()) instanceof PulseGeneratorBlockEntity repeater
                ? repeater.readDataNbt(Objects.requireNonNull(extraData.readNbt())) : null
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
            ModBlocks.PULSE_GENERATOR.get()
        );
    }

    public void setStartMode(byte mode) {
        this.blockEntity.setStartMode(mode);
    }

    public void setOutputInvert(boolean isInvert) {
        this.blockEntity.setOutputInvert(isInvert);
    }

    public void addWaitingTime(int delta) {
        this.blockEntity.setWaitingTime(this.blockEntity.getWaitingTime() + delta);
    }

    public void addSignalDuration(int delta) {
        this.blockEntity.setSignalDuration(this.blockEntity.getSignalDuration() + delta);
    }
}
