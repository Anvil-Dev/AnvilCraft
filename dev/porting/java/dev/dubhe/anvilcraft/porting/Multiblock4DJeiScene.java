package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

@JeiPlugin
public final class Multiblock4DJeiScene implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;
    private static IRecipeCategory<RecipeHolder<Multiblock4DRecipe>> category;
    private static List<RecipeHolder<Multiblock4DRecipe>> recipes;
    private static IRecipeLayoutDrawable<RecipeHolder<Multiblock4DRecipe>> layout;
    private static RecipeHolder<Multiblock4DRecipe> recipe;
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    @Override
    public Identifier getPluginUid() {
        return AnvilCraft.of("port_4d_jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("4D JEI stage " + stage);
        if (runtime == null || client.getOverlay() != null || capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                initialize(client);
                advance();
            }
            case 1 -> capture(client, "step1");
            case 2 -> {
                click(140, 36);
                checkStep(1);
                advance();
            }
            case 3 -> capture(client, "step2");
            case 4 -> {
                click(140, 36);
                checkStep(2);
                advance();
            }
            case 5 -> capture(client, "step3");
            case 6 -> {
                click(140, 36);
                checkStep(0);
                click(152, 36);
                checkStep(2);
                click(128, 13);
                if (levels().stream().anyMatch(LevelLike::isAllLayersVisible)) throw new IllegalStateException("Layer mode not shared");
                click(140, 13);
                checkLayer(2, 1);
                advance();
            }
            case 7 -> capture(client, "layer2");
            case 8 -> {
                click(152, 36);
                checkStep(1);
                checkLayer(1, 0);
                click(152, 13);
                checkLayer(1, 2);
                advance();
            }
            case 9 -> capture(client, "previous-layer");
            case 10 -> {
                click(128, 13);
                if (levels().stream().anyMatch(level -> !level.isAllLayersVisible())) {
                    throw new IllegalStateException("All layers not restored");
                }
                var hypercube = find("4d_multiblock/hypercube");
                runtime.getRecipesGui().showRecipes(category, List.of(hypercube), List.of());
                advance();
            }
            case 11 -> capture(client, "browser");
            case 12 -> {
                AnvilCraft.LOGGER.info("PORT_4D_JEI_PASSED: recipes, catalysts, input/output lookup, aggregate item/tag counts, "
                    + "step wrapping, shared layer mode, per-step layers and six live layouts");
                stage++;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void initialize(Minecraft client) {
        var manager = runtime.getRecipeManager();
        category = manager.getRecipeCategory(AnvilCraftJeiPlugin.MULTIBLOCK_4D);
        recipes = manager.createRecipeLookup(AnvilCraftJeiPlugin.MULTIBLOCK_4D).get().toList();
        if (recipes.size() < 3) throw new IllegalStateException("4D recipes not registered");
        var stations = manager.createCraftingStationLookup(AnvilCraftJeiPlugin.MULTIBLOCK_4D).getItemStack().toList();
        for (var item : List.of(ModBlocks.GIANT_ANVIL.asItem(), ModBlocks.TRANSPARENT_CRAFTING_TABLE.asItem(),
            Items.CRAFTING_TABLE, ModBlocks.SPACETIME_SUPERCOMPUTER.asItem())) {
            if (stations.stream().noneMatch(stack -> stack.is(item))) throw new IllegalStateException("Missing catalyst " + item);
        }
        for (var role : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.OUTPUT)) {
            var stack = role == RecipeIngredientRole.INPUT ? ModBlocks.TEMPERING_GLASS.asStack() : ModBlocks.HYPERCUBE.asStack();
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack);
            if (manager.createRecipeLookup(AnvilCraftJeiPlugin.MULTIBLOCK_4D).limitFocus(List.of(focus)).get().count() != 1) {
                throw new IllegalStateException("Missing hypercube lookup " + role);
            }
        }
        checkCount(create(find("4d_multiblock/hypercube")), ModBlocks.TEMPERING_GLASS.asItem(), 81);
        var tagged = create(find("port_4d/jei_tags"));
        var slots = tagged.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT);
        if (slots.size() != 2 || slots.stream().noneMatch(slot -> slot.getItemStacks().allMatch(stack -> stack.getCount() == 54)
            && slot.getItemStacks().count() > 1)) {
            throw new IllegalStateException("Aggregated tag count or choices");
        }
        recipe = find("port_4d/progress");
        layout = create(recipe);
        for (var item : List.of(Items.DIRT, Items.COBBLESTONE, Items.OAK_PLANKS)) checkCount(layout, item, 27);
        for (var level : levels()) {
            if (level.verticalSize() != 3 || level.horizontalSize() != 3) throw new IllegalStateException("Preview dimensions");
            MultiblockPreviewProbe.verify(level);
        }
        freezeAnimation();
        client.options.guiScale().set(2);
        client.resizeGui();
        client.setScreen(new Preview());
    }

    private static RecipeHolder<Multiblock4DRecipe> find(String path) {
        return recipes.stream().filter(value -> value.id().identifier().getPath().equals(path)).findFirst().orElseThrow();
    }

    private static IRecipeLayoutDrawable<RecipeHolder<Multiblock4DRecipe>> create(RecipeHolder<Multiblock4DRecipe> holder) {
        return runtime.getRecipeManager().createRecipeLayoutDrawable(category, holder,
            runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
    }

    private static void checkCount(IRecipeLayoutDrawable<?> value, Item item, int count) {
        var matches = value.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT).stream()
            .flatMap(slot -> slot.getItemStacks()).filter(stack -> stack.is(item)).toList();
        if (matches.size() != 1 || matches.getFirst().getCount() != count) throw new IllegalStateException("Material count " + item);
    }

    private static void click(int x, int y) {
        var input = (IJeiUserInput) Proxy.newProxyInstance(IJeiUserInput.class.getClassLoader(),
            new Class<?>[]{IJeiUserInput.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getKey" -> InputConstants.Type.MOUSE.getOrCreate(0);
                case "isSimulate" -> true;
                case "getModifiers" -> 0;
                default -> false;
            });
        var area = layout.getRect();
        if (!layout.getInputHandler().handleInput(area.getX() + x, area.getY() + y, input)) {
            throw new IllegalStateException("JEI button did not receive click " + x + "," + y);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name) {
        try {
            var field = category.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(category);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<LevelLike> levels() {
        return Multiblock4DJeiScene.<Map<RecipeHolder<Multiblock4DRecipe>, List<LevelLike>>>field("levelCache").get(recipe);
    }

    private static void checkStep(int expected) {
        int actual = Multiblock4DJeiScene.<Map<RecipeHolder<Multiblock4DRecipe>, Integer>>field("stepMap").getOrDefault(recipe, 0);
        if (Math.floorMod(actual, levels().size()) != expected) throw new IllegalStateException("Time step " + actual);
    }

    private static void checkLayer(int step, int expected) {
        if (levels().get(step).getCurrentVisibleLayer() != expected) throw new IllegalStateException("Visible layer " + step);
    }

    private static void freezeAnimation() {
        try {
            var field = category.getClass().getDeclaredField("timer");
            field.setAccessible(true);
            field.set(category, new ITickTimer() {
                @Override
                public int getValue() {
                    return 0;
                }

                @Override
                public int getMaxValue() {
                    return 60;
                }
            });
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void advance() {
        stage++;
        next = System.currentTimeMillis() + 1500;
    }

    private static void capture(Minecraft client, String name) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "4d-jei-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_4D_JEI_CAPTURED: {}", name);
                capturing = false;
                advance();
            }));
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("4D Multiblock"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            Minecraft.getInstance().level.getLevelData().setGameTime(0);
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            layout.setPosition((this.width - 162) / 2, (this.height - 136) / 2);
            layout.drawRecipe(graphics, -1, -1);
        }
    }
}
