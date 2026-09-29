package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 为玻璃管道显示提供"该气体在此容器内的显示填充率"（0..1）。
 *
 * <p>玻璃管道内气体的透明度由扩散系内各容器取平均得出（见
 * {@code FluidPipeNetwork#avgGasAlphaFill}），目标是与该容器渲染器保持一致。
 * 多数容器的口径可由 {@link net.neoforged.neoforge.fluids.capability.IFluidHandler}
 * 的容量推导——{@code 该气体存量 / 容器总容量}——但以下两类容器推导不出，必须由实现方给出：
 *
 * <ul>
 *   <li><b>增强态大型储罐</b>：渲染口径是 {@code 存量 / max(总存量, 无限阈值)}，
 *       而 {@code getTanks()} 会多报一个用于接纳新流体的空槽，容量求和与之不符；</li>
 *   <li><b>创造流体储罐</b>：渲染口径恒为满，而容量报告为 {@link Integer#MAX_VALUE}。</li>
 * </ul>
 */
public interface GasDisplayFillProvider {
    /** 该气体在此容器内的显示填充率（0..1）；容器没有该气体时返回 0。 */
    float gasDisplayFill(FluidStack gas);
}
