package dev.dubhe.anvilcraft.porting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared source/native sampler of generated terrain and actual fall damage. */
public final class MunTerrainSamples {
    public static List<String> sample(ServerLevel level) {
        List<String> rows = new ArrayList<>();
        rows.add("seed," + level.getSeed());
        for (int[] chunk : new int[][]{{64, 0}, {96, -8}, {-80, 32}, {12, 40}}) {
            level.getChunk(chunk[0], chunk[1]);
            for (int x = chunk[0] * 16; x < chunk[0] * 16 + 16; x++) {
                for (int z = chunk[1] * 16; z < chunk[1] * 16 + 16; z++) {
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                    rows.add("terrain," + x + "," + z + "," + y + "," + block(level, x, y, z)
                        + "," + block(level, x, y - 1, z) + "," + block(level, x, 36, z)
                        + "," + block(level, x, 0, z) + "," + block(level, x, -64, z));
                }
            }
        }
        for (float distance : new float[]{20, 20.1F, 20.99F, 21, 26, 27, 33, 80}) {
            for (float multiplier : new float[]{0, 0.5F, 1, 2}) {
                for (double safe : new double[]{3, 5}) {
                    for (double damageMultiplier : new double[]{0.5, 1}) {
                        var pig = EntityType.PIG.create(level, EntitySpawnReason.COMMAND);
                        if (pig == null) throw new IllegalStateException("Unable to create fall-damage sample");
                        try {
                            Objects.requireNonNull(pig.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1000);
                            Objects.requireNonNull(pig.getAttribute(Attributes.SAFE_FALL_DISTANCE)).setBaseValue(safe);
                            Objects.requireNonNull(pig.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER)).setBaseValue(damageMultiplier);
                            pig.setHealth(1000);
                            pig.setPos(1030, 100, 4);
                            pig.causeFallDamage(distance, multiplier, level.damageSources().fall());
                            rows.add("fall," + distance + "," + multiplier + "," + safe + "," + damageMultiplier
                                + "," + (1000 - pig.getHealth()));
                        } finally {
                            pig.discard();
                        }
                    }
                }
            }
        }
        return rows;
    }

    private static String block(ServerLevel level, int x, int y, int z) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(new BlockPos(x, y, z)).getBlock()).toString();
    }
}
