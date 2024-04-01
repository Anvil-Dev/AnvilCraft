package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;

public class RoyalGrindstone extends GrindstoneBlock {

    private static final Component CONTAINER_TITLE = Component.translatable("container.grindstone_title");

    public RoyalGrindstone(Properties properties) {
        super(properties);
    }
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((i, inventory, player) -> {
            return new RoyalGrindstoneMenu(i, inventory, ContainerLevelAccess.create(level, pos));
        }, CONTAINER_TITLE);
    }
    

}

