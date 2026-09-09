package dev.dubhe.anvilcraft.block.cfa.item;

import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class CelestialForgingAnvilAmplifierBlockItem
    extends FlexibleMultiPartBlockItem<DirectionCube232PartHalf, DirectionProperty, Direction> {
    public CelestialForgingAnvilAmplifierBlockItem(
        FlexibleMultiPartBlock<DirectionCube232PartHalf, DirectionProperty, Direction> block,
        Properties properties
    ) {
        super(block, properties);
    }

    /// 增幅器占地 2x2，主块(BOTTOM_PART)固定在东南角。玩家点击 2x2 内任意一格时，
    /// 将点击位置吸附到主块所在格，使四格中的任一位置都能直接放置增幅器。
    /// 在 place 中吸附而非 useOn：useOn 拿到的 UseOnContext.getClickedPos() 是被点方块，
    /// 而实际放置格是 BlockPlaceContext 沿点击面偏移后的格子，两者差一个方向。
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        CelestialForgingAnvilAmplifierBlock amplifierBlock = (CelestialForgingAnvilAmplifierBlock) this.getBlock();
        BlockPos snapped = amplifierBlock.snapMainPos(level, placePos);
        if (snapped != null && !snapped.equals(placePos)) {
            context = new BlockPlaceContext(
                level,
                context.getPlayer(),
                context.getHand(),
                context.getItemInHand(),
                new BlockHitResult(
                    context.getClickLocation().add(
                        snapped.getX() - placePos.getX(),
                        snapped.getY() - placePos.getY(),
                        snapped.getZ() - placePos.getZ()
                    ),
                    context.getClickedFace(),
                    snapped,
                    false
                )
            );
        }
        return super.place(context);
    }
}
