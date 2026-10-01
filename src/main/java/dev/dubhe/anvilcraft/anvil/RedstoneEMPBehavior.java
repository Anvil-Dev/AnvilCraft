package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.network.RedstoneEmpEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RedstoneEMPBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos pos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        List<BlockPos> affectedTorches = new ArrayList<>();
        int radius = AnvilCraft.CONFIG.redstoneEmpRadius;
        int maxRadius = AnvilCraft.CONFIG.redstoneEmpMaxRadius;
        int distance = Math.min(((int) Math.ceil(fallDistance)) * radius, maxRadius);
        if (!level.getBlockState(pos.relative(Direction.EAST)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = 1; x < distance; x++) {
                for (int z = -distance; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z), affectedTorches);
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.WEST)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -1; x > -distance; x--) {
                for (int z = -distance; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z), affectedTorches);
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.SOUTH)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -distance; x < distance; x++) {
                for (int z = 1; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z), affectedTorches);
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.NORTH)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -distance; x < distance; x++) {
                for (int z = -1; z > -distance; z--) {
                    redstoneEmp(level, pos.offset(x, 0, z), affectedTorches);
                }
            }
        }
        PacketDistributor.sendToPlayersNear(
            serverLevel, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            distance * Math.sqrt(2) + 32,
            new RedstoneEmpEffectPacket(pos, distance, affectedTorches)
        );
        return false;
    }

    private void redstoneEmp(Level level, BlockPos pos, List<BlockPos> affectedTorches) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlockTags.REDSTONE_TORCH)) return;
        state = state.setValue(RedstoneTorchBlock.LIT, false);
        level.setBlockAndUpdate(pos, state);
        affectedTorches.add(pos);
    }
}
