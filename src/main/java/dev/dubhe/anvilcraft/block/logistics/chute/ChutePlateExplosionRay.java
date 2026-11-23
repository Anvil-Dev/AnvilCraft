package dev.dubhe.anvilcraft.block.logistics.chute;

import dev.dubhe.anvilcraft.block.entity.ChutePlates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 按原版爆炸的连续采样点检查线段，使斜穿方块转角的射线也能命中铁板。 */
public class ChutePlateExplosionRay {
    private final ServerLevel level;
    private final Vec3 center;
    private long section = Long.MIN_VALUE;
    private boolean sectionHasChutes;
    private double previousX;
    private double previousY;
    private double previousZ;

    public ChutePlateExplosionRay(ServerLevel level, Vec3 center) {
        this.level = level;
        this.center = center;
    }

    public boolean isBlocked(BlockPos pos, double x, double y, double z) {
        boolean start = x == this.center.x && y == this.center.y && z == this.center.z;
        long current = SectionPos.asLong(pos);
        boolean crossedSection = current != this.section;
        if (crossedSection) {
            this.section = current;
            var chunk = this.level.getChunkAt(pos);
            this.sectionHasChutes = chunk.getSection(chunk.getSectionIndex(pos.getY())).maybeHas(ChutePlateExplosionRay::isChute);
        }
        boolean blocked = false;
        if (this.sectionHasChutes || crossedSection && !start) {
            Vec3 to = new Vec3(x, y, z);
            Vec3 from = start ? to : new Vec3(this.previousX, this.previousY, this.previousZ);
            blocked = this.hitsPlate(from, to, pos);
        }
        this.previousX = x;
        this.previousY = y;
        this.previousZ = z;
        return blocked;
    }

    private boolean hitsPlate(Vec3 from, Vec3 to, BlockPos end) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(from), end)) {
            if (isChute(this.level.getBlockState(pos))
                && this.level.getBlockEntity(pos) instanceof ChutePlates.Holder chute
                && chute.getChutePlates().blocksExplosionRay(from, to)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isChute(BlockState state) {
        return state.getBlock() instanceof SimpleChuteBlock || state.getBlock() instanceof SimpleMagneticChuteBlock
            || state.getBlock() instanceof ChuteBlock || state.getBlock() instanceof MagneticChuteBlock;
    }
}
