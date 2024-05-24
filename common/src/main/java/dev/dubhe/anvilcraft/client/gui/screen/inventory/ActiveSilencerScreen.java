package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.SilencerSoundList;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class ActiveSilencerScreen extends AbstractContainerScreen<ActiveSilencerMenu> {

    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/active_silencer.png");

    private final ActiveSilencerMenu menu;

    private SilencerSoundList allSoundList;
    private SilencerSoundList mutedSoundList;

    public ActiveSilencerScreen(ActiveSilencerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;

    }

    @Override
    protected void init() {
        super.init();

        int offsetX = (this.width - this.imageWidth) / 2;
        int offsetY = (this.height - this.imageHeight) / 2;

        allSoundList = new SilencerSoundList(
                Minecraft.getInstance(),
                this,
                80,
                122,
                offsetX + 6,
                offsetY + 34,
                SilencerSoundList.ACTIVE_SILENCER_ADD
        );

        mutedSoundList = new SilencerSoundList(
                Minecraft.getInstance(),
                this,
                80,
                122,
                topPos + 92,
                leftPos + 34,
                SilencerSoundList.ACTIVE_SILENCER_REMOVE
        );

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (ResourceLocation sound : BuiltInRegistries.SOUND_EVENT.keySet()) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events != null && events.getSubtitle() != null) {
                allSoundList.addEntry(
                        sound,
                        events.getSubtitle(),
                        args -> {
                            System.out.println("Clicked add " + args);
                        }
                );
            }
        }
        for (ResourceLocation sound : menu.getBlockEntity().getMutedSound()) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events != null && events.getSubtitle() != null) {
                mutedSoundList.addEntry(
                        sound,
                        events.getSubtitle(),
                        args -> {
                            System.out.println("Clicked remove " + args);
                        }
                );
            }
        }

        addRenderableWidget(allSoundList);
        addRenderableWidget(mutedSoundList);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
    }
}
