package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.client.gui.screen.AnvilHammerScreen;
import dev.dubhe.anvilcraft.util.MathUtil;
import dev.dubhe.anvilcraft.util.function.Consumer4;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WheelWidget extends AbstractWidget {
    public static final int IGNORE_CURSOR_MOVE_LENGTH = 15;
    private static final Vector2f ROTATION_START = new Vector2f(0, 1);

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Vector2f centerPos;
    private final float ringInnerRadius;
    private final float ringOuterRadius;
    private final int delay; //ms
    private final int animationMs; //ms
    private final int closingAnimationMs; //ms
    private final int ringColor;
    private final int selectionEffectColor;
    private final int selectionEffectRadius;
    private final float selectionAnimationSpeedFactor;
    private final int textColor;
    private final float textScale;
    private final List<WheelSection> sections = new ArrayList<>();

    private long displayTime = System.currentTimeMillis();
    private float currentAngle = 0;
    @Getter
    private int currentSectionIndex = -1;
    private Vector2f selectionEffectPos;
    private boolean animationStarted = false;
    @Getter
    @Setter
    private boolean closingAnimationStarted = false;

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius, float textScale,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, textScale, sections);
    }

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius, float textScale, float degreeOffsetAngle,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, textScale, degreeOffsetAngle, sections);
    }

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(x, y, width, height, Component.empty(), ringInnerRadius, ringOuterRadius, sections);
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius, float textScale, float degreeOffsetAngle,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(
            x, y, width, height, message,
            ringInnerRadius, ringOuterRadius,
            150, 300, 150,
            0x88000000,
            0xddffff00, 20, 5f,
            0xfdfdfd, textScale, degreeOffsetAngle,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(
            x, y, width, height, message,
            ringInnerRadius, ringOuterRadius,
            150, 300, 150,
            0x88000000,
            0xddffff00, 20, 5f,
            0xfdfdfd, 1f, 0f,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius, float degreeOffsetAngle,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(
            x, y, width, height, message,
            ringInnerRadius, ringOuterRadius,
            150, 300, 150,
            0x88000000,
            0xddffff00, 20, 5f,
            0xfdfdfd, 1f, degreeOffsetAngle,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height,
        float ringInnerRadius, float ringOuterRadius,
        int delay, int animationMs, int closingAnimationMs,
        int ringColor,
        int selectionEffectColor, int selectionEffectRadius, float selectionAnimationSpeedFactor,
        int textColor, float textScale,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        this(
            x, y, width, height, Component.empty(),
            ringInnerRadius, ringOuterRadius,
            delay, animationMs, closingAnimationMs,
            ringColor,
            selectionEffectColor, selectionEffectRadius, selectionAnimationSpeedFactor,
            textColor, textScale, 0f,
            sections
        );
    }

    public WheelWidget(
        int x, int y, int width, int height, Component message,
        float ringInnerRadius, float ringOuterRadius,
        int delay, int animationMs, int closingAnimationMs,
        int ringColor,
        int selectionEffectColor, int selectionEffectRadius, float selectionAnimationSpeedFactor,
        int textColor, float textScale, float degreeOffsetAngle,
        List<Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>>> sections
    ) {
        super(x, y, width, height, message);
        this.centerPos = new Vector2f(this.getX() + this.getWidth() / 2f, this.getY() + this.getHeight() / 2f);
        this.ringInnerRadius = Math.max(ringInnerRadius, IGNORE_CURSOR_MOVE_LENGTH);
        this.ringOuterRadius = ringOuterRadius;
        this.delay = delay;
        this.animationMs = animationMs;
        this.closingAnimationMs = closingAnimationMs;
        this.ringColor = ringColor;
        this.selectionEffectColor = selectionEffectColor;
        this.selectionEffectRadius = selectionEffectRadius;
        this.selectionAnimationSpeedFactor = selectionAnimationSpeedFactor;
        this.textColor = textColor;
        this.textScale = textScale;
        float degreeEachRotation = 360f / sections.size();
        for (int i = 0; i < sections.size(); i++) {
            Pair<Component, Consumer4<GuiGraphics, PoseStack, Integer, Integer>> section = sections.get(i);
            float rotation = MathUtil.clampWithProportion((degreeEachRotation * i + degreeOffsetAngle) % 360, 0, 360);
            Vector2f rotated = MathUtil.rotationDegrees(ROTATION_START, rotation)
                .mul(1, -1)
                .mul(this.getSectionCircleDiameter())
                .add(this.centerPos);
            float detectionStart = (float) (Math.toRadians(rotation - degreeEachRotation / 2f) + Math.PI * 2);
            float detectionEnd = (float) (Math.toRadians(rotation + degreeEachRotation / 2f) + Math.PI * 2);
            detectionStart = detectionStart % (float) (Math.PI * 2);
            detectionEnd = detectionEnd % (float) (Math.PI * 2);
            this.sections.add(new WheelSection(
                rotated,
                (float) (Math.toRadians(rotation) % (Math.PI * 2)),
                detectionStart,
                detectionEnd,
                section.getFirst(),
                section.getSecond()
            ));
        }
        this.selectionEffectPos = MathUtil.rotate(
            MathUtil.copy(ROTATION_START)
                .mul(this.getSectionCircleDiameter()),
            this.currentAngle
        );
    }

    public WheelWidget setCurrentIndex(int index) {
        this.currentSectionIndex = index;
        this.currentAngle = this.sections.get(index).angle;
        this.selectionEffectPos = MathUtil.rotate(
            MathUtil.copy(ROTATION_START)
                .mul(this.getSectionCircleDiameter()),
            this.currentAngle
        );
        return this;
    }

    public float getSectionCircleDiameter() {
        return this.ringOuterRadius - this.ringInnerRadius + this.ringInnerRadius * 2;
    }

    public int getSectionSize() {
        return this.sections.size();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            if (this.currentSectionIndex == this.getSectionSize() - 1) {
                this.currentSectionIndex = 0;
            } else {
                this.currentSectionIndex++;
            }
        } else if (scrollY < 0) {
            if (this.currentSectionIndex == 0) {
                this.currentSectionIndex = this.getSectionSize() - 1;
            } else {
                this.currentSectionIndex--;
            }
        }
        for (WheelSection section : this.sections) {
            if (this.sections.indexOf(section) == this.currentSectionIndex) {
                this.currentAngle = section.angle;
                return true;
            }
        }
        return true;
    }

    public void checkMousePos(double mouseX, double mouseY) {
        if (this.closingAnimationStarted) return;
        float centerX = this.centerPos.x;
        float centerY = this.centerPos.y;
        Vector2f cursorPos = new Vector2f((float) mouseX - centerX, (float) mouseY - centerY);
        if (cursorPos.length() < IGNORE_CURSOR_MOVE_LENGTH) return;
        Vector2f rotationStart = new Vector2f(0, 1);
        cursorPos.normalize();
        double rot = Math.acos(rotationStart.dot(cursorPos) / (rotationStart.length() * cursorPos.length()));
        double rotation = cursorPos.x < 0 ? Math.PI - rot : Math.PI + rot;
        for (WheelSection section : this.sections) {
            if (section.angleStart > section.angleEnd && rotation >= section.angleStart
                || rotation >= section.angleStart && rotation <= section.angleEnd
            ) {
                this.currentAngle = section.angle;
                this.currentSectionIndex = this.sections.indexOf(section);
                break;
            }
        }
    }

    public boolean shouldRender() {
        if (this.animationStarted) return true;
        return (this.displayTime + this.delay) <= System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.checkMousePos(mouseX, mouseY);
        this.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        this.renderClosingAnimation(guiGraphics);
        if (!this.shouldRender()) {
            return;
        }
        if (this.closingAnimationStarted) return;
        if (!this.animationStarted) {
            this.animationStarted = true;
            this.displayTime = System.currentTimeMillis();
        }
        PoseStack poseStack = guiGraphics.pose();
        float delta = this.displayTime + this.animationMs - System.currentTimeMillis();
        if (delta > 0) {
            float progress = 1 - (delta / this.animationMs);
            progress = (float) (-Math.pow(progress, 2) + 2 * progress);
            if (progress == 0) return;
            this.renderProgressAnimation(guiGraphics, progress);
            return;
        }
        AnvilHammerScreen.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.ringColor,
            this.ringInnerRadius * 2,
            this.ringOuterRadius * 2
        );
        this.renderSelection(guiGraphics);
        for (WheelSection value : this.sections) {
            float x = value.center.x;
            float y = value.center.y;
            poseStack.pushPose();
            poseStack.translate(x - 10, y - 10, 100);
            value.renderer.accept(guiGraphics, poseStack, 20, 20);
            poseStack.popPose();
            poseStack.pushPose();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = (y - offsetY - 20 * this.textScale) / coordinateScale;

            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(coordinateScale, coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY, 0);
            poseStack.scale(this.textScale / coordinateScale, this.textScale / coordinateScale, this.textScale / coordinateScale);
            guiGraphics.drawCenteredString(
                minecraft.font,
                value.subTitle,
                0,
                0,
                (0xff << 24) | this.textColor
            );
            poseStack.popPose();
        }
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }

    public void renderClosingAnimation(GuiGraphics guiGraphics) {
        if (!this.closingAnimationStarted) return;
        float delta = this.displayTime + this.closingAnimationMs - System.currentTimeMillis();
        float progress = delta / this.closingAnimationMs;
        if (progress >= 1 || progress <= 0) {
            this.minecraft.setScreen(null);
        }
        this.renderProgressAnimation(guiGraphics, progress);
    }

    private void renderProgressAnimation(GuiGraphics guiGraphics, float progress) {
        progress = (float) (-Math.pow(progress, 2) + 2 * progress);
        if (progress == 0) return;
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        AnvilHammerScreen.renderRing(
            guiGraphics,
            this.centerPos.x,
            this.centerPos.y,
            this.ringColor,
            this.ringInnerRadius * 2 * progress,
            this.ringOuterRadius * 2 * progress
        );
        poseStack.popPose();
        if (this.currentSectionIndex != -1) {
            WheelSection section = this.sections.get(this.currentSectionIndex);
            Vector2f center = new Vector2f(
                (section.center.x - this.centerPos.x) / this.getSectionCircleDiameter(),
                (section.center.y - this.centerPos.y) / this.getSectionCircleDiameter()
            ).mul(this.getSectionCircleDiameter() * progress).add(this.centerPos.x, this.centerPos.y);
            AnvilHammerScreen.renderSelectionEffect(
                guiGraphics,
                center.x,
                center.y,
                this.selectionEffectColor,
                this.selectionEffectRadius
            );
        }
        for (WheelSection value : this.sections) {
            Vector2f center = new Vector2f(
                (value.center.x - this.centerPos.x) / this.getSectionCircleDiameter(),
                (value.center.y - this.centerPos.y) / this.getSectionCircleDiameter()
            ).mul(this.getSectionCircleDiameter() * progress).add(this.centerPos.x, this.centerPos.y);
            float x = center.x;
            float y = center.y;
            poseStack.pushPose();
            poseStack.translate(x - 10, y - 10, 100);
            value.renderer.accept(guiGraphics, poseStack, 20, 20);
            poseStack.popPose();
            int textAlpha = (int) (progress * 0xff) << 24;
            poseStack.pushPose();
            float coordinateScale = 0.7f;
            float offsetX = 0.1f * this.width;
            float offsetY = 0.1f * this.height;
            float adjustedX = (x - offsetX) / coordinateScale;
            float adjustedY = (y - offsetY - 20 * this.textScale) / coordinateScale;

            poseStack.translate(offsetX, offsetY, 0);
            poseStack.scale(coordinateScale, coordinateScale, coordinateScale);
            poseStack.translate(adjustedX, adjustedY, 0);
            poseStack.scale(this.textScale / coordinateScale, this.textScale / coordinateScale, this.textScale / coordinateScale);
            guiGraphics.drawCenteredString(
                this.minecraft.font,
                value.subTitle,
                0,
                0,
                textAlpha | 0xfdfdfd
            );
            poseStack.popPose();
        }
    }

    private void renderSelection(GuiGraphics guiGraphics) {
        float selectionEffectAngle = MathUtil.angle(
            MathUtil.copy(ROTATION_START),
            this.selectionEffectPos
        );

        float diffAngle = this.currentAngle - selectionEffectAngle;

        if (diffAngle > Math.PI) {
            diffAngle -= (float) (Math.PI * 2);
        } else if (diffAngle < -Math.PI) {
            diffAngle += (float) (Math.PI * 2);
        }

        this.selectionEffectPos = MathUtil.rotate(
            this.selectionEffectPos,
            diffAngle / this.selectionAnimationSpeedFactor
        );

        Vector2f pos = MathUtil.copy(this.selectionEffectPos)
            .mul(1, -1)
            .add(this.centerPos);

        AnvilHammerScreen.renderSelectionEffect(
            guiGraphics,
            pos.x,
            pos.y,
            this.selectionEffectColor,
            this.selectionEffectRadius
        );
    }

    public void onClosing() {
        if (this.shouldRender() && !this.closingAnimationStarted) {
            this.displayTime = System.currentTimeMillis();
            this.closingAnimationStarted = true;
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    public record WheelSection(
        Vector2f center,
        float angle,
        float angleStart,
        float angleEnd,
        Component subTitle,
        Consumer4<GuiGraphics, PoseStack, Integer, Integer> renderer
    ) {
    }
}
