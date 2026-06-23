package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.Collection;
import java.util.function.BiConsumer;

public class SapcetimeSupercomputerCommandSuggestions extends CommandSuggestions {
    private final BiConsumer<CommandDispatcher<ClientSuggestionProvider>, CommandBuildContext> commandFactory;

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
        BiConsumer<CommandDispatcher<ClientSuggestionProvider>, CommandBuildContext> commandFactory
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

    private static CommandDispatcher<ClientSuggestionProvider> buildCommands(
        BiConsumer<CommandDispatcher<ClientSuggestionProvider>, CommandBuildContext> consumer
    ) {
        CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();
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
        String command = this.input.getValue();
        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(command)) {
            this.currentParse = null;
            this.currentParseIsCommand = false;
            this.currentParseIsMessage = false;
        }

        if (!this.keepSuggestions) {
            this.input.setSuggestion(null);
            this.suggestions = null;
        }

        this.commandUsage.clear();
        StringReader reader = new StringReader(command);
        boolean startsWithSlash = reader.canRead() && reader.peek() == '/';
        if (startsWithSlash) {
            reader.skip();
        }

        boolean isCommand = this.commandsOnly || startsWithSlash;
        int cursorPosition = this.input.getCursorPosition();
        if (isCommand) {
            CommandDispatcher<ClientSuggestionProvider> commands = buildCommands(this.commandFactory);
            if (this.currentParse == null) {
                this.currentParse = commands.parse(reader, this.minecraft.player.connection.getSuggestionsProvider());
                this.currentParseIsCommand = true;
                this.currentParseIsMessage = hasMessageArguments(this.currentParse);
            }

            int parseStart = this.onlyShowIfCursorPastError ? reader.getCursor() : 1;
            if (cursorPosition >= parseStart && (this.suggestions == null || !this.keepSuggestions)) {
                this.pendingSuggestions = commands.getCompletionSuggestions(this.currentParse, cursorPosition);
                this.pendingSuggestions.thenAccept(suggestionResult -> {
                    if (this.pendingSuggestions.isDone()) {
                        this.updateUsageInfo(this.currentParse, suggestionResult);
                    }
                });
            }
        } else if (!command.isBlank()) {
            this.currentParseIsMessage = true;
            String partialCommand = command.substring(0, cursorPosition);
            int lastWord = getLastWordIndex(partialCommand);
            Collection<String> nonCommandSuggestions = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSuggestions();
            this.pendingSuggestions = SharedSuggestionProvider.suggest(
                    nonCommandSuggestions, new SuggestionsBuilder(partialCommand, lastWord)
            );
            if (this.currentParseIsMessage && !this.messagesAllowed) {
                this.commandUsage.add(MESSAGES_NOT_ALLOWED_TEXT.getVisualOrderText());
            }

            this.recomputeUsageBoxWidth();
            this.commandUsagePosition = 0;
        } else {
            this.pendingSuggestions = null;
        }
    }

    @Override
    public void extractUsage(GuiGraphicsExtractor graphics) {
        int i = 0;

        for (FormattedCharSequence formattedcharsequence : this.commandUsage) {
            int j = this.anchorToBottom ? this.screen.height - 14 - 13 * i : 72 + 12 * i;
            graphics.fill(
                    this.commandUsagePosition - 1, j, this.commandUsagePosition + this.commandUsageWidth + 1,
                    j + 12, this.fillColor
            );
            graphics.text(this.font, formattedcharsequence, this.commandUsagePosition, j + 2, -1);
            i++;
        }
    }
}
