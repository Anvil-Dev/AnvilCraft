package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.Collection;
import java.util.function.BiConsumer;

public class SapcetimeSupercomputerCommandSuggestions extends CommandSuggestions {
    private final BiConsumer<CommandDispatcher<SharedSuggestionProvider>, CommandBuildContext> commandFactory;

    public SapcetimeSupercomputerCommandSuggestions(
        Minecraft minecraft,
        Screen screen,
        EditBox input,
        Font font,
        boolean commandsOnly,
        boolean onlyShowIfCursorPastError,
        int lineStartOffset,
        int suggestionLineLimit,
        boolean anchorToBottom,
        int fillColor,
        BiConsumer<CommandDispatcher<SharedSuggestionProvider>, CommandBuildContext> commandFactory
    ) {
        super(
            minecraft,
            screen,
            input,
            font,
            commandsOnly,
            onlyShowIfCursorPastError,
            lineStartOffset,
            suggestionLineLimit,
            anchorToBottom,
            fillColor
        );
        this.commandFactory = commandFactory;
    }

    private static CommandDispatcher<SharedSuggestionProvider> buildCommands(
        BiConsumer<CommandDispatcher<SharedSuggestionProvider>, CommandBuildContext> consumer
    ) {
        CommandDispatcher<SharedSuggestionProvider> dispatcher = new CommandDispatcher<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            RegistryAccess registryAccess = level.registryAccess();
            FeatureFlagSet featureFlagSet = level.enabledFeatures();
            CommandBuildContext context = CommandBuildContext.simple(registryAccess, featureFlagSet);

            consumer.accept(dispatcher, context);
        }

        return dispatcher;
    }

    @Override
    public void updateCommandInfo() {
        String s = this.input.getValue();
        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(s)) {
            this.currentParse = null;
        }

        if (!this.keepSuggestions) {
            this.input.setSuggestion(null);
            this.suggestions = null;
        }

        this.commandUsage.clear();
        StringReader stringreader = new StringReader(s);
        boolean flag = stringreader.canRead() && stringreader.peek() == '/';
        if (flag) {
            stringreader.skip();
        }

        boolean flag1 = this.commandsOnly || flag;
        int i = this.input.getCursorPosition();
        if (flag1) {
            CommandDispatcher<SharedSuggestionProvider> commanddispatcher = buildCommands(this.commandFactory);
            if (this.currentParse == null) {
                if (this.minecraft.player != null) {
                    this.currentParse = commanddispatcher.parse(stringreader, this.minecraft.player.connection.getSuggestionsProvider());
                }
            }

            int j = this.onlyShowIfCursorPastError ? stringreader.getCursor() : 1;
            if (i >= j && (this.suggestions == null || !this.keepSuggestions)) {
                this.pendingSuggestions = commanddispatcher.getCompletionSuggestions(this.currentParse, i);
                this.pendingSuggestions.thenRun(() -> {
                    if (this.pendingSuggestions.isDone()) {
                        this.updateUsageInfo();
                    }
                });
            }
        } else {
            String s1 = s.substring(0, i);
            int k = getLastWordIndex(s1);
            if (this.minecraft.player != null) {
                Collection<String> collection = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
                this.pendingSuggestions = SharedSuggestionProvider.suggest(collection, new SuggestionsBuilder(s1, k));
            }
        }
    }

    @Override
    public void renderUsage(GuiGraphics guiGraphics) {
        int i = 0;

        for (FormattedCharSequence formattedcharsequence : this.commandUsage) {
            int j = this.anchorToBottom ? this.screen.height - 14 - 13 * i : 72 + 12 * i;
            guiGraphics.fill(
                this.commandUsagePosition - 1, j, this.commandUsagePosition + this.commandUsageWidth + 1,
                j + 12, this.fillColor
            );
            guiGraphics.drawString(this.font, formattedcharsequence, this.commandUsagePosition, j + 2, -1);
            i++;
        }
    }
}
