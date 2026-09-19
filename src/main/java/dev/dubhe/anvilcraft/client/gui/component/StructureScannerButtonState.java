package dev.dubhe.anvilcraft.client.gui.component;

/** 成型舱式三段/五段按钮及六段文件按钮状态；只有有效的松开操作才提交动作。 */
public final class StructureScannerButtonState {
    private final int frames;
    private int pressedInput = -1;

    public StructureScannerButtonState(int frames) {
        if (frames != 3 && frames != 5 && frames != 6) throw new IllegalArgumentException("Expected three, five or six frames");
        this.frames = frames;
    }

    public boolean press(int input, boolean enabled) {
        if (!enabled || input < 0 || this.pressedInput != -1) return false;
        this.pressedInput = input;
        return true;
    }

    public boolean release(int input, boolean enabled, boolean inside) {
        if (!this.pressedBy(input)) return false;
        this.cancel();
        return enabled && inside;
    }

    public boolean pressedBy(int input) {
        return input >= 0 && this.pressedInput == input;
    }

    public boolean keyboardPressed() {
        return this.pressedInput > 0;
    }

    public void cancel() {
        this.pressedInput = -1;
    }

    public int frame(boolean enabled, boolean hovered, boolean selected) {
        if (this.frames == 6) {
            if (!enabled) this.cancel();
            int frame = !enabled ? 0 : this.pressedInput != -1 && (hovered || this.keyboardPressed()) ? 2 : hovered ? 1 : 0;
            return (selected ? 3 : 0) + frame;
        }
        if (!enabled) {
            this.cancel();
            return this.frames == 5 && selected ? 3 : 0;
        }
        if (this.pressedInput != -1 && (this.frames == 5 || hovered || this.keyboardPressed())) return 2;
        if (this.frames == 5 && selected) return hovered ? 4 : 3;
        return hovered ? 1 : 0;
    }
}
