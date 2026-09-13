package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public final class RuinsStructure {
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

    private RuinsStructure() {
    }

    public static boolean isMultipart(BlockState state) {
        return state.getBlock() instanceof AbstractMultiPartBlock<?> && !(state.getBlock() instanceof LargeCakeBlock);
    }

    public static boolean isMainPart(RuinsBlockEntity ruins) {
        BlockState state = ruins.getDisplayState();
        return !isMultipart(state) || ((AbstractMultiPartBlock<?>) state.getBlock()).isMainPart(state);
    }

    public static RuinsBlockEntity main(Level level, RuinsBlockEntity ruins) {
        BlockState state = ruins.getDisplayState();
        if (!isMultipart(state)) return ruins;
        BlockPos main = ((AbstractMultiPartBlock<?>) state.getBlock()).getMainPartPos(ruins.getBlockPos(), state);
        if (level.isLoaded(main) && level.getBlockEntity(main) instanceof RuinsBlockEntity candidate
            && candidate.getDisplayState().is(state.getBlock()) && isMainPart(candidate)) return candidate;
        return ruins;
    }

    public static boolean canInteract(Player player, RuinsBlockEntity ruins) {
        if (player.canInteractWithBlock(ruins.getBlockPos(), 1.0)) return true;
        for (BlockPos member : members(player.level(), ruins)) {
            if (player.canInteractWithBlock(member, 1.0)) return true;
        }
        return false;
    }

    public static void destroyOthers(Level level, RuinsBlockEntity ruins, boolean drops, @Nullable Entity actor) {
        if (level.isClientSide || REMOVING.get() || !isMultipart(ruins.getDisplayState())) return;
        REMOVING.set(true);
        try {
            for (BlockPos member : members(level, ruins)) {
                if (!member.equals(ruins.getBlockPos())) level.destroyBlock(member, drops, actor);
            }
        } finally {
            REMOVING.remove();
        }
    }

    private static List<BlockPos> members(Level level, RuinsBlockEntity ruins) {
        BlockState state = ruins.getDisplayState();
        if (!isMultipart(state)) return List.of(ruins.getBlockPos());
        return members(level, ruins.getBlockPos(), state, (AbstractMultiPartBlock<?>) state.getBlock());
    }

    private static <P extends Enum<P>> List<BlockPos> members(
        Level level, BlockPos pos, BlockState state, AbstractMultiPartBlock<P> block
    ) {
        BlockPos main = block.getMainPartPos(pos, state);
        List<BlockPos> result = new ArrayList<>();
        for (P part : block.getParts()) {
            BlockPos member = pos.offset(block.offsetFrom(state, part));
            if (!level.isLoaded(member) || !(level.getBlockEntity(member) instanceof RuinsBlockEntity candidate)) continue;
            BlockState display = candidate.getDisplayState();
            if (display.is(block) && block.getMainPartPos(member, display).equals(main)) result.add(member);
        }
        return result;
    }
}
