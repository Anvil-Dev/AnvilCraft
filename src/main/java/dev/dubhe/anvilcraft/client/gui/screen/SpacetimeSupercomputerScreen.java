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
import dev.dubhe.anvilcraft.util.RenderUtil;
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

    private final SpacetimeSupercomputerBlockEntity spacetimeSupercomputerBlockEntity;
    private EditBox commandEditBox;
    private SapcetimeSupercomputerCommandSuggestions commandSuggestions;

    private int currentAvailableCommandButtonIndex = 0;
    private int currentHistoryCommandButtonIndex = 0;

    private final CommandEntry[] availableCommandsButton = new CommandEntry[9];
    private final CommandEntry[] historyCommandsButton = new CommandEntry[9];

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
        int buttonWidth = availableCommands.size() > 9 ? 55 : 61;
        if (startIndex < availableCommands.size()) {
            int index = 0;
            for (int i = startIndex; i < availableCommands.size() && i < startIndex + 9; i++) {
                CommandInfo commandInfo = availableCommands.get(i);
                MutableComponent component = Component.literal(commandInfo.command());
                if (!commandInfo.available()) {
                    component.withStyle((style) -> style.withStrikethrough(true).withColor(ChatFormatting.RED));
                }
                this.availableCommandsButton[index] = this.addRenderableWidget(
                    new CommandEntry(
                        x + 6, y + 25 + 15 * index,
                        buttonWidth, 15,
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
        int buttonWidth = historyCommands.size() > 9 ? 55 : 61;
        if (startIndex < historyCommands.size()) {
            int index = 0;
            for (int i = startIndex; i < historyCommands.size() && i < startIndex + 9; i++) {
                this.historyCommandsButton[index] = this.addRenderableWidget(
                    new CommandEntry(
                        x + 188, y + 25 + 15 * index,
                        buttonWidth, 15,
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
                x + 72, y + 16,
                112, 124,
                Component.empty()
            )
        );
        this.commandEditBox.setValue(this.spacetimeSupercomputerBlockEntity.getCommand());
        this.commandEditBox.setMaxLength(32500);
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
                x + 132, y + 144,
                16, 16,
                BUTTON_CONFIRM_RUN,
                16, 16, 32,
                (btn) -> this.onDone(true)
            )
        );
        this.addRenderableWidget(
            new TexturedButton(
                x + 150, y + 144,
                16, 16,
                BUTTON_CONFIRM_RETAIN,
                16, 16, 32,
                (btn) -> this.onDone(false)
            )
        );
        this.addRenderableWidget(
            new TexturedButton(
                x + 168, y + 144,
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
            if (this.currentAvailableCommandButtonIndex >= availableCommands.size() - 9 && scrollY < 0) {
                return true;
            }
            if (availableCommands.size() > 9) {
                int newIndex = Mth.clamp(this.currentAvailableCommandButtonIndex + (int) -scrollY, 0, availableCommands.size() - 9);
                this.fillAvailableCommandList(newIndex);
                this.availableCommandScrollOffset = newIndex;
                return true;
            }
        }
        if (this.mouseInHistoryCommandListArea(mouseX, mouseY)) {
            EvictingQueue<String> historyCommands = this.spacetimeSupercomputerBlockEntity.getHistoryCommands();
            if (this.currentHistoryCommandButtonIndex >= historyCommands.size() - 9 && scrollY < 0) {
                return true;
            }
            if (historyCommands.size() > 9) {
                int newIndex = Mth.clamp(this.currentHistoryCommandButtonIndex + (int) -scrollY, 0, historyCommands.size() - 9);
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
        return mouseX >= x + 6
            && mouseX <= x + 68
            && mouseY >= y + 25
            && mouseY <= y + 160;
    }

    private boolean mouseInHistoryCommandListArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        return mouseX >= x + 188
            && mouseX <= x + 250
            && mouseY >= y + 25
            && mouseY <= y + 160;
    }

    private boolean mouseInAvailableCommandListScrollBarArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        return mouseX >= x + 63
            && mouseX <= x + 68
            && mouseY >= y + 25
            && mouseY <= y + 160;
    }

    private boolean mouseInHistoryCommandListScrollBarArea(double mouseX, double mouseY) {
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        return mouseX >= x + 245
            && mouseX <= x + 250
            && mouseY >= y + 25
            && mouseY <= y + 160;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.draggedAvailableCommandScrollBarArea = false;
        this.draggedHistoryCommandScrollBarArea = false;
        if (button == 0) {
            int y = (this.height - 166) / 2;
            int trackY = y + 25;
            int trackHeight = 135 - 32;

            if (this.mouseInAvailableCommandListScrollBarArea(mouseX, mouseY)) {
                int maxIndex = this.spacetimeSupercomputerBlockEntity.getAvailableCommands().size() - 9;
                if (maxIndex > 0) {
                    this.draggedAvailableCommandScrollBarArea = true;
                    int newIndex = Mth.clamp((int) ((mouseY - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                    this.availableCommandScrollOffset = newIndex;
                    this.fillAvailableCommandList(newIndex);
                    return true;
                }
            }
            if (this.mouseInHistoryCommandListScrollBarArea(mouseX, mouseY)) {
                int maxIndex = this.spacetimeSupercomputerBlockEntity.getHistoryCommands().size() - 9;
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
        int trackY = y + 25;
        int trackHeight = 135 - 32;

        if (this.draggedAvailableCommandScrollBarArea) {
            int maxIndex = this.spacetimeSupercomputerBlockEntity.getAvailableCommands().size() - 9;
            if (maxIndex > 0) {
                int newIndex = Mth.clamp((int) ((mouseY - trackY) * (double) maxIndex / trackHeight), 0, maxIndex);
                this.availableCommandScrollOffset = newIndex;
                this.fillAvailableCommandList(newIndex);
            }
            return true;
        }
        if (this.draggedHistoryCommandScrollBarArea) {
            int maxIndex = this.spacetimeSupercomputerBlockEntity.getHistoryCommands().size() - 9;
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
        if (totalCount > 9) {
            int maxY = posY + 135 - 32;
            int trackHeight = 135 - 32;
            int maxIndex = totalCount - 9;
            int scrollY = posY + scrollOff * trackHeight / maxIndex;
            scrollY = Mth.clamp(scrollY, posY, maxY);

            guiGraphics.blitSprite(SharedTextures.SCROLLER_SPRITE, 6, 32, 0, 0, posX, scrollY, 6, 32);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染标题
        int x = (this.width - 256) / 2;
        int y = (this.height - 166) / 2;
        guiGraphics.drawString(this.font, this.title, x + (256 - this.font.width(this.title)) / 2, y, 4210752, false);

        // 渲染命令列表标题
        RenderUtil.drawScrollingShadowlessString(
            guiGraphics,
            this.font,
            Component.literal("Available Commands"),
            x + 6, x + 68,
            y + 15, -1
        );
        RenderUtil.drawScrollingShadowlessString(
            guiGraphics,
            this.font,
            Component.literal("History Commands"),
            x + 188, x + 250,
            y + 15, -1
        );

        // 渲染充能进度条
        guiGraphics.blit(BUTTON_CHARGING_PROGRESS, x + 72, y + 154, 0, 0, getChangingProgress(), 6, 56, 6);

        // 渲染命令建议
        this.commandSuggestions.render(guiGraphics, mouseX, mouseY);

        // 渲染滚动条
        this.renderScroller(
            guiGraphics,
            x + 62, y + 25,
            this.spacetimeSupercomputerBlockEntity.getAvailableCommands().size(),
            this.availableCommandScrollOffset
        );
        this.renderScroller(
            guiGraphics,
            x + 244, y + 25,
            this.spacetimeSupercomputerBlockEntity.getHistoryCommands().size(),
            this.historyCommandScrollOffset
        );

        // 渲染列表上下边界
        guiGraphics.hLine(x + 6, x + 67, y + 24, FastColor.ARGB32.color(128, 177, 177, 177));
        guiGraphics.hLine(x + 188, x + 249, y + 24, FastColor.ARGB32.color(128, 177, 177, 177));
    }

    private int getChangingProgress() {
        float chargingProgress = this.spacetimeSupercomputerBlockEntity.getChargingProgress();
        int changingProgress = (int) (chargingProgress * 56f / 100);
        if (changingProgress > 0 && changingProgress < 1.7f) {
            changingProgress = 1;
        }
        if (changingProgress > 98.2f && changingProgress < 100) {
            changingProgress = 55;
        }
        if (changingProgress >= 100) {
            return 56;
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
