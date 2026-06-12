package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.NeutronIrradiatorBlockEntity;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static dev.dubhe.anvilcraft.block.PropelPiston.createTickerHelper;

public class NeutronIrradiatorBlock extends Block implements IHammerRemovable, EntityBlock {
    public static VoxelShape MODEL = Shapes.or(
        Block.box(0, 0, 0, 16, 10, 16),
        Block.box(13, 10, 0, 16, 12, 3),
        Block.box(0, 10, 0, 3, 12, 3),
        Block.box(0, 10, 13, 3, 12, 16),
        Block.box(13, 10, 13, 16, 12, 16),
        Block.box(4, 10, 4, 12, 16, 12)
    );

    public static final EnumProperty<IrradiatorType> TYPE = EnumProperty.create("type", IrradiatorType.class);

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
    }

    public NeutronIrradiatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, IrradiatorType.NEUTRON));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NeutronIrradiatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.NEUTRON_IRRADIATOR.get(),
                (level1, pos, state1, entity) -> entity.tick(level1, pos, state1));
    }
}