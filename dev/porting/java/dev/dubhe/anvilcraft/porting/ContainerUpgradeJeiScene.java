package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.ContainerUpgradeRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ITickTimer;
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
import org.jspecify.annotations.Nullable;

import java.util.List;

@JeiPlugin
public final class ContainerUpgradeJeiScene implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;
    private static IRecipeCategory<ContainerUpgradeRecipe> category;
    private static List<ContainerUpgradeRecipe> recipes;
    private static int stage;
    private static long next;
    private static boolean capturing;
    private static int animation;
    private static boolean hover;
    private static long deadline;

    @Override
    public Identifier getPluginUid() {
        return AnvilCraft.of("port_container_upgrade_jei");
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
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Container JEI stage " + stage);
        if (runtime == null || client.getOverlay() != null || capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                var manager = runtime.getRecipeManager();
                category = manager.getRecipeCategory(AnvilCraftJeiPlugin.CONTAINER_UPGRADE);
                recipes = manager.createRecipeLookup(AnvilCraftJeiPlugin.CONTAINER_UPGRADE).get().toList();
                if (recipes.size() != 2) throw new IllegalStateException("Both container recipes must be registered");
                for (var output : List.of(ModBlocks.SHULKER_CONTAINER.asStack(), ModBlocks.HYPERDIMENSION_STORAGE_STATION.asStack())) {
                    var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(
                        RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, output);
                    if (manager.createRecipeLookup(AnvilCraftJeiPlugin.CONTAINER_UPGRADE).limitFocus(List.of(focus)).get().count() != 1) {
                        throw new IllegalStateException("Output lookup " + output);
                    }
                }
                var stations = manager.createCraftingStationLookup(AnvilCraftJeiPlugin.CONTAINER_UPGRADE).getItemStack().toList();
                for (var item : List.of(Items.ANVIL, ModBlocks.LARGE_CRATE.asItem(), ModBlocks.HYPERCUBE.asItem())) {
                    if (stations.stream().noneMatch(stack -> stack.is(item))) throw new IllegalStateException("Missing catalyst " + item);
                }
                freezeTimer();
                client.options.guiScale().set(2);
                client.resizeGui();
                show(client, ContainerUpgradeRecipe.Type.CRATE_TO_CONTAINER);
                advance(1);
            }
            case 1 -> capture(client, "crate", 2);
            case 2 -> {
                show(client, ContainerUpgradeRecipe.Type.CONTAINER_TO_STATION);
                advance(3);
            }
            case 3 -> capture(client, "station", 4);
            case 4 -> {
                animation = 15;
                advance(5);
            }
            case 5 -> capture(client, "raised", 6);
            case 6 -> {
                hover = true;
                advance(10);
            }
            case 10 -> capture(client, "hint", 11);
            case 11 -> {
                advance(7);
                runtime.getRecipesGui().showRecipes(category, recipes, List.of());
            }
            case 7 -> capture(client, "browser", 8);
            case 8 -> {
                AnvilCraft.LOGGER.info("PORT_CONTAINER_UPGRADE_JEI_PASSED: "
                    + "recipes, output lookup, catalysts, material counts and live layouts");
                stage = 9;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void freezeTimer() {
        try {
            var field = category.getClass().getDeclaredField("timer");
            field.setAccessible(true);
            field.set(category, new ITickTimer() {
                @Override
                public int getValue() {
                    return animation;
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

    private static void show(Minecraft client, ContainerUpgradeRecipe.Type type) {
        var recipe = recipes.stream().filter(value -> value.type() == type).findFirst().orElseThrow();
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
            runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
        if (type == ContainerUpgradeRecipe.Type.CRATE_TO_CONTAINER) {
            checkCount(layout, Items.NETHERITE_BLOCK, 6);
            checkCount(layout, ModBlocks.SPACE_OVERCOMPRESSOR.asItem(), 1);
            checkCount(layout, ModBlocks.LARGE_CRATE.asItem(), 1);
        } else {
            checkCount(layout, ModBlocks.SINGULARITY_CRYSTAL.asItem(), 1);
            checkCount(layout, ModBlocks.HYPERCUBE.asItem(), 16);
            checkCount(layout, ModBlocks.SPACE_OVERCOMPRESSOR.asItem(), 4);
            checkCount(layout, ModBlocks.SHULKER_CONTAINER.asItem(), 1);
        }
        client.setScreen(new Preview(layout));
    }

    private static void checkCount(IRecipeLayoutDrawable<?> layout, Item item, int count) {
        var input = layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT).stream()
            .flatMap(slot -> slot.getItemStacks()).filter(stack -> stack.is(item)).toList();
        if (input.size() != 1 || input.getFirst().getCount() != count) throw new IllegalStateException("Input count " + item);
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2000;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "container-jei-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static final class Preview extends Screen {
        private final IRecipeLayoutDrawable<ContainerUpgradeRecipe> layout;

        private Preview(IRecipeLayoutDrawable<ContainerUpgradeRecipe> layout) {
            super(Component.literal("Container Upgrade"));
            this.layout = layout;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            this.layout.setPosition((this.width - 162) / 2, (this.height - 64) / 2);
            this.layout.drawRecipe(graphics, -1, -1);
            if (hover) {
                int x = (this.width - 162) / 2 + 39;
                int y = (this.height - 64) / 2 + 49;
                var stack = this.layout.getItemStackUnderMouse(x, y).orElseThrow();
                if (!stack.is(ModBlocks.SPACE_OVERCOMPRESSOR.asItem()) || stack.getCount() != 4) {
                    throw new IllegalStateException("Expansion hint slot");
                }
                this.layout.drawOverlays(graphics, x, y);
            }
        }
    }
}
