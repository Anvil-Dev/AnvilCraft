package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 激光照射到方块时的命中特效：变体色粉尘与方块破坏粒子混合。
 *
 * <p>粉尘颜色取自 {@link BlockMiningEffect#getLaserColor()}，与光束渲染同源，
 * 因而普通/皇家（精准采集）/浮霜（瓦解）/余烬（熔炼）各变体自动对应各自颜色。
 *
 * <p>方块破坏粒子用 {@link ParticleTypes#BLOCK} 承载被照射方块的贴图，
 * 因而无需触发真实的方块破坏事件即可得到同款碎屑。
 */
public final class LaserParticleBehavior implements ILaserComponent {
    /** 每 N tick 发射一批，避免激光每 tick 触发造成粒子过载。 */
    private static final int EMIT_INTERVAL = 2;
    /** 基础扩散半径与速度（等级 1 时的手感）。 */
    private static final double BASE_SPREAD = 0.15;
    private static final double SPREAD_PER_SCALE = 0.25;
    private static final double SPEED = 0.03;
    private static final float DUST_SCALE = 1.0F;
    /** 基础粒子数（等级 1 时的手感）。 */
    private static final int BASE_DUST_COUNT = 2;
    private static final int BASE_BLOCK_COUNT = 3;
    private static final int DUST_PER_SCALE = 4;
    private static final int BLOCK_PER_SCALE = 7;
    /** 激光等级上限，与 {@code LaserCompiler.LASER_WIDTH} 的数组长度一致。 */
    private static final int MAX_LEVEL = 64;
    /**
     * 伽马激光颜色，与客户端 {@code LaserCompiler} 中的常量保持一致。
     * 伽马光束不采用透镜颜色，因此这里同样优先于 {@link BlockMiningEffect#getLaserColor()}。
     */
    private static final int GAMMA_COLOR = 0x991AFF;

    private int lastEmitTick = -1;
    /** 缓存当前变体颜色的粉尘参数，避免每批重复创建。 */
    private @Nullable DustParticleOptions dust;
    private int dustColor = -1;

    @Override
    public void onEmissionStopped(ILaserComponentOwner owner) {
        lastEmitTick = -1;
    }

    @Override
    public boolean onHitBlock(ILaserComponentOwner owner, Level level, BlockPos blockPos) {
        if (!(level instanceof ServerLevel serverLevel)) return true;
        int tick = owner.getLaserTicks();
        if (lastEmitTick >= 0 && tick - lastEmitTick < EMIT_INTERVAL) return true;
        lastEmitTick = tick;
        BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) return true;
        // 命中面：从方块中心朝激光来向偏移半个方块，使碎屑贴着被照的那一面
        Vec3 normal = Vec3.atLowerCornerOf(owner.getLaserDirection().getOpposite().getNormal());
        Vec3 center = Vec3.atCenterOf(blockPos).add(normal.scale(0.5 + 0.02));
        // 等级越高，粒子越多、扩散范围越大
        float scale = levelScale(LaserStrengthComponent.getStrength(owner));
        double spread = BASE_SPREAD + SPREAD_PER_SCALE * scale;
        serverLevel.sendParticles(
            this.dustFor(owner),
            center.x, center.y, center.z,
            BASE_DUST_COUNT + Math.round(DUST_PER_SCALE * scale),
            spread, spread, spread,
            SPEED
        );
        serverLevel.sendParticles(
            new BlockParticleOption(ParticleTypes.BLOCK, state),
            center.x, center.y, center.z,
            BASE_BLOCK_COUNT + Math.round(BLOCK_PER_SCALE * scale),
            spread, spread, spread,
            SPEED
        );
        return true;
    }

    /**
     * 将激光等级映射为 0..1 的强度系数。
     *
     * <p>沿用 {@code LaserCompiler} 中光束宽度的平方根曲线（width ∝ √level），
     * 使特效的扩散范围与光束粗细同步增长，而非随等级线性膨胀。
     * 等级 1 时为 0，即保持基础特效不变。
     */
    private static float levelScale(int laserLevel) {
        if (laserLevel <= 1) return 0.0F;
        int clamped = Math.min(laserLevel, MAX_LEVEL);
        return (float) ((Math.sqrt(clamped) - 1.0) / (Math.sqrt(MAX_LEVEL) - 1.0));
    }

    private DustParticleOptions dustFor(ILaserComponentOwner owner) {
        int color = LaserTypeComponent.isGamma(owner)
            ? GAMMA_COLOR
            : LaserMiningComponent.getEffect(owner).getLaserColor();
        if (this.dust == null || this.dustColor != color) {
            this.dustColor = color;
            this.dust = new DustParticleOptions(Vec3.fromRGB24(color).toVector3f(), DUST_SCALE);
        }
        return this.dust;
    }

    // 特效无配置，始终可与其他同类组件合并，保留连续照射的节流状态。
    @Override
    public boolean equals(Object other) {
        return other instanceof LaserParticleBehavior;
    }

    @Override
    public int hashCode() {
        return LaserParticleBehavior.class.hashCode();
    }
}
