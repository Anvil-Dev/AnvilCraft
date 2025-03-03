package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;


@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TransparentCraftingTableBlock extends TransparentBlock implements IHammerRemovable {

    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

    public TransparentCraftingTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, Type.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asItem())) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ModMenuTypes.open((ServerPlayer) player, getMenuProvider(state, level, pos));
        player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new CraftingMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
            Component.translatable("container.crafting")
        );
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(this)) return;
        if (this.tryFormMatrix(level, pos)) {
            return;
        }
        if (state.getValue(TYPE) != Type.SINGLE) level.setBlockAndUpdate(pos, state.setValue(TYPE, Type.SINGLE));
        Direction.Plane.HORIZONTAL.stream()
            .map(pos::relative)
            .filter(poz -> {
                BlockState adjacentState = level.getBlockState(poz);
                return adjacentState.is(this) && adjacentState.getValue(TYPE) != Type.SINGLE;
            })
            .forEach(poz -> deformMatrix(level, poz));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (newState.is(this)) return;
        if (state.getValue(TYPE) != Type.SINGLE) {
            this.deformMatrix(level, pos);
            return;
        }
        Direction.Plane.HORIZONTAL.stream()
            .map(pos::relative)
            .forEach(poz -> this.tryFormMatrix(level, poz));
    }

    /**
     * 以某个方块为起始点，尝试构建一个有透明工作台组成的矩阵。
     * 若与该方块相连的所有透明工作台不构成一个长方形，构建失败。
     *
     * @param level 尝试构建矩阵的维度
     * @param pos   尝试构建矩阵的方块位置
     * @return 是否成功构建透明工作台矩阵
     */
    private boolean tryFormMatrix(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this)) return false;
        int maxSize = AnvilCraft.config.transparentCraftingTableMaxMatrixSize;
        int x0 = pos.getX();
        int y0 = pos.getY();
        int z0 = pos.getZ();
        BlockPos.MutableBlockPos mpos = pos.mutable();
        int xMin = x0;
        int xMax = x0;
        while ((xMax - xMin < maxSize) && level.getBlockState(mpos.set(xMin - 1, y0, z0)).is(this)) {
            xMin--;
        }
        while ((xMax - xMin < maxSize) && level.getBlockState(mpos.set(xMax + 1, y0, z0)).is(this)) {
            xMax++;
        }
        int xSize = xMax - xMin + 1;
        if (xSize < 2 || xSize > maxSize) return false;
        int zMin = z0;
        int zMax = z0;
        while ((zMax - zMin < maxSize) && level.getBlockState(mpos.set(x0, y0, zMin - 1)).is(this)) {
            zMin--;
        }
        while ((zMax - zMin < maxSize) && level.getBlockState(mpos.set(x0, y0, zMax + 1)).is(this)) {
            zMax++;
        }
        int zSize = zMax - zMin + 1;
        if (zSize < 2 || zSize > maxSize) return false;
        for (int x = xMin; x <= xMax; x++) {
            if (x == x0) continue;
            for (int z = zMin; z <= zMax; z++) {
                if (z == z0) continue;
                if (!level.getBlockState(mpos.set(x, y0, z)).is(this)) return false;
            }
        }
        for (int x = xMin; x <= xMax; x++) {
            if (level.getBlockState(mpos.set(x, y0, zMin - 1)).is(this)) return false;
            if (level.getBlockState(mpos.set(x, y0, zMax + 1)).is(this)) return false;
        }
        for (int z = zMin; z <= zMax; z++) {
            if (level.getBlockState(mpos.set(xMin - 1, y0, z)).is(this)) return false;
            if (level.getBlockState(mpos.set(xMax + 1, y0, z)).is(this)) return false;
        }
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                int xIndex = x == xMax ? 2 : x > xMin ? 1 : 0;
                int zIndex = z == zMax ? 2 : z > zMin ? 1 : 0;
                BlockState state = level.getBlockState(mpos.set(x, y0, z));
                level.setBlockAndUpdate(mpos, state.setValue(TYPE, Type.LOOKUP[xIndex][zIndex]));
            }
        }
        return true;
    }

    /**
     * 以某个方块为起始点，尝试移除该方块所属的透明工作台组成的矩阵。
     *
     * @param level 尝试移除矩阵的维度
     * @param pos   尝试移除矩阵的方块位置
     */
    private void deformMatrix(Level level, BlockPos pos) {
        int x0 = pos.getX();
        int y0 = pos.getY();
        int z0 = pos.getZ();
        BlockPos.MutableBlockPos mpos = pos.mutable();
        int xMin = x0;
        int xMax = x0;
        while (level.getBlockState(mpos.set(xMin - 1, y0, z0)).is(this)) {
            xMin--;
        }
        while (level.getBlockState(mpos.set(xMax + 1, y0, z0)).is(this)) {
            xMax++;
        }
        int zMin = z0;
        int zMax = z0;
        while (level.getBlockState(mpos.set(x0, y0, zMin - 1)).is(this)) {
            zMin--;
        }
        while (level.getBlockState(mpos.set(x0, y0, zMax + 1)).is(this)) {
            zMax++;
        }
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                BlockState state = level.getBlockState(mpos.set(x, y0, z));
                if (!state.is(this)) continue;
                level.setBlockAndUpdate(mpos, state.setValue(TYPE, Type.SINGLE));
            }
        }
    }

    public enum Type implements StringRepresentable {
        SINGLE("single"),
        CENTER("center"),
        SIDE_NORTH("side_n"),
        SIDE_EAST("side_e"),
        SIDE_SOUTH("side_s"),
        SIDE_WEST("side_w"),
        CORNER_NORTH_WEST("corner_nw"),
        CORNER_NORTH_EAST("corner_ne"),
        CORNER_SOUTH_WEST("corner_sw"),
        CORNER_SOUTH_EAST("corner_se");

        public static final Type[][] LOOKUP = {
            {
                CORNER_NORTH_WEST,
                SIDE_WEST,
                CORNER_SOUTH_WEST
            },
            {
                SIDE_NORTH,
                CENTER,
                SIDE_SOUTH
            },
            {
                CORNER_NORTH_EAST,
                SIDE_EAST,
                CORNER_SOUTH_EAST
            }
        };

        final String serializedName;

        Type(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }


        @Override
        public String toString() {
            return getSerializedName();
        }
    }
}
