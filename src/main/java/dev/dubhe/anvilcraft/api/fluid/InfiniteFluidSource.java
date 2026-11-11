package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * 当前以无限存量供给流体的处理器。
 *
 * <p>用于管道网络识别“无限储罐”：创造流体储罐、小型/大型流体储罐在增强并达到
 * 无限阈值后，都会实现本接口。仓储流体端口据此把自身切换为无限存量，断网后再恢复为满存量。
 */
public interface InfiniteFluidSource {
    /**
     * 当前无限供给的所有流体。
     *
     * <p>大型储罐可能同时有多种流体达到无限，调用方需结合过滤阀的实际放行结果选择。
     *
     * @return 流体副本列表；不是无限源或没有流体时为空列表
     */
    List<FluidStack> getInfiniteFluids();
}