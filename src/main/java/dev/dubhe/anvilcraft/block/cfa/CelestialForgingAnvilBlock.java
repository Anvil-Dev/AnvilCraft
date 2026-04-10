package dev.dubhe.anvilcraft.block.cfa;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.PropelPiston;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import org.jetbrains.annotations.Nullable;

public class CelestialForgingAnvilBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, CelestialForgingAnvilBlock>, IHammerRemovable {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    public CelestialForgingAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER));
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror));
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return PropelPiston.createTickerHelper(
                type,
                ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1)
            );
        }
        return null;
    }

    @Override
    public CelestialForgingAnvilBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CELESTIAL_FORGING_ANVIL.create(pos, state);
    }
}
