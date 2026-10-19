package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.SilencerButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.network.SilencerAddMutedPacket;
import dev.dubhe.anvilcraft.network.SilencerRemoveMutedPacket;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ActiveSilencerScreen extends AbstractContainerScreen<ActiveSilencerMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "active_silencer");
    public static final Identifier SLIDER = SharedTextures.SMALL_MACHINE_SLIDER;

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

    private boolean isDraggingLeft;
    private boolean isDraggingRight;
    private final Map<Identifier, Integer> soundCounts = new HashMap<>();
    private final List<Pair<Identifier, Component>> allSounds = new ArrayList<>();
    private final List<Pair<Identifier, Component>> filteredSounds = new ArrayList<>();
    private final List<Pair<Identifier, Component>> mutedSounds = new ArrayList<>();
    private Comparator<Pair<Identifier, Component>> soundOrder = Comparator.comparing(sound -> sound.left().toString());

    private void onSearchTextChange(@Nullable String text) {
        this.leftScrollOff = 0;
        this.filteredSounds.clear();
        Set<Identifier> mutedIds = this.mutedSounds.stream().map(Pair::left).collect(Collectors.toSet());
        if (text == null || text.isEmpty()) {
            this.filterText = "";
            this.allSounds.stream().filter(sound -> !mutedIds.contains(sound.left())).forEach(this.filteredSounds::add);
            return;
        } else {
            this.filterText = text;
        }

        if (text.startsWith("#")) {
            String search = text.replaceFirst("#", "");
            this.allSounds.stream()
                .filter(it -> it.left().toString().contains(search))
                .filter(it -> !mutedIds.contains(it.left()))
                .forEach(this.filteredSounds::add);
        } else {
            if (text.startsWith("~")) {
                try {
                    Pattern search = Pattern.compile(text.replaceFirst("~", ""));
                    this.allSounds.stream()
                        .filter(it -> search.matcher(it.left().toString()).matches())
                        .filter(it -> !mutedIds.contains(it.left()))
                        .forEach(this.filteredSounds::add);
                } catch (Exception ignored) {
                    // intentionally empty
                }
            }
            this.allSounds.stream()
                .filter(it -> it.right().getString().contains(this.filterText))
                .filter(it -> !mutedIds.contains(it.left()))
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

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void recordCanceledSound(PlayLevelSoundEvent event) {
        if (!event.isCanceled() || event.getSound() == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ActiveSilencerScreen screen)) return;
        if (minecraft.level == null || !minecraft.level.dimension().equals(event.getLevel().dimension())) return;
        Vec3 pos;
        if (event instanceof PlayLevelSoundEvent.AtEntity atEntity) {
            pos = atEntity.getEntity().position();
        } else if (event instanceof PlayLevelSoundEvent.AtPosition atPosition) {
            pos = atPosition.getPosition();
        } else {
            return;
        }
        Identifier sound = event.getSound().value().location();
        minecraft.execute(() -> {
            if (minecraft.screen == screen) screen.recordSound(sound, pos);
        });
    }

    public void recordSound(Identifier sound, Vec3 pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(() -> this.recordSound(sound, pos));
            return;
        }
        if (minecraft.screen != this) return;
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.shape().contains(pos)) {
            this.soundCounts.merge(sound, 1, Integer::sum);
        }
    }

    private void refreshSoundOrder() {
        Map<Identifier, Integer> counts = Map.copyOf(this.soundCounts);
        this.soundOrder = Comparator
            .<Pair<Identifier, Component>>comparingInt(sound -> counts.getOrDefault(sound.left(), 0))
            .reversed()
            .thenComparing(sound -> sound.left().toString());
        this.allSounds.sort(this.soundOrder);
        this.mutedSounds.sort(this.soundOrder);
        this.rightScrollOff = 0;
        this.refreshSoundList();
    }

    private Component getSoundName(Identifier sound) {
        WeighedSoundEvents event = Minecraft.getInstance().getSoundManager().getSoundEvent(sound);
        Component subtitle = event == null ? null : event.getSubtitle();
        return subtitle == null ? Component.literal(sound.toString()) : subtitle;
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
        this.mutedSounds.add(Pair.of(sound, this.getSoundName(sound)));
        this.mutedSounds.sort(this.soundOrder);
    }

    void removeMutedSound(Identifier sound) {
        this.menu.removeSound(sound);
        this.mutedSounds.removeIf(it -> it.left().equals(sound));
    }

    /// 获取屏幕上某一项的声音字幕
    public Component getSoundTextAt(int index, int variant) {
        int actualIndex = index;
        if (variant == ActiveSilencerScreen.SOUND_FILTERED) {
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
        if (variant == ActiveSilencerScreen.SOUND_FILTERED) {
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
        if (this.menu.getBlockEntity() == null) {
            this.onClose();
            return;
        }
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        int buttonTop = this.topPos + 35;
        for (int l = 0; l < 8; ++l) {
            this.addRenderableWidget(new SilencerButton(
                this.leftPos + ActiveSilencerScreen.START_LEFT_X,
                buttonTop,
                l,
                ActiveSilencerScreen.SOUND_FILTERED,
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

        buttonTop = this.topPos + 35;
        for (int l = 0; l < 8; ++l) {
            this.addRenderableWidget(new SilencerButton(
                this.leftPos + ActiveSilencerScreen.START_RIGHT_X,
                buttonTop,
                l,
                ActiveSilencerScreen.SOUND_MUTED,
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

        EditBox search = this.addRenderableWidget(new EditBox(
            Objects.requireNonNull(this.minecraft).font,
            this.leftPos + 78,
            this.topPos + 19,
            100,
            12,
            Component.translatable("screen.anvilcraft.active_silencer.search")
        ));
        search.setValue(this.filterText);
        search.setResponder(this::onSearchTextChange);

        this.addRenderableWidget(Button.builder(
            Component.translatable("screen.anvilcraft.active_silencer.refresh"), button -> this.refreshSoundOrder()
        ).bounds(this.leftPos + 78, this.topPos + 170, 100, 20).build());

        this.allSounds.clear();
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        manager.getAvailableSounds().forEach(sound -> this.allSounds.add(Pair.of(sound, this.getSoundName(sound))));
        this.allSounds.sort(this.soundOrder);
        this.applyMutedSounds(List.copyOf(this.menu.getBlockEntity().getMuting()));
    }

    private boolean mouseInLeft(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + ActiveSilencerScreen.START_LEFT_X,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y,
            leftPos + ActiveSilencerScreen.SCROLL_BAR_START_LEFT_X + ActiveSilencerScreen.SCROLL_BAR_WIDTH,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y + ActiveSilencerScreen.SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInRight(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + ActiveSilencerScreen.START_RIGHT_X,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y,
            leftPos + ActiveSilencerScreen.SCROLL_BAR_START_RIGHT_X + ActiveSilencerScreen.SCROLL_BAR_WIDTH,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y + ActiveSilencerScreen.SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInLeftSlider(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + ActiveSilencerScreen.SCROLL_BAR_START_LEFT_X,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y,
            leftPos + ActiveSilencerScreen.SCROLL_BAR_START_LEFT_X + ActiveSilencerScreen.SCROLL_BAR_WIDTH,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y + ActiveSilencerScreen.SCROLL_BAR_HEIGHT
        );
    }

    private boolean mouseInRightSlider(double mouseX, double mouseY, int leftPos, int topPos) {
        return MathUtil.isInRange(
            mouseX,
            mouseY,
            leftPos + ActiveSilencerScreen.SCROLL_BAR_START_RIGHT_X,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y,
            leftPos + ActiveSilencerScreen.SCROLL_BAR_START_RIGHT_X + ActiveSilencerScreen.SCROLL_BAR_WIDTH,
            topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y + ActiveSilencerScreen.SCROLL_BAR_HEIGHT
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
            int j = this.topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y;
            int k = j + ActiveSilencerScreen.SCROLL_BAR_HEIGHT;
            int dragMax = i - 7;
            float scroll = (float) ((event.y() - j - 13.5F) / ((k - j) - 27.0F));
            scroll = scroll * dragMax + 0.5F;
            this.leftScrollOff = Mth.clamp((int) scroll, 0, dragMax);
            return true;
        } else if (this.isDraggingRight) {
            int i = this.mutedSounds.size();
            int j = this.topPos + ActiveSilencerScreen.SCROLL_BAR_TOP_POS_Y;
            int k = j + ActiveSilencerScreen.SCROLL_BAR_HEIGHT;
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
            int maxY = posY + ActiveSilencerScreen.SCROLL_BAR_HEIGHT - ActiveSilencerScreen.SCROLLER_HEIGHT;
            int scrollY = (int) (posY + (scrollOff / (totalCount - 7F)) * (ActiveSilencerScreen.SCROLL_BAR_HEIGHT
                                                                           - ActiveSilencerScreen.SCROLLER_HEIGHT));
            scrollY = Mth.clamp(scrollY, posY, maxY);

            graphics.blit(RenderPipelines.GUI_TEXTURED, ActiveSilencerScreen.SLIDER, posX, scrollY, 0, 0, 5, 9, 10, 9);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ActiveSilencerScreen.SLIDER, posX, posY, 0, 0, 5, 9, 10, 9);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractScroller(graphics, this.leftPos + 119, this.topPos + 35, this.filteredSounds.size(), this.leftScrollOff);
        this.extractScroller(graphics, this.leftPos + 245, this.topPos + 35, this.mutedSounds.size(), this.rightScrollOff);
    }

    public void handleSync(List<Identifier> sounds) {
        this.applyMutedSounds(sounds);
        this.menu.handleSync(sounds);
    }

    private void applyMutedSounds(List<Identifier> sounds) {
        this.rightScrollOff = 0;
        this.mutedSounds.clear();
        for (Identifier sound : sounds) {
            this.mutedSounds.add(Pair.of(sound, this.getSoundName(sound)));
        }
        this.mutedSounds.sort(this.soundOrder);
        this.onSearchTextChange(this.filterText);
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
}
