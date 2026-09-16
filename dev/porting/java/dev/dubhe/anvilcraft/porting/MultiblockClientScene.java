package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.LevelLikeDisplaySupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;

@JeiPlugin
public final class MultiblockClientScene implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;
    private static boolean initialized;
    private static boolean capturing;
    private static int stage;
    private static int frames;

    @Override
    public Identifier getPluginUid() {
        return AnvilCraft.of("port_multiblock_validation");
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
        if (runtime == null || RecipesRecord.CLIENTSIDE == null || client.getOverlay() != null || capturing) return;
        if (!initialized) {
            initialized = true;
            client.options.guiScale().set(2);
            client.resizeGui();
            var recipe = RecipesRecord.CLIENTSIDE.byType(ModRecipeTypes.MULTIBLOCK.get()).stream()
                .filter(holder -> holder.id().identifier().getPath().equals("multiblock/large_fluid_tank"))
                .findFirst().orElseThrow();
            var ingredients = LevelLikeDisplaySupport.tagIngredients(recipe.value().getPattern());
            if (ingredients.size() != 1 || ingredients.getFirst().size() < 2
                || ingredients.getFirst().stream().anyMatch(stack -> stack.getCount() != 4)) {
                throw new IllegalStateException("标签材料候选或数量未同步到客户端");
            }
            var preview = LevelLikeDisplaySupport.asLevelLike(recipe.value().getPattern());
            LevelLikeDisplaySupport.cycleTags(preview, 0);
            var first = preview.getBlockState(new BlockPos(-1, 0, 0));
            LevelLikeDisplaySupport.cycleTags(preview, 1);
            if (first == preview.getBlockState(new BlockPos(-1, 0, 0))) throw new IllegalStateException("标签方块预览未切换变体");
            var category = runtime.getRecipeManager().getRecipeCategory(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
            runtime.getRecipesGui().showRecipes(category, List.of(recipe), List.of());
        }
        if (stage >= 2) {
            AnvilCraft.LOGGER.info("PORT_MULTIBLOCK_CLIENT_PASSED: recipe sync, tag quantities, tag variants, native JEI categories");
            client.stop();
            return;
        }
        if (client.screen == null || !client.screen.getClass().getName().contains("RecipesGui")) {
            throw new IllegalStateException("JEI 配方界面未打开");
        }
        if (++frames < 160) return;
        capturing = true;
        Screenshot.grab(client.gameDirectory, "multiblock-definition-26.1-" + stage + ".png", client.getMainRenderTarget(), 1, message -> {
            client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_MULTIBLOCK_CLIENT_CAPTURED: {}", message.getString());
                stage++;
                frames = 0;
                capturing = false;
                if (stage == 1) {
                    var recipe = RecipesRecord.CLIENTSIDE.byType(ModRecipeTypes.MULTIBLOCK_CONVERSION.get()).stream()
                        .filter(holder -> holder.id().identifier().getPath().equals("multiblock_conversion/large_fluid_tank"))
                        .findFirst().orElseThrow();
                    var category = runtime.getRecipeManager().getRecipeCategory(AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
                    runtime.getRecipesGui().showRecipes(category, List.of(recipe), List.of());
                }
            });
        });
    }
}
