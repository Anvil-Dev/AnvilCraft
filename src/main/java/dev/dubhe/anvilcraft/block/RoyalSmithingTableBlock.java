package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

public class RoyalSmithingTableBlock extends SmithingTableBlock {
    private static final Component CONTAINER_TITLE = Component.translatable("container.upgrade");

    public RoyalSmithingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((i, inventory, player) -> new RoyalSmithingMenu(i, inventory, ContainerLevelAccess.create(level, pos)), CONTAINER_TITLE);
    }
}
