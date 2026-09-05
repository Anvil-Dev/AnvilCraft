package dev.dubhe.anvilcraft.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 环形山密度函数：按多个图层（不同尺度）以网格单元随机撒布环形山，
 * 山脊为高斯凸起，坑内为平底洼地。
 *
 * <p>坑与坑重叠时（包括跨图层）按优先级从旧到新依次施加：
 * 新坑以坑心处的旧地形为基准向下挖掘坑底、在旧地形上堆叠山脊，
 * 因此新坑不会把旧坑内部抬回基准地面，也不会残留旧坑轮廓；
 * 坑外的山脊照常叠加，重叠处隆起更高。</p>
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
    /** 完全嵌套在其他坑内部的环形山，山脊高度按此系数削弱。 */
    private static final double NESTED_RIM_FACTOR = 0.2;

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
        List<CellCrater> containing = new ArrayList<>();
        List<CellCrater> outside = new ArrayList<>();
        for (Layer layer : this.layers) {
            int cellX = Math.floorDiv(x, layer.cellSize);
            int cellZ = Math.floorDiv(z, layer.cellSize);
            // 山脊高斯尾可能越过单元边界，需检查 3x3 邻域
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                    CellCrater crater = this.cellCrater(layer, cx, cz);
                    if (crater == null) continue;
                    if (crater.distance(x, z) < crater.radius) {
                        containing.add(crater);
                    } else {
                        outside.add(crater);
                    }
                }
            }
        }
        if (containing.isEmpty()) {
            // 坑外：所有坑的山脊照常叠加，重叠处隆起更高
            double rimSum = 0.0;
            for (CellCrater crater : outside) {
                rimSum += this.rim(crater, x, z);
            }
            return rimSum;
        }
        // 坑内：按优先级从旧到新依次施加，新坑以坑心处旧地形为基准向下挖掘、向上堆山脊
        containing.sort(Comparator.comparingDouble(CellCrater::priority));
        double winnerPriority = containing.getLast().priority();
        double height = 0.0;
        for (CellCrater crater : containing) {
            double distance = crater.distance(x, z);
            // 完全嵌套在其他坑内部的小坑仅保留微弱山脊，呈现为浅凹陷而非环形围墙
            double rim = this.rim(crater, x, z) * (this.isNested(crater) ? NESTED_RIM_FACTOR : 1.0);
            double wallFactor = Mth.clamp(
                (distance - crater.radius * FLAT_FLOOR_FRACTION)
                    / (crater.radius * (1.0 - FLAT_FLOOR_FRACTION)),
                0.0,
                1.0
            );
            double wall = wallFactor * wallFactor * (3.0 - 2.0 * wallFactor);
            double bowl = -crater.layer.bowlDepth * (1.0 - wall) * crater.strength;
            double floorLevel = this.referenceDelta(crater) + bowl;
            double ejectaLevel = height + rim;
            height = Mth.lerp(wall, floorLevel, ejectaLevel);
        }
        // 比最新坑更新的坑，山脊仍叠加在最上方；更旧的坑轮廓已被抹除
        for (CellCrater crater : outside) {
            if (crater.priority > winnerPriority) height += this.rim(crater, x, z);
        }
        return height;
    }

    /** 山脊：半径处的高斯凸起。 */
    private double rim(CellCrater crater, double x, double z) {
        double rimOffset = (crater.distance(x, z) - crater.radius) / crater.layer.rimWidth;
        return crater.layer.rimHeight * Math.exp(-rimOffset * rimOffset) * crater.strength;
    }

    /** 估算一座环形山坑心处的旧地形增量：所有更旧环形山在坑心的轮廓之和（单级近似）。 */
    private double referenceDelta(CellCrater crater) {
        double delta = 0.0;
        for (Layer layer : this.layers) {
            int cellX = Math.floorDiv(Mth.floor(crater.centerX), layer.cellSize);
            int cellZ = Math.floorDiv(Mth.floor(crater.centerZ), layer.cellSize);
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                    if (layer == crater.layer && cx == crater.cellX && cz == crater.cellZ) continue;
                    CellCrater other = this.cellCrater(layer, cx, cz);
                    if (other == null || other.priority >= crater.priority) continue;
                    delta += this.rim(other, crater.centerX, crater.centerZ);
                    double distance = other.distance(crater.centerX, crater.centerZ);
                    if (distance >= other.radius) continue;
                    double wallFactor = Mth.clamp(
                        (distance - other.radius * FLAT_FLOOR_FRACTION)
                            / (other.radius * (1.0 - FLAT_FLOOR_FRACTION)),
                        0.0,
                        1.0
                    );
                    double wall = wallFactor * wallFactor * (3.0 - 2.0 * wallFactor);
                    delta -= other.layer.bowlDepth * (1.0 - wall) * other.strength;
                }
            }
        }
        return delta;
    }

    /** 判断一座环形山是否被另一座更大的环形山完全包含。 */
    private boolean isNested(CellCrater crater) {
        for (Layer layer : this.layers) {
            int cellX = Math.floorDiv(Mth.floor(crater.centerX), layer.cellSize);
            int cellZ = Math.floorDiv(Mth.floor(crater.centerZ), layer.cellSize);
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                for (int cz = cellZ - 1; cz <= cellZ + 1; cz++) {
                    if (layer == crater.layer && cx == crater.cellX && cz == crater.cellZ) continue;
                    CellCrater other = this.cellCrater(layer, cx, cz);
                    if (other == null || other.radius <= crater.radius) continue;
                    if (other.distance(crater.centerX, crater.centerZ) + crater.radius <= other.radius) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        return new CellCrater(layer, cellX, cellZ, centerX, centerZ, radius, strength, priorityRoll);
    }

    /** 一个图层单元内的环形山参数。 */
    private record CellCrater(
        Layer layer,
        int cellX,
        int cellZ,
        double centerX,
        double centerZ,
        double radius,
        double strength,
        double priority
    ) {
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
