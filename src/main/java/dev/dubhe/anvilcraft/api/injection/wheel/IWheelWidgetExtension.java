package dev.dubhe.anvilcraft.api.injection.wheel;

/** 锤子轮盘在鼠标移动前保留键盘或滚轮确定的选区。 */
public interface IWheelWidgetExtension {
    void anvilcraft$holdSelection(double mouseX, double mouseY);

    void anvilcraft$setTextScale(float scale);
}
