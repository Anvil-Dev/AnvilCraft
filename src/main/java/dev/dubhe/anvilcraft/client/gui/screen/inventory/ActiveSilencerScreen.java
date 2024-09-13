package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.SilencerButton;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.network.AddMutedSoundPacket;
import dev.dubhe.anvilcraft.network.RemoveMutedSoundPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ActiveSilencerScreen extends AbstractContainerScreen<ActiveSilencerMenu> {

    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/active_silencer.png");

    public static final ResourceLocation ACTIVE_SILENCER_SLIDER =
            AnvilCraft.of("textures/gui/container/machine/active_silencer_slider.png");

    private static final int SCROLL_BAR_HEIGHT = 120;
    private static final int SCROLL_BAR_TOP_POS_Y = 35;
    private static final int START_LEFT_X = 6;
    private static final int START_RIGHT_X = 132;
    private static final int SCROLL_BAR_START_LEFT_X = 120;
    private static final int SCROLL_BAR_START_RIGHT_X = 245;
    private static final int SCROLL_BAR_WIDTH = 5;
    private static final int SCROLLER_HEIGHT = 9;

    public static final int SOUND_FILTERED = 0;
    public static final int SOUND_MUTED = 1;

    private final ActiveSilencerMenu menu;
    private final SilencerButton[] allSoundButtons = new SilencerButton[8];
    private final SilencerButton[] mutedSoundButtons = new SilencerButton[8];
    private EditBox editBox;
    private int leftScrollOff;
    private int rightScrollOff;

    @Getter
    private String filterText = "";

    private boolean isDraggingLeft;
    private boolean isDraggingRight;
    private final List<Pair<ResourceLocation, Component>> allSounds = new ArrayList<>();
    private final List<Pair<ResourceLocation, Component>> filteredSounds = new ArrayList<>();
    private final List<Pair<ResourceLocation, Component>> mutedSounds = new ArrayList<>();

    private void onSearchTextChange(String text) {
        leftScrollOff = 0;
        filteredSounds.clear();
        if (text == null || text.isEmpty()) {
            this.filterText = "";
            filteredSounds.addAll(allSounds);
            filteredSounds.removeAll(mutedSounds);
            return;
        } else {
            this.filterText = text;
        }

        if (text.startsWith("#")) {
            String search = text.replaceFirst("#", "");
            allSounds.stream()
                    .filter(it -> it.left().toString().contains(search))
                    .filter(it ->
                            mutedSounds.stream().noneMatch(it1 -> it1.left().equals(it.first())))
                    .forEach(filteredSounds::add);
        } else {
            if (text.startsWith("~")) {
                try {
                    Pattern search = Pattern.compile(text.replaceFirst("~", ""));
                    allSounds.stream()
                            .filter(it -> search.matcher(it.left().toString()).matches())
                            .filter(it -> mutedSounds.stream()
                                    .noneMatch(it1 -> it1.left().equals(it.first())))
                            .forEach(filteredSounds::add);
                } catch (Exception ignored) {
                    // intentionally empty
                }
            }
            allSounds.stream()
                    .filter(it -> it.right().getString().contains(filterText))
                    .filter(it ->
                            mutedSounds.stream().noneMatch(it1 -> it1.left().equals(it.first())))
                    .forEach(filteredSounds::add);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers);
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private void refreshSoundList() {
        onSearchTextChange(filterText);
    }

    private void onAllSoundButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += leftScrollOff;
        if (filteredSounds.isEmpty() || actualIndex >= filteredSounds.size()) return;
        ResourceLocation sound = filteredSounds.get(actualIndex).left();
        addMutedSound(sound);
        PacketDistributor.sendToServer(new AddMutedSoundPacket(sound));
        refreshSoundList();
    }

    private void onMutedSoundButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += rightScrollOff;
        if (mutedSounds.isEmpty() || actualIndex >= mutedSounds.size()) return;
        ResourceLocation sound = mutedSounds.get(actualIndex).left();
        removeMutedSound(sound);
        PacketDistributor.sendToServer(new RemoveMutedSoundPacket(sound));
        refreshSoundList();
    }

    void addMutedSound(ResourceLocation sound) {
        this.menu.addSound(sound);
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        WeighedSoundEvents event = manager.getSoundEvent(sound);
        if (event == null) return;
        this.mutedSounds.add(Pair.of(sound, event.getSubtitle() == null ? Component.empty() : event.getSubtitle()));
    }

    void removeMutedSound(ResourceLocation sound) {
        this.menu.removeSound(sound);
        this.mutedSounds.removeIf(it -> it.left().equals(sound));
    }

    /**
     * 获取屏幕上某一项的声音字幕
     */
    public Component getSoundTextAt(int index, int variant) {
        int actualIndex = index;
        if (variant == SOUND_FILTERED) {
            actualIndex += leftScrollOff;
            if (filteredSounds.isEmpty() || actualIndex >= filteredSounds.size()) return Component.empty();
            return filteredSounds.get(actualIndex).right();
        } else {
            actualIndex += rightScrollOff;
            if (mutedSounds.isEmpty() || actualIndex >= mutedSounds.size()) return Component.empty();
            return mutedSounds.get(actualIndex).right();
        }
    }

    /**
     * 获取屏幕上某一项的声音id
     */
    public ResourceLocation getSoundIdAt(int index, int variant) {
        int actualIndex = index;
        if (variant == SOUND_FILTERED) {
            actualIndex += leftScrollOff;
            if (filteredSounds.isEmpty() || actualIndex >= filteredSounds.size()) return null;
            return filteredSounds.get(actualIndex).left();
        } else {
            actualIndex += rightScrollOff;
            if (mutedSounds.isEmpty() || actualIndex >= mutedSounds.size()) return null;
            return mutedSounds.get(actualIndex).left();
        }
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
            SilencerButton button = new SilencerButton(
                    leftPos + START_LEFT_X,
                    buttonTop,
                    l,
                    SOUND_FILTERED,
                    b -> {
                        if (b instanceof SilencerButton silencerButton) {
                            onAllSoundButtonClick(silencerButton.getIndex());
                        }
                    },
                    this,
                    "add");
            button.setWidth(112);
            this.allSoundButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }

        buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            SilencerButton button = new SilencerButton(
                    leftPos + START_RIGHT_X,
                    buttonTop,
                    l,
                    SOUND_MUTED,
                    b -> {
                        if (b instanceof SilencerButton silencerButton) {
                            onMutedSoundButtonClick(silencerButton.getIndex());
                        }
                    },
                    this,
                    "remove");
            this.mutedSoundButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }

        editBox = new EditBox(
                this.minecraft.font,
                leftPos + 78,
                topPos + 19,
                100,
                12,
                Component.translatable("screen.anvilcraft.active_silencer.search"));
        editBox.setResponder(this::onSearchTextChange);
        addRenderableWidget(editBox);

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        BuiltInRegistries.SOUND_EVENT.stream()
                .map(it -> Pair.of(it.getLocation(), manager.getSoundEvent(it.getLocation())))
                .filter(it -> it.second() != null)
                .filter(it -> it.second().getSubtitle() != null)
                .forEach(it -> allSounds.add(Pair.of(it.first(), it.second().getSubtitle())));
        filteredSounds.addAll(allSounds);
    }

    private boolean mouseInLeft(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + START_LEFT_X
                && mouseX <= leftPos + SCROLL_BAR_START_LEFT_X + SCROLL_BAR_WIDTH
                && mouseY >= topPos + SCROLL_BAR_TOP_POS_Y
                && mouseY <= topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT;
    }

    private boolean mouseInRight(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + START_RIGHT_X
                && mouseX <= leftPos + SCROLL_BAR_START_RIGHT_X + SCROLL_BAR_WIDTH
                && mouseY >= topPos + SCROLL_BAR_TOP_POS_Y
                && mouseY <= topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT;
    }

    private boolean mouseInLeftSlider(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + SCROLL_BAR_START_LEFT_X
                && mouseX <= leftPos + SCROLL_BAR_START_LEFT_X + SCROLL_BAR_WIDTH
                && mouseY >= topPos + SCROLL_BAR_TOP_POS_Y
                && mouseY <= topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT;
    }

    private boolean mouseInRightSlider(double mouseX, double mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + SCROLL_BAR_START_RIGHT_X
                && mouseX <= leftPos + SCROLL_BAR_START_RIGHT_X + SCROLL_BAR_WIDTH
                && mouseY >= topPos + SCROLL_BAR_TOP_POS_Y
                && mouseY <= topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double pScrollX, double pScrollY) {
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;
        if (mouseInLeft(mouseX, mouseY, leftPos, topPos)) {
            if (this.filteredSounds.size() > 8) {
                this.leftScrollOff = (int) Mth.clamp(this.leftScrollOff - pScrollY, 0, this.filteredSounds.size() - 7);
            }
        } else {
            if (mouseInRight(mouseX, mouseY, leftPos, topPos)) {
                if (this.mutedSounds.size() > 8) {
                    this.rightScrollOff =
                            (int) Mth.clamp(this.rightScrollOff - pScrollY, 0, this.mutedSounds.size() - 7);
                }
            }
        }
        return true;
    }

    /**
     * 鼠标拖动事件
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;
        if (mouseInLeftSlider(mouseX, mouseY, leftPos, topPos)) {
            int i = filteredSounds.size();
            if (this.isDraggingLeft) {
                int j = this.topPos + SCROLL_BAR_TOP_POS_Y;
                int k = j + SCROLL_BAR_HEIGHT;
                int dragMax = i - 7;
                float scroll = (float) ((mouseY - j - 13.5F) / ((k - j) - 27.0F));
                scroll = scroll * dragMax + 0.5F;
                this.leftScrollOff = Mth.clamp((int) scroll, 0, dragMax);
                return true;
            } else {
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
        } else {
            if (mouseInRightSlider(mouseX, mouseY, leftPos, topPos)) {
                int i = mutedSounds.size();
                if (this.isDraggingRight) {
                    int j = this.topPos + SCROLL_BAR_TOP_POS_Y;
                    int k = j + SCROLL_BAR_HEIGHT;
                    int dragMax = i - 7;
                    float scroll = (float) ((mouseY - j - 13.5F) / ((k - j) - 27.0F));
                    scroll = scroll * dragMax + 0.5F;
                    this.rightScrollOff = Mth.clamp((int) scroll, 0, dragMax);
                    return true;
                } else {
                    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 鼠标点击
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        isDraggingLeft = false;
        isDraggingRight = false;
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;
        if (mouseInLeftSlider(mouseX, mouseY, leftPos, topPos) && filteredSounds.size() > 8) {
            this.isDraggingLeft = true;
        }
        if (mouseInRightSlider(mouseX, mouseY, leftPos, topPos) && mutedSounds.size() > 8) {
            this.isDraggingRight = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderScroller(GuiGraphics guiGraphics, int posX, int posY, int totalCount, int scrollOff) {
        int i = totalCount + 1 - 8;
        if (i > 1) {
            int maxY = posY + SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
            int scrollY = (int) (posY + (scrollOff / (float) totalCount) * SCROLL_BAR_HEIGHT);
            scrollY = Mth.clamp(scrollY, posY, maxY);

            guiGraphics.blit(ACTIVE_SILENCER_SLIDER, posX, scrollY, 0, 0, 5, 9, 10, 9);
        } else {
            guiGraphics.blit(ACTIVE_SILENCER_SLIDER, posX, posY, 0, 0, 5, 9, 10, 9);
        }
    }

    /**
     * 渲染
     */
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderScroller(guiGraphics, leftPos + 119, topPos + 35, filteredSounds.size(), leftScrollOff);

        this.renderScroller(guiGraphics, leftPos + 245, topPos + 35, mutedSounds.size(), rightScrollOff);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 处理静音同步包
     */
    public void handleSync(List<ResourceLocation> sounds) {
        rightScrollOff = 0;
        mutedSounds.clear();
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (ResourceLocation sound : sounds) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events == null || events.getSubtitle() == null) return;
            mutedSounds.add(Pair.of(sound, events.getSubtitle()));
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
