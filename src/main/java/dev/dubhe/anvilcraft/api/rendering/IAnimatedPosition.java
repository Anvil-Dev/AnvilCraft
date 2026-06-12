package dev.dubhe.anvilcraft.api.rendering;

/**
 * 支持平滑位移渲染的接口。
 * <p>
 * 由 {@link AnimatedPositionBlockEntity} 实现，
 * 供 {@link dev.dubhe.anvilcraft.client.renderer.blockentity.AnimatedPositionRenderer} 使用。
 * </p>
 */
public interface IAnimatedPosition {

    /**
     * 获取渲染时使用的插值 X 偏移。
     */
    float getRenderOffsetX(float partialTick);

    /**
     * 获取渲染时使用的插值 Y 偏移。
     */
    float getRenderOffsetY(float partialTick);

    /**
     * 获取渲染时使用的插值 Z 偏移。
     */
    float getRenderOffsetZ(float partialTick);

    /** 获取当前 X 偏移（未插值） */
    float getOffsetX();

    /** 获取当前 Y 偏移（未插值） */
    float getOffsetY();

    /** 获取当前 Z 偏移（未插值） */
    float getOffsetZ();
}
