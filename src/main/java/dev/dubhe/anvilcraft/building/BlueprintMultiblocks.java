package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.PistonType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/** 核心选择与部件展开只用于生成完整蓝图；部署只变换已保存的每一格。 */
public final class BlueprintMultiblocks {
    private BlueprintMultiblocks() {
    }

    public record PlacedBlock(BlockPos pos, BlockState state, Optional<CompoundTag> nbt) {
    }

    public static boolean shouldRecord(BlockState state) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block) return block.isMainPart(state);
        if (isDoubleBlock(state)) return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
        if (state.getBlock() instanceof BedBlock) return state.getValue(BedBlock.PART) == BedPart.FOOT;
        return !(state.getBlock() instanceof PistonHeadBlock);
    }

    public static BlockPos core(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block) return block.getMainPartPos(pos, state);
        if (isDoubleBlock(state) && !shouldRecord(state)) return pos.below();
        if (state.getBlock() instanceof BedBlock && !shouldRecord(state)) {
            return pos.relative(state.getValue(BedBlock.FACING).getOpposite());
        }
        if (state.getBlock() instanceof PistonHeadBlock) {
            return pos.relative(state.getValue(PistonHeadBlock.FACING).getOpposite());
        }
        return pos;
    }

    private static boolean isDoubleBlock(BlockState state) {
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof DoublePlantBlock;
    }

    public static void forEachPart(BlockPos pos, BlockState state, BiConsumer<BlockPos, BlockState> consumer) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block && block.isMainPart(state)) {
            addParts(pos, state, block, consumer);
            return;
        }
        consumer.accept(pos, state);
        if (!shouldRecord(state)) return;
        if (isDoubleBlock(state)) {
            consumer.accept(pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        } else if (state.getBlock() instanceof BedBlock) {
            consumer.accept(pos.relative(state.getValue(BedBlock.FACING)), state.setValue(BedBlock.PART, BedPart.HEAD));
        } else if (state.getBlock() instanceof PistonBaseBlock && state.getValue(PistonBaseBlock.EXTENDED)) {
            consumer.accept(pos.relative(state.getValue(PistonBaseBlock.FACING)), Blocks.PISTON_HEAD.defaultBlockState()
                .setValue(PistonHeadBlock.FACING, state.getValue(PistonBaseBlock.FACING))
                .setValue(PistonHeadBlock.TYPE, state.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT));
        }
    }

    public static void forEachPart(
        BlockPos pos, BlockState state, BlueprintPlacement placement, BiConsumer<BlockPos, BlockState> consumer
    ) {
        forEachPart(pos, state, (partPos, partState) -> consumer.accept(placement.worldOf(partPos), placement.stateOf(partState)));
    }

    private static <P extends Enum<P>> void addParts(
        BlockPos pos, BlockState state, AbstractMultiPartBlock<P> block, BiConsumer<BlockPos, BlockState> consumer
    ) {
        for (P part : block.getParts()) consumer.accept(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state));
    }

    public static List<PlacedBlock> expand(StructureSnapshot snapshot, BlueprintPlacement placement, int layer) {
        List<PlacedBlock> blocks = new ArrayList<>();
        for (var entry : snapshot.blocks()) {
            if (layer >= 0 && entry.pos().getY() != layer) continue;
            BlockState state = snapshot.stateOf(entry);
            if (OrdinaryBlockAdapter.mapping(state) == OrdinaryBlockAdapter.Mapping.AIR) continue;
            blocks.add(new PlacedBlock(placement.worldOf(entry.pos()), placement.stateOf(state), entry.nbt()));
        }
        return List.copyOf(blocks);
    }
}
