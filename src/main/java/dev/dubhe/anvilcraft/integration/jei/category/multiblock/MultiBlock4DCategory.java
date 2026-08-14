package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.JeiButton;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextureConstants;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.ItemStackMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MultiBlock4DCategory implements IRecipeCategory<RecipeHolder<Multiblock4DRecipe>> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.4d_multiblock");

    private static final Comparator<ItemStack> BY_COUNT_DECREASING =
        Comparator.comparing(ItemStack::getCount).thenComparing(ItemStack::getDescriptionId).reversed();

    public static final int WIDTH = 162;
    public static final int START_HEIGHT = 100;
    public static final int ROWS = 2;

    public static final int SCALE_FAC = 80;
    private final Map<RecipeHolder<Multiblock4DRecipe>, List<LevelLike>> levelCache = new HashMap<>();
    private final Map<RecipeHolder<Multiblock4DRecipe>, Integer> sizeCache = new HashMap<>();
    private final Map<RecipeHolder<Multiblock4DRecipe>, Integer> stepMap = new HashMap<>();

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable layerUp;
    private final IDrawable layerUpHovered;
    private final IDrawable layerDown;
    private final IDrawable layerDownHovered;
    private final IDrawable renderSwitchOn;
    private final IDrawable renderSwitchOff;
    private final IDrawable arrowOut;
    private final IDrawable conversion;
    private final ITickTimer timer;

    public MultiBlock4DCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack());
        arrowOut = JeiRenderHelper.getArrowInput(helper);
        slot = JeiRenderHelper.getSlotDefault(helper);
        timer = helper.createTickTimer(30, 60, true);
        conversion = helper.drawableBuilder(JeiTextureConstants.BLOCK_CRAFTING, 0, 0, 594, 418)
            .setTextureSize(594, 418)
            .build();
        layerUp = helper.drawableBuilder(JeiTextureConstants.LAYER_UP, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        layerUpHovered = helper.drawableBuilder(JeiTextureConstants.LAYER_UP, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        layerDown = helper.drawableBuilder(JeiTextureConstants.LAYER_DOWN, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        layerDownHovered = helper.drawableBuilder(JeiTextureConstants.LAYER_DOWN, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        renderSwitchOff = helper.drawableBuilder(JeiTextureConstants.LAYER_SWITCH, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        renderSwitchOn = helper.drawableBuilder(JeiTextureConstants.LAYER_SWITCH, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
    }

    @Override
    public RecipeType<RecipeHolder<Multiblock4DRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTIBLOCK_4D;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return START_HEIGHT + ROWS * 18;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<Multiblock4DRecipe> recipe, IFocusGroup focuses) {
        levelCache.computeIfAbsent(recipe, this::buildLevels);
        sizeCache.computeIfAbsent(recipe, this::maxSize);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 70)
            .addItemStack(recipe.value().getResult().copy());

        var level = Minecraft.getInstance().level;
        Map<ItemStack, Integer> itemCounts = ItemStackMap.createTypeAndTagMap();
        Map<TagKey<Block>, Integer> tagCounts = new HashMap<>();
        for (var definition : recipe.value().getDefinitions()) {
            var pattern = MultiblockUtil.toBlockPattern(definition);
            for (ItemStack stack : pattern.toIngredientList(level == null ? null : level.registryAccess())) {
                int count = stack.getCount();
                ItemStack key = stack.copy();
                key.setCount(1);
                itemCounts.merge(key, count, Integer::sum);
            }
            pattern.getTagIngredientCounts().forEach((tag, count) -> tagCounts.merge(tag, count, Integer::sum));
        }

        List<ItemStack> ingredientList = new ArrayList<>();
        itemCounts.forEach((stack, count) -> {
            stack.setCount(count);
            ingredientList.add(stack);
        });
        ingredientList.sort(BY_COUNT_DECREASING);

        int slotIndex = 0;
        for (ItemStack stack : ingredientList) {
            int row = slotIndex / 9;
            int col = slotIndex % 9;
            builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, START_HEIGHT + row * 18 + 1)
                .addItemStack(stack);
            slotIndex++;
        }

        for (var entry : tagCounts.entrySet()) {
            TagKey<Block> blockTag = entry.getKey();
            int count = entry.getValue();
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, blockTag.location());
            int row = slotIndex / 9;
            int col = slotIndex % 9;
            var slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, START_HEIGHT + row * 18 + 1);
            BuiltInRegistries.ITEM.getTag(itemTag).ifPresent(tag -> tag.stream().forEach(
                holder -> slotBuilder.addItemStack(new ItemStack(holder.value(), count))
            ));
            slotIndex++;
        }
    }

    @Override
    public void draw(
        RecipeHolder<Multiblock4DRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        List<LevelLike> levels = levelCache.computeIfAbsent(recipe, this::buildLevels);
        int count = levels.size();
        int step = count == 0 ? 0 : Math.floorMod(stepMap.getOrDefault(recipe, 0), count);
        LevelLike level = count == 0 ? null : levels.get(step);

        final boolean renderAllLayers = level != null && level.isAllLayersVisible();
        final int visibleLayer = level == null ? 0 : level.getCurrentVisibleLayer();
        if (level != null) {
            RenderSupport.renderLevelLike(level, guiGraphics, 45, 50, SCALE_FAC, 2.0f);
        }
        final Minecraft minecraft = Minecraft.getInstance();
        int sizeY = level == null ? 0 : level.verticalSize();
        Component layerComponent;
        if (level == null || renderAllLayers) {
            layerComponent = Component.translatable("gui.anvilcraft.category.multiblock.all_layers");
            renderSwitchOff.draw(guiGraphics, 125, 10);
        } else {
            layerComponent = Component.translatable(
                "gui.anvilcraft.category.multiblock.single_layer", visibleLayer + 1, sizeY);
            renderSwitchOn.draw(guiGraphics, 125, 10);
            this.layerUpButton(mouseX, mouseY).draw(guiGraphics, 137, 10);
            this.layerDownButton(mouseX, mouseY).draw(guiGraphics, 149, 10);
        }

        this.timeUpButton(mouseX, mouseY).draw(guiGraphics, 137, 33);
        this.timeDownButton(mouseX, mouseY).draw(guiGraphics, 149, 33);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(114.5, 50.15, 0);
        pose.scale(0.035f, 0.035f, 1.0f);
        conversion.draw(guiGraphics, 0, 0);
        pose.popPose();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer) / 3;
        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            125F,
            44.8f + anvilYOffset,
            20,
            5,
            RenderSupport.SINGLE_BLOCK
        );
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 0.8f);
        int textX = Math.round(WIDTH / 0.8f - minecraft.font.width(layerComponent) - 5);
        guiGraphics.drawString(minecraft.font, layerComponent, textX, 0, 0xFF000000, false);
        Component stepComponent = count == 0
            ? Component.translatable("gui.anvilcraft.category.4d_multiblock.step", 0, 0)
            : Component.translatable("gui.anvilcraft.category.4d_multiblock.step", step + 1, count);
        int stepTextX = Math.round(WIDTH / 0.8f - minecraft.font.width(stepComponent) - 5);
        guiGraphics.drawString(minecraft.font, stepComponent, stepTextX, 29, 0xFF000000, false);
        int size = sizeCache.computeIfAbsent(recipe, this::maxSize);
        guiGraphics.drawString(
            minecraft.font,
            Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
            85, 115, 0xFF000000, false
        );
        pose.popPose();
        arrowOut.draw(guiGraphics, 97, 60);
        slot.draw(guiGraphics, 116, 69);

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < 9; j++) {
                slot.draw(guiGraphics, j * 18, START_HEIGHT + i * 18);
            }
        }
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 10 && mouseY < 20) ? layerUpHovered : layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 10 && mouseY < 20) ? layerDownHovered : layerDown;
    }

    private IDrawable timeUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 33 && mouseY < 43) ? layerUpHovered : layerUp;
    }

    private IDrawable timeDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 33 && mouseY < 43) ? layerDownHovered : layerDown;
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<Multiblock4DRecipe> recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new JeiButton<>(
            125,
            10,
            10,
            it -> {
                List<LevelLike> levels = levelCache.computeIfAbsent(it, this::buildLevels);
                if (levels.isEmpty()) return;
                boolean value = !levels.getFirst().isAllLayersVisible();
                levels.forEach(level -> level.setAllLayersVisible(value));
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            137,
            10,
            10,
            it -> {
                LevelLike level = this.currentLevel(it);
                if (level != null && !level.isAllLayersVisible()) level.nextLayer();
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            149,
            10,
            10,
            it -> {
                LevelLike level = this.currentLevel(it);
                if (level != null && !level.isAllLayersVisible()) level.previousLayer();
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            137,
            33,
            10,
            it -> {
                int count = levelCache.computeIfAbsent(it, this::buildLevels).size();
                if (count == 0) return;
                stepMap.merge(it, 1, (old, add) -> Math.floorMod(old + add, count));
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            149,
            33,
            10,
            it -> {
                int count = levelCache.computeIfAbsent(it, this::buildLevels).size();
                if (count == 0) return;
                stepMap.merge(it, -1, (old, add) -> Math.floorMod(old + add, count));
            },
            recipe
        ));
    }

    private @Nullable LevelLike currentLevel(RecipeHolder<Multiblock4DRecipe> recipe) {
        List<LevelLike> levels = levelCache.computeIfAbsent(recipe, this::buildLevels);
        int count = levels.size();
        if (count == 0) return null;
        return levels.get(Math.floorMod(stepMap.getOrDefault(recipe, 0), count));
    }

    private List<LevelLike> buildLevels(RecipeHolder<Multiblock4DRecipe> recipe) {
        return recipe.value().getDefinitions().stream()
            .map(RecipeUtil::asLevelLike)
            .toList();
    }

    private int maxSize(RecipeHolder<Multiblock4DRecipe> recipe) {
        return recipe.value().getDefinitions().stream()
            .mapToInt(definition -> MultiblockUtil.toBlockPattern(definition).getSize())
            .max()
            .orElse(0);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MULTIBLOCK_4D,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIBLOCK_4D_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_4D);
        registration.addRecipeCatalyst(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_4D);
        registration.addRecipeCatalyst(Items.CRAFTING_TABLE.getDefaultInstance(), AnvilCraftJeiPlugin.MULTIBLOCK_4D);
        registration.addRecipeCatalyst(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_4D);
    }
}
