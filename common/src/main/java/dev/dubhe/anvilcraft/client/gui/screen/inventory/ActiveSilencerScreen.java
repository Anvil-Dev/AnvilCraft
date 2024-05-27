package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.SilencerButton;
import dev.dubhe.anvilcraft.client.gui.component.SilencerSoundList;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.network.ServerboundAddMutedSoundPacket;
import dev.dubhe.anvilcraft.network.ServerboundRemoveMutedSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ActiveSilencerScreen extends AbstractContainerScreen<ActiveSilencerMenu> {

    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/active_silencer.png");

    private final ActiveSilencerMenu menu;
    private final SilencerButton[] allSoundButtons = new SilencerButton[8];
    private final SilencerButton[] mutedSoundButtons = new SilencerButton[8];
    int allSoundButtonScrollOff;
    int mutedSoundButtonScrollOff;
    private boolean isDragging;


    private void onAllSoundButtonClick(int selectedIndex) {

    }

    private void onMutedSoundButtonClick(int selectedIndex) {

    }

    /**
     * 主动消音器gui
     */
    public ActiveSilencerScreen(ActiveSilencerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();

        int buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            SilencerButton button = new SilencerButton(leftPos + 7, buttonTop, l, "add", b -> {
                if (b instanceof SilencerButton silencerButton) {
                    onAllSoundButtonClick(silencerButton.getIndex() + this.allSoundButtonScrollOff);
                }
            });
            this.allSoundButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }

        buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            SilencerButton button = new SilencerButton(leftPos + 5, buttonTop, l, "remove", b -> {
                if (b instanceof SilencerButton silencerButton) {
                    onMutedSoundButtonClick(silencerButton.getIndex() + this.mutedSoundButtonScrollOff);
                }
            });
            this.allSoundButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }
    }

    private void add(SilencerSoundList.SoundEntryClickEventArgs args) {
        new ServerboundAddMutedSoundPacket(args.sound()).send();
    }

    private void remove(SilencerSoundList.SoundEntryClickEventArgs args) {
        new ServerboundRemoveMutedSoundPacket(args.sound()).send();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

    }

    /**
     * 处理静音同步包
     */
    public void handleSync(List<ResourceLocation> sounds) {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (ResourceLocation sound : sounds) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);

        }
        menu.handleSync(sounds);
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
