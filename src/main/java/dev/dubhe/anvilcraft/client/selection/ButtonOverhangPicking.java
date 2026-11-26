package dev.dubhe.anvilcraft.client.selection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

/** 将越过宿主格的帽登记到它占用的区段，准星射线可以从侧面直接命中它。 */
public final class ButtonOverhangPicking {
    private static final Map<Level, SetMultimap<SectionPos, BlockPos>> LEVELS = new WeakHashMap<>();

    private ButtonOverhangPicking() {
    }

    public static void update(Level level, BlockPos pos, @Nullable AABB previous, @Nullable AABB next) {
        if (previous != null) {
            var index = LEVELS.get(level);
            if (index != null) {
                sections(previous).forEach(section -> index.remove(section, pos));
            }
        }
        if (next != null) {
            var index = LEVELS.computeIfAbsent(level, ignored -> HashMultimap.create());
            sections(next).forEach(section -> index.put(section, pos));
        }
    }

    public static HitResult pick(HitResult original, Entity camera, double range, float partialTick) {
        Level level = camera.level();
        var index = LEVELS.get(level);
        if (index == null || index.isEmpty()) {
            return original;
        }
        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 end = eye.add(camera.getViewVector(partialTick).scale(range));
        double distance = Math.min(range * range, eye.distanceToSqr(original.getLocation()));
        Set<BlockPos> candidates = new HashSet<>();
        BlockGetter.traverseBlocks(eye.scale(1d / 16), end.scale(1d / 16), candidates, (found, cell) -> {
            found.addAll(index.get(SectionPos.of(cell.getX(), cell.getY(), cell.getZ())));
            return null;
        }, found -> null);
        HitResult nearest = original;
        for (BlockPos pos : candidates) {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BigRedButtonBlock)) {
                continue;
            }
            BlockHitResult hit = BigRedButtonBlock.clipCap(state, level, pos, eye, end);
            if (hit == null || new AABB(pos).inflate(1E-6).contains(hit.getLocation())) {
                continue;
            }
            double hitDistance = eye.distanceToSqr(hit.getLocation());
            if (hitDistance < distance) {
                distance = hitDistance;
                nearest = hit;
            }
        }
        return nearest;
    }

    private static Stream<SectionPos> sections(AABB area) {
        return SectionPos.betweenClosedStream(
            Mth.floor(area.minX) >> 4, Mth.floor(area.minY) >> 4, Mth.floor(area.minZ) >> 4,
            Mth.floor(area.maxX) >> 4, Mth.floor(area.maxY) >> 4, Mth.floor(area.maxZ) >> 4
        );
    }
}
