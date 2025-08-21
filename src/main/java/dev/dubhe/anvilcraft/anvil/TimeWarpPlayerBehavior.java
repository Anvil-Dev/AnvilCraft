package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TimeWarpPlayerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        BlockState below = level.getBlockState(hitBlockPos.below());
        if (!below.is(ModBlocks.CORRUPTED_BEACON)) return false;
        if (!below.getValue(CorruptedBeaconBlock.LIT)) return false;
        List<ServerPlayer> players = serverLevel.getPlayers(player -> player.getOnPos().equals(hitBlockPos));
        if (players.isEmpty()) return false;
        for (ServerPlayer player : players) player.hurt(ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
        return true;
    }
}
