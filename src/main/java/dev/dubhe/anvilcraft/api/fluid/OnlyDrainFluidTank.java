package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;

/**
 * 仅允许抽取的流体容器实现。
 *
 * <p>
 * 该类继承自框架提供的 {@link FluidTank}，但重写了 fill 方法以禁止通过常规接口填充流体（返回 0 表示不接受任何填充）。
 * 如果模块内部确实需要直接填充，请使用带有 {@link ApiStatus.Internal} 标注的 internalFill 方法，
 * 该方法会委托给父类实现以执行真实的填充操作。
 */
public class OnlyDrainFluidTank extends FluidTank {
    public OnlyDrainFluidTank(int capacity) {
        super(capacity);
    }

    public OnlyDrainFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    /**
     * 禁止通过常规 API 填充该容器。
     *
     * @param resource 要填充的流体及数量（不会被接受）
     * @param action   表示是否为模拟操作
     * @return 永远返回 0，表示未接受任何流体填充
     */
    @Override
    public final int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    /**
     * 内部使用的填充方法，允许调用父类的填充逻辑。
     *
     * <p>
     * 该方法被标注为 {@link ApiStatus.Internal}，意味着它不是公共 API 的一部分，
     * 仅用于模块内部需要绕过禁止填充限制的场景（例如初始化或特定行为）。
     *
     * @param resource 要填充的流体和数量
     * @param action   表示是否为模拟操作
     * @return 实际接受的流体量（委托给父类实现）
     */
    @ApiStatus.Internal
    public final int internalFill(FluidStack resource, FluidAction action) {
        return super.fill(resource, action);
    }
}
