package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SilencerSoundList extends ObjectSelectionList<SilencerSoundList.SoundEntry> {

    public static final ResourceLocation ACTIVE_SILENCER_ADD =
            AnvilCraft.of("textures/gui/container/machine/active_silencer_button_add.png");
    public static final ResourceLocation ACTIVE_SILENCER_REMOVE =
            AnvilCraft.of("textures/gui/container/machine/active_silencer_button_remove.png");
    public static final ResourceLocation ACTIVE_SILENCER_SLIDER =
            AnvilCraft.of("textures/gui/container/machine/active_silencer_slider.png");

    private final ResourceLocation buttonTexture;
    private final ActiveSilencerScreen parent;
    private final int listWidth;
    private SoundEntry hovered;

    /**
     * 主动消音器的列表 View
     *
     * @param listWidth 列表宽度
     * @param top       列表顶部 y 坐标
     * @param bottom    列表底部 y 坐标
     * @param texture   按钮材质
     */
    public SilencerSoundList(Minecraft minecraft,
                             ActiveSilencerScreen parent,
                             int listWidth,
                             int top,
                             int bottom,
                             ResourceLocation texture) {
        super(minecraft, listWidth, parent.height, top, bottom, 15);
        this.parent = parent;
        this.buttonTexture = texture;
        this.listWidth = listWidth;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.setRenderHeader(false, 0);
        this.setRenderSelection(true);
    }

    /**
     * 添加新声音项
     */
    public void addEntry(
            ResourceLocation sound,
            Component text,
            Consumer<SoundEntryClickEventArgs> callback
    ) {
        SoundEntry entry = new SoundEntry(sound, text, buttonTexture, this) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                callback.accept(new SoundEntryClickEventArgs(SilencerSoundList.this, this.getSound(), this.getText()));
                return true;
            }
        };
        addEntry(entry);
    }

    /**
     * 添加新声音项
     *
     * @param handler 对新声音项进行添加前处理
     */
    public void addEntry(
            ResourceLocation sound,
            Component text,
            Consumer<SoundEntryClickEventArgs> callback,
            Consumer<SoundEntry> handler
    ) {
        SoundEntry entry = new SoundEntry(sound, text, buttonTexture, this) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                callback.accept(new SoundEntryClickEventArgs(SilencerSoundList.this, this.getSound(), this.getText()));
                return true;
            }
        };
        handler.accept(entry);
        addEntry(entry);
    }

    public void removeEntry(ResourceLocation sound) {
        children().removeIf(it -> it.sound.equals(sound));
    }

    public record SoundEntryClickEventArgs(
            SilencerSoundList thiz,
            ResourceLocation sound,
            Component text
    ) {
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.listWidth - 4;
    }

    @Override
    public int getRowWidth() {
        return this.listWidth;
    }

    /**
     * 渲染
     */
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int scrollbarPosition = this.getScrollbarPosition();
        this.hovered = this.isMouseOver(mouseX, mouseY) ? this.getEntryAtPosition(mouseX, mouseY) : null;
        this.enableScissor(guiGraphics);
        this.renderList(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();
        int m = this.getMaxScroll();
        if (m > 0) {
            int n = (int) ((float) ((this.y1 - this.y0) * (this.y1 - this.y0)) / (float) this.getMaxPosition());
            n = Mth.clamp(n, 32, this.y1 - this.y0 - 8);
            int o = (int) this.getScrollAmount() * (this.y1 - this.y0 - n) / m + this.y0;
            if (o < this.y0) {
                o = this.y0;
            }
            guiGraphics.blit(ACTIVE_SILENCER_SLIDER, scrollbarPosition + 1, o, 0, 0, 5, 9, 10, 9);
        }
        this.renderDecorations(guiGraphics, mouseX, mouseY);
        if (hovered != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.renderComponentTooltip(
                    Minecraft.getInstance().font,
                    List.of(
                            hovered.text.copy().withStyle(ChatFormatting.WHITE),
                            Component.literal(hovered.sound.toString()).copy().withStyle(ChatFormatting.GRAY)
                    ),
                    mouseX,
                    mouseY
            );
            guiGraphics.pose().popPose();
        }
        RenderSystem.disableBlend();
    }

    @Nullable
    @Override
    public SilencerSoundList.SoundEntry getHovered() {
        return hovered;
    }

    protected void renderItem(
            @NotNull GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            int index,
            int left,
            int top,
            int width,
            int height
    ) {
        SoundEntry entry = this.getEntry(index);
        entry.renderBack(
                guiGraphics, index, top, left, width, height, mouseX, mouseY,
                Objects.equals(this.hovered, entry),
                partialTick);
        entry.render(
                guiGraphics, index, top, left, width, height, mouseX, mouseY,
                Objects.equals(this.hovered, entry),
                partialTick
        );
    }

    /**
     * 声音项
     */
    @Getter
    public abstract static class SoundEntry extends ObjectSelectionList.Entry<SoundEntry> {
        private final ResourceLocation sound;
        private final Component text;
        private final ResourceLocation background;
        private final SilencerSoundList parent;
        @Setter
        private int textOffsetX = 0;

        /**
         * 声音项
         */
        public SoundEntry(ResourceLocation sound, Component text, ResourceLocation background, SilencerSoundList parent) {
            this.sound = sound;
            this.text = text;
            this.background = background;
            this.parent = parent;
        }

        @Override
        public @NotNull Component getNarration() {
            return text;
        }

        @Override
        public void render(
                @NotNull GuiGraphics guiGraphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovering,
                float partialTick
        ) {
            guiGraphics.pose().pushPose();

            hovering = parent.hovered.equals(this);

            guiGraphics.blit(
                    background,
                    left,
                    top,
                    0,
                    hovering ? 30 : 0,
                    112,
                    15,
                    112,
                    30
            );

            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    text,
                    left + 2 + textOffsetX,
                    top + 3,
                    0xff_ff_ff
            );
            guiGraphics.pose().popPose();
        }

        @Override
        public abstract boolean mouseClicked(double mouseX, double mouseY, int button);
    }
}
