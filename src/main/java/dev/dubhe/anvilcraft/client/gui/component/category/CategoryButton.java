package dev.dubhe.anvilcraft.client.gui.component.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.component.MultilineComponentHelper;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.support.GuiRenderSupport;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@Getter
@Setter
public class CategoryButton extends Button {
    public static final ResourceLocation NORMAL = StorageScreen.texture("category");
    public static final ResourceLocation SMALL = StorageScreen.texture("category_small");
    private final Font font = Minecraft.getInstance().font;
    private final CategoryList.ButtonInfo info;
    private final PlayerSetting setting;
    private final int index;
    private CategoryMode mode;

    public CategoryButton(CategoryList.ButtonInfo info, PlayerSetting setting, int index, CategoryMode mode, OnPress onPress) {
        super(
            0,
            0,
            info.width(),
            info.height(),
            Component.empty(),
            onPress,
            DEFAULT_NARRATION
        );
        this.info = info;
        this.setting = setting;
        this.index = index;
        this.mode = mode;
    }

    protected CategoryEntry entry() {
        return this.setting.listed().get(this.index);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = this.mode.getTexYDiff();
        if (this.isHovered) {
            offsetV = 20; // Hovered part
        }

        graphics.blit(
            this.info.button(),
            this.getX(),
            this.getY(),
            0,
            offsetV,
            this.width,
            this.height,
            this.width,
            this.height * 4
        );

        ICategory category = this.entry().getCategory();
        Component name = category.name();
        this.setTooltip(Tooltip.create(
            MultilineComponentHelper.create()
                .addln("screen.anvilcraft.storage.category.name", name)
                .addln("screen.anvilcraft.storage.category.mode", this.mode.getModeName())
                .build()
        ));

        this.info.extraRenderer().render(this, graphics, mouseX, mouseY, a);
    }

    public void renderIcon(GuiGraphics graphics, int extraX) {
        ICategory category = this.entry().getCategory();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.getX(), this.getY(), 0);
        pose.scale(0.75f, 0.75f, 1.0f);

        ItemStack icon = category.icon().copy();
        int y = 4;
        graphics.renderItem(icon, extraX, y);
        graphics.renderItemDecorations(this.font, icon, extraX, y);

        pose.popPose();
    }

    public void renderName(GuiGraphics graphics) {
        ICategory category = this.entry().getCategory();

        Component name = category.name();
        int left = this.getX() + 17;
        int top = this.getY() + 5;
        GuiRenderSupport.centeredEllipsisText(graphics, this.font, name, left, top, 65);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.mode = this.entry().changeMode(button == 1);
        super.onClick(mouseX, mouseY, button);
    }

    public interface ExtraRenderer {
        ExtraRenderer NOOP = (btn, graphics, mouseX, mouseY, partialTick) -> {};

        void render(CategoryButton btn, GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    }
}
