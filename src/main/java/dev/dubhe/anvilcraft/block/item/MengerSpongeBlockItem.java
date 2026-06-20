package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.FishTankBlock;
import dev.dubhe.anvilcraft.block.ObsidianCauldron;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class MengerSpongeBlockItem extends BlockItem {
    public MengerSpongeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) return super.useOn(context);
        if (!player.isShiftKeyDown()) {
            if (state.getBlock() instanceof AbstractCauldronBlock abstractCauldronBlock
                && !(abstractCauldronBlock instanceof ObsidianCauldron)) {
                level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
                return InteractionResult.SUCCESS;
            }
            if (state.getBlock() instanceof FishTankBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof FishTankBlockEntity fishTank)) return super.useOn(context);

                fishTank.getFluidHandler().drain(1000, IFluidHandler.FluidAction.EXECUTE);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }
}
