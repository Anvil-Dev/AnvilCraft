package dev.dubhe.anvilcraft.client.support;

/** 旧版 isGui3d 区分生成物品模型与方块几何，与 gui_light 无关。 */
public interface ProcessingModelShape {
    boolean anvilcraft$isThreeDimensional();

    void anvilcraft$setThreeDimensional(boolean value);
}
