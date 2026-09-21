package dev.dubhe.anvilcraft.fluid;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * 水泥流体在世界中的行为。
 *
 * <p>由随机刻驱动，每次只做常数次邻格查询，不做全区扫描：</p>
 * <ul>
 *   <li>源头被随机刻选中，且自己的流动部分已经流到了更低一格时，源头整体挪下去
 *       （流动会先向下淌，源头再跟着沉降）；</li>
 *   <li>源头无处可下移时，有 {@value #SOLIDIFY_CHANCE} 概率连同紧贴它的两层流动水泥
 *       一起凝固成对应颜色的原版混凝土；</li>
 *   <li>接触糖块的源头不凝固；接触粘液块的源头不下移；接触蜂蜜块的源头两者都不。</li>
 * </ul>
 */
public abstract class CementFluid extends BaseFlowingFluid {
    /** 源头静止时凝固的概率。 */
    private static final float SOLIDIFY_CHANCE = 0.25F;
    /**
     * 会跟随源头一起凝固的两圈流动水泥，以流体自身的 amount 表示。
     *
     * <p>流动水泥的 amount 自源头向外逐圈递减：源头为 8，第一圈 7，第二圈 6。
     * 换算成方块状态即 {@code LEVEL} 的 1 与 2（0 是源头）。</p>
     */
    private static final int SOLIDIFY_AMOUNT_RING_1 = 7;
    private static final int SOLIDIFY_AMOUNT_RING_2 = 6;
    /**
     * 向下寻路时最多访问的格数，用于给单次随机刻的查询量封顶。
     *
     * <p>只沿同色水泥自身的连通水体扩展，正常台阶上很快就能命中，这个上限只是兜底。</p>
     */
    private static final int MAX_SETTLE_SEARCH = 64;

    private final Color color;
    /** 对应的原版混凝土，首次凝固时解析并缓存，避免在注册阶段触碰方块注册表。 */
    @Nullable
    private Block concrete;
    /** 上一次随机刻的位置/维度/游戏刻，用于识别原版对同一格的重复派发。 */
    @Nullable
    private BlockPos lastTickPos;
    @Nullable
    private Level lastTickLevel;
    private long lastTickTime = Long.MIN_VALUE;

    protected CementFluid(Properties properties, Color color) {
        super(properties);
        this.color = color;
    }

    @Override
    protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        // 传入的 state 是派发前捕获的快照。原版 ServerLevel#tickChunk 对同一格会连续派发两次
        // 随机刻：先经方块（LiquidBlock 再转交流体），再经流体状态；水泥方块的随机刻资格又
        // 委托给流体，于是两次都会落到这里，且携带同一份旧快照。若第一次已把源头挪走，
        // 第二次照着旧快照就会在别处再生成一个源头，源头因而成倍增殖。
        // 故这里一律以实时状态为准：位置已非本流体的源头就立刻放弃。
        FluidState current = level.getFluidState(pos);
        if (!current.isSource() || current.getType() != this) {
            return;
        }
        // 两次派发都落在同一游戏刻的同一格，第二次视为重复，避免凝固判定被掷两次
        if (this.isDuplicateDispatch(level, pos)) {
            return;
        }
        boolean sugar = false;
        boolean sticky = false;
        boolean slick = false;
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(ModBlocks.SUGAR_BLOCK.get())) {
                sugar = true;
            } else if (neighbor.is(Blocks.SLIME_BLOCK)) {
                sticky = true;
            } else if (neighbor.is(Blocks.HONEY_BLOCK)) {
                slick = true;
            }
        }
        // 蜂蜜块最“黏”：既不凝固也不下移；粘液块只拦住下移；糖块只拦住凝固
        if (!sticky && !slick) {
            BlockPos destination = this.findLowerFlow(level, pos);
            if (destination != null) {
                this.moveDown(level, pos, current, destination);
                return;
            }
        }
        if (!sugar && !slick && random.nextFloat() < SOLIDIFY_CHANCE) {
            this.solidify(level, pos);
        }
    }

    /**
     * 记录并识别「同一游戏刻、同一位置」的重复随机刻派发。
     *
     * <p>不去重的话，凝固判定每刻会被掷两次，实际概率从
     * {@value #SOLIDIFY_CHANCE} 抬高到约 44%，与规格不符。</p>
     *
     * @return 本次是否属于重复派发
     */
    private boolean isDuplicateDispatch(Level level, BlockPos pos) {
        long time = level.getGameTime();
        if (this.lastTickLevel == level && this.lastTickTime == time && pos.equals(this.lastTickPos)) {
            return true;
        }
        this.lastTickLevel = level;
        this.lastTickPos = pos.immutable();
        this.lastTickTime = time;
        return false;
    }

    /**
     * 找出源头可以挪下去的格子：沿同色水泥自身的连通水体做广度优先，
     * 取最近的一个 {@code y-1} 流动格。
     *
     * <p>「沿连通水体」是关键。若改为按半径平铺搜索，金字塔这类水流交错的地形上，
     * 源头会够到旁边那股不属于自己的水，于是到处乱窜、反复重新铺开。沿自己淌出的
     * 那股走，就近下沉，不会去够别处的水面。</p>
     *
     * <p>只用源头所在层与其下一层，且访问的格子数由 {@link #MAX_SETTLE_SEARCH} 封顶，
     * 因此单次随机刻的查询量有固定上界，不随水泥摊开的面积增长。</p>
     *
     * @return 可用的目标格，没有时返回 {@code null}
     */
    @Nullable
    private BlockPos findLowerFlow(Level level, BlockPos pos) {
        int sourceY = pos.getY();
        int targetY = sourceY - 1;
        Set<BlockPos> visited = new ObjectOpenHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(pos);
        queue.add(pos);
        int expanded = 0;
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                int y = next.getY();
                // 只关心源头所在层（横向铺开）与下一层（落点），其余方向不必查
                if (y != targetY && y != sourceY) {
                    continue;
                }
                if (!visited.add(next)) {
                    continue;
                }
                FluidState fluid = level.getFluidState(next);
                if (!(fluid.getType() instanceof CementFluid cement) || cement.color != this.color) {
                    continue;
                }
                if (y == targetY && !fluid.isSource()) {
                    return next;
                }
                if (++expanded > MAX_SETTLE_SEARCH) {
                    return null;
                }
                queue.add(next);
            }
        }
        return null;
    }

    /** 把源头整体挪到目标格，原格清空。 */
    private void moveDown(Level level, BlockPos pos, FluidState state, BlockPos destination) {
        BlockState sourceBlock = state.createLegacyBlock();
        level.setBlockAndUpdate(destination, sourceBlock);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    /**
     * 源头与其紧邻的两层流动水泥一起凝固。
     *
     * <p>流动水泥自源头向外逐层变薄：紧贴源头的一圈是 amount 7，再外一圈是 amount 6。
     * 第二圈与源头相隔一格，故须先取第一圈再向外扩，而不能只看源头的直接邻格。</p>
     */
    private void solidify(Level level, BlockPos pos) {
        BlockState concreteState = this.concrete().defaultBlockState();
        level.setBlockAndUpdate(pos, concreteState);
        // 第一圈：紧贴源头的流动部分
        Set<BlockPos> inner = new ObjectOpenHashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (this.isFlowOfAmount(level, neighbor, SOLIDIFY_AMOUNT_RING_1)) {
                inner.add(neighbor);
            }
        }
        // 第二圈：与源头相隔一格，须先定位内圈再向外扩。此处仍在改写内圈之前读取外圈，
        // 因此拿到的都是真正的流动水泥状态。用 Set 去重：相邻的两个内圈格会指向同一个外圈格。
        Set<BlockPos> outer = new ObjectOpenHashSet<>();
        for (BlockPos innerPos : inner) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = innerPos.relative(direction);
                if (this.isFlowOfAmount(level, neighbor, SOLIDIFY_AMOUNT_RING_2)) {
                    outer.add(neighbor);
                }
            }
        }
        for (BlockPos innerPos : inner) {
            level.setBlockAndUpdate(innerPos, concreteState);
        }
        for (BlockPos outerPos : outer) {
            level.setBlockAndUpdate(outerPos, concreteState);
        }
    }

    /** 该格是否为本颜色的流动水泥，且稀薄程度恰为给定 amount。 */
    private boolean isFlowOfAmount(Level level, BlockPos pos, int amount) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.getType() instanceof CementFluid cement
            && cement.color == this.color
            && !fluid.isSource()
            && fluid.getAmount() == amount;
    }

    private Block concrete() {
        Block cached = this.concrete;
        if (cached == null) {
            cached = BuiltInRegistries.BLOCK.get(
                ResourceLocation.withDefaultNamespace(this.color.getSerializedName() + "_concrete")
            );
            this.concrete = cached;
        }
        return cached;
    }

    public static class Source extends CementFluid {
        public Source(Properties properties, Color color) {
            super(properties, color);
        }

        /**
         * 只有源头参与随机刻。
         *
         * <p>流动部分即便被随机刻选中也不做任何事，让它参与只会白白增加随机刻负载
         * （水泥在地面上会摊开很大一片，流动格数量远超源头）。</p>
         */
        @Override
        protected boolean isRandomlyTicking() {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends CementFluid {
        public Flowing(Properties properties, Color color) {
            super(properties, color);
            this.registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
