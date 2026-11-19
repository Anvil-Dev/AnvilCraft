package dev.dubhe.anvilcraft.inventory;

import net.minecraft.util.Mth;

/**
 * 计算左右两侧口袋在 GUI 中的布局。坐标以 GUI 左上角为原点，渲染时再叠加屏幕偏移。
 * 前 9 个槽位组成 3×3 核心，第 10~12 个补底行，之后从内向外按列扩展到 4 行；布局同时描述边框、填充区域与槽位位置。
 */
public record PocketLayout(
    int x,
    int y,
    int width,
    int height,
    int slots,
    int columns,
    int rows,
    int shortRows,
    int shortColumns,
    int side,
    Shape shape
) {
    public static final int BORDER = 2;
    public static final int PADDING = 2;
    public static final int BOTTOM_BORDER = 4;
    public static final int SLOT_SIZE = 18;
    public static final int SLOTS_PER_COLUMN = 3;
    public static final int CORE_SLOTS = SLOTS_PER_COLUMN * SLOTS_PER_COLUMN;
    private static final int BASE_COLUMNS = SLOTS_PER_COLUMN;
    private static final int BASE_ROWS = SLOTS_PER_COLUMN + 1;
    private static final int BASE_SLOTS = BASE_COLUMNS * BASE_ROWS;
    public static final int ICON_WIDTH = 16;
    public static final int ICON_HEIGHT = 7;
    public static final int ICON_MARGIN = 2;
    public static final int CONTENT_TOP = BORDER + PADDING + ICON_HEIGHT + ICON_MARGIN;
    public static final int SLOT_ITEM_OFFSET = BORDER + PADDING + 1;

    public static PocketLayout of(int capacity, int side, int imageWidth, int top) {
        int total = Mth.clamp(capacity, 0, PocketInventory.MAX_SIZE);
        int perSide = (total + 1) / 2;
        int slots = side == 0 ? perSide : total - perSide;
        if (slots <= 0) {
            return new PocketLayout(0, top, 0, 0, 0, 0, 0, 0, 0, side, Shape.RECTANGLE);
        }
        Spec spec = spec(slots);
        int width = BORDER * 2 + PADDING * 2 + spec.columns() * SLOT_SIZE;
        int height = BORDER + PADDING + ICON_HEIGHT + ICON_MARGIN
            + spec.rows() * SLOT_SIZE + PADDING + BOTTOM_BORDER;
        int x = side == 0 ? -width - 2 : imageWidth + 2;
        return new PocketLayout(
            x, top, width, height, slots, spec.columns(), spec.rows(),
            spec.shortRows(), spec.shortColumns(), side, spec.shape()
        );
    }

    public boolean isEmpty() {
        return this.slots == 0;
    }

    public boolean stepped() {
        return this.shape == Shape.SIDE_STEP;
    }

    public boolean bottomStepped() {
        return this.shape == Shape.BOTTOM_STEP;
    }

    public int gridX() {
        return this.x + BORDER + PADDING;
    }

    public int gridY() {
        return this.y + CONTENT_TOP;
    }

    public int iconX() {
        return this.x + (this.width - ICON_WIDTH) / 2;
    }

    public int iconY() {
        return this.y + BORDER + PADDING;
    }

    public int slotItemX(int index) {
        int column = cell(index).column();
        int columnFromOuter = this.side == 0 ? this.columns - 1 - column : column;
        return this.x + SLOT_ITEM_OFFSET + columnFromOuter * SLOT_SIZE;
    }

    public int slotItemY(int index) {
        return this.y + CONTENT_TOP + 1 + cell(index).row() * SLOT_SIZE;
    }

    public int slotBackgroundX(int index) {
        return this.slotItemX(index) - 1;
    }

    public int slotBackgroundY(int index) {
        return this.slotItemY(index) - 1;
    }

    public int stepY() {
        int baseRows = this.shape == Shape.BOTTOM_STEP ? this.rows - 1 : this.shortRows;
        return this.gridY() + baseRows * SLOT_SIZE + PADDING;
    }

    public int stepBorderX() {
        if (this.shape == Shape.SIDE_STEP) {
            return this.side == 1
                ? this.gridX() + (this.columns - 1) * SLOT_SIZE + PADDING
                : this.gridX() + SLOT_SIZE - PADDING - BORDER;
        }
        if (this.shape == Shape.BOTTOM_STEP) {
            return this.side == 1
                ? this.gridX() + this.shortColumns * SLOT_SIZE + PADDING
                : this.gridX() + (this.columns - this.shortColumns) * SLOT_SIZE - PADDING - BORDER;
        }
        return 0;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
            && mouseY >= this.y && mouseY < this.y + this.height;
    }

    private static Spec spec(int slots) {
        if (slots <= 3) {
            return new Spec(1, slots, Shape.RECTANGLE, 0, 0);
        }
        if (slots <= CORE_SLOTS) {
            int columns = (slots + 2) / SLOTS_PER_COLUMN;
            int shortRows = slots % SLOTS_PER_COLUMN;
            if (shortRows == 0) {
                return new Spec(columns, SLOTS_PER_COLUMN, Shape.RECTANGLE, 0, 0);
            }
            return new Spec(columns, SLOTS_PER_COLUMN, Shape.SIDE_STEP, shortRows, 0);
        }
        if (slots < BASE_SLOTS) {
            return new Spec(BASE_COLUMNS, BASE_ROWS, Shape.BOTTOM_STEP, 0, slots - CORE_SLOTS);
        }
        if (slots == BASE_SLOTS) {
            return new Spec(BASE_COLUMNS, BASE_ROWS, Shape.RECTANGLE, 0, 0);
        }
        int extra = slots - BASE_SLOTS;
        int columns = BASE_COLUMNS + (extra + BASE_ROWS - 1) / BASE_ROWS;
        int shortRows = extra - (columns - BASE_COLUMNS) * BASE_ROWS;
        if (shortRows == BASE_ROWS) {
            return new Spec(columns, BASE_ROWS, Shape.RECTANGLE, 0, 0);
        }
        return new Spec(columns, BASE_ROWS, Shape.SIDE_STEP, shortRows, 0);
    }

    private static Cell cell(int index) {
        if (index < CORE_SLOTS) {
            return new Cell(index / SLOTS_PER_COLUMN, index % SLOTS_PER_COLUMN);
        }
        if (index < BASE_SLOTS) {
            return new Cell(index - CORE_SLOTS, SLOTS_PER_COLUMN);
        }
        int extra = index - BASE_SLOTS;
        return new Cell(BASE_COLUMNS + extra / BASE_ROWS, extra % BASE_ROWS);
    }

    public enum Shape {
        RECTANGLE,
        SIDE_STEP,
        BOTTOM_STEP
    }

    private record Spec(int columns, int rows, Shape shape, int shortRows, int shortColumns) {
    }

    private record Cell(int column, int row) {
    }
}
