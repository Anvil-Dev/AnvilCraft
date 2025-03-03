package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.ISimpleMultiPartBlockState;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleMultiPartBlockItem<P extends Enum<P> & ISimpleMultiPartBlockState<P>> extends BlockItem {
    private final SimpleMultiPartBlock<P> block;

    public SimpleMultiPartBlockItem(SimpleMultiPartBlock<P> block, Properties properties) {
        super(block, properties);
        this.block = block;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (P part : this.block.getParts()) {
            BlockPos offset = pos.offset(part.getOffset());
            BlockState blockState =
                level.isWaterAt(offset) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(offset, blockState, 27);
        }
        return super.placeBlock(context, state);
    }

    private int getMaxOffsetDistance(Direction clickedFace) {
        Vec3i normal = clickedFace.getOpposite().getNormal();
        int i = 0;
        for (P part : this.block.getParts()) {
            int x = part.getOffsetX() * normal.getX()
                + part.getOffsetY() * normal.getY()
                + part.getOffsetZ() * normal.getZ();
            i = Math.max(x, i);
        }
        return ++i;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        Direction clickedFace = context.getClickedFace();
        if (result == InteractionResult.FAIL) {
            return super.useOn(new UseOnContext(
                context.getLevel(),
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand(),
                new BlockHitResult(
                    context.getClickLocation().relative(clickedFace, this.getMaxOffsetDistance(clickedFace)),
                    clickedFace,
                    context.getClickedPos().relative(clickedFace, this.getMaxOffsetDistance(clickedFace)),
                    false)));
        }
        return result;
    }
}
