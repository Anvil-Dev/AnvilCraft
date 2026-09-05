package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.client.gui.screen.CelestialForgingAnvilScreen;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.Contract;

public final class CelestialMapsGuideWidget extends AbstractWidget {
    /// 星图指南贴图
    public static final ResourceLocation TEX_CELESTIAL_MAPS = SharedTextures.texture("block/celestial_maps");
    public static final int MAP_SIZE = 160;
    /// 浅绿色，75%透明度
    public static final int COLOR_TIME = 0xBF_A0FFA0;
    /// 青色，75%透明度
    public static final int COLOR_SPACE = 0xBF_00FFFF;
    /// 浅黄色，75%透明度
    public static final int COLOR_MASS = 0xBF_FFFFA0;
    /// 浅红色，75%透明度
    public static final int COLOR_ENERGY = 0xBF_FF8080;
    public static final int X1 = 11;
    public static final int X2 = 91;
    public static final int Y1 = 12;
    public static final int Y2 = 92;

    @Contract(pure = true)
    private static boolean isIn(double x, double y) {
        return x >= 0 && x < 80 && y >= 0 && y < 80;
    }

    /// 渲染带4条彩色指示线的星图指南。
    /// 每条线代表一种砧子类型，根据其数量（1-64）移动。
    /// 当对应数量为0时，线条和数量标签隐藏。
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Contract(mutates = "param1")
    public static void renderCelestialMapsGuide(
        GuiGraphics guiGraphics,
        Font font,
        int time,
        int space,
        int mass,
        int energy
    ) {
        /// 以160x160渲染星图图像
        guiGraphics.blit(TEX_CELESTIAL_MAPS, 0, 0, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        /// 时间砧子：浅绿色，垂直线，全高（160px），x=12-76（从左起索引1）
        if (time > 0) {
            int x = X1 + Math.round((time - 1) * 64f / 63f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE, COLOR_TIME); /// 2px → 0.5x缩放后1px
            String text = String.valueOf(time);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.drawString(font, text, textX, textY, COLOR_TIME, false);
        }

        /// 空间砧子：青色，水平线，全宽（160px），y=92-156（从底部起索引1）
        if (space > 0) {
            int y = 160 - Y2 - Math.round((space - 1) * 64f / 63f) - 2; /// -2缩放像素 = -1屏幕像素
            guiGraphics.fill(0, y, MAP_SIZE, y + 2, COLOR_SPACE); /// 2px → 0.5x缩放后1px
            String text = String.valueOf(space);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.drawString(font, text, textX, textY, COLOR_SPACE, false);
        }

        /// 质量砧子：浅黄色，垂直线，上半部分（80px），x=92-156（从左起索引1）
        if (mass > 0) {
            int x = X2 + Math.round((mass - 1) * 64f / 63f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE / 2, COLOR_MASS); /// 上半部分，2px→1px
            String text = String.valueOf(mass);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.drawString(font, text, textX, textY, COLOR_MASS, false);
        }

        /// 能量砧子：浅红色，水平线，左半部分（80px），y=12-76（从底部起索引1）
        if (energy > 0) {
            int y = 160 - Y1 - Math.round((energy - 1) * 64f / 63f) - 2; /// -2缩放像素 = -1屏幕像素
            guiGraphics.fill(0, y, MAP_SIZE / 2, y + 2, COLOR_ENERGY); /// 左半部分，2px→1px
            String text = String.valueOf(energy);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.drawString(font, text, textX, textY, COLOR_ENERGY, false);
        }

        /// 地图右侧的三步指南文本

        renderGuideStepText(guiGraphics, font, time, space, mass, energy);
    }

    /// 在地图右下角渲染三步天体类型指南文本。
    @Contract(mutates = "param1")
    private static void renderGuideStepText(GuiGraphics guiGraphics, Font font, int time, int space, int mass, int energy) {
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        int corner = 88; /// 地图右下角空白区域
        poseStack.translate(corner, corner, 0);
        poseStack.scale(2, 2, 1);
        int lineSpacing = font.lineHeight + 2; /// 缩放后的单位

        /// 步骤1：↑ 从质量-半径图表推导的类型（质量 + 空间）
        int step1Rgb = CelestialBodyMatcher.getMassRadiusRgb(mass, space);
        String step1Name = getTypeDisplayName(step1Rgb);
        drawGuideLine(guiGraphics, font, "↑" + step1Name, 0, 0);

        /// 步骤2：← 从年龄-温度图表推导的类型（时间 + 能量）
        /// 棕矮星使用 age_temp_sp；其他使用 age_temp
        CelestialBodyClass step1Class = CelestialBodyClass.fromRgb(step1Rgb);
        int step2Rgb;
        if (step1Class != null && step1Class.step2UsesSp()) {
            step2Rgb = CelestialBodyMatcher.getAgeTempSpRgb(time, energy);
        } else {
            step2Rgb = CelestialBodyMatcher.getAgeTempRgb(time, energy);
        }
        String step2Name = getTypeDisplayName(step2Rgb);
        drawGuideLine(guiGraphics, font, "←" + step2Name, 0, lineSpacing * 2);

        /// 步骤3：↖ 从年龄-半径图表推导的类型（时间 + 空间）
        int step3Rgb = CelestialBodyMatcher.getAgeRadiusRgb(time, space);
        String step3Name = getTypeDisplayName(step3Rgb);
        drawGuideLine(guiGraphics, font, "↖" + step3Name, 0, lineSpacing);
        poseStack.popPose();
    }

    /// 通过翻译键将图表RGB颜色转换为显示名称。
    /// 使用现有的 screen.anvilcraft.cfa.class.<name> 键模式。
    /// 岩石行星子类型全部映射到 rocky_planet。
    @Contract(pure = true)
    private static String getTypeDisplayName(int rgb) {
        if (rgb == 0x000000) {
            return Component.translatable("screen.anvilcraft.cfa.class.no_match").getString();
        }
        CelestialBodyClass bodyClass = CelestialBodyClass.fromRgb(rgb);
        if (bodyClass == null) {
            return Component.translatable("screen.anvilcraft.cfa.class.no_match").getString();
        }
        String key;
        if (bodyClass.isRockyPlanet()) {
            key = "screen.anvilcraft.cfa.class.rocky_planet";
        } else {
            key = "screen.anvilcraft.cfa.class." + bodyClass.name().toLowerCase();
        }
        return Component.translatable(key).getString();
    }

    @Contract(mutates = "param1")
    private static void drawGuideLine(GuiGraphics guiGraphics, Font font, String text, int x, int y) {
        guiGraphics.drawString(font, text, x, y, 0xFFCCCCCC, false);
    }

    private final IntList anvilCounts;

    public CelestialMapsGuideWidget(IntList anvilCounts) {
        super(0, 0, 1, 1, Component.empty());
        this.anvilCounts = anvilCounts;
    }

    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(getX(), getY(), 0);
        poseStack.scale((float) getWidth() / MAP_SIZE, (float) getHeight() / MAP_SIZE, 1);
        renderCelestialMapsGuide(
            guiGraphics,
            getFont(),
            anvilCounts.getInt(0),
            anvilCounts.getInt(1),
            anvilCounts.getInt(2),
            anvilCounts.getInt(3)
        );
        poseStack.popPose();
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        if (!super.clicked(mouseX, mouseY) || !(Minecraft.getInstance().screen instanceof CelestialForgingAnvilScreen)) {
            return false;
        }
        var x0 = (mouseX - getX()) / getWidth() * MAP_SIZE;
        var y0 = (mouseY - getY()) / getHeight() * MAP_SIZE;
        return isIn(x0, y0)
            || isIn(x0 - 80, y0)
            || isIn(x0, y0 - 80);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        var interactor = Minecraft.getInstance().gameMode;
        var player = Minecraft.getInstance().player;
        if (interactor == null || player == null || !(Minecraft.getInstance().screen instanceof CelestialForgingAnvilScreen screen)) {
            return;
        }
        var containerId = screen.getMenu().containerId;
        var x0 = (mouseX - getX()) / getWidth() * MAP_SIZE;
        var y0 = (mouseY - getY()) / getHeight() * MAP_SIZE;
        var time = Math.clamp((int) Math.floor(x0 - X1), 0, 64);
        var space = Math.clamp(64 - (int) (Math.floor(y0 - Y1 + 8) * 64 / 63), 0, 64);
        var energy = Math.clamp(64 - (int) (Math.floor(y0 - Y2 + 8) * 64 / 63), 0, 64);
        var mass = Math.clamp((int) Math.floor(x0 - X2), 0, 64);
        if (isIn(x0, y0)) {
            interactor.handleInventoryMouseClick(containerId, 0, time, ClickType.QUICK_CRAFT, player);
            interactor.handleInventoryMouseClick(containerId, 1, space, ClickType.QUICK_CRAFT, player);
        } else if (isIn(x0 - 80, y0)) {
            interactor.handleInventoryMouseClick(containerId, 2, mass, ClickType.QUICK_CRAFT, player);
            interactor.handleInventoryMouseClick(containerId, 1, space, ClickType.QUICK_CRAFT, player);
        } else if (isIn(x0, y0 - 80)) {
            interactor.handleInventoryMouseClick(containerId, 0, time, ClickType.QUICK_CRAFT, player);
            interactor.handleInventoryMouseClick(containerId, 3, energy, ClickType.QUICK_CRAFT, player);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var interactor = Minecraft.getInstance().gameMode;
        var player = Minecraft.getInstance().player;
        if (interactor == null || player == null || !(Minecraft.getInstance().screen instanceof CelestialForgingAnvilScreen screen)) {
            return false;
        }
        var containerId = screen.getMenu().containerId;
        var x0 = (mouseX - getX()) / getWidth() * MAP_SIZE;
        var y0 = (mouseY - getY()) / getHeight() * MAP_SIZE;
        if (Screen.hasShiftDown() && scrollX == 0) {
            scrollX = scrollY;
            scrollY = 0;
        }
        var addX = scrollX < 0 ? 4 : 0;
        var addY = scrollY < 0 ? 4 : 0;
        var scrolledX = scrollX != 0;
        var scrolledY = scrollY != 0;
        if (isIn(x0, y0)) {
            if (scrolledX) {
                interactor.handleInventoryButtonClick(containerId, 1 + addX);
            }
            if (scrolledY) {
                interactor.handleInventoryButtonClick(containerId, 2 + addY);
            }
        } else if (isIn(x0 - 80, y0)) {
            if (scrolledX) {
                interactor.handleInventoryButtonClick(containerId, 3 + addX);
            }
            if (scrolledY) {
                interactor.handleInventoryButtonClick(containerId, 2 + addY);
            }
        } else if (isIn(x0, y0 - 80)) {
            if (scrolledX) {
                interactor.handleInventoryButtonClick(containerId, 1 + addX);
            }
            if (scrolledY) {
                interactor.handleInventoryButtonClick(containerId, 4 + addY);
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void playDownSound(SoundManager handler) {

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
