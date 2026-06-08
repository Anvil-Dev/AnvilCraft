package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class RedstoneEMPBehavior implements IAnvilBehavior {
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(
        new Vector3f(1.0f, 0.0f, 0.0f), 1.0f
    );
    private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(
        new Vector3f(1.0f, 0.3f, 0.0f), 0.8f
    );

    @Override
    public boolean handle(
        Level level,
        BlockPos pos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        int radius = AnvilCraft.CONFIG.redstoneEmpRadius;
        int maxRadius = AnvilCraft.CONFIG.redstoneEmpMaxRadius;
        int distance = Math.min(((int) Math.ceil(fallDistance)) * radius, maxRadius);
        if (!level.getBlockState(pos.relative(Direction.EAST)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = 1; x < distance; x++) {
                for (int z = -distance; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.WEST)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -1; x > -distance; x--) {
                for (int z = -distance; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.SOUTH)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -distance; x < distance; x++) {
                for (int z = 1; z < distance; z++) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        if (!level.getBlockState(pos.relative(Direction.NORTH)).is(Blocks.IRON_TRAPDOOR)) {
            for (int x = -distance; x < distance; x++) {
                for (int z = -1; z > -distance; z--) {
                    redstoneEmp(level, pos.offset(x, 0, z));
                }
            }
        }
        // 在红石块中心生成爆发粒子
        if (AnvilCraft.CLIENT_CONFIG.redstoneEmpParticlesEnabled) {
            spawnCenterBurst(level, pos, distance);
        }
        return false;
    }

    private void redstoneEmp(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlockTags.REDSTONE_TORCH)) return;
        state = state.setValue(RedstoneTorchBlock.LIT, false);
        level.setBlockAndUpdate(pos, state);
        // 在被熄灭的火把位置生成红色粒子
        if (AnvilCraft.CLIENT_CONFIG.redstoneEmpParticlesEnabled && level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                RED_DUST,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.25, 0.25, 0.25, 0.05
            );
        }
    }

    private void spawnCenterBurst(Level level, BlockPos pos, int distance) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // 中心爆发（立即）
        serverLevel.sendParticles(RED_DUST, cx, cy, cz, 30, 0.4, 0.2, 0.4, 0.2);

        // 逐环扩散（每 tick 推出一圈，模拟冲击波动态向外扩张）
        var server = serverLevel.getServer();
        Thread.startVirtualThread(() -> {
            for (int r = 1; r <= distance; r++) {
                int radius = r;
                int count = Math.max(8, Math.min((int) (Math.PI * radius * 1.5), 48));
                double angleOffset = radius * 0.7;
                DustParticleOptions particle = radius < distance * 0.4 ? RED_DUST : ORANGE_DUST;
                server.execute(() -> {
                    for (int i = 0; i < count; i += 2) {
                        double angle = 2 * Math.PI * i / count + angleOffset;
                        serverLevel.sendParticles(
                            particle,
                            cx + radius * Math.cos(angle),
                            cy + (radius % 2 == 0 ? 0.3 : 0.6),
                            cz + radius * Math.sin(angle),
                            2, 0.08, 0.08, 0.08, 0.0
                        );
                    }
                });
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }
}
