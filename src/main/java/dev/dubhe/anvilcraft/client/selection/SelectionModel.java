package dev.dubhe.anvilcraft.client.selection;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/** 26.1 将方块状态模型和独立模型分开注册，选择几何保留同样的键。 */
public sealed interface SelectionModel {
    record State(BlockState state) implements SelectionModel {
    }

    record Standalone(StandaloneModelKey<?> key) implements SelectionModel {
    }

    static SelectionModel standalone(StandaloneModelKey<?> key) {
        return new Standalone(key);
    }
}
