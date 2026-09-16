package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.common.Internal;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public final class StorageJeiScene {
    private static boolean started;
    private static boolean executing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static StorageScreen screen;
    private static RecipeHolder<StonecutterRecipe> stoneRecipe;
    private static int stage;
    private static long nextAction;
    private static boolean capturing;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (executing) return;
        executing = true;
        try {
            runFrame(client, corePos);
        } finally {
            executing = false;
        }
    }

    private static void runFrame(Minecraft client, BlockPos corePos) {
        if (!started) {
            started = true;
            SettingClientStub.updateFlipped(false);
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    var storage = Storages.get().get(core.getId()).orElseThrow();
                    storage.setCrafting(CraftingStorage.EMPTY);
                    var items = storage.getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        for (int i = 0; i < items.size(); i++) {
                            if (!items.getResource(i).isEmpty()) items.extract(i, items.getResource(i), Integer.MAX_VALUE, transaction);
                        }
                        items.insert(ItemResource.of(Items.STONE), 70, transaction);
                        items.insert(ItemResource.of(Items.BIRCH_PLANKS), 3, transaction);
                        transaction.commit();
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS));
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("JEI 仓储界面验证失败", failure);
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        var runtime = Internal.getJeiRuntime();
        var manager = runtime.getRecipeManager();
        switch (stage) {
            case 0 -> {
                if (!client.player.getInventory().getItem(9).is(Items.OAK_PLANKS)) return;
                screen = new StorageScreen(corePos);
                client.setScreen(screen);
                advance(1);
            }
            case 1 -> {
                if (!screen.canTransferRecipe()) return;
                var recipe = manager.createRecipeLookup(RecipeTypes.CRAFTING).get()
                    .filter(holder -> holder.id().identifier().toString().equals("minecraft:crafting_table")).findFirst().orElseThrow();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.CRAFTING), List.of(recipe), List.of());
                advance(2);
            }
            case 2 -> {
                require(client.screen instanceof RecipesGui gui && gui.getParentContainerMenu() == screen.getMenu(), "JEI 父容器错误");
                require(buttonActive(client), "背包与仓储的混合木板必须启用转移按钮");
                capture(client, "ready", 3);
            }
            case 3 -> {
                clickTransfer(client);
                advance(4);
            }
            case 4 -> {
                if (client.screen != screen || !screen.canTransferRecipe()) return;
                var crafting = (CraftingStorage) field(screen, "crafting");
                int oak = crafting.craftingInput().stream().filter(stack -> stack.is(Items.OAK_PLANKS)).mapToInt(ItemStack::getCount).sum();
                int birch = crafting.craftingInput().stream().filter(stack -> stack.is(Items.BIRCH_PLANKS))
                    .mapToInt(ItemStack::getCount).sum();
                require(oak == 1 && birch == 3, "标签配方未按实际余量混合分配材料");
                require(((ItemStack) field(screen, "craftingResult")).is(Items.CRAFTING_TABLE), "JEI 合成格位置错误，未形成工作台配方");
                capture(client, "grid", 5);
            }
            case 5 -> {
                var recipe = manager.createRecipeLookup(RecipeTypes.CRAFTING).get()
                    .filter(holder -> holder.id().identifier().toString().equals("minecraft:diamond_block")).findFirst().orElseThrow();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.CRAFTING), List.of(recipe), List.of());
                advance(6);
            }
            case 6 -> {
                require(!buttonActive(client) && transferButton(client).isVisible(), "缺料按钮应可见但禁用");
                capture(client, "missing", 7);
            }
            case 7 -> {
                var previous = client.screen;
                clickTransfer(client);
                require(client.screen == previous, "禁用按钮不能转移或关闭页面");
                client.screen.onClose();
                advance(8);
            }
            case 8 -> {
                require(client.screen == screen, "JEI 返回原仓储屏幕失败");
                click(screen.getLeftPos() + 285, screen.getTopPos() + 6, client);
                advance(9);
            }
            case 9 -> {
                click(screen.getLeftPos() + 225, screen.getTopPos() + 139, client);
                require(client.screen instanceof RecipesGui, "翻转后的切石配方入口失效");
                stoneRecipe = manager.createRecipeLookup(RecipeTypes.STONECUTTING).get().filter(holder ->
                    holder.value().matches(new SingleRecipeInput(new ItemStack(Items.STONE)), client.level)
                        && holder.value().assemble(new SingleRecipeInput(new ItemStack(Items.STONE))).is(Items.STONE_SLAB))
                    .findFirst().orElseThrow();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.STONECUTTING), List.of(stoneRecipe), List.of());
                advance(10);
            }
            case 10 -> {
                require(buttonActive(client), "仓储石头应启用切石转移按钮");
                clickTransfer(client);
                advance(11);
            }
            case 11 -> {
                if (client.screen != screen || !screen.canTransferRecipe()) return;
                var crafting = (CraftingStorage) field(screen, "crafting");
                require(crafting.stonecutterInput().getCount() == 1, "普通切石转移应只填一个");
                var result = screen.getItemUnderMouse(screen.getLeftPos() + 285, screen.getTopPos() + 170);
                require(result != null && result.is(Items.STONE_SLAB), "切石配方选择或 JEI 产物悬停不正确");
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.STONECUTTING), List.of(stoneRecipe), List.of());
                advance(12);
            }
            case 12 -> {
                var category = manager.getRecipeCategory(RecipeTypes.STONECUTTING);
                var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(screen.getMenu(), category).orElseThrow();
                IRecipeLayoutDrawable<?> layout = layout(client).getRecipeLayout();
                require(handler.transferRecipe(screen.getMenu(), stoneRecipe, layout.getRecipeSlotsView(),
                    client.player, true, true) == null,
                    "已注册切石 handler 必须支持最大转移");
                client.screen.onClose();
                advance(13);
            }
            case 13 -> {
                if (client.screen != screen || !screen.canTransferRecipe()) return;
                var crafting = (CraftingStorage) field(screen, "crafting");
                require(crafting.stonecutterInput().getCount() == 64, "最大转移未填满切石输入");
                require(screen.getTransferMaterials().get(ItemResource.of(Items.OAK_PLANKS)) == 1
                    && screen.getTransferMaterials().get(ItemResource.of(Items.BIRCH_PLANKS)) == 3, "旧合成材料未完整归还");
                capture(client, "stonecutter", 14);
            }
            case 14 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_JEI_SCENE_PASSED: actual buttons, variants, missing inputs, flip, stonecutter, max");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown JEI stage " + stage);
        }
    }

    private static IRecipeLayoutWithButtons<?> layout(Minecraft client) {
        var layouts = (List<?>) field(field(client.screen, "layouts"), "recipeLayoutsWithButtons");
        return (IRecipeLayoutWithButtons<?>) layouts.getFirst();
    }

    private static IconButton transferButton(Minecraft client) {
        return (IconButton) ((List<?>) field(layout(client), "buttons")).getFirst();
    }

    private static boolean buttonActive(Minecraft client) {
        var button = transferButton(client);
        return button.isVisible() && ((AbstractWidget) field(button, "button")).active;
    }

    private static void clickTransfer(Minecraft client) {
        var area = transferButton(client).getArea();
        click(area.getX() + area.getWidth() / 2, area.getY() + area.getHeight() / 2, client);
    }

    private static Object field(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void click(int x, int y, Minecraft client) {
        var current = client.screen;
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        current.mouseClicked(event, false);
        current.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_STORAGE_JEI_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-jei-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
