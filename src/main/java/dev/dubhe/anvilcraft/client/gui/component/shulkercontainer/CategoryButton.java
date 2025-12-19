package dev.dubhe.anvilcraft.client.gui.component.shulkercontainer;

import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.category.CategoryMode;
import dev.dubhe.anvilcraft.api.container.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.client.util.RegistryUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.init.ModRegistries;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Getter
@Setter
public class CategoryButton extends Button {
    private final Font font = Minecraft.getInstance().font;
    private final ContainerStorage storage;
    private final int providerId;
    private CategoryMode mode;

    protected CategoryButton(int x, ContainerStorage storage, int providerId, OnPress onPress) {
        this(x, storage, providerId, CategoryMode.UNLIMITED, onPress);
    }

    protected CategoryButton(int x, ContainerStorage storage, int providerId, CategoryMode mode, OnPress onPress) {
        super(
            x,
            0,
            86,
            20,
            Component.empty(),
            button -> {
                if (!(button instanceof CategoryButton category)) return;
                category.mode = category.storage.getClientCategories().changeMode(category.provider());
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

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
        int left = this.getX() + 17;
        int top = this.getY() + 5;
        int width = this.font.width(name);
        if (width < 65) { // 小于最大宽度，需要居中
            graphics.drawCenteredString(this.font, name, left + 32, top, 0xFFFFFFFF);
        } else if (width > 65) { // 大于最大宽度，需要左右横跳
            graphics.drawScrollingString(this.font, name, left, left + 65, top, 0xFFFFFFFF);
        } else { // 等于最大宽度，直接渲染
            graphics.drawString(this.font, name, left, top, 0xFFFFFFFF, false);
        }
    }
}
