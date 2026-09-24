package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/** 物品一次摆出蛋糕，各格仅依赖自身下方的支撑。 */
public class LargeCakeBlockItem extends BlockItem {
    public LargeCakeBlockItem(LargeCakeBlock block, Properties properties) {
        super(block, properties);
    }

    public static BlockPos origin(BlockPos pos, BlockState state) {
        return pos.subtract(state.getValue(LargeCakeBlock.HALF).getOffset());
    }

    public static void forEachPlacedBlock(BlockPos origin, BlockState state, BiConsumer<BlockPos, BlockState> consumer) {
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            consumer.accept(origin.offset(part.getOffset()), state.setValue(LargeCakeBlock.HALF, part));
        }
    }

    @Override
    @Nullable
    public BlockState getPlacementState(BlockPlaceContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = context.getClickedPos().offset(part.getOffset());
            if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getBlockState(pos).canBeReplaced()
                || part.getOffsetY() == 0 && !this.getBlock().defaultBlockState().canSurvive(level, pos)
                || player != null && (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) {
                return null;
            }
        }
        return super.getPlacementState(context);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (!super.placeBlock(context, state)) return false;
        forEachPlacedBlock(context.getClickedPos(), state, (pos, partState) -> {
            if (!pos.equals(context.getClickedPos())) context.getLevel().setBlockAndUpdate(pos, partState);
        });
        return true;
    }
}
