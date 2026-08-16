package dev.dubhe.anvilcraft.client.gui.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.OverseerBlock;
import dev.dubhe.anvilcraft.block.RemoteTransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;
import dev.dubhe.anvilcraft.item.property.component.StructureData;
import dev.dubhe.anvilcraft.recipe.IDatagen;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockBuilder;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public class StructureToolScreen extends AbstractContainerScreen<StructureToolMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("misc", "structure_tool");
    private static final ResourceLocation BUTTON = SharedTextures.textureGui("misc/structure_tool/button");

    private static final Component REGULAR_RECIPE_TOOLTIP =
        Component.translatable("screen.anvilcraft.structure_tool.regular_recipe");
    private static final Component CONVERSION_RECIPE_TOOLTIP =
        Component.translatable("screen.anvilcraft.structure_tool.conversion_recipe");
    private static final Component CONVERSION_OUTPUT_TOOLTIP =
        Component.translatable("screen.anvilcraft.structure_tool.conversion_output");
    private static final List<Component> RESULT_SLOT_TOOLTIPS = ImmutableList.of(
        REGULAR_RECIPE_TOOLTIP,
        CONVERSION_RECIPE_TOOLTIP,
        CONVERSION_OUTPUT_TOOLTIP
    );

    private static char currentSymbol;

    private static final int SLOT_ID_RESULT = 36;

    @Setter
    private @Nullable StructureData structureData;

    public StructureToolScreen(StructureToolMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    protected void init() {
        super.init();
        int offsetX = (this.width - this.imageWidth) / 2;
        int offsetY = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(new TexturedButton(
            offsetX + 122,
            offsetY + 21,
            46,
            16,
            BUTTON,
            16,
            46,
            32,
            button -> {
                Recipe<?> recipe = toRecipe();
                if (recipe instanceof IDatagen datagenRecipe) {
                    minecraft.keyboardHandler.setClipboard(datagenRecipe.toDatagen());
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.copied_to_clipboard"),
                        false
                    );
                } else {
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_filed").withStyle(ChatFormatting.RED),
                        false
                    );
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_check").withStyle(ChatFormatting.RED),
                        false
                    );
                }

                minecraft.player.closeContainer();
            }
        ));
        this.addRenderableWidget(new TexturedButton(
            offsetX + 122,
            offsetY + 37,
            46,
            16,
            BUTTON,
            16,
            46,
            32,
            button -> button.setFocused(false)
        ));
        this.addRenderableWidget(new TexturedButton(
            offsetX + 122,
            offsetY + 53,
            46,
            16,
            BUTTON,
            16,
            46,
            32,
            button -> {
                Recipe<?> recipe = toRecipe();
                if (recipe == null) {
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_filed")
                            .withStyle(ChatFormatting.RED),
                        false
                    );
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.code_gen_check")
                            .withStyle(ChatFormatting.RED),
                        false
                    );
                    return;
                }
                String defaultName = recipe instanceof IDatagen datagenRecipe
                    ? datagenRecipe.getSuggestedName()
                    : Integer.toHexString(recipe.hashCode());
                String pathString = getFilePath(defaultName);
                if (pathString == null) {
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.no_file_selected")
                            .withStyle(ChatFormatting.RED),
                        false
                    );
                    return;
                }
                Path path = Paths.get(pathString);
                JsonElement json = Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipe).getOrThrow();
                try {
                    String jsonString = AnvilCraft.GSON.toJson(json);
                    Files.writeString(
                        path,
                        jsonString,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE
                    );
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.file_saved", pathString),
                        false
                    );
                } catch (IOException e) {
                    AnvilCraft.LOGGER.error("Error occurred when saving file {}: {}", path, e);
                    minecraft.player.displayClientMessage(
                        Component.translatable("message.anvilcraft.file_save_failed", pathString, e.getMessage())
                            .withStyle(ChatFormatting.RED),
                        false
                    );
                }
                minecraft.player.closeContainer();
            }
        ));
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
            for (int x = structureData.minX(); x <= structureData.maxX(); x++) {
                for (int y = structureData.minY(); y <= structureData.maxY(); y++) {
                    for (int z = structureData.minZ(); z <= structureData.maxZ(); z++) {
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
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot != null && this.hoveredSlot.index == SLOT_ID_RESULT && !this.hoveredSlot.hasItem()) {
            guiGraphics.renderComponentTooltip(font, RESULT_SLOT_TOOLTIPS, x, y);
        }
        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Nullable
    private static String getFilePath(String defaultName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filterBuffer = stack.mallocPointer(1);
            filterBuffer.put(stack.UTF8("*.json"));
            filterBuffer.flip();
            return TinyFileDialogs.tinyfd_saveFileDialog("Save", defaultName, filterBuffer, null);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    private Recipe<?> toRecipe() {
        ScannedStructure inputPattern = this.scanStructure(this.structureData);
        if (inputPattern == null) return null;
        ItemStack result = menu.slots.get(SLOT_ID_RESULT).getItem().copy();
        if (result.is(ModItems.STRUCTURE_TOOL)) {
            StructureData outputData = result.get(ModComponents.STRUCTURE_DATA);
            if (outputData == null) return null;
            if (!outputData.isCube()) {
                minecraft.player.displayClientMessage(
                    Component.translatable("tooltip.anvilcraft.item.structure_tool.must_cube")
                        .withStyle(ChatFormatting.RED),
                    false);
                return null;
            }
            if (!outputData.isOddCubeWithinSize(15)) {
                minecraft.player.displayClientMessage(
                    Component.translatable("tooltip.anvilcraft.item.structure_tool.must_odd")
                        .withStyle(ChatFormatting.RED),
                    false);
                return null;
            }
            if (this.structureData.getSizeX() != outputData.getSizeX()) {
                minecraft.player.displayClientMessage(
                    Component.translatable("tooltip.anvilcraft.item.structure_tool.inconsistent_size")
                        .withStyle(ChatFormatting.RED),
                    false);
                return null;
            }
            ScannedStructure outputPattern = this.scanStructure(outputData, true);
            if (outputPattern == null) return null;
            return toConversionRecipe(inputPattern, outputPattern);
        } else if (!result.isEmpty()) {
            return toMultiblockRecipe(inputPattern, result);
        }
        return null;
    }

    private static MultiblockRecipe toMultiblockRecipe(ScannedStructure pattern, ItemStack result) {
        MultiblockBuilder builder = MultiblockRecipe.builder(result.getItem(), result.getCount());
        pattern.layers().forEach(builder::layer);
        pattern.symbols().forEach(builder::symbol);
        return builder.buildRecipe();
    }

    private static MultiblockConversionRecipe toConversionRecipe(ScannedStructure input, ScannedStructure output) {
        MultiblockConversionRecipe.Builder builder = MultiblockConversionRecipe.builder();
        input.layers().forEach(builder::inputLayer);
        output.layers().forEach(builder::outputLayer);
        input.symbols().forEach(builder::inputSymbol);
        output.symbols().forEach(builder::outputSymbol);
        return builder.buildRecipe();
    }

    public static final Set<Property<?>> DEFAULT_RECORDED_PROPERTIES = ImmutableSet.of(
        // about block's orientation
        BlockStateProperties.FACING,
        BlockStateProperties.FACING_HOPPER,
        BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.VERTICAL_DIRECTION,
        BlockStateProperties.ROTATION_16,
        BlockStateProperties.ORIENTATION,
        BlockStateProperties.AXIS,
        BlockStateProperties.HORIZONTAL_AXIS,
        BlockStateProperties.RAIL_SHAPE,
        BlockStateProperties.RAIL_SHAPE_STRAIGHT,
        BlockStateProperties.HALF,
        // about block's attachment
        BlockStateProperties.ATTACH_FACE,
        BlockStateProperties.BELL_ATTACHMENT,
        BlockStateProperties.HANGING,
        // about fluid state
        BlockStateProperties.WATERLOGGED,
        // about piston state
        BlockStateProperties.EXTENDED,
        BlockStateProperties.PISTON_TYPE,
        // about doors and trapdoors' openness
        BlockStateProperties.OPEN,
        // about count of items need for place
        BlockStateProperties.FLOWER_AMOUNT,
        BlockStateProperties.CANDLES,
        BlockStateProperties.EGGS,
        BlockStateProperties.PICKLES,
        BlockStateProperties.LAYERS,
        BlockStateProperties.LIT,
        BlockStateProperties.LEVEL_CAULDRON,
        BlockStateProperties.SLAB_TYPE,
        // about part of multipart blocks
        BlockStateProperties.BED_PART,
        BlockStateProperties.DOUBLE_BLOCK_HALF,
        GiantAnvilBlock.CUBE,
        GiantAnvilBlock.HALF,
        RemoteTransmissionPoleBlock.HALF,
        TransmissionPoleBlock.HALF,
        TeslaTowerBlock.HALF,
        OverseerBlock.HALF,
        LargeCakeBlock.HALF,
        AccelerationRingBlock.HALF
    );

    private BlockStatePredicate.Builder buildPredicate(BlockState state, boolean recordAllStates) {
        Block block = state.getBlock();
        BlockStatePredicate.Builder builder = BlockStatePredicate.builder().of(block);
        state.getProperties().stream()
            .filter(p -> recordAllStates
                || DEFAULT_RECORDED_PROPERTIES.contains(p)
                || (BlockPlacementUtil.isMultifaceLike(block)
                && p instanceof BooleanProperty
                && PipeBlock.PROPERTY_BY_DIRECTION.containsValue(p)))
            .forEach(p -> StructureToolScreen.recordProperty(builder, state, p));
        return builder;
    }

    private static <T extends Comparable<T>> void recordProperty(
        BlockStatePredicate.Builder builder,
        BlockState state,
        Property<T> property
    ) {
        builder.with(property, state.getValue(property));
    }

    @Nullable
    private ScannedStructure scanStructure(@Nullable StructureData data) {
        return this.scanStructure(data, false);
    }

    @SuppressWarnings("DataFlowIssue")
    @Nullable
    private ScannedStructure scanStructure(@Nullable StructureData data, boolean recordAllStates) {
        ClientLevel level = minecraft.level;
        if (data == null || level == null) {
            return null;
        }
        currentSymbol = '@';
        Map<Character, BlockStatePredicate.Builder> symbols = new LinkedHashMap<>();
        Map<BlockState, Character> symbolByState = new HashMap<>();
        List<List<String>> layers = new ArrayList<>();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int y = data.minY(); y <= data.maxY(); y++) {
            List<String> layer = new ArrayList<>();
            for (int z = data.minZ(); z <= data.maxZ(); z++) {
                StringBuilder sb = new StringBuilder();
                for (int x = data.minX(); x <= data.maxX(); x++) {
                    BlockState state = level.getBlockState(mpos.set(x, y, z));
                    if (state.is(Blocks.AIR)) {
                        sb.append(' ');
                        continue;
                    }
                    BlockStatePredicate.Builder predicate = this.buildPredicate(state, recordAllStates);
                    sb.append(getAndPutSymbol(symbols, symbolByState, predicate));
                }
                layer.add(sb.toString());
            }
            layers.add(layer);
        }
        return new ScannedStructure(layers, symbols);
    }

    private char getAndPutSymbol(
        Map<Character, BlockStatePredicate.Builder> symbols,
        Map<BlockState, Character> symbolByState,
        BlockStatePredicate.Builder predicate
    ) {
        BlockState key = MultiblockUtil.getDefaultState(predicate.build());
        Character existing = symbolByState.get(key);
        if (existing != null) {
            return existing;
        }
        currentSymbol++;
        symbols.put(currentSymbol, predicate);
        symbolByState.put(key, currentSymbol);
        return currentSymbol;
    }

    private record ScannedStructure(
        List<List<String>> layers,
        Map<Character, BlockStatePredicate.Builder> symbols
    ) {
    }
}
