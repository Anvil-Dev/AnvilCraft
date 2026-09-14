package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.EvictingQueue;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.CommandEntry;
import dev.dubhe.anvilcraft.client.gui.component.SapcetimeSupercomputerCommandSuggestions;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.model.CommandInfo;
import dev.dubhe.anvilcraft.network.SpacetimeSupercomputerExecuteCommandPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SpacetimeSupercomputerScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
        SharedTextures.bg("machine", "spacetime_supercomputer");
    private static final ResourceLocation BUTTON_CONFIRM_RUN =
        SharedTextures.textureGui("machine/spacetime_supercomputer/confirm_run");
    private static final ResourceLocation BUTTON_CONFIRM_RETAIN =
        SharedTextures.textureGui("machine/spacetime_supercomputer/confirm_retain");
    private static final ResourceLocation BUTTON_CANCEL = SharedTextures.textureGui("machine/spacetime_supercomputer/cancel");
    private static final ResourceLocation BUTTON_CHARGING_PROGRESS =
        SharedTextures.textureGui("machine/spacetime_supercomputer/charging_progress");

    // 两个命令列表区域：与背景图左右面板的内区域严格对齐（各 120 × 88）。
    // 面板顶部留出标题带，其余高度均分给 5 行。
    private static final int LIST_LEFT_X = 6;
    private static final int LIST_RIGHT_X = 130;
    private static final int LIST_Y = 16;
    private static final int LIST_WIDTH = 120;
    private static final int LIST_HEIGHT = 88;
    private static final int LIST_ROWS = 5;
    // 面板内顶部的标题带；标题文字再内缩 2px，带高需容纳 9px 字高 + 1px 分隔线
    private static final int LIST_TITLE_HEIGHT = 12;
    private static final int LIST_TITLE_Y = LIST_Y + 2;
    private static final int LIST_ROWS_Y = LIST_Y + LIST_TITLE_HEIGHT;
    private static final int LIST_ROW_HEIGHT = (LIST_HEIGHT - LIST_TITLE_HEIGHT) / LIST_ROWS;
    private static final int SCROLLER_WIDTH = 6;
    private static final int SCROLLER_HEIGHT = 32;
    // 滚动条在标题带下方、行区域内可移动的距离
    private static final int LIST_TRACK_HEIGHT = LIST_HEIGHT - LIST_TITLE_HEIGHT - SCROLLER_HEIGHT;
    // 列表可滚动时为右侧滚动条预留的宽度
    private static final int LIST_SCROLLER_GAP = 7;

    private final SpacetimeSupercomputerBlockEntity spacetimeSupercomputerBlockEntity;
    private EditBox commandEditBox = null;
    private SapcetimeSupercomputerCommandSuggestions commandSuggestions = null;

    private int currentAvailableCommandButtonIndex = 0;
    private int currentHistoryCommandButtonIndex = 0;

    private final CommandEntry[] availableCommandsButton = new CommandEntry[LIST_ROWS];
    private final CommandEntry[] historyCommandsButton = new CommandEntry[LIST_ROWS];

    private int availableCommandScrollOffset;
    private int historyCommandScrollOffset;
    private boolean draggedAvailableCommandScrollBarArea = false;
    private boolean draggedHistoryCommandScrollBarArea = false;

    public SpacetimeSupercomputerScreen(SpacetimeSupercomputerBlockEntity blockEntity) {
        super(Component.translatable("block.anvilcraft.spacetime_supercomputer"));
        this.spacetimeSupercomputerBlockEntity = blockEntity;
    }

    private void buildCommand(CommandDispatcher<SharedSuggestionProvider> dispatcher, CommandBuildContext context) {
        LiteralArgumentBuilder<SharedSuggestionProvider> locate = LiteralArgumentBuilder.literal("locate");
        locate.then(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("biome")
                .then(
                    RequiredArgumentBuilder.argument("biome",
                        ResourceOrTagArgument.resourceOrTag(context, Registries.BIOME))
                )
        );
        locate.then(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("structure")
                .then(
                    RequiredArgumentBuilder.argument("structure",
                        ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                )
        );
        locate.then(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("poi")
                .then(
                    RequiredArgumentBuilder.argument("poi",
                        ResourceOrTagArgument.resourceOrTag(context, Registries.POINT_OF_INTEREST_TYPE))
                )
        );
        dispatcher.register(locate);

        dispatcher.register(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("time")
                .then(
                    LiteralArgumentBuilder.<SharedSuggestionProvider>literal("add")
                        .then(
                            RequiredArgumentBuilder.argument("time", TimeArgument.time())
                        )
                )
        );
        dispatcher.register(
            LiteralArgumentBuilder.<SharedSuggestionProvider>literal("tick")
                .then(
                    LiteralArgumentBuilder.<SharedSuggestionProvider>literal("sprint")
                        .then(
                            LiteralArgumentBuilder.literal("stop")
                        )
                        .then(
                            RequiredArgumentBuilder.<SharedSuggestionProvider, Integer>argument("time", TimeArgument.time(1))
                                .suggests((ctx, builder) ->
                                    SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, builder))
                        )
                )
        );
    }

    private void fillAvailableCommandList(int startIndex) {
        if (startIndex < 0) {
            return;
        }
        for (CommandEntry commandEntry : this.availableCommandsButton) {
            this.removeWidget(commandEntry);
        }
        this.currentAvailableCommandButtonIndex = startIndex;
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        List<CommandInfo> availableCommands = this.spacetimeSupercomputerBlockEntity.getAvailableCommands();
        int buttonWidth = availableCommands.size() > LIST_ROWS ? LIST_WIDTH - LIST_SCROLLER_GAP : LIST_WIDTH;
        if (startIndex < availableCommands.size()) {
            int index = 0;
            for (int i = startIndex; i < availableCommands.size() && i < startIndex + LIST_ROWS; i++) {
                CommandInfo commandInfo = availableCommands.get(i);
                MutableComponent component = Component.literal(commandInfo.command());
                if (!commandInfo.available()) {
                    component.withStyle((style) -> style.withStrikethrough(true).withColor(ChatFormatting.RED));
                }
                this.availableCommandsButton[index] = this.addRenderableWidget(
                    new CommandEntry(
                        x + LIST_LEFT_X, y + LIST_ROWS_Y + LIST_ROW_HEIGHT * index,
                        buttonWidth, LIST_ROW_HEIGHT,
                        component,
                        (btn) -> this.onPress(btn, false)
                    )
                );
                index++;
            }
        }
    }

    private void fillHistoryCommandList(int startIndex) {
        if (startIndex < 0) {
            return;
        }
        for (CommandEntry commandEntry : this.historyCommandsButton) {
            this.removeWidget(commandEntry);
        }
        this.currentHistoryCommandButtonIndex = startIndex;
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        List<String> historyCommands = new ArrayList<>(this.spacetimeSupercomputerBlockEntity.getHistoryCommands()).reversed();
        int buttonWidth = historyCommands.size() > LIST_ROWS ? LIST_WIDTH - LIST_SCROLLER_GAP : LIST_WIDTH;
        if (startIndex < historyCommands.size()) {
            int index = 0;
            for (int i = startIndex; i < historyCommands.size() && i < startIndex + LIST_ROWS; i++) {
                this.historyCommandsButton[index] = this.addRenderableWidget(
                    new CommandEntry(
                        x + LIST_RIGHT_X, y + LIST_ROWS_Y + LIST_ROW_HEIGHT * index,
                        buttonWidth, LIST_ROW_HEIGHT,
                        Component.literal(historyCommands.get(i)),
                        (btn) -> this.onPress(btn, true)
                    )
                );
                index++;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;

        if (this.minecraft == null) {
            return;
        }

        // TODO: 以后需改为可换行编辑框
        this.commandEditBox = this.addRenderableWidget(
            new EditBox(
                this.font,
                x + 8, y + 108,
                216, 53,
                Component.empty()
            )
        );
        this.commandEditBox.setValue(this.spacetimeSupercomputerBlockEntity.getCommand());
        this.commandEditBox.setMaxLength(32500);
        this.commandEditBox.setBordered(false);
        this.commandEditBox.setResponder(this::onEdited);
        this.commandSuggestions = new SapcetimeSupercomputerCommandSuggestions(
            this.minecraft,
            this,
            this.commandEditBox,
            this.font,
            true,
            true,
            0,
            7,
            true,
            Integer.MIN_VALUE,
            this::buildCommand
        );
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();

        this.fillAvailableCommandList(0);
        this.fillHistoryCommandList(0);

        this.addRenderableWidget(
            new TexturedButton(
                x + 234, y + 108,
                16, 16,
                BUTTON_CONFIRM_RUN,
                16, 16, 32,
                (btn) -> this.onDone(true)
            )
        );
        this.addRenderableWidget(
            new TexturedButton(
                x + 234, y + 126,
                16, 16,
                BUTTON_CONFIRM_RETAIN,
                16, 16, 32,
                (btn) -> this.onDone(false)
            )
        );
        this.addRenderableWidget(
            new TexturedButton(
                x + 234, y + 144,
                16, 16,
                BUTTON_CANCEL,
                16, 16, 32,
                (btn) -> this.onClose()
            )
        );
    }

    private void onPress(CommandEntry button, boolean isHistory) {
        Component text = button.getText();
        if (!text.getStyle().isStrikethrough()) {
            if (isHistory) {
                this.commandEditBox.setValue(button.getText().getString());
            } else {
                this.commandEditBox.setValue(button.getText().getString() + " ");
            }
        }
    }

    private void onDone(boolean running) {
        if (this.minecraft != null) {
            ClientPacketListener connection = this.minecraft.getConnection();
            if (connection != null) {
                connection.send(new SpacetimeSupercomputerExecuteCommandPacket(
                    this.spacetimeSupercomputerBlockEntity.getBlockPos(),
                    this.commandEditBox.getValue(),
                    running
                ));
            }
        }
        this.onClose();
    }

    private void onEdited(String command) {
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.commandEditBox);
    }

    @Override
    protected Component getUsageNarration() {
        return this.commandSuggestions.isVisible()
            ? this.commandSuggestions.getUsageNarration()
            : super.getUsageNarration();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String cmd = this.commandEditBox.getValue();
        this.init(minecraft, width, height);
        this.commandEditBox.setValue(cmd);
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.mouseInAvailableCommandListArea(mouseX, mouseY)) {
            List<CommandInfo> availableCommands = this.spacetimeSupercomputerBlockEntity.getAvailableCommands();
            if (this.currentAvailableCommandButtonIndex >= availableCommands.size() - LIST_ROWS && scrollY < 0) {
                return true;
            }
            if (availableCommands.size() > LIST_ROWS) {
                int newIndex = Mth.clamp(this.currentAvailableCommandButtonIndex + (int) -scrollY, 0, availableCommands.size() - LIST_ROWS);
                this.fillAvailableCommandList(newIndex);
                this.availableCommandScrollOffset = newIndex;
                return true;
            }
        }
        if (this.mouseInHistoryCommandListArea(mouseX, mouseY)) {
            EvictingQueue<String> historyCommands = this.spacetimeSupercomputerBlockEntity.getHistoryCommands();
            if (this.currentHistoryCommandButtonIndex >= historyCommands.size() - LIST_ROWS && scrollY < 0) {
                return true;
            }
            if (historyCommands.size() > LIST_ROWS) {
                int newIndex = Mth.clamp(this.currentHistoryCommandButtonIndex + (int) -scrollY, 0, historyCommands.size() - LIST_ROWS);
                this.fillHistoryCommandList(newIndex);
                this.historyCommandScrollOffset = newIndex;
                return true;
            }
        }
        if (this.commandSuggestions.mouseScrolled(scrollY)) {
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }

    private boolean mouseInAvailableCommandListArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        return mouseX >= x + LIST_LEFT_X
            && mouseX <= x + LIST_LEFT_X + LIST_WIDTH
            && mouseY >= y + LIST_Y
            && mouseY <= y + LIST_Y + LIST_HEIGHT;
    }

    private boolean mouseInHistoryCommandListArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        return mouseX >= x + LIST_RIGHT_X
            && mouseX <= x + LIST_RIGHT_X + LIST_WIDTH
            && mouseY >= y + LIST_Y
            && mouseY <= y + LIST_Y + LIST_HEIGHT;
    }

    private boolean mouseInAvailableCommandListScrollBarArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        int scrollerX = x + LIST_LEFT_X + LIST_WIDTH - SCROLLER_WIDTH;
        return mouseX >= scrollerX
            && mouseX <= scrollerX + SCROLLER_WIDTH
            && mouseY >= y + LIST_Y
            && mouseY <= y + LIST_Y + LIST_HEIGHT;
    }

    private boolean mouseInHistoryCommandListScrollBarArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        int scrollerX = x + LIST_RIGHT_X + LIST_WIDTH - SCROLLER_WIDTH;
        return mouseX >= scrollerX
            && mouseX <= scrollerX + SCROLLER_WIDTH
            && mouseY >= y + LIST_Y
            && mouseY <= y + LIST_Y + LIST_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.draggedAvailableCommandScrollBarArea = false;
        this.draggedHistoryCommandScrollBarArea = false;
        if (button == 0) {
            int y = (this.height - 166) / 2;
            int trackY = y + LIST_ROWS_Y;
            int trackHeight = LIST_TRACK_HEIGHT;

            if (this.mouseInAvailableCommandListScrollBarArea(mouseX, mouseY)) {
                int maxIndex = this.spacetimeSupercomputerBlockEntity.getAvailableCommands().size() - LIST_ROWS;
                if (maxIndex > 0) {
                    this.draggedAvailableCommandScrollBarArea = true;
                    int newIndex = Mth.clamp((int) ((mouseY - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                    this.availableCommandScrollOffset = newIndex;
                    this.fillAvailableCommandList(newIndex);
                    return true;
                }
            }
            if (this.mouseInHistoryCommandListScrollBarArea(mouseX, mouseY)) {
                int maxIndex = this.spacetimeSupercomputerBlockEntity.getHistoryCommands().size() - LIST_ROWS;
                if (maxIndex > 0) {
                    this.draggedHistoryCommandScrollBarArea = true;
                    int newIndex = Mth.clamp((int) ((mouseY - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                    this.historyCommandScrollOffset = newIndex;
                    this.fillHistoryCommandList(newIndex);
                    return true;
                }
            }
        }
        return this.commandSuggestions.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int y = (this.height - 166) / 2;
        int trackY = y + LIST_ROWS_Y;
        int trackHeight = LIST_TRACK_HEIGHT;

        if (this.draggedAvailableCommandScrollBarArea) {
            int maxIndex = this.spacetimeSupercomputerBlockEntity.getAvailableCommands().size() - LIST_ROWS;
            if (maxIndex > 0) {
                int newIndex = Mth.clamp((int) ((mouseY - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                this.availableCommandScrollOffset = newIndex;
                this.fillAvailableCommandList(newIndex);
            }
            return true;
        }
        if (this.draggedHistoryCommandScrollBarArea) {
            int maxIndex = this.spacetimeSupercomputerBlockEntity.getHistoryCommands().size() - LIST_ROWS;
            if (maxIndex > 0) {
                int newIndex = Mth.clamp((int) ((mouseY - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                this.historyCommandScrollOffset = newIndex;
                this.fillHistoryCommandList(newIndex);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggedAvailableCommandScrollBarArea = false;
        this.draggedHistoryCommandScrollBarArea = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderScroller(GuiGraphics guiGraphics, int posX, int posY, int totalCount, int scrollOff) {
        if (totalCount > LIST_ROWS) {
            int maxY = posY + LIST_TRACK_HEIGHT;
            int maxIndex = totalCount - LIST_ROWS;
            int scrollY = posY + scrollOff * LIST_TRACK_HEIGHT / maxIndex;
            scrollY = Mth.clamp(scrollY, posY, maxY);

            guiGraphics.blitSprite(
                SharedTextures.SCROLLER_SPRITE,
                SCROLLER_WIDTH, SCROLLER_HEIGHT, 0, 0,
                posX, scrollY, SCROLLER_WIDTH, SCROLLER_HEIGHT
            );
        }
    }

    /**
     * 在列表区域横向居中绘制标题。
     *
     * <p>文本宽度超出列表宽度时按宽度截断。
     */
    private void drawCenteredListTitle(GuiGraphics guiGraphics, Component title, int minX, int y) {
        String text = this.font.plainSubstrByWidth(title.getString(), LIST_WIDTH);
        guiGraphics.drawString(
            this.font, text,
            minX + (LIST_WIDTH - this.font.width(text)) / 2, y,
            -1, false
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染标题
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        guiGraphics.drawString(this.font, this.title, x + (256 - this.font.width(this.title)) / 2, y + 2, 4210752, false);

        // 渲染命令列表标题
        this.drawCenteredListTitle(
            guiGraphics,
            Component.translatable("screen.anvilcraft.spacetime_supercomputer.available_commands"),
            x + LIST_LEFT_X, y + LIST_TITLE_Y
        );
        this.drawCenteredListTitle(
            guiGraphics,
            Component.translatable("screen.anvilcraft.spacetime_supercomputer.history_commands"),
            x + LIST_RIGHT_X, y + LIST_TITLE_Y
        );

        // 渲染充能进度条（自下而上：贴图底边锚定在进度条底部，随充能向上展开）
        int chargingBarHeight = getChangingProgress();
        guiGraphics.blit(
            BUTTON_CHARGING_PROGRESS,
            x + 226, y + 108 + (52 - chargingBarHeight),
            0, 52 - chargingBarHeight,
            6, chargingBarHeight,
            6, 52
        );

        // 渲染命令建议
        this.commandSuggestions.render(guiGraphics, mouseX, mouseY);

        // 渲染滚动条
        this.renderScroller(
            guiGraphics,
            x + LIST_LEFT_X + LIST_WIDTH - SCROLLER_WIDTH, y + LIST_ROWS_Y,
            this.spacetimeSupercomputerBlockEntity.getAvailableCommands().size(),
            this.availableCommandScrollOffset
        );
        this.renderScroller(
            guiGraphics,
            x + LIST_RIGHT_X + LIST_WIDTH - SCROLLER_WIDTH, y + LIST_ROWS_Y,
            this.spacetimeSupercomputerBlockEntity.getHistoryCommands().size(),
            this.historyCommandScrollOffset
        );

        // 渲染列表标题下方的分隔线
        guiGraphics.hLine(x + LIST_LEFT_X, x + LIST_LEFT_X + LIST_WIDTH - 1, y + LIST_ROWS_Y - 1,
            FastColor.ARGB32.color(128, 177, 177, 177));
        guiGraphics.hLine(x + LIST_RIGHT_X, x + LIST_RIGHT_X + LIST_WIDTH - 1, y + LIST_ROWS_Y - 1,
            FastColor.ARGB32.color(128, 177, 177, 177));
    }

    private int getChangingProgress() {
        float chargingProgress = this.spacetimeSupercomputerBlockEntity.getChargingProgress();
        int changingProgress = (int) (chargingProgress * 52f / 100);
        if (changingProgress > 0 && changingProgress < 1.7f) {
            changingProgress = 1;
        }
        if (changingProgress > 98.2f && changingProgress < 100) {
            changingProgress = 51;
        }
        if (changingProgress >= 100) {
            return 52;
        }
        return changingProgress;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, 256, 166);
    }

    public void updateGui() {
        this.init();
        this.commandEditBox.setValue(this.spacetimeSupercomputerBlockEntity.getCommand());
        this.setInitialFocus();
    }
}
