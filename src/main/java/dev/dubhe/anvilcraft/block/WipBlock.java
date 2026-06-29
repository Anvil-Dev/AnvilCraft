package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WipBlock extends BaseEntityBlock implements IMoveableEntityBlock {

    public WipBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(WipBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return WipBlockEntity.createInstance(ModBlockEntities.WIP_BLOCK.get(), blockPos, blockState);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof WipBlockEntity wipBe)) {
            return super.getDrops(state, params);
        }
        BlockState initialBlockState = wipBe.getInitialBlock();
        if (initialBlockState == null || initialBlockState.isAir()) {
            return super.getDrops(state, params);
        }

        return initialBlockState.getDrops(params);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity e = level.getBlockEntity(pos);
        if (e instanceof WipBlockEntity wipBlockEntity) {
            if (wipBlockEntity.getStepCount() >= 15) return 15;
            if (wipBlockEntity.getStepCount() <= 0) return 0;
            return wipBlockEntity.getStepCount();
        }
        return 0;
    }

}
