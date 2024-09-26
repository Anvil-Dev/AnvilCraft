package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;
import dev.dubhe.anvilcraft.item.StructureToolItem;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StructureToolScreen extends AbstractContainerScreen<StructureToolMenu> {
    private final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/structure_tool/background.png");

    private static final WidgetSprites SPRITES = new WidgetSprites(
            AnvilCraft.of("widget/structure_tool/button"), AnvilCraft.of("widget/structure_tool/button_highlighted"));

    private static char currentSymbol;

    private ImageButton dataGenButton;
    private ImageButton kubejsButton;
    private ImageButton jsonButton;

    @Setter
    private StructureToolItem.StructureData structureData;

    public StructureToolScreen(StructureToolMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int offsetX = (this.width - this.imageWidth) / 2;
        int offsetY = (this.height - this.imageHeight) / 2;

        dataGenButton = addRenderableWidget(new ImageButton(offsetX + 122, offsetY + 21, 46, 16, SPRITES, button -> {
            MultiblockRecipe recipe = toRecipe();
            if (recipe != null) {
                ItemStack result = recipe.getResult();
                StringBuilder codeBuilder = new StringBuilder("MultiblockRecipe.builder(\"%s\", %d)"
                        .formatted(BuiltInRegistries.ITEM.getKey(result.getItem()), result.getCount()));
                codeBuilder.append("\n");

                for (List<String> layer : recipe.pattern.getLayers()) {
                    codeBuilder.append("    .layer(");
                    codeBuilder.append(layer.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
                    codeBuilder.append(")");
                    codeBuilder.append("\n");
                }
                recipe.pattern.getSymbols().forEach((symbol, predicate) -> {
                    codeBuilder.append("    .symbol(");
                    codeBuilder.append("'").append(symbol).append("'");
                    codeBuilder.append(", ");
                    if (predicate.getProperties().isEmpty()) {
                        codeBuilder.append("\"");
                        codeBuilder.append(BuiltInRegistries.BLOCK.getKey(predicate.getBlock()));
                        codeBuilder.append("\"");
                        codeBuilder.append(")");
                    } else {
                        codeBuilder.append("BlockPredicateWithState.of(");
                        codeBuilder.append(BuiltInRegistries.BLOCK.getKey(predicate.getBlock()));
                        codeBuilder.append(")");
                        codeBuilder.append("\n");
                        predicate.getProperties().forEach((stateName, stateValue) -> {
                            codeBuilder.append("        .hasState(");
                            codeBuilder.append("\"").append(stateName).append("\"");
                            codeBuilder.append(", ");
                            codeBuilder.append("\"").append(stateValue).append("\"");
                            codeBuilder.append("\n");
                        });
                        codeBuilder.append("    )");
                    }
                    codeBuilder.append("\n");
                });
                codeBuilder.append("    .save(provider);");
                minecraft.keyboardHandler.setClipboard(codeBuilder.toString());
                minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.copied_to_clipboard"), false);
            } else {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_filed")
                                .withStyle(ChatFormatting.RED),
                        false);
                minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_check")
                                .withStyle(ChatFormatting.RED),
                        false);
            }

            minecraft.player.closeContainer();
        }));
        kubejsButton = addRenderableWidget(new ImageButton(offsetX + 122, offsetY + 37, 46, 16, SPRITES, button -> {
            button.setFocused(false);
        }));
        jsonButton = addRenderableWidget(new ImageButton(offsetX + 122, offsetY + 53, 46, 16, SPRITES, button -> {
            MultiblockRecipe recipe = toRecipe();
            if (recipe != null) {
                ItemStack result = recipe.getResult();
                String pathString = getFilePath(
                        BuiltInRegistries.ITEM.getKey(result.getItem()).getPath(), "*.json");
                if (pathString != null) {
                    Path path = Paths.get(pathString);
                    JsonElement json =
                            Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipe).getOrThrow();
                    try {
                        String jsonString = AnvilCraft.GSON.toJson(json);
                        Files.writeString(
                                path,
                                jsonString,
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                        minecraft.player.displayClientMessage(
                                Component.translatable("message.anvilcraft.file_saved", pathString), false);
                    } catch (IOException e) {
                        AnvilCraft.LOGGER.error("Saving {} has error", path, e);
                        minecraft.player.displayClientMessage(
                                Component.translatable(
                                                "message.anvilcraft.file_save_failed", pathString, e.getMessage())
                                        .withStyle(ChatFormatting.RED),
                                false);
                    }
                } else {
                    minecraft.player.displayClientMessage(
                            Component.translatable("message.anvilcraft.no_file_selected")
                                    .withStyle(ChatFormatting.RED),
                            false);
                }
            } else {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_filed")
                                .withStyle(ChatFormatting.RED),
                        false);
                minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_check")
                                .withStyle(ChatFormatting.RED),
                        false);
            }
            minecraft.player.closeContainer();
        }));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        PoseStack pose = guiGraphics.pose();
        ClientLevel level = Minecraft.getInstance().level;
        // structureData Text render
        if (structureData != null && level != null) {
            pose.pushPose();

            pose.translate((this.width - this.imageWidth) / 2f, (this.height - this.imageHeight) / 2f, 0);
            pose.scale(0.75F, 0.75F, 0.75F);

            guiGraphics.drawString(
                    font, Component.translatable("screen.anvilcraft.structure_tool.size"), 18, 30, 0xFFFFFFFF, true);
            guiGraphics.drawString(font, "X: " + structureData.getSizeX(), 24, 40, 0xFFFFFFFF, true);
            guiGraphics.drawString(font, "Y: " + structureData.getSizeY(), 24, 50, 0xFFFFFFFF, true);
            guiGraphics.drawString(font, "Z: " + structureData.getSizeZ(), 24, 60, 0xFFFFFFFF, true);

            int blockCount = 0;
            for (int x = structureData.getMinX(); x <= structureData.getMaxX(); x++) {
                for (int y = structureData.getMinY(); y <= structureData.getMaxY(); y++) {
                    for (int z = structureData.getMinZ(); z <= structureData.getMaxZ(); z++) {
                        if (!level.getBlockState(new BlockPos(x, y, z)).is(Blocks.AIR)) {
                            blockCount++;
                        }
                    }
                }
            }

            guiGraphics.drawString(
                    font,
                    Component.translatable("screen.anvilcraft.structure_tool.count", blockCount),
                    18,
                    72,
                    0xFFFFFFFF,
                    true);
            pose.popPose();
        }
        // button text render
        pose.pushPose();

        pose.translate((this.width - this.imageWidth) / 2f, (this.height - this.imageHeight) / 2f, 0);
        pose.scale(0.7F, 0.7F, 0.7F);

        guiGraphics.drawString(
                font,
                Component.translatable("screen.anvilcraft.structure_tool.to_data_gen"),
                177,
                37,
                0xFFFFFFFF,
                true);
        guiGraphics.drawString(
                font, Component.translatable("screen.anvilcraft.structure_tool.to_kubejs"), 177, 60, 0xFFFFFFFF, true);
        guiGraphics.drawString(
                font, Component.translatable("screen.anvilcraft.structure_tool.to_json"), 177, 83, 0xFFFFFFFF, true);

        pose.popPose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Nullable private static String getFilePath(String defaultName, String filter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filterBuffer = stack.mallocPointer(1);
            filterBuffer.put(stack.UTF8(filter));
            filterBuffer.flip();
            return TinyFileDialogs.tinyfd_saveFileDialog("Save", defaultName, filterBuffer, null);
        }
    }

    @Nullable private MultiblockRecipe toRecipe() {
        BlockPattern pattern = toBlockPattern();
        ItemStack result = menu.slots.get(4 * 9).getItem().copy();
        if (pattern != null && !result.isEmpty()) {
            return new MultiblockRecipe(pattern, result);
        }
        return null;
    }

    @Nullable private BlockPattern toBlockPattern() {
        ClientLevel level = minecraft.level;
        if (structureData != null && level != null) {
            BlockPattern pattern = BlockPattern.create();
            currentSymbol = '@';
            for (int y = structureData.getMinY(); y <= structureData.getMaxY(); y++) {
                List<String> layer = new ArrayList<>();
                for (int z = structureData.getMinZ(); z <= structureData.getMaxZ(); z++) {
                    StringBuilder sb = new StringBuilder();
                    for (int x = structureData.getMinX(); x <= structureData.getMaxX(); x++) {
                        BlockState state = level.getBlockState(new BlockPos(x, y, z));
                        if (state.is(Blocks.AIR)) {
                            sb.append(' ');
                            continue;
                        }
                        BlockPredicateWithState predicate = BlockPredicateWithState.of(state.getBlock());
                        if (state.hasProperty(BlockStateProperties.FACING)) {
                            predicate.hasState(
                                    BlockStateProperties.FACING, state.getValue(BlockStateProperties.FACING));
                        }
                        if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
                            predicate.hasState(
                                    BlockStateProperties.FACING_HOPPER,
                                    state.getValue(BlockStateProperties.FACING_HOPPER));
                        }
                        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            predicate.hasState(
                                    BlockStateProperties.HORIZONTAL_FACING,
                                    state.getValue(BlockStateProperties.HORIZONTAL_FACING));
                        }
                        if (state.hasProperty(BlockStateProperties.AXIS)) {
                            predicate.hasState(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
                        }
                        if (state.hasProperty(BlockStateProperties.SLAB_TYPE)) {
                            predicate.hasState(
                                    BlockStateProperties.SLAB_TYPE, state.getValue(BlockStateProperties.SLAB_TYPE));
                        }
                        if (state.hasProperty(BlockStateProperties.HALF)) {
                            predicate.hasState(BlockStateProperties.HALF, state.getValue(BlockStateProperties.HALF));
                        }
                        sb.append(getAndPutSymbol(pattern.getSymbols(), predicate));
                    }
                    layer.add(sb.toString());
                }
                pattern.layer(layer);
            }
            pattern.checkSymbols();
            return pattern;
        } else {
            return null;
        }
    }

    private char getAndPutSymbol(Map<Character, BlockPredicateWithState> symbols, BlockPredicateWithState predicate) {
        if (symbols.entrySet().stream().noneMatch(e -> e.getValue().equals(predicate))) {
            currentSymbol++;
            symbols.put(currentSymbol, predicate);
        } else {
            for (Map.Entry<Character, BlockPredicateWithState> entry : symbols.entrySet()) {
                if (entry.getValue().equals(predicate)) {
                    return entry.getKey();
                }
            }
        }
        return currentSymbol;
    }
}
