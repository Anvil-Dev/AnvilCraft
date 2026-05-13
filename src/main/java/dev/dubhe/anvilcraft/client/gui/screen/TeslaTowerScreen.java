package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.api.teslatower.TeslaFilter;
import dev.dubhe.anvilcraft.client.gui.component.TeslaTowerButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import dev.dubhe.anvilcraft.network.TeslaAddFilterPacket;
import dev.dubhe.anvilcraft.network.TeslaRemoveFilterPacket;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"MismatchedReadAndWriteOfArray", "FieldCanBeLocal"})
public class TeslaTowerScreen extends AbstractContainerScreen<TeslaTowerMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "tesla_tower");

    private static final int SCROLL_BAR_HEIGHT = 120;
    private static final int SCROLL_BAR_TOP_POS_Y = 35;
    private static final int START_LEFT_X = 6;
    private static final int START_RIGHT_X = 132;
    private static final int SCROLL_BAR_START_LEFT_X = 120;
    private static final int SCROLL_BAR_START_RIGHT_X = 245;
    private static final int SCROLL_BAR_WIDTH = 5;
    private static final int SCROLLER_HEIGHT = 9;

    public static final int FILTER_FILTERED = 0;
    public static final int SOUND_MUTED = 1;

    private final TeslaTowerMenu menu;
    private final TeslaTowerButton[] allFilterButtons = new TeslaTowerButton[8];
    private final TeslaTowerButton[] mutedSoundButtons = new TeslaTowerButton[8];
    private EditBox editBox;
    private int leftScrollOff;
    private int rightScrollOff;

    @Getter
    private String filterText = "";

    private boolean isDraggingLeft;
    private boolean isDraggingRight;
    private final List<Pair<TeslaFilter, String>> allFilter = new ArrayList<>();
    private final List<Pair<TeslaFilter, String>> filteredFilters = new ArrayList<>();
    private final List<Pair<TeslaFilter, String>> whiteFilters = new ArrayList<>();

    private void onSearchTextChange(String text) {
        this.leftScrollOff = 0;
        this.filteredFilters.clear();
        if (text.isEmpty()) {
            this.filterText = "";
            this.filteredFilters.addAll(this.allFilter);
            this.filteredFilters.removeIf(it -> this.whiteFilters.stream()
                .anyMatch(it2 -> it.left().getId().equals(it2.left().getId()) && it.right().equals(it2.right()))
            );
            return;
        } else {
            this.filterText = text;
        }

        if (text.startsWith("#")) {
            String search = text.replaceFirst("#", "");
            this.allFilter.stream()
                .filter(it -> it.right().contains(search))
                .filter(it -> this.whiteFilters.stream()
                    .noneMatch(it2 -> it.left().getId().equals(it2.left().getId()) && it.right().equals(it2.right()))
                )
                .forEach(this.filteredFilters::add);
        } else {
            if (text.startsWith("~")) {
                try {
                    Pattern search = Pattern.compile(text.replaceFirst("~", ""));
                    this.allFilter.stream()
                        .filter(it -> search.matcher(it.left().getId()).matches())
                        .forEach(this.filteredFilters::add);
                } catch (Exception ignored) {
                    // intentionally empty
                }
            }
            this.allFilter.stream()
                .filter(it -> it.left().title().getString().contains(this.filterText))
                .filter(it -> this.whiteFilters.stream()
                    .noneMatch(it2 -> it.left().getId().equals(it2.left().getId()) && it.right().equals(it2.right()))
                )
                .forEach(this.filteredFilters::add);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft.options.keyInventory.matches(event)) {
            return this.getFocused() != null && this.getFocused().keyPressed(event);
        } else {
            return super.keyPressed(event);
        }
    }

    private void refreshFilterList() {
        this.onSearchTextChange(this.filterText);
    }

    private void onAllFilterButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += this.leftScrollOff;
        if (this.filteredFilters.isEmpty() || actualIndex >= this.filteredFilters.size()) return;
        String id = this.filteredFilters.get(actualIndex).left().getId();
        String arg = this.filteredFilters.get(actualIndex).right();
        this.addWhiteFilter(id, arg);
        ClientPacketDistributor.sendToServer(new TeslaAddFilterPacket(id, arg));
        this.refreshFilterList();
    }

    private void onWhiteListFilterButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += this.rightScrollOff;
        if (this.whiteFilters.isEmpty() || actualIndex >= this.whiteFilters.size()) return;
        String id = this.whiteFilters.get(actualIndex).left().getId();
        String arg = this.whiteFilters.get(actualIndex).right();
        this.removeWhiteFilter(id, arg);
        ClientPacketDistributor.sendToServer(new TeslaRemoveFilterPacket(id, arg));
        this.refreshFilterList();
    }

    void addWhiteFilter(String id, String arg) {
        this.menu.addFilter(id, arg);
        this.whiteFilters.add(Pair.of(TeslaFilter.getFilter(id), arg));
    }

    void removeWhiteFilter(String id, String arg) {
        this.menu.removeFilter(id, arg);
        this.whiteFilters.removeIf(it -> it.left().getId().equals(id) && it.right().equals(arg));
    }

    public Component getFilterTitle(int index, int variant) {
        int actualIndex = index;
        if (variant == FILTER_FILTERED) {
            actualIndex += this.leftScrollOff;
            if (this.filteredFilters.isEmpty() || actualIndex >= this.filteredFilters.size()) return Component.empty();
            return this.filteredFilters.get(actualIndex).left().title();
        } else {
            actualIndex += this.rightScrollOff;
            if (this.whiteFilters.isEmpty() || actualIndex >= this.whiteFilters.size()) return Component.empty();
            return this.whiteFilters.get(actualIndex).left().title();
        }
    }

    public @Nullable String getFilterToolTipAt(int index, int variant) {
        int actualIndex = index;
        if (variant == FILTER_FILTERED) {
            actualIndex += this.leftScrollOff;
            if (this.filteredFilters.isEmpty() || actualIndex >= this.filteredFilters.size()) return null;
            Pair<TeslaFilter, String> filter = this.filteredFilters.get(actualIndex);
            return filter.left().tooltip(filter.right());
        } else {
            actualIndex += this.rightScrollOff;
            if (this.whiteFilters.isEmpty() || actualIndex >= this.whiteFilters.size()) return null;
            Pair<TeslaFilter, String> filter = this.whiteFilters.get(actualIndex);
            return filter.left().tooltip(filter.right());
        }
    }

    /**
     * 主动消音器gui
     */
    public TeslaTowerScreen(TeslaTowerMenu menu, Inventory playerInventory, Component title) {
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
            TeslaTowerButton button = new TeslaTowerButton(
                leftPos + START_LEFT_X,
                buttonTop,
                l,
                FILTER_FILTERED,
                b -> {
                    if (b instanceof TeslaTowerButton silencerButton) {
                        this.onAllFilterButtonClick(silencerButton.getIndex());
                    }
                },
                this,
                "add");
            button.setWidth(112);
            this.allFilterButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }

        buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            TeslaTowerButton button = new TeslaTowerButton(
                leftPos + START_RIGHT_X,
                buttonTop,
                l,
                SOUND_MUTED,
                b -> {
                    if (b instanceof TeslaTowerButton silencerButton) {
                        this.onWhiteListFilterButtonClick(silencerButton.getIndex());
                    }
                },
                this,
                "remove");
            this.mutedSoundButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }

        this.editBox = new EditBox(
            this.minecraft.font,
            leftPos + 78,
            topPos + 19,
            100,
            12,
            Component.translatable("screen.anvilcraft.active_silencer.search"));
        this.editBox.setResponder(this::onSearchTextChange);
        this.addRenderableWidget(this.editBox);

        this.allFilter.addAll(TeslaFilter.all()
            .stream()
            .filter(it -> !it.needArg())
            .map(it -> Pair.of(it, ""))
            .toList()
        );
        assert Minecraft.getInstance().player != null;
        this.allFilter.addAll(Minecraft.getInstance().player.connection.getOnlinePlayers().stream()
            .map(it -> Pair.of(TeslaFilter.getFilter("IsPlayerIdFilter"), it.getProfile().name()))
            .toList()
        );
        this.allFilter.addAll(BuiltInRegistries.ENTITY_TYPE.stream()
            .map(it -> Pair.of(TeslaFilter.getFilter("IsEntityIdFilter"), Component.translatable(it.getDescriptionId()).getString()))
            .toList()
        );
        this.filteredFilters.addAll(this.allFilter);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int leftPos = (this.width - this.getImageWidth()) / 2;
        int topPos = (this.height - this.getImageHeight()) / 2;
        if (this.mouseInLeft(mouseX, mouseY, leftPos, topPos)) {
            if (this.filteredFilters.size() > 8) {
                this.leftScrollOff = (int) Mth.clamp(this.leftScrollOff - scrollY, 0, this.filteredFilters.size() - 7);
            }
        } else {
            if (this.mouseInRight(mouseX, mouseY, leftPos, topPos)) {
                if (this.whiteFilters.size() > 8) {
                    this.rightScrollOff =
                        (int) Mth.clamp(this.rightScrollOff - scrollY, 0, this.whiteFilters.size() - 7);
                }
            }
        }
        return true;
    }

    /**
     * 鼠标拖动事件
     */
    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        int leftPos = (this.width - this.getImageWidth()) / 2;
        int topPos = (this.height - this.getImageHeight()) / 2;
        if (this.mouseInLeftSlider(event.x(), event.y(), leftPos, topPos)) {
            int i = this.filteredFilters.size();
            if (this.isDraggingLeft) {
                int j = this.topPos + SCROLL_BAR_TOP_POS_Y;
                int k = j + SCROLL_BAR_HEIGHT;
                int dragMax = i - 7;
                float scroll = (float) ((event.y() - j - 13.5F) / ((k - j) - 27.0F));
                scroll = scroll * dragMax + 0.5F;
                this.leftScrollOff = Mth.clamp((int) scroll, 0, dragMax);
                return true;
            } else {
                return super.mouseDragged(event, dragX, dragY);
            }
        } else {
            if (this.mouseInRightSlider(event.x(), event.y(), leftPos, topPos)) {
                int i = this.whiteFilters.size();
                if (this.isDraggingRight) {
                    int j = this.topPos + SCROLL_BAR_TOP_POS_Y;
                    int k = j + SCROLL_BAR_HEIGHT;
                    int dragMax = i - 7;
                    float scroll = (float) ((event.y() - j - 13.5F) / ((k - j) - 27.0F));
                    scroll = scroll * dragMax + 0.5F;
                    this.rightScrollOff = Mth.clamp((int) scroll, 0, dragMax);
                    return true;
                } else {
                    return super.mouseDragged(event, dragX, dragY);
                }
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    /**
     * 鼠标点击
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        this.isDraggingLeft = false;
        this.isDraggingRight = false;
        int leftPos = (this.width - this.getImageWidth()) / 2;
        int topPos = (this.height - this.getImageHeight()) / 2;
        if (this.mouseInLeftSlider(event.x(), event.y(), leftPos, topPos) && this.filteredFilters.size() > 8) {
            this.isDraggingLeft = true;
        }
        if (this.mouseInRightSlider(event.x(), event.y(), leftPos, topPos) && this.whiteFilters.size() > 8) {
            this.isDraggingRight = true;
        }
        return super.mouseClicked(event, handled);
    }

    private void extractScroller(GuiGraphicsExtractor graphics, int posX, int posY, int totalCount, int scrollOff) {
        int i = totalCount + 1 - 8;
        if (i > 1) {
            int maxY = posY + SCROLL_BAR_HEIGHT - SCROLLER_HEIGHT;
            int scrollY = (int) (posY + (scrollOff / (float) totalCount) * SCROLL_BAR_HEIGHT);
            scrollY = Mth.clamp(scrollY, posY, maxY);

            graphics.blit(SharedTextures.SMALL_SLIDER, posX, scrollY, 0, 0, 5, 9, 10, 9);
        } else {
            graphics.blit(SharedTextures.SMALL_SLIDER, posX, posY, 0, 0, 5, 9, 10, 9);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractScroller(graphics, this.leftPos + 119, this.topPos + 35, this.filteredFilters.size(), this.leftScrollOff);
        this.extractScroller(graphics, this.leftPos + 245, this.topPos + 35, this.whiteFilters.size(), this.rightScrollOff);
    }

    /**
     * 处理同步包
     */
    public void handleSync(List<Pair<dev.dubhe.anvilcraft.api.teslatower.TeslaFilter, String>> filters) {
        this.rightScrollOff = 0;
        this.whiteFilters.clear();
        this.whiteFilters.addAll(filters);
        this.onSearchTextChange("");
        this.menu.handleSync(filters);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            this.getImageWidth(),
            this.getImageHeight()
        );
    }
}
