package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.block.ITranscendiumBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** 可以执行全部锻造操作的超限锻造台。 */
public class TranscendenceSmithingTableBlock extends SmithingTableBlock
    implements IHammerRemovable, ITranscendiumBlock {
    private static final Component CONTAINER_TITLE =
        Component.translatable("block.anvilcraft.transcendence_smithing_table");

    public TranscendenceSmithingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new TranscendenceSmithingMenu(
                id,
                inventory,
                ContainerLevelAccess.create(level, pos)
            ),
            TranscendenceSmithingTableBlock.CONTAINER_TITLE
        );
    }
}
