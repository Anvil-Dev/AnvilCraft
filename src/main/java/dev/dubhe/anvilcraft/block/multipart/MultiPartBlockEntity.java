package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface MultiPartBlockEntity<P extends Enum<P>, T extends AbstractMultiPartBlock<P>> extends EntityBlock {
    T getMultiBlock();

    @Override
    default @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (getMultiBlock().isMainPart(state)) {
            return createBlockEntity(pos, state);
        }
        return null;
    }

    BlockEntity createBlockEntity(BlockPos pos, BlockState state);
}
