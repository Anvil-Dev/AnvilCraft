package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.api.fluid.FluidStackResourceHandler;
import dev.dubhe.anvilcraft.block.cauldron.ObsidianCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.FishTankBlock;
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
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

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
        if (player.isShiftKeyDown()) return super.useOn(context);
        InteractionResult success = this.clearFluid(context, state, level, pos);
        if (success == null) return super.useOn(context);
        return success;
    }

    public @Nullable InteractionResult clearFluid(UseOnContext context, BlockState state, Level level, BlockPos pos) {
        if (state.getBlock() instanceof AbstractCauldronBlock abstractCauldronBlock
            && !(abstractCauldronBlock instanceof ObsidianCauldronBlock)) {
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            return InteractionResult.SUCCESS;
        }
        if (state.getBlock() instanceof FishTankBlock) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof FishTankBlockEntity fishTank)) return super.useOn(context);

            FluidStackResourceHandler handler = fishTank.getFluidHandler();

            try (Transaction transaction = Transaction.openRoot()) {
                handler.extract(0, handler.getResource(0), 1000, transaction);
                transaction.commit();
            }
            return InteractionResult.SUCCESS;
        }
        return null;
    }
}
