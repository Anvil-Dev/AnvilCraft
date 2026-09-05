package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 环形山密度函数：按多个图层（不同尺度）以网格单元随机撒布环形山，
 * 山脊为高斯凸起，坑内为平底洼地。
 *
 * <p>坑与坑重叠时（包括跨图层），落入新坑（优先级更高）内部的位置会被抹平，
 * 只保留新坑自身轮廓；坑外的山脊照常叠加，重叠处隆起更高。</p>
 *
 * <p>地形修饰仅作用于水平方向（忽略 y），竖向衰减在噪声设置 JSON 中组合。</p>
 *
 * @param noise  种子化噪声，用于每个单元的「是否存在 / 中心抖动 / 半径 / 强度 / 优先级」随机参数
 * @param layers 环形山图层列表，通常从大到小排列
 */
public record CraterDensityFunction(NoiseHolder noise, List<Layer> layers) implements DensityFunction {
    public static final MapCodec<CraterDensityFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            NoiseHolder.CODEC.fieldOf("noise").forGetter(CraterDensityFunction::noise),
            Layer.CODEC.listOf().fieldOf("layers").forGetter(CraterDensityFunction::layers)
        ).apply(instance, CraterDensityFunction::new)
    );
    private static final KeyDispatchDataCodec<CraterDensityFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** 单元随机参数的采样步长，使相邻单元的噪声采样尽量去相关。 */
    private static final double CELL_SAMPLE_STEP = 3.7;
    /** 存在判定阈值：单元噪声高于该值才生成环形山。 */
    private static final double PRESENCE_THRESHOLD = 0.15;
    /** 平底坑的地面平台占半径比例，平台之外向坑壁平滑抬升。 */
    private static final double FLAT_FLOOR_FRACTION = 0.55;
    /** 强度随机量的上界，用于估算值域。 */
    private static final double MAX_STRENGTH = 1.3;

    /**
     * 一个环形山图层的参数。
     *
     * @param cellSize  网格单元边长（格），需大于两倍 maxRadius
     * @param minRadius 环形山最小半径（格）
     * @param maxRadius 环形山最大半径（格）
     * @param rimHeight 山脊最大密度增量
     * @param bowlDepth 坑底最大密度减量
     * @param rimWidth  山脊高斯凸起半宽
     */
    public record Layer(
        int cellSize,
        double minRadius,
        double maxRadius,
        double rimHeight,
        double bowlDepth,
        double rimWidth
    ) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                Codec.INT.fieldOf("cell_size").forGetter(Layer::cellSize),
                Codec.DOUBLE.fieldOf("min_radius").forGetter(Layer::minRadius),
                Codec.DOUBLE.fieldOf("max_radius").forGetter(Layer::maxRadius),
                Codec.DOUBLE.fieldOf("rim_height").forGetter(Layer::rimHeight),
                Codec.DOUBLE.fieldOf("bowl_depth").forGetter(Layer::bowlDepth),
                Codec.DOUBLE.fieldOf("rim_width").forGetter(Layer::rimWidth)
            ).apply(instance, Layer::new)
        );
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        double rimSum = 0.0;
        CellCrater containing = null;
        double containingRim = 0.0;
        for (Layer layer : this.layers) {
            int cellX = Math.floorDiv(x, layer.cellSize);
            int cellZ = Math.floorDiv(z, layer.cellSize);
            // 山脊高斯尾可能越过单元边界，需检查 3x3 邻域
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                    CellCrater crater = this.cellCrater(layer, cx, cz);
                    if (crater == null) continue;
                    double distance = crater.distance(x, z);
                    double rimOffset = (distance - crater.radius) / layer.rimWidth;
                    double rim = layer.rimHeight * Math.exp(-rimOffset * rimOffset) * crater.strength;
                    if (distance < crater.radius) {
                        // 落入坑内：只保留优先级最高（最新）的坑
                        if (containing == null || crater.priority > containing.priority) {
                            containing = crater;
                            containingRim = rim;
                        }
                    } else {
                        rimSum += rim;
                    }
                }
            }
        }
        if (containing == null) return rimSum;
        // 新坑内部抹平旧坑轮廓：只保留本坑的山脊与平底洼地
        Layer layer = containing.layer;
        double distance = containing.distance(x, z);
        double wallFactor = Mth.clamp(
            (distance - containing.radius * FLAT_FLOOR_FRACTION)
                / (containing.radius * (1.0 - FLAT_FLOOR_FRACTION)),
            0.0,
            1.0
        );
        double wall = wallFactor * wallFactor * (3.0 - 2.0 * wallFactor);
        double bowl = -layer.bowlDepth * (1.0 - wall) * containing.strength;
        return containingRim + bowl;
    }

    /** 计算指定图层某单元的环形山参数；该单元没有环形山时返回 null。 */
    private @Nullable CellCrater cellCrater(Layer layer, int cellX, int cellZ) {
        double sampleX = cellX * CELL_SAMPLE_STEP + 0.5;
        double sampleZ = cellZ * CELL_SAMPLE_STEP + 0.5;
        // 同一图层同一单元内采样结果恒定，用不同偏移取六个独立随机量
        double presence = this.noise.getValue(sampleX, 17.0 + layer.cellSize, sampleZ);
        if (presence < PRESENCE_THRESHOLD) return null;
        double jitterX = this.noise.getValue(sampleX + 100.5, -43.0 + layer.cellSize, sampleZ + 100.5);
        double jitterZ = this.noise.getValue(sampleX + 200.5, 91.0 + layer.cellSize, sampleZ + 200.5);
        double radiusRoll = this.noise.getValue(sampleX + 300.5, 7.0 + layer.cellSize, sampleZ + 300.5);
        double strengthRoll = this.noise.getValue(sampleX + 400.5, -61.0 + layer.cellSize, sampleZ + 400.5);
        double priorityRoll = this.noise.getValue(sampleX + 500.5, -77.0 + layer.cellSize, sampleZ + 500.5);

        // 半径分布向小型偏斜（近似真实撞击坑的幂律分布）
        double radiusT = Mth.clamp((radiusRoll + 1.0) * 0.5, 0.0, 1.0);
        double radius = Mth.lerp(radiusT * radiusT, layer.minRadius, layer.maxRadius);
        // 每座环形山的坑深与脊高在 0.7–1.3 倍间随机
        double strength = 0.7 + 0.6 * Mth.clamp((strengthRoll + 1.0) * 0.5, 0.0, 1.0);
        double maxJitter = layer.cellSize * 0.5 - layer.maxRadius - 4.0;
        double centerX = (cellX + 0.5) * layer.cellSize + jitterX * maxJitter;
        double centerZ = (cellZ + 0.5) * layer.cellSize + jitterZ * maxJitter;
        return new CellCrater(layer, centerX, centerZ, radius, strength, priorityRoll);
    }

    /** 一个图层单元内的环形山参数。 */
    private record CellCrater(Layer layer, double centerX, double centerZ, double radius, double strength, double priority) {
        double distance(double x, double z) {
            return Math.hypot(x - this.centerX, z - this.centerZ);
        }
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public CraterDensityFunction mapAll(Visitor visitor) {
        return new CraterDensityFunction(visitor.visitNoise(this.noise), this.layers);
    }

    @Override
    public double minValue() {
        double maxBowlDepth = 0.0;
        for (Layer layer : this.layers) {
            maxBowlDepth = Math.max(maxBowlDepth, layer.bowlDepth);
        }
        return -maxBowlDepth * MAX_STRENGTH;
    }

    @Override
    public double maxValue() {
        // 山脊可在坑外叠加，按所有图层全部重叠估算上界
        double rimSum = 0.0;
        for (Layer layer : this.layers) {
            rimSum += layer.rimHeight;
        }
        return rimSum * MAX_STRENGTH;
    }

    @Override
    public KeyDispatchDataCodec<CraterDensityFunction> codec() {
        return CODEC;
    }
}
