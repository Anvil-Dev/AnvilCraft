package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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

public class ActiveSilencerMenu extends AbstractContainerMenu {

    @Getter
    private final ActiveSilencerBlockEntity blockEntity;
    private final Level level;

    /**
     * 主动消音器的ScreenHandler
     */
    public ActiveSilencerMenu(@Nullable MenuType<?> menuType,
                              int containerId,
                              Inventory inventory,
                              @NotNull BlockEntity machine) {
        super(menuType, containerId);
        blockEntity = (ActiveSilencerBlockEntity) machine;
        this.level = inventory.player.level();
    }

    public ActiveSilencerMenu(
            @Nullable MenuType<?> menuType,
            int containerId,
            Inventory inventory,
            @NotNull FriendlyByteBuf extraData
    ) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
                ContainerLevelAccess.create(
                        level,
                        blockEntity.getBlockPos()
                ),
                player,
                ModBlocks.ACTIVE_SILENCER.get()
        );
    }

    public void addSound(ResourceLocation soundId) {
        blockEntity.addSound(soundId);
    }

    public void removeSound(ResourceLocation soundId) {
        blockEntity.removeSound(soundId);
    }
}
