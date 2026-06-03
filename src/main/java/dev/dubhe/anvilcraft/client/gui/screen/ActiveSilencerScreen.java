package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.gui.component.SilencerButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.network.SilencerAddMutedPacket;
import dev.dubhe.anvilcraft.network.SilencerRemoveMutedPacket;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ActiveSilencerScreen extends AbstractContainerScreen<ActiveSilencerMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "active_silencer");
    public static final Identifier SLIDER = SharedTextures.SMALL_SLIDER;

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
    private int leftScrollOff;
    private int rightScrollOff;

    @Getter
    private String filterText = "";

    @Setter
    private @Nullable List<ClientTooltipComponent> tooltipComponents;

    private boolean isDraggingLeft;
    private boolean isDraggingRight;
    private final List<Pair<Identifier, Component>> allSounds = new ArrayList<>();
    private final List<Pair<Identifier, Component>> filteredSounds = new ArrayList<>();
    private final List<Pair<Identifier, Component>> mutedSounds = new ArrayList<>();

    private void onSearchTextChange(@Nullable String text) {
        this.leftScrollOff = 0;
        this.filteredSounds.clear();
        if (text == null || text.isEmpty()) {
            this.filterText = "";
            this.filteredSounds.addAll(this.allSounds);
            this.filteredSounds.removeAll(this.mutedSounds);
            return;
        } else {
            this.filterText = text;
        }

        if (text.startsWith("#")) {
            String search = text.replaceFirst("#", "");
            this.allSounds.stream()
                .filter(it -> it.left().toString().contains(search))
                .filter(it -> this.mutedSounds.stream().noneMatch(it1 -> it1.left().equals(it.first())))
                .forEach(this.filteredSounds::add);
        } else {
            if (text.startsWith("~")) {
                try {
                    Pattern search = Pattern.compile(text.replaceFirst("~", ""));
                    this.allSounds.stream()
                        .filter(it -> search.matcher(it.left().toString()).matches())
                        .filter(it -> this.mutedSounds.stream()
                            .noneMatch(it1 -> it1.left().equals(it.first())))
                        .forEach(this.filteredSounds::add);
                } catch (Exception ignored) {
                    // intentionally empty
                }
            }
            this.allSounds.stream()
                .filter(it -> it.right().getString().contains(this.filterText))
                .filter(it -> this.mutedSounds.stream().noneMatch(it1 -> it1.left().equals(it.first())))
                .forEach(this.filteredSounds::add);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.getFocused() instanceof EditBox) {
            return this.getFocused().keyPressed(event);
        } else {
            return super.keyPressed(event);
        }
    }

    private void refreshSoundList() {
        this.onSearchTextChange(this.filterText);
    }

    private void onAllSoundButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += this.leftScrollOff;
        if (this.filteredSounds.isEmpty() || actualIndex >= this.filteredSounds.size()) return;
        Identifier sound = this.filteredSounds.get(actualIndex).left();
        this.addMutedSound(sound);
        ClientPacketDistributor.sendToServer(new SilencerAddMutedPacket(sound));
        this.refreshSoundList();
    }

    private void onMutedSoundButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += this.rightScrollOff;
        if (this.mutedSounds.isEmpty() || actualIndex >= this.mutedSounds.size()) return;
        Identifier sound = this.mutedSounds.get(actualIndex).left();
        this.removeMutedSound(sound);
        ClientPacketDistributor.sendToServer(new SilencerRemoveMutedPacket(sound));
        this.refreshSoundList();
    }

    void addMutedSound(Identifier sound) {
        this.menu.addSound(sound);
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        WeighedSoundEvents event = manager.getSoundEvent(sound);
        if (event == null) return;
        Component subtitle = event.getSubtitle();
        this.mutedSounds.add(Pair.of(sound, Objects.requireNonNullElseGet(subtitle, Component::empty)));
    }

    void removeMutedSound(Identifier sound) {
        this.menu.removeSound(sound);
        this.mutedSounds.removeIf(it -> it.left().equals(sound));
    }

    /// 获取屏幕上某一项的声音字幕
    public Component getSoundTextAt(int index, int variant) {
        int actualIndex = index;
        if (variant == SOUND_FILTERED) {
            actualIndex += this.leftScrollOff;
            if (this.filteredSounds.isEmpty() || actualIndex >= this.filteredSounds.size()) return Component.empty();
            return this.filteredSounds.get(actualIndex).right();
        } else {
            actualIndex += this.rightScrollOff;
            if (this.mutedSounds.isEmpty() || actualIndex >= this.mutedSounds.size()) return Component.empty();
            return this.mutedSounds.get(actualIndex).right();
        }
    }

    /// 获取屏幕上某一项的声音id
    public @Nullable Identifier getSoundIdAt(int index, int variant) {
        int actualIndex = index;
        if (variant == SOUND_FILTERED) {
            actualIndex += this.leftScrollOff;
            if (this.filteredSounds.isEmpty() || actualIndex >= this.filteredSounds.size()) return null;
            return this.filteredSounds.get(actualIndex).left();
        } else {
            actualIndex += this.rightScrollOff;
            if (this.mutedSounds.isEmpty() || actualIndex >= this.mutedSounds.size()) return null;
            return this.mutedSounds.get(actualIndex).left();
        }
    }

    /// 主动消音器gui
    public ActiveSilencerScreen(ActiveSilencerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 166);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        int buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            this.addRenderableWidget(new SilencerButton(
                leftPos + START_LEFT_X,
                buttonTop,
                l,
                SOUND_FILTERED,
                b -> {
                    if (b instanceof SilencerButton silencerButton) {
                        this.onAllSoundButtonClick(silencerButton.getIndex());
                    }
                },
                this,
                "add"
            )).setWidth(112);
            buttonTop += 15;
        }

        buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            this.addRenderableWidget(new SilencerButton(
                leftPos + START_RIGHT_X,
                buttonTop,
                l,
                SOUND_MUTED,
                b -> {
                    if (b instanceof SilencerButton silencerButton) {
                        this.onMutedSoundButtonClick(silencerButton.getIndex());
                    }
                },
                this,
                "remove"
            ));
            buttonTop += 15;
        }

        this.addRenderableWidget(new EditBox(
            Objects.requireNonNull(this.minecraft).font,
            leftPos + 78,
            topPos + 19,
            100,
            12,
            Component.translatable("screen.anvilcraft.active_silencer.search")
        )).setResponder(this::onSearchTextChange);

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        // noinspection NullableProblems
        BuiltInRegistries.SOUND_EVENT.stream()
            .map(it -> Pair.of(it.location(), manager.getSoundEvent(it.location())))
            .filter(it -> it.second() != null)
            .filter(it -> it.second().getSubtitle() != null)
            .forEach(it -> this.allSounds.add(Pair.of(it.first(), it.second().getSubtitle())));
        this.filteredSounds.addAll(this.allSounds);
    }

    private boolean mouseInLeft(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + START_LEFT_X,
            topPos + SCROLL_BAR_TOP_POS_Y,
            leftPos + SCROLL_BAR_START_LEFT_X + SCROLL_BAR_WIDTH,
            topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInRight(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + START_RIGHT_X,
            topPos + SCROLL_BAR_TOP_POS_Y,
            leftPos + SCROLL_BAR_START_RIGHT_X + SCROLL_BAR_WIDTH,
            topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInLeftSlider(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + SCROLL_BAR_START_LEFT_X,
            topPos + SCROLL_BAR_TOP_POS_Y,
            leftPos + SCROLL_BAR_START_LEFT_X + SCROLL_BAR_WIDTH,
            topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInRightSlider(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + SCROLL_BAR_START_RIGHT_X,
            topPos + SCROLL_BAR_TOP_POS_Y,
            leftPos + SCROLL_BAR_START_RIGHT_X + SCROLL_BAR_WIDTH,
            topPos + SCROLL_BAR_TOP_POS_Y + SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInEditBox(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + 78,
            topPos + 19,
            leftPos + 78 + 100,
            topPos + 19 + 12
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.mouseInLeft(mouseX, mouseY, this.leftPos, this.topPos)) {
            if (this.filteredSounds.size() > 8) {
                this.leftScrollOff = (int) Mth.clamp(this.leftScrollOff - scrollY, 0, this.filteredSounds.size() - 7);
            }
        } else {
            if (this.mouseInRight(mouseX, mouseY, this.leftPos, this.topPos)) {
                if (this.mutedSounds.size() > 8) {
                    this.rightScrollOff =
                        (int) Mth.clamp(this.rightScrollOff - scrollY, 0, this.mutedSounds.size() - 7);
                }
            }
        }
        return true;
    }

    /// 鼠标拖动事件
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.isDraggingLeft) {
            int i = this.filteredSounds.size();
            int j = this.topPos + SCROLL_BAR_TOP_POS_Y;
            int k = j + SCROLL_BAR_HEIGHT;
            int dragMax = i - 7;
            float scroll = (float) ((event.y() - j - 13.5F) / ((k - j) - 27.0F));
            scroll = scroll * dragMax + 0.5F;
            this.leftScrollOff = Mth.clamp((int) scroll, 0, dragMax);
            return true;
        } else if (this.isDraggingRight) {
            int i = this.mutedSounds.size();
            int j = this.topPos + SCROLL_BAR_TOP_POS_Y;
            int k = j + SCROLL_BAR_HEIGHT;
            int dragMax = i - 7;
            float scroll = (float) ((event.y() - j - 13.5F) / ((k - j) - 27.0F));
            scroll = scroll * dragMax + 0.5F;
            this.rightScrollOff = Mth.clamp((int) scroll, 0, dragMax);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    /// 鼠标点击
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        this.isDraggingLeft = false;
        this.isDraggingRight = false;
        int leftPos = (this.width - this.getImageWidth()) / 2;
        int topPos = (this.height - this.getImageHeight()) / 2;
        if (this.mouseInLeftSlider(event.x(), event.y(), leftPos, topPos) && this.filteredSounds.size() > 8) {
            this.isDraggingLeft = true;
        }
        if (this.mouseInRightSlider(event.x(), event.y(), leftPos, topPos) && this.mutedSounds.size() > 8) {
            this.isDraggingRight = true;
        }
        if (!this.mouseInEditBox(event.x(), event.y(), leftPos, topPos)) {
            this.setFocused(false);
        }
        return super.mouseClicked(event, handled);
    }

    private void extractScroller(GuiGraphicsExtractor graphics, int posX, int posY, int totalCount, int scrollOff) {
        int i = totalCount + 1 - 8;
        if (i > 1) {
            int maxY = posY + SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
            int scrollY = (int) (posY + (scrollOff / (totalCount - 7F)) * (SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT));
            scrollY = Mth.clamp(scrollY, posY, maxY);

            graphics.blit(RenderPipelines.GUI_TEXTURED, SLIDER, posX, scrollY, 0, 0, 5, 9, 10, 9);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLIDER, posX, posY, 0, 0, 5, 9, 10, 9);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.tooltipComponents = null;
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractScroller(graphics, this.leftPos + 119, this.topPos + 35, this.filteredSounds.size(), this.leftScrollOff);
        this.extractScroller(graphics, this.leftPos + 245, this.topPos + 35, this.mutedSounds.size(), this.rightScrollOff);
    }

    public void handleSync(List<Identifier> sounds) {
        this.rightScrollOff = 0;
        this.mutedSounds.clear();
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        for (Identifier sound : sounds) {
            WeighedSoundEvents events = manager.getSoundEvent(sound);
            if (events == null || events.getSubtitle() == null) return;
            // noinspection NullableProblems
            this.mutedSounds.add(Pair.of(sound, events.getSubtitle()));
        }
        this.onSearchTextChange("");
        this.menu.handleSync(sounds);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ActiveSilencerScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.tooltipComponents == null) return;
        graphics.tooltip(this.font, this.tooltipComponents, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }
}
