package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * 网络中一个控制阀的运行态：持有 BE 引用 + 本 tick 的剩余通过预算。
 */
public final class ValveState {
    private final ControlValveBlockEntity be;
    private int remaining;

    public ValveState(ControlValveBlockEntity be) {
        this.be = be;
        this.remaining = be.getEffectiveMaxRate();
    }

    public void resetBudget() {
        this.remaining = this.be.getEffectiveMaxRate();
    }

    public boolean allows(FluidResource fluid) {
        return this.be.allows(fluid);
    }

    public int remaining() {
        return this.remaining;
    }

    public void consume(int amount) {
        this.remaining = Math.max(0, this.remaining - amount);
    }
}
