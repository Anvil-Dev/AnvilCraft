package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class WipBlock extends BaseEntityBlock implements IMoveableEntityBlock {

    public WipBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(WipBlock::new);
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
        if (initialBlockState.isAir()) {
            return super.getDrops(state, params);
        }
        return initialBlockState.getDrops(params);
    }

    /// 返回进程方块应当继承属性的初始方块，没有可继承的目标时返回 null。
    public static @Nullable BlockState getInheritedBlock(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof WipBlockEntity wip)) return null;
        BlockState initialBlock = wip.getInitialBlock();
        if (initialBlock.isAir() || initialBlock.getBlock() instanceof WipBlock) return null;
        return initialBlock;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        BlockState initialBlock = WipBlock.getInheritedBlock(level, pos);
        if (initialBlock == null) return super.canHarvestBlock(state, level, pos, player);
        return initialBlock.getBlock().canHarvestBlock(initialBlock, level, pos, player);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState initialBlock = WipBlock.getInheritedBlock(level, pos);
        if (initialBlock == null) return super.getDestroyProgress(state, player, level, pos);
        return initialBlock.getDestroyProgress(player, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity e = level.getBlockEntity(pos);
        if (e instanceof WipBlockEntity wipBlockEntity) {
            if (wipBlockEntity.getStepCount() >= 15) return 15;
            return Math.max(wipBlockEntity.getStepCount(), 0);
        }
        return 0;
    }
}
