package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.inventory.RoyalGrindstoneMenu;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


import java.util.stream.Stream;

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

