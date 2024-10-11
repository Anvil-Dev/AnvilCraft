package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * 透明工作台
 *
 * @author DancingSnow
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("deprecation")
public class TransparentCraftingTableBlock extends AbstractGlassBlock implements IHammerRemovable {

    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

    public TransparentCraftingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, Type.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asItem())) {
            return InteractionResult.PASS;
        }
        player.openMenu(state.getMenuProvider(level, pos));
        player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new CraftingMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
            Component.translatable("container.crafting")
        );
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        travelAndCheck(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        for (Direction direction : Utils.HORIZONTAL_DIRECTIONS) {
            travelAndCheck(level, pos.relative(direction));
        }
    }


    private static void travelAndCheck(Level level, BlockPos pos) {
        List<BlockPos> posList = new ArrayList<>();
        BlockPos.breadthFirstTraversal(
            pos,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Utils::acceptHorizontalDirections,
            blockPos -> {
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.is(ModBlocks.TRANSPARENT_CRAFTING_TABLE.get())) {
                    posList.add(blockPos);
                    return true;
                }
                return false;
            }
        );
        if (!posList.isEmpty()) {
            checkPosAndUpdate(level, posList);
        }
    }

    /**
     * 检查给定坐标是否是矩形，且更新他们的方块状态
     *
     * @param level   level
     * @param posList 方块列表
     */
    private static void checkPosAndUpdate(Level level, List<BlockPos> posList) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        int posY = posList.get(0).getY();

        for (BlockPos pos : posList) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // 检查区域内是否全部为水晶工作台
        boolean flag = true;
        for (int x = minX; x <= maxX && flag; x++) {
            for (int z = minZ; z <= maxZ && flag; z++) {
                BlockPos checkPos = new BlockPos(x, posY, z);
                if (!level.getBlockState(checkPos).is(ModBlocks.TRANSPARENT_CRAFTING_TABLE.get())) {
                    flag = false;
                }
            }
        }

        int width = maxX - minX + 1;
        int height = maxZ - minZ + 1;

        if (flag && width > 1 && height > 1) {
            // 修改状态
            // center
            for (int x = minX + 1; x <= maxX - 1; x++) {
                for (int z = minZ + 1; z <= maxZ - 1; z++) {
                    BlockPos pos = new BlockPos(x, posY, z);
                    level.setBlockAndUpdate(
                        pos,
                        ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.CENTER)
                    );
                }
            }

            // corner
            level.setBlockAndUpdate(
                new BlockPos(minX, posY, minZ),
                ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.CORNER_NORTH_WEST)
            );
            level.setBlockAndUpdate(
                new BlockPos(maxX, posY, minZ),
                ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.CORNER_NORTH_EAST)
            );
            level.setBlockAndUpdate(
                new BlockPos(minX, posY, maxZ),
                ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.CORNER_SOUTH_WEST)
            );
            level.setBlockAndUpdate(
                new BlockPos(maxX, posY, maxZ),
                ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.CORNER_SOUTH_EAST)
            );

            //side
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                level.setBlockAndUpdate(
                    new BlockPos(minX, posY, z),
                    ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.SIDE_WEST)
                );
                level.setBlockAndUpdate(
                    new BlockPos(maxX, posY, z),
                    ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.SIDE_EAST)
                );
            }
            for (int x = minX + 1; x <= maxX - 1; x++) {
                level.setBlockAndUpdate(
                    new BlockPos(x, posY, minZ),
                    ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.SIDE_NORTH)
                );
                level.setBlockAndUpdate(
                    new BlockPos(x, posY, maxZ),
                    ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState().setValue(TYPE, Type.SIDE_SOUTH)
                );
            }
        } else {
            // 恢复无连接状态
            for (BlockPos pos : posList) {
                level.setBlockAndUpdate(pos, ModBlocks.TRANSPARENT_CRAFTING_TABLE.getDefaultState());
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
