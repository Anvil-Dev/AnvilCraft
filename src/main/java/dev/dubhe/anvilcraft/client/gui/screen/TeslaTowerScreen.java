package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.taslatower.TeslaFilter;
import dev.dubhe.anvilcraft.client.gui.component.TeslaTowerButton;
import dev.dubhe.anvilcraft.inventory.TeslaTowerMenu;
import dev.dubhe.anvilcraft.network.AddTeslaFilterPacket;
import dev.dubhe.anvilcraft.network.RemoveTeslaFilterPacket;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"MismatchedReadAndWriteOfArray", "FieldCanBeLocal"})
public class TeslaTowerScreen extends AbstractContainerScreen<TeslaTowerMenu> {

    private static final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/machine/background/tesla_tower.png");

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
        leftScrollOff = 0;
        filteredFilters.clear();
        if (text == null || text.isEmpty()) {
            this.filterText = "";
            filteredFilters.addAll(allFilter);
            filteredFilters.removeIf(it -> whiteFilters.stream().anyMatch(it2 -> it.left().getId().equals(it2.left().getId()) && it.right().equals(it2.right())));
            return;
        } else {
            this.filterText = text;
        }

        if (text.startsWith("#")) {
            String search = text.replaceFirst("#", "");
            allFilter.stream()
                    .filter(it -> it.right().contains(search))
                    .filter(it -> whiteFilters.stream().anyMatch(it2 -> it.left().getId().equals(it2.left().getId()) && it.right().equals(it2.right())))
                    .forEach(filteredFilters::add);
        } else {
            if (text.startsWith("~")) {
                try {
                    Pattern search = Pattern.compile(text.replaceFirst("~", ""));
                    allFilter.stream()
                            .filter(it -> search.matcher(it.left().getId()).matches())
                            .filter(it -> whiteFilters.stream().anyMatch(it2 -> it.left().getId().equals(it2.left().getId()) && it.right().equals(it2.right())))
                            .forEach(filteredFilters::add);
                } catch (Exception ignored) {
                    // intentionally empty
                }
            }
            allFilter.stream()
                    .filter(it -> it.left().title().getString().contains(filterText))
                    .filter(it ->
                            whiteFilters.stream().noneMatch(it1 -> it1.left().equals(it.first())))
                    .forEach(filteredFilters::add);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        assert this.minecraft != null;
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers);
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private void refreshFilterList() {
        onSearchTextChange(filterText);
    }

