package dev.dubhe.anvilcraft.fluid;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
 * <ul>
 *   <li>只要水泥——源头或流动部分都算——判断出自己可以向下淌，就把附近最近的源头挪到该格
 *       下方，于是源头顺着建筑逐格下沉，而不是像原版那样留在原处、只把流动部分送下去。
 *       下沉由计划刻驱动，与随机刻无关；</li>
 *   <li>下不去的源头被随机刻选中时，有 {@value #SOLIDIFY_CHANCE} 概率连同紧贴它的两层
 *       流动水泥一起凝固成对应颜色的原版混凝土；</li>
 *   <li>接触糖块的源头不凝固；接触粘液块的源头不下移；接触蜂蜜块的源头两者都不。</li>
 * </ul>
 */
public abstract class CementFluid extends BaseFlowingFluid {
    /** 源头静止时凝固的概率。 */
    private static final float SOLIDIFY_CHANCE = 0.1F;
    /**
     * 会跟随源头一起凝固的两圈流动水泥，以流体自身的 amount 表示。
     *
     * <p>流动水泥的 amount 自源头向外逐圈递减：源头为 8，第一圈 7，第二圈 6。
     * 换算成方块状态即 {@code LEVEL} 的 1 与 2（0 是源头）。</p>
     */
    private static final int SOLIDIFY_AMOUNT_RING_1 = 7;
    private static final int SOLIDIFY_AMOUNT_RING_2 = 6;
    /** 找源头时最多考察的水泥格数，仅作兜底；水泥自身的铺开面积本来就是有限的。 */
    private static final int MAX_SOURCE_SEARCH = 64;
    /**
     * 找源头时的搜索方向：四个水平方向，加上正上方。
     *
     * <p>含正上方是必需的。水泥从高处直直落下时，源头在上一格、流动格在它正下方，
     * 含正上方的搜索一步就能命中；只看水平方向则永远够不到，源头再也不会被挪下来，
     * 表现为「完全不触发」。</p>
     *
     * <p>不含正下方：那是要落进去的落点，不会同时是本该跟着走的源头。</p>
     */
    private static final Direction[] SEARCH_DIRECTIONS = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

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

    /**
     * 在水泥铺开的过程中接管「向下淌」这一步：只要有水泥格能向下淌，就把最近的源头跟着挪下来。
     *
     * <p>触发方不必是源头本身。水泥在阶梯、金字塔这类建筑上会先在原地横向铺开，直到铺到
     * 边缘才落下去；此时能向下淌的是边缘上的流动格，源头还在上面一层。若只让源头自己触发，
     * 源头脚下是实心台阶，永远等不到下沉，就会一直卡在顶上。</p>
     *
     * <p>下沉走的是计划刻：{@code LiquidBlock} 在放置与邻居变化时都会安排计划刻，挪下去的
     * 源头又会被 {@code onPlace} 重新排程，于是逐格往下走，节奏与原版流动的 tick 间隔一致。
     * 也正因走计划刻而非随机刻，不会遇到原版把同一格随机刻派发两次、使源头被重复复制而
     * 越流越多的问题。</p>
     */
    @Override
    protected void spread(ServerLevel level, BlockPos pos, BlockState blockState, FluidState state) {
        if (this.transferSourceDown(level, pos)) {
            return;
        }
        // 不是本格被挪走时，照旧走原版：流动部分该往下淌就往下淌
        super.spread(level, pos, blockState, state);
    }

    /**
     * 把最近的源头挪到本格下方，原格清空。
     *
     * <p>源头始终是「挪」而不是「复制」，因此无论怎么下沉，源头数量都不变。</p>
     *
     * @return 被挪走的源头是否就是本格；是则本格已空，调用方不必再走原版的铺开逻辑
     */
    private boolean transferSourceDown(ServerLevel level, BlockPos pos) {
        FluidState here = level.getFluidState(pos);
        if (here.isEmpty() || !this.canFlowDown(level, pos)) {
            return false;
        }
        BlockPos sourcePos = here.isSource() ? pos : this.findNearestSource(level, pos);
        if (sourcePos == null || this.isMoveBlocked(level, sourcePos)) {
            return false;
        }
        FluidState sourceState = level.getFluidState(sourcePos);
        // 只比颜色，不能比实例：源头是 Source、流动部分是 Flowing，是两个不同的对象，
        // 由流动格触发时 getType() != this 恒成立，源头就永远挪不动。
        if (!sourceState.isSource() || !this.isSameCement(sourceState)) {
            return false;
        }
        BlockPos target = pos.below();
        // 落点已被同色源头占住就不再并源，否则两个源头会叠在一起
        if (level.getFluidState(target).isSource()) {
            return false;
        }
        level.setBlockAndUpdate(target, sourceState.createLegacyBlock());
        level.setBlockAndUpdate(sourcePos, Blocks.AIR.defaultBlockState());
        return sourcePos.equals(pos);
    }

    /**
     * 在本格附近找最近的同色源头，广度优先，因此命中的就是跳数最少的那个。
     *
     * <p>搜索方向见 {@link #SEARCH_DIRECTIONS}：四个水平方向加正上方。纵向只允许偏离本格
     * 一格，避免够到远处那一摞水。只把水泥格放进队列，所以展开次数天然受水泥铺开面积限制，
     * 不会因为周围大量空气格而提前耗尽 {@link #MAX_SOURCE_SEARCH}。</p>
     *
     * @return 最近的源头；范围内没有则返回 {@code null}
     */
    @Nullable
    private BlockPos findNearestSource(Level level, BlockPos pos) {
        Set<BlockPos> visited = new ObjectOpenHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(pos);
        queue.add(pos);
        int expanded = 0;
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : SEARCH_DIRECTIONS) {
                BlockPos next = current.relative(direction);
                if (Math.abs(next.getY() - pos.getY()) > 1) {
                    continue;
                }
                if (!visited.add(next)) {
                    continue;
                }
                FluidState fluid = level.getFluidState(next);
                if (!this.isSameCement(fluid)) {
                    continue;
                }
                if (fluid.isSource()) {
                    return next;
                }
                if (++expanded > MAX_SOURCE_SEARCH) {
                    return null;
                }
                queue.add(next);
            }
        }
        return null;
    }

    /**
     * 该状态是否为本颜色的水泥——源头或流动部分都算。
     *
     * <p>必须按<i>颜色</i>判定，不能写成 {@code state.getType() != this}：源头是
     * {@link Source}、流动部分是 {@link Flowing}，两者是不同的对象。由流动格触发转移时，
     * 被挪的源头是 {@code Source} 而 {@code this} 是 {@code Flowing}，那种写法恒为真，
     * 源头就永远挪不动。</p>
     */
    private boolean isSameCement(FluidState state) {
        return state.getType() instanceof CementFluid cement && cement.color == this.color;
    }

    /**
     * 本格此刻能否向下淌，直接用原版 {@code spread} 判断下流时的那个条件。
     *
     * <p>即先取「下方若是流体该是什么」，再用与原版相同的可替换性、遮挡与能否容纳流体
     * 这三项检查，从而与水泥自身的铺开参数保持一致。</p>
     */
    private boolean canFlowDown(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        FluidState newLiquid = this.getNewLiquid(level, below, belowState);
        return level.getFluidState(below).canBeReplacedWith(level, below, newLiquid.getType(), Direction.DOWN)
            && canPassThroughWall(Direction.DOWN, level, pos, level.getBlockState(pos), below, belowState)
            && canHoldFluid(level, below, belowState, newLiquid.getType());
    }

    /** 接触粘液块或蜂蜜块的源头不下移。 */
    private boolean isMoveBlocked(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(Blocks.SLIME_BLOCK) || neighbor.is(Blocks.HONEY_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    /** 接触糖块或蜂蜜块的源头不凝固。 */
    private boolean isSolidifyBlocked(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(ModBlocks.SUGAR_BLOCK.get()) || neighbor.is(Blocks.HONEY_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void randomTick(ServerLevel level, BlockPos pos, FluidState state, RandomSource random) {
        // 同样只认实时状态：源头可能已被计划刻挪走，照旧快照处理会凭空复制出一个源头
        FluidState current = level.getFluidState(pos);
        if (!current.isSource() || !this.isSameCement(current)) {
            return;
        }
        // 原版对同一格会连续派发两次随机刻，第二次视为重复，避免凝固判定被掷两次
        if (this.isDuplicateDispatch(level, pos)) {
            return;
        }
        // 还能向下转移的源头不凝固
        if (!this.isMoveBlocked(level, pos) && this.canFlowDown(level, pos)) {
            return;
        }
        if (!this.isSolidifyBlocked(level, pos) && random.nextFloat() < SOLIDIFY_CHANCE) {
            this.solidify(level, pos);
        }
    }

    /**
     * 记录并识别「同一游戏刻、同一位置」的重复随机刻派发。
     *
     * <p>不去重的话，凝固判定每刻会被掷两次，实际概率变成
     * {@code 1 - (1 - SOLIDIFY_CHANCE)^2}，明显偏高。</p>
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
        return this.isSameCement(fluid)
            && !fluid.isSource()
            && fluid.getAmount() == amount;
    }

    private Block concrete() {
        Block cached = this.concrete;
        if (cached == null) {
            cached = BuiltInRegistries.BLOCK.getValue(
                Identifier.withDefaultNamespace(this.color.getSerializedName() + "_concrete")
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
         * 只有源头参与随机刻，用于凝固判定。下沉走的是计划刻，与随机刻无关。
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
