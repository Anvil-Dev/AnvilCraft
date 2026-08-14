package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 网络中一个控制阀的运行态：持有 BE 引用（实时读取过滤/流速，故阀门设置改动
 * <b>无需令网络缓存失效</b>）+ 本 tick 的剩余通过预算。
 *
 * <p>每 tick 分配开始前调 {@link #resetBudget()} 把预算重置为阀门当前最大流速（仅上限，
 * 不是最低起送量）；分配过程中流体每穿过本阀门就 {@link #consume} 扣减，扣至 0 本 tick 不再放行。
 */
public final class ValveState {
    private final ControlValveBlockEntity be;
    private int remaining;

    public ValveState(ControlValveBlockEntity be) {
        this.be = be;
        this.remaining = be.getEffectiveMaxRate();
    }

    /** 每 tick 分配前重置预算为阀门当前有效流速（红石锁定时为 0；实时读取，改流速即时生效）。 */
    public void resetBudget() {
        this.remaining = be.getEffectiveMaxRate();
    }

    /** 该阀门是否允许此流体通过（白名单实时读取；空过滤=全放行）。 */
    public boolean allows(FluidStack fluid) {
        return be.allows(fluid);
    }

    /** 本 tick 剩余可通过量（mB）。 */
    public int remaining() {
        return remaining;
    }

    /** 扣减已通过量。 */
    public void consume(int amount) {
        this.remaining = Math.max(0, this.remaining - amount);
    }
}
