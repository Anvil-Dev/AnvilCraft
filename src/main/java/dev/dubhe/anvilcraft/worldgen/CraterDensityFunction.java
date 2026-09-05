package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * 环形山密度函数：以网格单元为基础随机撒布环形山，
 * 山脊为高斯凸起，坑内为平底洼地。
 *
 * <p>地形修饰仅作用于水平方向（忽略 y），竖向衰减在噪声设置 JSON 中组合。</p>
 *
 * @param noise     种子化噪声，用于每个单元的「是否存在 / 中心抖动 / 半径」随机参数
 * @param cellSize  网格单元边长（格），需大于两倍 maxRadius
 * @param minRadius 环形山最小半径（格）
 * @param maxRadius 环形山最大半径（格）
 * @param rimHeight 山脊最大密度增量
 * @param bowlDepth 坑底最大密度减量
 * @param rimWidth  山脊高斯凸起半宽
 */
public record CraterDensityFunction(
    NoiseHolder noise,
    int cellSize,
    double minRadius,
    double maxRadius,
    double rimHeight,
    double bowlDepth,
    double rimWidth
) implements DensityFunction {
    public static final MapCodec<CraterDensityFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("noise").forGetter(CraterDensityFunction::noise),
            Codec.INT.fieldOf("cell_size").forGetter(CraterDensityFunction::cellSize),
            Codec.DOUBLE.fieldOf("min_radius").forGetter(CraterDensityFunction::minRadius),
            Codec.DOUBLE.fieldOf("max_radius").forGetter(CraterDensityFunction::maxRadius),
            Codec.DOUBLE.fieldOf("rim_height").forGetter(CraterDensityFunction::rimHeight),
            Codec.DOUBLE.fieldOf("bowl_depth").forGetter(CraterDensityFunction::bowlDepth),
            Codec.DOUBLE.fieldOf("rim_width").forGetter(CraterDensityFunction::rimWidth)
        ).apply(instance, CraterDensityFunction::new)
    );
    private static final KeyDispatchDataCodec<CraterDensityFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** 单元随机参数的采样步长，使相邻单元的噪声采样尽量去相关。 */
    private static final double CELL_SAMPLE_STEP = 3.7;
    /** 存在判定阈值：单元噪声高于该值才生成环形山。 */
    private static final double PRESENCE_THRESHOLD = 0.15;
    /** 平底坑的地面平台占半径比例，平台之外向坑壁平滑抬升。 */
    private static final double FLAT_FLOOR_FRACTION = 0.55;

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        int cellX = Math.floorDiv(x, this.cellSize);
        int cellZ = Math.floorDiv(z, this.cellSize);
        double sampleX = cellX * CELL_SAMPLE_STEP + 0.5;
        double sampleZ = cellZ * CELL_SAMPLE_STEP + 0.5;
        // 同一单元内采样结果恒定，用不同偏移取四个独立随机量
        double presence = this.noise.getValue(sampleX, 17.0, sampleZ);
        if (presence < PRESENCE_THRESHOLD) return 0.0;
        double jitterX = this.noise.getValue(sampleX + 100.5, -43.0, sampleZ + 100.5);
        double jitterZ = this.noise.getValue(sampleX + 200.5, 91.0, sampleZ + 200.5);
        double radiusRoll = this.noise.getValue(sampleX + 300.5, 7.0, sampleZ + 300.5);
        double strengthRoll = this.noise.getValue(sampleX + 400.5, -61.0, sampleZ + 400.5);

        // 半径分布向小型偏斜（近似真实撞击坑的幂律分布）
        double radiusT = Mth.clamp((radiusRoll + 1.0) * 0.5, 0.0, 1.0);
        double radius = Mth.lerp(radiusT * radiusT, this.minRadius, this.maxRadius);
        // 每座环形山的坑深与脊高在 0.7–1.3 倍间随机
        double strength = 0.7 + 0.6 * Mth.clamp((strengthRoll + 1.0) * 0.5, 0.0, 1.0);
        double maxJitter = this.cellSize * 0.5 - this.maxRadius - 4.0;
        double centerX = (cellX + 0.5) * this.cellSize + jitterX * maxJitter;
        double centerZ = (cellZ + 0.5) * this.cellSize + jitterZ * maxJitter;

        double dx = x - centerX;
        double dz = z - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        // 山脊：半径处的高斯凸起
        double rimOffset = (distance - radius) / this.rimWidth;
        double rim = this.rimHeight * Math.exp(-rimOffset * rimOffset);
        // 坑内：中心为平地，外侧向坑壁平滑抬升
        double wallFactor = Mth.clamp(
            (distance - radius * FLAT_FLOOR_FRACTION) / (radius * (1.0 - FLAT_FLOOR_FRACTION)),
            0.0,
            1.0
        );
        double wall = wallFactor * wallFactor * (3.0 - 2.0 * wallFactor);
        double bowl = distance < radius ? -this.bowlDepth * (1.0 - wall) : 0.0;
        return (rim + bowl) * strength;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public CraterDensityFunction mapAll(Visitor visitor) {
        return new CraterDensityFunction(
            visitor.visitNoise(this.noise),
            this.cellSize,
            this.minRadius,
            this.maxRadius,
            this.rimHeight,
            this.bowlDepth,
            this.rimWidth
        );
    }

    @Override
    public double minValue() {
        // 乘以强度随机量的上界 1.3
        return -this.bowlDepth * 1.3;
    }

    @Override
    public double maxValue() {
        return this.rimHeight * 1.3;
    }

    @Override
    public KeyDispatchDataCodec<CraterDensityFunction> codec() {
        return CODEC;
    }
}
