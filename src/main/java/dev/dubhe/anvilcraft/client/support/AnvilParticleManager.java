package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class AnvilParticleManager {
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
    private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.3f, 0.0f), 0.8f);
    private static final List<Shockwave> SHOCKWAVES = new ArrayList<>();
    @Nullable
    private static ClientLevel currentLevel;

    public static void redstoneEmp(ClientLevel level, BlockPos center, int radius, List<BlockPos> affectedTorches) {
        AnvilParticleManager.setLevel(level);
        for (BlockPos pos : affectedTorches) {
            AnvilParticleManager.spawn(level, AnvilParticleManager.RED_DUST, pos.getCenter(), 6, new Vec3(0.25, 0.25, 0.25), 0.05);
        }
        AnvilParticleManager.spawn(level, AnvilParticleManager.RED_DUST, center.getCenter(), 30, new Vec3(0.4, 0.2, 0.4), 0.2);
        AnvilParticleManager.SHOCKWAVES.add(new RedstoneEMPShockwave(center, radius));
    }

    public static void giantAnvilShock(ClientLevel level, BlockPos center, int radius) {
        AnvilParticleManager.setLevel(level);
        AnvilParticleManager.SHOCKWAVES.add(new GiantAnvilShockwave(center, radius));
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        AnvilParticleManager.setLevel(level);
        if (level == null || minecraft.isPaused()) return;
        AnvilParticleManager.SHOCKWAVES.removeIf(shockwave -> !shockwave.isEnabled() || shockwave.tick(level));
    }

    private static void setLevel(@Nullable ClientLevel level) {
        if (AnvilParticleManager.currentLevel == level) return;
        AnvilParticleManager.SHOCKWAVES.clear();
        AnvilParticleManager.currentLevel = level;
    }

    private static void spawn(ClientLevel level, ParticleOptions particle, Vec3 pos, int count, Vec3 spread, double speed) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            level.addParticle(
                particle,
                pos.x + random.nextGaussian() * spread.x,
                pos.y + random.nextGaussian() * spread.y,
                pos.z + random.nextGaussian() * spread.z,
                random.nextGaussian() * speed,
                random.nextGaussian() * speed,
                random.nextGaussian() * speed
            );
        }
    }

    private abstract static class Shockwave {
        protected final BlockPos center;
        protected final int radius;
        protected int age;
        protected int nextRing = 1;

        protected Shockwave(BlockPos center, int radius) {
            this.center = center;
            this.radius = radius;
        }

        public boolean tick(ClientLevel level) {
            this.age++;
            // 将原来每圈 30 ms 的延迟换算为客户端游戏刻，暂停时不继续推进。
            while (this.nextRing <= this.radius && this.nextRing * 3 <= this.age * 5) {
                this.spawnRing(level, this.nextRing++);
            }
            return this.nextRing > this.radius;
        }

        public abstract boolean isEnabled();

        public abstract void spawnRing(ClientLevel level, int ring);
    }

    private static class RedstoneEMPShockwave extends Shockwave {
        public RedstoneEMPShockwave(BlockPos center, int radius) {
            super(center, radius);
        }

        @Override
        public boolean isEnabled() {
            return AnvilCraft.CLIENT_CONFIG.effects.displayRedstoneEmpParticles;
        }

        @Override
        public void spawnRing(ClientLevel level, int ring) {
            int count = Math.clamp((int) (Math.PI * ring * 1.5), 8, 48);
            double angleOffset = ring * 0.7;
            DustParticleOptions particle = ring < this.radius * 0.4 ? AnvilParticleManager.RED_DUST : AnvilParticleManager.ORANGE_DUST;
            for (int i = 0; i < count; i += 2) {
                double angle = 2 * Math.PI * i / count + angleOffset;
                Vec3 pos = this.center.getCenter().add(
                    ring * Math.cos(angle),
                    ring % 2 == 0 ? 0.3 : 0.6,
                    ring * Math.sin(angle)
                );
                AnvilParticleManager.spawn(level, particle, pos, 2, new Vec3(0.08, 0.08, 0.08), 0.0);
            }
        }
    }

    private static class GiantAnvilShockwave extends Shockwave {
        public GiantAnvilShockwave(BlockPos center, int radius) {
            super(center, radius);
        }

        @Override
        public boolean isEnabled() {
            return AnvilCraft.CLIENT_CONFIG.effects.displayGiantAnvilShockParticles;
        }

        @Override
        public void spawnRing(ClientLevel level, int ring) {
            if (ring <= 1) return;
            int count = AnvilCraft.CLIENT_CONFIG.effects.giantAnvilShockParticlesCount;
            if (count <= 0) return;
            double chance = AnvilCraft.CLIENT_CONFIG.effects.giantAnvilShockParticlesChance;
            if (chance <= 0) return;
            double jumpHeight = 0.15 + (1.0 - (double) ring / this.radius) * 0.5;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    BlockPos pos = this.center.offset(dx, 0, dz);
                    if (level.getBlockState(pos).isAir() || level.random.nextFloat() >= chance) continue;
                    AnvilParticleManager.spawn(
                        level, ParticleTypes.POOF, pos.getCenter().add(0, 0.8, 0), count,
                        new Vec3(0.15, jumpHeight * 0.2, 0.15), 0.15 + jumpHeight * 0.2
                    );
                }
            }
        }
    }
}
