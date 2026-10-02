package dev.dubhe.anvilcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StructureVoidBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * 普通方块材料映射:有放置物品的方块消耗一份对应物品,朝向等属性随蓝图恢复;
 * 门上半、床头、活塞头,以及成对大箱子的左半是依附格,不重复扣料。
 * 成对大箱子的右半一次扣两只箱子;落单的 LEFT/RIGHT 仍按单箱处理并在提交时写成 SINGLE。
 */
public final class OrdinaryBlockAdapter {
    public enum Mapping {
        AIR,
        PLACE,
        ATTACHED,
        UNSUPPORTED
    }

    private OrdinaryBlockAdapter() {
    }

    public static Mapping mapping(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof StructureVoidBlock) {
            return Mapping.AIR;
        }
        if (state.getBlock() instanceof LiquidBlock) {
            return Mapping.UNSUPPORTED;
        }
        if (isAttachedHalf(state)) {
            return Mapping.ATTACHED;
        }
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return Mapping.UNSUPPORTED;
        }
        return Mapping.PLACE;
    }

    public static ItemStack material(BlockState state) {
        if (mapping(state) != Mapping.PLACE) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(state.getBlock().asItem());
    }

    public static ItemStack pairedChestMaterial(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, 2);
    }

    public static boolean isAttachedHalf(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)
            && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return true;
        }
        return state.getBlock() instanceof PistonHeadBlock;
    }

    public static boolean isDoubleChestHalf(BlockState state) {
        return state.hasProperty(BlockStateProperties.CHEST_TYPE)
            && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE
            && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING);
    }

    public static boolean isPairedChestAttached(BlockPos pos, BlockState state, Map<Long, BlockState> declared) {
        return isDoubleChestHalf(state)
            && state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.LEFT
            && isComplementary(state, declared.get(partnerPos(pos, state).asLong()));
    }

    public static boolean isPairedChestCore(BlockPos pos, BlockState state, Map<Long, BlockState> declared) {
        return isDoubleChestHalf(state)
            && state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.RIGHT
            && isComplementary(state, declared.get(partnerPos(pos, state).asLong()));
    }

    public static BlockPos partnerPos(BlockPos pos, BlockState state) {
        return pos.relative(connectedDirection(state));
    }

    /**
     * 规划里没有成对另一半时,投影按单箱画,避免 BER 画出半个大箱子。
     */
    public static BlockState projectionState(BlockState target, BlockPos pos, Map<Long, BlockState> overlay) {
        if (!isDoubleChestHalf(target)) {
            return target;
        }
        if (isComplementary(target, overlay.get(partnerPos(pos, target).asLong()))) {
            return target;
        }
        return target.setValue(BlockStateProperties.CHEST_TYPE, ChestType.SINGLE);
    }

    /**
     * 取消或提交时,另一半还没交付就把 LEFT/RIGHT 收成 SINGLE,不要把半个大箱子写进世界。
     */
    public static BlockState commitState(BlockState target, BlockPos pos, Set<Long> delivered) {
        if (!isDoubleChestHalf(target)) {
            return target;
        }
        if (delivered.contains(partnerPos(pos, target).asLong())) {
            return target;
        }
        return target.setValue(BlockStateProperties.CHEST_TYPE, ChestType.SINGLE);
    }

    static Direction connectedDirection(BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return switch (state.getValue(BlockStateProperties.CHEST_TYPE)) {
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
            case SINGLE -> facing;
        };
    }

    private static boolean isComplementary(BlockState state, @Nullable BlockState partner) {
        if (partner == null || partner.getBlock() != state.getBlock() || !isDoubleChestHalf(partner)) {
            return false;
        }
        if (partner.getValue(BlockStateProperties.CHEST_TYPE) != state.getValue(BlockStateProperties.CHEST_TYPE).getOpposite()) {
            return false;
        }
        if (partner.getValue(BlockStateProperties.HORIZONTAL_FACING)
            != state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            return false;
        }
        return connectedDirection(partner) == connectedDirection(state).getOpposite();
    }
}