    private void onAllFilterButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += leftScrollOff;
        if (filteredFilters.isEmpty() || actualIndex >= filteredFilters.size()) return;
        String id = filteredFilters.get(actualIndex).left().getId();
        String arg = filteredFilters.get(actualIndex).right();
        addWhiteFilter(id, arg);
        PacketDistributor.sendToServer(new AddTeslaFilterPacket(id, arg));
        refreshFilterList();
    }

    private void onWhiteListFilterButtonClick(int selectedIndex) {
        int actualIndex = selectedIndex;
        actualIndex += rightScrollOff;
        if (whiteFilters.isEmpty() || actualIndex >= whiteFilters.size()) return;
        String id = whiteFilters.get(actualIndex).left().getId();
        String arg = whiteFilters.get(actualIndex).right();
        removeWhiteFilter(id, arg);
        PacketDistributor.sendToServer(new RemoveTeslaFilterPacket(id, arg));
        refreshFilterList();
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
            actualIndex += leftScrollOff;
            if (filteredFilters.isEmpty() || actualIndex >= filteredFilters.size()) return Component.empty();
            return filteredFilters.get(actualIndex).left().title();
        } else {
            actualIndex += rightScrollOff;
            if (whiteFilters.isEmpty() || actualIndex >= whiteFilters.size()) return Component.empty();
            return whiteFilters.get(actualIndex).left().title();
        }
    }

    public String getFilterToolTipAt(int index, int variant) {
        int actualIndex = index;
        if (variant == FILTER_FILTERED) {
            actualIndex += leftScrollOff;
            if (filteredFilters.isEmpty() || actualIndex >= filteredFilters.size()) return null;
            Pair<TeslaFilter, String> filter = filteredFilters.get(actualIndex);
            return filter.left().tooltip(filter.right());
        } else {
            actualIndex += rightScrollOff;
            if (whiteFilters.isEmpty() || actualIndex >= whiteFilters.size()) return null;
            Pair<TeslaFilter, String> filter = whiteFilters.get(actualIndex);
            return filter.left().tooltip(filter.right());
        }
    }

    /**
     * 主动消音器gui
     */
    public TeslaTowerScreen(TeslaTowerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @SuppressWarnings("ExtractMethodRecommender")
    @Override
    protected void init() {
        super.init();

        int buttonTop = topPos + 35;
        for (int l = 0; l < 8; ++l) {
            TeslaTowerButton button = new TeslaTowerButton(
                    leftPos + START_LEFT_X,
                    buttonTop,
                    l,
                    FILTER_FILTERED,
                    b -> {
                        if (b instanceof TeslaTowerButton silencerButton) {
                            onAllFilterButtonClick(silencerButton.getIndex());
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
                            onWhiteListFilterButtonClick(silencerButton.getIndex());
                        }
                    },
                    this,
                    "remove");
            this.mutedSoundButtons[l] = this.addRenderableWidget(button);
            buttonTop += 15;
        }

        assert this.minecraft != null;
        editBox = new EditBox(
                this.minecraft.font,
                leftPos + 78,
                topPos + 19,
                100,
                12,
                Component.translatable("screen.anvilcraft.active_silencer.search"));
        editBox.setResponder(this::onSearchTextChange);
        addRenderableWidget(editBox);

        allFilter.addAll(TeslaFilter.all()
                .stream()
                .filter(it -> !it.needArg())
                .map(it -> Pair.of(it, ""))
                .toList()
        );
        assert Minecraft.getInstance().player != null;
        allFilter.addAll(Minecraft.getInstance().player.connection.getOnlinePlayers().stream()
                .map(it -> Pair.of(TeslaFilter.getFilter("IsPlayerIdFilter"), it.getProfile().getName()))
                .toList()
        );
        allFilter.addAll(BuiltInRegistries.ENTITY_TYPE.stream()
                .map(it -> Pair.of(TeslaFilter.getFilter("IsEntityIdFilter"), it.getDescriptionId()))
                .toList()
        );
        filteredFilters.addAll(allFilter);
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
            if (this.filteredFilters.size() > 8) {
                this.leftScrollOff = (int) Mth.clamp(this.leftScrollOff - pScrollY, 0, this.filteredFilters.size() - 7);
            }
        } else {
            if (mouseInRight(mouseX, mouseY, leftPos, topPos)) {
                if (this.whiteFilters.size() > 8) {
                    this.rightScrollOff =
                            (int) Mth.clamp(this.rightScrollOff - pScrollY, 0, this.whiteFilters.size() - 7);
                }
            }
        }
        return true;
    }

    /**
     * 鼠标拖动事件
     */
    @SuppressWarnings("DuplicatedCode")
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int leftPos = (this.width - this.imageWidth) / 2;
        int topPos = (this.height - this.imageHeight) / 2;
        if (mouseInLeftSlider(mouseX, mouseY, leftPos, topPos)) {
            int i = filteredFilters.size();
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
                int i = whiteFilters.size();
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
        if (mouseInLeftSlider(mouseX, mouseY, leftPos, topPos) && filteredFilters.size() > 8) {
            this.isDraggingLeft = true;
        }
        if (mouseInRightSlider(mouseX, mouseY, leftPos, topPos) && whiteFilters.size() > 8) {
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
        this.renderScroller(guiGraphics, leftPos + 119, topPos + 35, filteredFilters.size(), leftScrollOff);

        this.renderScroller(guiGraphics, leftPos + 245, topPos + 35, whiteFilters.size(), rightScrollOff);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 处理同步包
     */
    public void handleSync(List<Pair<TeslaFilter, String>> filters) {
        rightScrollOff = 0;
        whiteFilters.clear();
        whiteFilters.addAll(filters);
        onSearchTextChange("");
        menu.handleSync(filters);
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
