package dev.dubhe.anvilcraft.block.state;

/**
 * 3x3 单层多方块部件：仅在同一水平面上的九宫格布局（垂直无偏移），
 * 用于石碑大型装饰方块按 3x3 落地。
 */
public enum Cube3x3Part implements ISimpleMultiPartBlockState<Cube3x3Part> {
    CENTER("center", 0, 0, 0),
    W("w", -1, 0, 0),
    E("e", 1, 0, 0),
    N("n", 0, 0, -1),
    S("s", 0, 0, 1),
    WN("wn", -1, 0, -1),
    WS("ws", -1, 0, 1),
    EN("en", 1, 0, -1),
    ES("es", 1, 0, 1);

    private final String name;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    Cube3x3Part(String name, int offsetX, int offsetY, int offsetZ) {
        this.name = name;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    @Override
    public int getOffsetX() {
        return this.offsetX;
    }

    @Override
    public int getOffsetY() {
        return this.offsetY;
    }

    @Override
    public int getOffsetZ() {
        return this.offsetZ;
    }
}
