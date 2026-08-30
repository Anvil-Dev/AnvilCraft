package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

public class LargeCrateBlockItem extends SimpleMultiPartBlockItem<Cube3x3PartHalf> {
    public LargeCrateBlockItem(SimpleMultiPartBlock<Cube3x3PartHalf> block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockState(pos).is(ModBlocks.CRATE);
    }
}
