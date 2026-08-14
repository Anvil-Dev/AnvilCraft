package dev.dubhe.anvilcraft.client.gui.component.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.component.MultilineComponentHelper;
import dev.dubhe.anvilcraft.client.support.GuiRenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
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
    public static final ResourceLocation BACKGROUND = SharedTextures.textureGui("misc/storage_station/category");
    private final Font font = Minecraft.getInstance().font;
    private final PlayerSetting setting;
    private final int index;
    private CategoryMode mode;

    public CategoryButton(int x, PlayerSetting setting, int index, CategoryMode mode, OnPress onPress) {
        super(
            x,
            0,
            86,
            20,
            Component.empty(),
            button -> {
                if (!(button instanceof CategoryButton category)) return;
                category.mode = category.entry().changeMode();
                onPress.onPress(button);
            },
            DEFAULT_NARRATION
        );
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
            CategoryButton.BACKGROUND,
            this.getX(),
            this.getY(),
            0,
            offsetV,
            this.width,
            this.height,
            86,
            80
        );

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.getX(), this.getY(), 0);
        pose.scale(0.75f, 0.75f, 1.0f);

        ICategory category = this.entry().getCategory();

        ItemStack icon = category.icon().copy();
        int x = 4;
        int y = 4;
        graphics.renderItem(icon, x, y);
        graphics.renderItemDecorations(this.font, icon, x, y);

        pose.popPose();

        Component name = category.name();
        this.setTooltip(Tooltip.create(
            MultilineComponentHelper.create()
                .addln("screen.anvilcraft.storage.category.name", name)
                .addln("screen.anvilcraft.storage.category.mode", this.mode.getModeName())
                .build()
        ));

        int left = this.getX() + 17;
        int top = this.getY() + 5;
        GuiRenderSupport.centeredEllipsisText(graphics, this.font, name, left, top, 65);
    }
}
