package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterGrindstoneBlock;
import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RoyalGrindstone extends BetterGrindstoneBlock implements IHammerRemovable {

    private static final Component CONTAINER_TITLE =
            Component.translatable("container.grindstone_title");

    public RoyalGrindstone(Properties properties) {
        super(properties);
    }

    @Override
    public MenuProvider getMenuProvider(
            @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return new SimpleMenuProvider(
                (i, inventory, player) ->
                        new RoyalGrindstoneMenu(i, inventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE);
    }
}
