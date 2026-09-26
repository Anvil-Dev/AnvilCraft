package dev.dubhe.anvilcraft.api.fluid.network;

/**
 * 以无穷大气压供给自身所存气体的流体处理器。
 *
 * <p>气体在网络内由压强驱动均衡（见 {@link FluidPipeNetwork}）：压强通常由填充率与泵偏置推导。
 * 实现本接口并在当前状态下返回 {@code true} 时，该端点按无穷大气压参与比较——它始终是网内
 * 最高压的源，能把自身存有的气体压向任意端点；但存量照常消耗，不会像创造流体储罐那样无限供给。
 *
 * <p>实现方自行判断生效条件，例如锻星砧流体接口仅在主动（输出）模式且供电正常时返回 {@code true}。
 */
public interface InfiniteGasPressureSource {
    /** 当前是否以无穷大气压输出自身存有的气体。 */
    boolean isSupplyingInfiniteGasPressure();
}
