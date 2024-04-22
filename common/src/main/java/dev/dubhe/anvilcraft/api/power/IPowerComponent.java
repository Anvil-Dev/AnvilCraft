package dev.dubhe.anvilcraft.api.power;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 电力元件
 */
@SuppressWarnings("unused")
public interface IPowerComponent {
    BooleanProperty OVERLOAD = BooleanProperty.create("overload");
    EnumProperty<Switch> SWITCH = EnumProperty.create("switch", Switch.class);

    Level getCurrentLevel();

    /**
     * @return 元件位置
     */
    @NotNull
    BlockPos getPos();

    /**
     * @return 覆盖范围
     */
    default VoxelShape getShape() {
        return Shapes.box(
            -this.getRange(), -this.getRange(), -this.getRange(),
            this.getRange() + 1, this.getRange() + 1, this.getRange() + 1
        );
    }

    default int getRange() {
        return 0;
    }

    /**
     * 设置电网
     *
     * @param grid 电网
     */
    void setGrid(@Nullable PowerGrid grid);

    /**
     * 获取电网
     *
     * @return 电网
     */
    @Nullable
    PowerGrid getGrid();

    /**
     * @return 元件类型
     */
    @NotNull
    PowerComponentType getComponentType();

    enum Switch implements StringRepresentable {
        ON("on"),
        OFF("off");
        private final String name;

        Switch(String name) {
            this.name = name;
        }

        public @NotNull String toString() {
            return this.getSerializedName();
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }

    /**
     * @param level 世界
     * @param pos   位置
     */
    default void flushState(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(OVERLOAD)) return;
        if (this.getGrid() == null) {
            if (!state.getValue(OVERLOAD)) {
                level.setBlockAndUpdate(pos, state.setValue(OVERLOAD, true));
            }
            return;
        }
        if (this.getGrid().isWork() && state.getValue(OVERLOAD)) {
            level.setBlockAndUpdate(pos, state.setValue(OVERLOAD, false));
        } else if (!this.getGrid().isWork() && !state.getValue(OVERLOAD)) {
            level.setBlockAndUpdate(pos, state.setValue(OVERLOAD, true));
        }
    }
}
