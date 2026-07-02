package dev.dubhe.anvilcraft.block.state;

import lombok.Getter;
import net.minecraft.core.Direction;

/// 锻星砧传送门多方块的部件状态。
/// 占地 3（宽）× 1（深）× 3（高），共 9 个部件。
/// 宽度方向随 FACING 旋转（宽度轴 = FACING.getClockWise()），深度恒为 0，
/// 因此整个结构会随朝向旋转，始终保证长度 3 的边正对玩家。
/// 核心控制器为 BOTTOM_CENTER（底层中心），模型承载部件为 MID_CENTER（正中心）。
@Getter
public enum DirectionGate331PartHalf
    implements IFlexibleMultiPartBlockState<DirectionGate331PartHalf, Direction> {
    BOTTOM_CENTER("bottom_center", 0, 0),
    BOTTOM_LEFT("bottom_left", -1, 0),
    BOTTOM_RIGHT("bottom_right", 1, 0),
    MID_CENTER("mid_center", 0, 1),
    MID_LEFT("mid_left", -1, 1),
    MID_RIGHT("mid_right", 1, 1),
    TOP_CENTER("top_center", 0, 2),
    TOP_LEFT("top_left", -1, 2),
    TOP_RIGHT("top_right", 1, 2);

    private final String name;
    /// 沿宽度方向的索引：-1（左）/ 0（中）/ +1（右），相对玩家视角。
    private final int widthIndex;
    /// 高度层索引：0（底）/ 1（中）/ 2（顶）。
    private final int heightIndex;

    DirectionGate331PartHalf(String name, int widthIndex, int heightIndex) {
        this.name = name;
        this.widthIndex = widthIndex;
        this.heightIndex = heightIndex;
    }

    @Override
    public int getOffsetX(Direction facing) {
        return this.widthIndex * facing.getClockWise().getStepX();
    }

    @Override
    public int getOffsetY(Direction facing) {
        return this.heightIndex;
    }

    @Override
    public int getOffsetZ(Direction facing) {
        return this.widthIndex * facing.getClockWise().getStepZ();
    }

    @Override
    public boolean isMain() {
        return this == BOTTOM_CENTER;
    }

    public boolean isCenterColumn() {
        return this.widthIndex == 0;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
