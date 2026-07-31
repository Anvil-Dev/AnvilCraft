package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TimeWarpPlayerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(ServerLevel level, BlockPos hitBlockPos, BlockState hitBlockState, double fallDistance, AnvilEvent.OnLand event) {
        boolean activeBeacon = CauldronUtil.getBottomPositions(hitBlockPos, hitBlockState).stream()
            .map(level::getBlockState)
            .anyMatch(TimeWarpPlayerBehavior::isActiveCorruptedBeacon);
        if (!activeBeacon) return false;
        List<ServerPlayer> players = level.getPlayers(
            player -> CauldronUtil.isEntityInside(hitBlockPos, hitBlockState, player)
        );
        if (players.isEmpty()) return false;
        for (ServerPlayer player : players) {
            EntityUtil.hurtOrSimulate(player, ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
        }
        return true;
    }

    private static boolean isActiveCorruptedBeacon(BlockState state) {
        return state.is(ModBlocks.CORRUPTED_BEACON) && state.getValue(CorruptedBeaconBlock.LIT);
    }
}
