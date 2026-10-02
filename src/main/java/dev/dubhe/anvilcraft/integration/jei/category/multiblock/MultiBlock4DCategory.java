package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import dev.dubhe.anvilcraft.client.support.LevelLikeDisplaySupport;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.JeiButton;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextures;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import dev.dubhe.anvilcraft.util.LevelLike;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlock4DCategory implements IRecipeCategory<RecipeHolder<Multiblock4DRecipe>> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.4d_multiblock");

    private static final Comparator<ItemStack> BY_COUNT_DECREASING =
        Comparator.comparing(ItemStack::getCount).thenComparing(stack -> stack.getItem().getDescriptionId()).reversed();

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
        this.icon = helper.createDrawableItemStack(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack());
        this.arrowOut = JeiRenderHelper.getArrowInput(helper);
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.conversion = helper.drawableBuilder(JeiTextures.BLOCK_4D, 0, 0, 594, 418)
            .setTextureSize(594, 418)
            .build();
        this.layerUp = helper.drawableBuilder(JeiTextures.LAYER_UP, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerUpHovered = helper.drawableBuilder(JeiTextures.LAYER_UP, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerDown = helper.drawableBuilder(JeiTextures.LAYER_DOWN, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerDownHovered = helper.drawableBuilder(JeiTextures.LAYER_DOWN, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.renderSwitchOff = helper.drawableBuilder(JeiTextures.LAYER_SWITCH, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.renderSwitchOn = helper.drawableBuilder(JeiTextures.LAYER_SWITCH, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
    }

    @Override
    public IRecipeHolderType<Multiblock4DRecipe> getRecipeType() {
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
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<Multiblock4DRecipe> recipe, IFocusGroup focuses) {
        this.levelCache.computeIfAbsent(recipe, this::buildLevels);
        this.sizeCache.computeIfAbsent(recipe, this::maxSize);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 70)
            .add(recipe.value().getResult().create());

        var level = Minecraft.getInstance().level;
        Map<ItemStack, Integer> itemCounts = ItemStackMap.createTypeAndTagMap();
        Map<TagKey<Block>, Integer> tagCounts = new HashMap<>();
        for (var definition : recipe.value().getDefinitions()) {
            for (ItemStack stack : MultiblockUtil.ingredientList(
                definition, level == null ? null : level.registryAccess())) {
                int count = stack.getCount();
                ItemStack key = stack.copy();
                key.setCount(1);
                itemCounts.merge(key, count, Integer::sum);
            }
            MultiblockUtil.tagIngredientCounts(definition)
                .forEach((tag, count) -> tagCounts.merge(tag, count, Integer::sum));
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
                .add(stack);
            slotIndex++;
        }

        for (var entry : tagCounts.entrySet()) {
            TagKey<Block> blockTag = entry.getKey();
            int count = entry.getValue();
            TagKey<Item> itemTag = TagKey.create(Registries.ITEM, blockTag.location());
            int row = slotIndex / 9;
            int col = slotIndex % 9;
            var slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, START_HEIGHT + row * 18 + 1);
            BuiltInRegistries.ITEM.get(itemTag).ifPresent(tag -> tag.stream().forEach(
                holder -> slotBuilder.add(new ItemStack(holder.value(), count))
            ));
            slotIndex++;
        }
    }

    @Override
    public void draw(
        RecipeHolder<Multiblock4DRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {
        List<LevelLike> levels = this.levelCache.computeIfAbsent(recipe, this::buildLevels);
        int count = levels.size();
        int step = count == 0 ? 0 : Math.floorMod(this.stepMap.getOrDefault(recipe, 0), count);
        LevelLike level = count == 0 ? null : levels.get(step);

        final boolean renderAllLayers = level != null && level.isAllLayersVisible();
        final int visibleLayer = level == null ? 0 : level.getCurrentVisibleLayer();
        if (level != null) {
            LevelLikeDisplaySupport.cycleTags(level);
            RenderSupport.renderLevelLikeAt(level, guiGraphics, 45, 50, SCALE_FAC, 2.0F);
        }
        final Minecraft minecraft = Minecraft.getInstance();
        int sizeY = level == null ? 0 : level.verticalSize();
        Component layerComponent;
        if (level == null || renderAllLayers) {
            layerComponent = Component.translatable("gui.anvilcraft.category.multiblock.all_layers");
            this.renderSwitchOff.draw(guiGraphics, 125, 10);
        } else {
            layerComponent = Component.translatable(
                "gui.anvilcraft.category.multiblock.single_layer", visibleLayer + 1, sizeY);
            this.renderSwitchOn.draw(guiGraphics, 125, 10);
            this.layerUpButton(mouseX, mouseY).draw(guiGraphics, 137, 10);
            this.layerDownButton(mouseX, mouseY).draw(guiGraphics, 149, 10);
        }

        this.timeUpButton(mouseX, mouseY).draw(guiGraphics, 137, 33);
        this.timeDownButton(mouseX, mouseY).draw(guiGraphics, 149, 33);

        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(114.5F, 50.15F);
        pose.scale(0.035F, 0.035F);
        this.conversion.draw(guiGraphics, 0, 0);
        pose.popMatrix();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3;
        RenderSupport.renderBlockAt(
            guiGraphics,
            JeiBlockIngredientUtil.getRenderablePreviewState(ModBlocks.GIANT_ANVIL.getDefaultState()),
            125F,
            44.8f + anvilYOffset,
            5
        );
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        int textX = Math.round(WIDTH / 0.8f - minecraft.font.width(layerComponent) - 5);
        guiGraphics.text(minecraft.font, layerComponent, textX, 0, 0xFF000000, false);
        Component stepComponent = count == 0
            ? Component.translatable("gui.anvilcraft.category.4d_multiblock.step", 0, 0)
            : Component.translatable("gui.anvilcraft.category.4d_multiblock.step", step + 1, count);
        int stepTextX = Math.round(WIDTH / 0.8f - minecraft.font.width(stepComponent) - 5);
        guiGraphics.text(minecraft.font, stepComponent, stepTextX, 29, 0xFF000000, false);
        int size = this.sizeCache.computeIfAbsent(recipe, this::maxSize);
        guiGraphics.text(
            minecraft.font,
            Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
            85, 115, 0xFF000000, false
        );
        pose.popMatrix();
        this.arrowOut.draw(guiGraphics, 97, 60);
        this.slot.draw(guiGraphics, 116, 69);

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < 9; j++) {
                this.slot.draw(guiGraphics, j * 18, START_HEIGHT + i * 18);
            }
        }
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 10 && mouseY < 20) ? this.layerUpHovered : this.layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 10 && mouseY < 20) ? this.layerDownHovered : this.layerDown;
    }

    private IDrawable timeUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 33 && mouseY < 43) ? this.layerUpHovered : this.layerUp;
    }

    private IDrawable timeDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 33 && mouseY < 43) ? this.layerDownHovered : this.layerDown;
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<Multiblock4DRecipe> recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new JeiButton<>(
            125,
            10,
            10,
            it -> {
                List<LevelLike> levels = this.levelCache.computeIfAbsent(it, this::buildLevels);
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
                int count = this.levelCache.computeIfAbsent(it, this::buildLevels).size();
                if (count == 0) return;
                this.stepMap.merge(it, 1, (old, add) -> Math.floorMod(old + add, count));
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            149,
            33,
            10,
            it -> {
                int count = this.levelCache.computeIfAbsent(it, this::buildLevels).size();
                if (count == 0) return;
                this.stepMap.merge(it, -1, (old, add) -> Math.floorMod(old + add, count));
            },
            recipe
        ));
    }

    private @Nullable LevelLike currentLevel(RecipeHolder<Multiblock4DRecipe> recipe) {
        List<LevelLike> levels = this.levelCache.computeIfAbsent(recipe, this::buildLevels);
        int count = levels.size();
        if (count == 0) return null;
        return levels.get(Math.floorMod(this.stepMap.getOrDefault(recipe, 0), count));
    }

    private List<LevelLike> buildLevels(RecipeHolder<Multiblock4DRecipe> recipe) {
        return recipe.value().getDefinitions().stream()
            .map(LevelLikeDisplaySupport::asLevelLike)
            .toList();
    }

    private int maxSize(RecipeHolder<Multiblock4DRecipe> recipe) {
        return recipe.value().getDefinitions().stream()
            .mapToInt(MultiblockUtil::size)
            .max()
            .orElse(0);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MULTIBLOCK_4D,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIBLOCK_4D.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_4D, ModBlocks.GIANT_ANVIL.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_4D, ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_4D, Items.CRAFTING_TABLE.getDefaultInstance());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_4D, ModBlocks.SPACETIME_SUPERCOMPUTER.asStack());
    }
}
