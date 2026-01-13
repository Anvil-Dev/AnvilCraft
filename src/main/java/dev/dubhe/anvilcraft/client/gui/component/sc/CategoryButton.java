package dev.dubhe.anvilcraft.client.gui.component.sc;

import dev.dubhe.anvilcraft.api.sc.category.CategoryMode;
import dev.dubhe.anvilcraft.api.sc.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.client.util.RegistryUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorage;
import dev.dubhe.anvilcraft.util.component.MultilineComponentHelper;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

@Getter
@Setter
public class CategoryButton extends Button {
    private final Font font = Minecraft.getInstance().font;
    private final ClientSCStorage storage;
    private final int providerId;
    private CategoryMode mode;

    protected CategoryButton(int x, ClientSCStorage storage, int providerId, CategoryMode mode, OnPress onPress) {
        super(
            x,
            0,
            86,
            20,
            Component.empty(),
            button -> {
                if (!(button instanceof CategoryButton category)) return;
                category.mode = category.storage.getCategories().changeMode(category.provider());
                onPress.onPress(button);
            },
            DEFAULT_NARRATION
        );
        this.storage = storage;
        this.providerId = providerId;
        this.mode = mode;
    }

    protected CategoryProvider provider() {
        return this.storage.getCategories().getProviders().get(this.providerId);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        int offsetV = this.mode.getTexYDiff();
        if (this.isHovered) {
            offsetV = 20; // Hovered part
        }
        graphics.blit(
            TextureConstants.SHULKER_CONTAINER_CATEGORY,
            this.getX(),
            this.getY(),
            0,
            offsetV,
            this.width,
            this.height,
            86,
            80
        );

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.getX(), this.getY(), 20);
        pose.scale(0.75f, 0.75f, 1);

        @SuppressWarnings("DataFlowIssue")
        var category = this.provider().get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));

        ItemStack icon = category.icon();
        int x = 4;
        int y = 4;
        graphics.renderFakeItem(icon, x, y);
        graphics.renderItemDecorations(this.font, icon, x, y);

        pose.popPose();

        Component name = category.name();
        this.setTooltip(Tooltip.create(
            MultilineComponentHelper.create()
                .addln("screen.anvilcraft.shulker_container.category.name", name)
                .addln("screen.anvilcraft.shulker_container.category", this.mode.getDisplayName())
                .build()
        ));
        int left = this.getX() + 17;
        int top = this.getY() + 5;
        int width = this.font.width(name);
        if (width < 65) { // 小于最大宽度，需要居中
            graphics.drawCenteredString(this.font, name, left + 32, top, 0xFFFFFFFF);
        } else if (width > 65) { // 大于最大宽度，需要截断
            graphics.drawString(
                this.font,
                Language.getInstance().getVisualOrder(FormattedText.composite(
                    this.font.substrByWidth(
                        name,
                        65 - this.font.width(CommonComponents.ELLIPSIS)
                    ),
                    CommonComponents.ELLIPSIS
                )),
                left,
                top,
                0xFFFFFFFF,
                true
            );
        } else { // 等于最大宽度，直接渲染
            graphics.drawString(this.font, name, left, top, 0xFFFFFFFF, false);
        }
    }
}
