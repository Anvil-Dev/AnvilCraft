package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.gui.component.category.CategorySetting;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.StorageMenu;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "storage_station");
    private static final Identifier CAPACITY = SharedTextures.textureGui("misc/storage_station/capacity");
    private @Nullable CategorySetting setting;
    @Getter
    private @Nullable CategoryList categories;

    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 300, 222);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - 106 - this.font.width(this.title)) / 2 + 106;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        PlayerSetting setting = PlayerSettings.getSetting(this.menu.getPlayer());
        this.setting = new CategorySetting(
            this.leftPos,
            this.topPos,
            setting,
            this
        );
        this.setting.active = false;
        this.setting.visible = false;

        this.categories = this.addRenderableWidget(new CategoryList(
            this.leftPos + 7,
            this.topPos + 49,
            setting,
            _ -> this.reorder(),
            _ -> {
                this.setting.rebuild();
                this.setting.active = true;
                this.setting.visible = true;
                this.categories.active = false;
                this.categories.visible = false;
            }
        ));

        this.addRenderableWidget(this.setting);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            StorageScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            512,
            256
        );

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            StorageScreen.CAPACITY,
            this.leftPos + 106,
            this.topPos,
            0,
            0,
            Mth.ceil(194 * this.menu.getState().getFullness()),
            13,
            194,
            13
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        if (this.setting.active) {
            return;
        }
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    protected void reorder() {
    }
}
