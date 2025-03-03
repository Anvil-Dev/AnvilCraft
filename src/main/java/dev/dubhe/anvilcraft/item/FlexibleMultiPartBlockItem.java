package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.IFlexibleMultiPartBlockState;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FlexibleMultiPartBlockItem<P extends Enum<P> & IFlexibleMultiPartBlockState<P, E>, T extends Property<E>, E extends Comparable<E>> extends BlockItem {
    private final FlexibleMultiPartBlock<P, T, E> block;

    public FlexibleMultiPartBlockItem(FlexibleMultiPartBlock<P, T, E> block, Properties properties) {
        super(block, properties);
        this.block = block;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (P part : this.block.getParts()) {
            BlockPos offset = pos.offset(part.getOffset(state.getValue(block.getAdditionalProperty())));
            BlockState blockState =
                level.isWaterAt(offset) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(offset, blockState, 27);
        }
        return super.placeBlock(context, state);
    }

    private int getMaxOffsetDistance(BlockState state, Direction clickedFace) {
        Vec3i normal = clickedFace.getOpposite().getNormal();
        int i = 0;
        for (P part : this.block.getParts()) {
            int x = part.getOffsetX(state.getValue(block.getAdditionalProperty())) * normal.getX()
                + part.getOffsetY(state.getValue(block.getAdditionalProperty())) * normal.getY()
                + part.getOffsetZ(state.getValue(block.getAdditionalProperty())) * normal.getZ();
            i = Math.max(x, i);
        }
        return ++i;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        Direction clickedFace = context.getClickedFace();
        BlockState state = this.block.getPlacementState(new BlockPlaceContext(context));
        if (state == null) return InteractionResult.FAIL;
        if (result == InteractionResult.FAIL) {
            return super.useOn(new UseOnContext(
                context.getLevel(),
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand(),
                new BlockHitResult(
                    context.getClickLocation().relative(clickedFace, this.getMaxOffsetDistance(state, clickedFace)),
                    clickedFace,
                    context.getClickedPos().relative(clickedFace, this.getMaxOffsetDistance(state, clickedFace)),
                    false)));
        }
        return result;
    }

    @Override
    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!block.hasEnoughSpace(state, context.getClickedPos(), context.getLevel())) return false;
        return super.canPlace(context, state);
    }
}
