package dev.dubhe.anvilcraft.block.cfa.interfaces;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class CelestialForgingAnvilInterfaceBlock
    extends HorizontalDirectionalBlock
    implements IHammerRemovable, IHammerChangeable {
    public static final BooleanProperty ACTIVE = BlockStateProperties.ENABLED;
    public static final VoxelShape BASE_NORTH = ShapeUtil.merge(
        new AABB(0, 0, 2, 16, 4, 16),
        new AABB(0, 4, 8, 16, 8, 16),
        new AABB(0, 8, 6, 16, 12, 16),
        new AABB(7, 2, -1, 9, 3.75, 0),
        new AABB(3, 0, 0, 13, 1.75, 2),
        new AABB(5, 0, -2, 11, 1.75, 0),
        new AABB(7, 0, -4, 9, 1.75, -2)
    );

    public CelestialForgingAnvilInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false));
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected abstract void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder);

    /// 用铁砧锤右键在主动/被动模式之间切换（替代红石信号控制）。
    /// 返回 ACTIVE 属性使锤子选择轮切换主被动并预览两种模型，
    /// 而非旋转朝向——接口的朝向必须始终指向锻星砧。
    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return ACTIVE;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        if (!state.hasProperty(ACTIVE)) return false;
        level.setBlock(blockPos, state.cycle(ACTIVE), 3);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // 接口朝向背离锻星砧，沿反方向找到相邻部件，再通过 HALF 偏移定位底部中心控制器。
        Direction towardsCfa = state.getValue(FACING).getOpposite();
        BlockPos cfaBlockPos = pos.relative(towardsCfa);
        BlockState cfaBlockState = level.getBlockState(cfaBlockPos);
        if (cfaBlockState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaBlockState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaBlockPos.offset(half.getOffset().multiply(-1));
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof CelestialForgingAnvilBlockEntity cfaBe
                && player instanceof ServerPlayer sp) {
                ModMenuTypes.open(sp, cfaBe, controllerPos);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
