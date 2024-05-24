package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.ActiveSilencerScreen;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SilencerSoundList extends ObjectSelectionList<SilencerSoundList.SoundEntry> {

    public static final ResourceLocation ACTIVE_SILENCER_ADD =
            AnvilCraft.of("textures/gui/container/machine/active_silencer_button_remove.png");
    public static final ResourceLocation ACTIVE_SILENCER_REMOVE =
            AnvilCraft.of("textures/gui/container/machine/active_silencer_button_add.png");

    private final ResourceLocation buttonTexture;

    public SilencerSoundList(Minecraft minecraft,
                             ActiveSilencerScreen screen,
                             int width,
                             int height,
                             int x0,
                             int y0,
                             ResourceLocation texture) {
        super(minecraft, width, height, y0, y0 + height, 20);
        this.buttonTexture = texture;
        this.x0 = x0;
        this.x1 = x0 + width;
        this.centerListVertically = false;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.setRenderHeader(false, 0);
        this.setRenderSelection(true);
    }

    public void addEntry(
            ResourceLocation sound,
            Component text,
            Consumer<SoundEntryClickEventArgs> callback
    ) {
        SoundEntry entry = new SoundEntry(sound, text, buttonTexture) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                callback.accept(new SoundEntryClickEventArgs(SilencerSoundList.this, this.getSound()));
                return true;
            }
        };
        addEntry(entry);
    }

    public void removeEntry(ResourceLocation sound) {
        children().removeIf(it -> it.sound.equals(sound));
    }

    public record SoundEntryClickEventArgs(
            SilencerSoundList thiz,
            ResourceLocation sound
    ) {
    }

    @Getter
    public abstract static class SoundEntry extends ObjectSelectionList.Entry<SoundEntry> {
        private final ResourceLocation sound;
        private final Component text;
        private final ResourceLocation background;

        public SoundEntry(ResourceLocation sound, Component text, ResourceLocation background) {
            this.sound = sound;
            this.text = text;
            this.background = background;
        }

        @Override
        public Component getNarration() {
            return Component.literal("");
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
            guiGraphics.blit(
                    background,
                    top,
                    left,
                    0,
                    hovering ? 20 : 0,
                    width,
                    height,
                    75,
                    40
            );
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    text,
                    top + 2,
                    left + 2,
                    0x404040
            );
        }

        @Override
        public abstract boolean mouseClicked(double mouseX, double mouseY, int button);
    }
}
