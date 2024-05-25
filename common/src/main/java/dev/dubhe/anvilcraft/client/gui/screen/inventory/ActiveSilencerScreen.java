package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.SilencerSoundList;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.network.ServerboundAddMutedSoundPacket;
import dev.dubhe.anvilcraft.network.ServerboundRemoveMutedSoundPacket;
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

import java.util.List;
import java.util.Set;

public class ActiveSilencerScreen extends AbstractContainerScreen<ActiveSilencerMenu> {

    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/active_silencer.png");

    private final ActiveSilencerMenu menu;

    private SilencerSoundList allSoundList;
    private SilencerSoundList mutedSoundList;

    /**
     * 主动消音器gui
     */
    public ActiveSilencerScreen(ActiveSilencerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;

    }

    @Override
    protected void init() {
        super.init();

        allSoundList = new SilencerSoundList(
                Minecraft.getInstance(),
                this,
                80,
                topPos + 33,
                topPos + 155,
                SilencerSoundList.ACTIVE_SILENCER_ADD
        );
        allSoundList.setLeftPos(leftPos + 3);

        mutedSoundList = new SilencerSoundList(
                Minecraft.getInstance(),
                this,
                80,
                topPos + 33,
                topPos + 155,
                SilencerSoundList.ACTIVE_SILENCER_REMOVE
        );
        mutedSoundList.setLeftPos(leftPos + 89);

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        Set<ResourceLocation> muted = menu.getBlockEntity().getMutedSound();
        for (ResourceLocation sound : BuiltInRegistries.SOUND_EVENT.keySet()) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events != null && events.getSubtitle() != null) {
                allSoundList.addEntry(
                        sound,
                        events.getSubtitle(),
                        this::add
                );
            }
        }
        for (ResourceLocation sound : muted) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events != null && events.getSubtitle() != null) {
                mutedSoundList.addEntry(
                        sound,
                        events.getSubtitle(),
                        this::remove,
                        (x) -> x.setTextOffsetX(8)
                );
                allSoundList.removeEntry(sound);
            }
        }

        addRenderableWidget(allSoundList);
        addRenderableWidget(mutedSoundList);
    }

    private void add(SilencerSoundList.SoundEntryClickEventArgs args) {
        allSoundList.removeEntry(args.sound());
        mutedSoundList.addEntry(args.sound(), args.text(), this::remove, (x) -> x.setTextOffsetX(8));
        new ServerboundAddMutedSoundPacket(args.sound()).send();
    }

    private void remove(SilencerSoundList.SoundEntryClickEventArgs args) {
        mutedSoundList.removeEntry(args.sound());
        allSoundList.addEntry(args.sound(), args.text(), this::add);
        new ServerboundRemoveMutedSoundPacket(args.sound()).send();
    }

    public void handleSync(List<ResourceLocation> sounds) {
        mutedSoundList.children().clear();
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (ResourceLocation sound : sounds) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events != null && events.getSubtitle() != null) {
                mutedSoundList.addEntry(
                        sound,
                        events.getSubtitle(),
                        this::remove,
                        (x) -> x.setTextOffsetX(8)
                );
                allSoundList.removeEntry(sound);
            }
        }
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
