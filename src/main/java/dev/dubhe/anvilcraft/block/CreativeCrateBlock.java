package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CreativeCrateBlock extends BaseEntityBlock implements IHammerRemovable {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CreativeCrateBlock::new);
    }

    protected CreativeCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return null;
    }
}
