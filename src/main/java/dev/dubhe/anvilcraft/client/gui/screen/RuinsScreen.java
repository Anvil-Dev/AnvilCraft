package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.dubhe.anvilcraft.network.RuinsUpdatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RuinsScreen extends Screen {
    private final BlockPos pos;
    private final String initialDrops;
    private final List<String> lootTables;
    private EditBox input;
    private CommandSuggestions suggestions;
    private Button save;
    private boolean fragile;

    public RuinsScreen(BlockPos pos, String drops, boolean fragile, List<String> lootTables) {
        super(Component.translatable("screen.anvilcraft.ruins.title"));
        this.pos = pos;
        this.initialDrops = drops;
        this.fragile = fragile;
        this.lootTables = lootTables;
    }

    public static void open(BlockPos pos, String drops, boolean fragile, List<String> lootTables) {
        Minecraft.getInstance().setScreen(new RuinsScreen(pos, drops, fragile, lootTables));
    }

    @Override
    protected void init() {
        String value = this.input == null ? this.initialDrops : this.input.getValue();
        int left = this.width / 2 - 150;
        int top = this.height / 2 - 60;
        this.input = this.addRenderableWidget(new EditBox(this.font, left, top, 300, 20,
            Component.translatable("screen.anvilcraft.ruins.loot_table")));
        this.input.setMaxLength(256);
        this.input.setValue(value);
        this.save = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            PacketDistributor.sendToServer(new RuinsUpdatePacket(this.pos, this.input.getValue(), this.fragile));
            this.onClose();
        }).bounds(left, top + 130, 145, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
            .bounds(left + 155, top + 130, 145, 20).build());
        this.addRenderableWidget(Button.builder(this.interactionLabel(), button -> {
            this.fragile = !this.fragile;
            button.setMessage(this.interactionLabel());
        }).bounds(left, top + 104, 300, 20).build());
        this.suggestions = new LootSuggestions(Minecraft.getInstance(), this, this.input);
        this.suggestions.setAllowSuggestions(true);
        this.input.setResponder(text -> {
            this.validateInput();
            this.suggestions.updateCommandInfo();
        });
        this.validateInput();
        this.setInitialFocus(this.input);
        this.suggestions.updateCommandInfo();
    }

    private Component interactionLabel() {
        return Component.translatable(this.fragile ? "screen.anvilcraft.ruins.fragile" : "screen.anvilcraft.ruins.inert");
    }

    private void validateInput() {
        ResourceLocation id = ResourceLocation.tryParse(this.input.getValue());
        this.save.active = this.input.getValue().equals(this.initialDrops) || id != null && this.lootTables.contains(id.toString());
        this.input.setTextColor(this.save.active ? 0xFFFFFF : 0xFF5555);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("screen.anvilcraft.ruins.loot_table"),
            this.width / 2 - 150, this.height / 2 - 75, 0xFFFFFF);
        this.suggestions.render(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.suggestions.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.suggestions.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.suggestions.mouseScrolled(scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class LootSuggestions extends CommandSuggestions {
        LootSuggestions(Minecraft minecraft, Screen screen, EditBox input) {
            super(minecraft, screen, input, RuinsScreen.this.font, false, false, 0, 6, false, 0xD0000000);
        }

        @Override
        public void updateCommandInfo() {
            if (this.keepSuggestions) return;
            this.input.setSuggestion(null);
            this.suggestions = null;
            String text = this.input.getValue().substring(0, this.input.getCursorPosition());
            this.pendingSuggestions = SharedSuggestionProvider.suggestResource(
                RuinsScreen.this.lootTables.stream().map(ResourceLocation::parse), new SuggestionsBuilder(text, 0));
            this.showSuggestions(false);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY) {
            int offset = this.input.getY() + this.input.getHeight() + 2 - 72;
            graphics.pose().pushPose();
            graphics.pose().translate(0, offset, 0);
            super.render(graphics, mouseX, mouseY - offset);
            graphics.pose().popPose();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int offset = this.input.getY() + this.input.getHeight() + 2 - 72;
            return super.mouseClicked(mouseX, mouseY - offset, button);
        }
    }
}
