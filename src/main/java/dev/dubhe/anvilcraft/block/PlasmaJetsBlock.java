package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.PlasmaJetsBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PlasmaJetsBlock extends BaseEntityBlock {
    public PlasmaJetsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(PlasmaJetsBlock::new);
    }

    public static boolean trySpawn(BlockPos pos, Level level) {
        BlockState cauldron = level.getBlockState(pos.below());
        BlockState heater = level.getBlockState(pos.below().below());
        if (!cauldron.is(ModBlocks.FIRE_CAULDRON)
            || !heater.is(ModBlocks.HEATER)
            || heater.getValue(HeaterBlock.OVERLOAD)
        ) return false;
        for (int i = 0; i < 8; i++) {
            if (!level.getBlockState(pos.above(i)).isAir()) return false;
        }
        for (Direction direction : Direction.values()) {
            if (!direction.getAxis().isHorizontal()) continue;
            if (level.getBlockState(pos.relative(direction)).isAir()) return false;
        }
        level.setBlock(pos, ModBlocks.PLASMA_JETS.getDefaultState(), 3);
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PLASMA_JETS.get(), PlasmaJetsBlockEntity::tick);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PLASMA_JETS.create(pos, state);
    }
}
