package dev.dubhe.anvilcraft.inventory;

import net.minecraft.util.Mth;

import java.util.List;

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
    /**
     * 普通物品栏与创造物品栏的 GUI 宽度、面板顶位差异。槽位坐标统一按普通物品栏基准计算，
     * 创造界面在此基准上平移；常量必须放在普通类里，Mixin 类中的非私有静态字段会被 Mixin 拒绝。
     */
    public static final int CREATIVE_IMAGE_WIDTH = 195;
    public static final int CREATIVE_PANEL_TOP = 40;
    public static final int CREATIVE_X_SHIFT = CREATIVE_IMAGE_WIDTH - PocketSlot.SCREEN_IMAGE_WIDTH;
    public static final int CREATIVE_Y_SHIFT = CREATIVE_PANEL_TOP - PocketSlot.SCREEN_PANEL_TOP;
    /**
     * 原版配方书的定位常量，取自 {@code RecipeBookComponent} 与 {@code AbstractRecipeBookScreen}：
     * 书页宽 147、可见时 xOffset 基数 86、GUI 右侧留白 200、GUI 右移基数 177。
     */
    private static final int RECIPE_BOOK_WIDTH = 147;
    private static final int RECIPE_BOOK_OFFSET = 86;
    private static final int RECIPE_BOOK_LEFT_MARGIN = 200;
    private static final int RECIPE_BOOK_SHIFT_BASE = 177;
    /** 口袋面板与 GUI、配方书之间的水平间距。 */
    private static final int RECIPE_BOOK_GAP = 2;

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

    /**
     * 配方书展开时 GUI 右移、配方书左移，两者都必须留在屏幕内，否则配方书空白处的点击会被当作界外点击。
     * 返回同时满足「配方书页签左边缘不越界」与「右侧口袋面板不越界」的最小界面宽度。
     */
    public static int recipeBookMinWidth(int capacity, int imageWidth) {
        if (capacity <= 0) {
            return 0;
        }
        int leftWidth = of(capacity, 0, imageWidth, 0).width();
        int rightWidth = of(capacity, 1, imageWidth, 0).width();
        // 页签左边缘 = (width - 147) / 2 - xOffset - 30，其中 xOffset = 86 + leftWidth + 2，要求 >= 0。
        int forTabs = 2 * (RECIPE_BOOK_OFFSET + leftWidth + RECIPE_BOOK_GAP + 30) + RECIPE_BOOK_WIDTH;
        // 右侧面板右边缘 = leftPos + imageWidth + 2 + rightWidth，
        // leftPos = 177 + (width - imageWidth - 200) / 2，要求 <= width。
        int forRightPanel = 2 * (RECIPE_BOOK_SHIFT_BASE + imageWidth + 2 + rightWidth)
            - imageWidth - RECIPE_BOOK_LEFT_MARGIN;
        return Math.max(forTabs, forRightPanel);
    }

    /**
     * 面板的实际轮廓。矩形面板只有一段；台阶面板拆成上下两段，下半段收窄到台阶线一侧。
     * 命中判定与 JEI 避让区域都由此派生，避免两处各写一套坐标。
     */
    public List<Segment> segments() {
        if (this.isEmpty()) {
            return List.of();
        }
        if (this.shape == Shape.RECTANGLE) {
            return List.of(new Segment(this.x, this.y, this.width, this.height));
        }
        // 台阶的水平过渡边占 [stepY, stepY + BOTTOM_BORDER)，归上半段。
        int stepBottom = this.stepY() + BOTTOM_BORDER;
        int upperHeight = stepBottom - this.y;
        int lowerHeight = this.y + this.height - stepBottom;
        Segment upper = new Segment(this.x, this.y, this.width, upperHeight);
        if (lowerHeight <= 0) {
            return List.of(upper);
        }
        Segment lower = this.side == 1
            ? new Segment(this.x, stepBottom, this.stepBorderX() + BORDER - this.x, lowerHeight)
            : new Segment(this.stepBorderX(), stepBottom, this.x + this.width - this.stepBorderX(), lowerHeight);
        return List.of(upper, lower);
    }

    /**
     * 判断某个 GUI 局部坐标是否落在任一侧口袋面板的实际轮廓内。
     */
    public static boolean containsPocket(int capacity, int imageWidth, int top, double mouseX, double mouseY) {
        return of(capacity, 0, imageWidth, top).contains(mouseX, mouseY)
            || of(capacity, 1, imageWidth, top).contains(mouseX, mouseY);
    }

    /**
     * 命中判定。直接复用 {@link #segments()}，台阶下方的透明缺口不会被误判为面板内。
     */
    public boolean contains(double mouseX, double mouseY) {
        for (Segment segment : this.segments()) {
            if (segment.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
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
        int fullColumns = extra / BASE_ROWS;
        int remainingRows = extra % BASE_ROWS;
        int columns = BASE_COLUMNS + fullColumns + (remainingRows > 0 ? 1 : 0);
        if (remainingRows == 0) {
            return new Spec(columns, BASE_ROWS, Shape.RECTANGLE, 0, 0);
        }
        return new Spec(columns, BASE_ROWS, Shape.SIDE_STEP, remainingRows, 0);
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

    /**
     * 面板轮廓的一段矩形，坐标以 GUI 左上角为原点。
     */
    public record Segment(int x, int y, int width, int height) {
        public boolean contains(double pointX, double pointY) {
            return pointX >= this.x && pointX < this.x + this.width
                && pointY >= this.y && pointY < this.y + this.height;
        }
    }

    private record Spec(int columns, int rows, Shape shape, int shortRows, int shortColumns) {
    }

    private record Cell(int column, int row) {
    }
}
