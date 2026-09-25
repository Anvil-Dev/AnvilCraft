package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.common.Internal;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import mezz.jei.gui.recipes.RecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class TerminalJeiScene {
    private static final BlockPos TABLE = new BlockPos(30, 81, 3);
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static BaseStorage<?> storage;
    private static Screen parent;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;
    private static boolean missingHovered;
    private static boolean cacheCleared;
    private static CompletableFuture<?> snapshot;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 150000;
        }
        if (failure != null) throw new IllegalStateException("终端 JEI 验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端 JEI 超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        var runtime = Internal.getJeiRuntime();
        var manager = runtime.getRecipeManager();
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 30.5 82 2.5");
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                        var terminal = new ItemStack(ModItems.SHULKER_TERMINAL.get());
                        player.getInventory().setItem(0, terminal);
                        player.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS));
                        storage = TerminalSessions.targetStorage(player, terminal, false);
                        require(storage != null, "缺少世界潜影目标");
                        var items = storage.getItems();
                        try (Transaction transaction = Transaction.openRoot()) {
                            for (int slot = 0; slot < items.size(); slot++) {
                                if (!items.getResource(slot).isEmpty()) {
                                    items.extract(slot, items.getResource(slot), Integer.MAX_VALUE, transaction);
                                }
                            }
                            items.insert(ItemResource.of(Items.BIRCH_PLANKS), 3, transaction);
                            items.insert(ItemResource.of(Items.STONE), 70, transaction);
                            items.insert(ItemResource.of(Items.DIAMOND), 8, transaction);
                            items.insert(ItemResource.of(Items.SAND), 4, transaction);
                            items.insert(ItemResource.of(Items.BUCKET), 2, transaction);
                            transaction.commit();
                        }
                        var pos = new BlockPos(28, 81, 0);
                        server.overworld().setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.getDefaultState(), Block.UPDATE_ALL);
                        var port = (StorageFluidPortBlockEntity) server.overworld().getBlockEntity(pos);
                        port.getTank().set(0, FluidResource.of(Fluids.WATER), 1000);
                        port.tickServer();
                        openCrafting(client);
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !(client.player.containerMenu instanceof CraftingMenu)) return;
                parent = client.screen;
                showCrafting("minecraft:crafting_table");
                advance(2);
            }
            case 2 -> {
                if (TerminalJeiStorageCache.get(TerminalJeiStorageCache.boundStorages(client.player)) == null) return;
                require(client.screen instanceof RecipesGui, "应打开实际 JEI 配方页面");
                require(active(client), "背包一块橡木加仓储三块白桦木应满足工作台配方");
                capture(client, "ready", 3);
            }
            case 3 -> {
                clickTransfer(client);
                advance(4);
            }
            case 4 -> {
                if (TerminalJeiStorageCache.isBusy() || !client.player.containerMenu.getSlot(0).getItem().is(Items.CRAFTING_TABLE)) return;
                require(client.screen == parent, "填充后应返回原工作台界面");
                int oak = 0;
                int birch = 0;
                for (int slot = 1; slot <= 9; slot++) {
                    var stack = client.player.containerMenu.getSlot(slot).getItem();
                    if (stack.is(Items.OAK_PLANKS)) oak += stack.getCount();
                    if (stack.is(Items.BIRCH_PLANKS)) birch += stack.getCount();
                }
                require(oak == 1 && birch == 3, "原版合成格未按标签变体的真实数量填充");
                capture(client, "grid", 5);
            }
            case 5 -> {
                snapshot = TerminalJeiStorageCache.ensure(TerminalJeiStorageCache.boundStorages(client.player));
                advance(6);
            }
            case 6 -> {
                if (!snapshot.isDone()) return;
                snapshot.join();
                showCrafting("minecraft:diamond_block");
                advance(7);
            }
            case 7 -> {
                require(!active(client) && button(client).isVisible(), "八颗钻石不能被九个输入槽重复使用");
                var recipe = manager.createRecipeLookup(RecipeTypes.CRAFTING).get()
                    .filter(holder -> holder.id().identifier().toString().equals("minecraft:diamond_block")).findFirst().orElseThrow();
                var handler = runtime.getRecipeTransferManager()
                    .getRecipeTransferHandler(client.player.containerMenu, manager.getRecipeCategory(RecipeTypes.CRAFTING)).orElseThrow();
                var error = handler.transferRecipe(client.player.containerMenu, recipe,
                    layout(client).getRecipeLayout().getRecipeSlotsView(),
                    client.player, false, false);
                require(error != null && error.getMissingCountHint() == 1, "只能高亮真正缺少的一个输入槽");
                if (!missingHovered) {
                    var area = button(client).getArea();
                    moveMouse(client, area.getX() + area.getWidth() / 2, area.getY() + area.getHeight() / 2);
                    missingHovered = true;
                    nextAction = System.currentTimeMillis() + 300;
                    return;
                }
                capture(client, "missing", 8);
            }
            case 8 -> {
                client.screen.onClose();
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    server.overworld().setBlock(TABLE.east(), Blocks.FURNACE.defaultBlockState(), Block.UPDATE_ALL);
                    player.openMenu((MenuProvider) server.overworld().getBlockEntity(TABLE.east()));
                });
                advance(9);
            }
            case 9 -> {
                if (!(client.player.containerMenu instanceof FurnaceMenu)) return;
                var recipe = manager.createRecipeLookup(RecipeTypes.SMELTING).get().filter(holder ->
                    holder.value().matches(new SingleRecipeInput(new ItemStack(Items.STONE)), client.level)).findFirst().orElseThrow();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.SMELTING), List.of(recipe), List.of());
                advance(10);
            }
            case 10 -> {
                require(active(client), "熔炉界面应能使用终端库存");
                var category = manager.getRecipeCategory(RecipeTypes.SMELTING);
                var recipe = manager.createRecipeLookup(RecipeTypes.SMELTING).get().filter(holder ->
                    holder.value().matches(new SingleRecipeInput(new ItemStack(Items.STONE)), client.level)).findFirst().orElseThrow();
                var handler = runtime.getRecipeTransferManager()
                    .getRecipeTransferHandler(client.player.containerMenu, category).orElseThrow();
                require(handler.transferRecipe(client.player.containerMenu, recipe, layout(client).getRecipeLayout().getRecipeSlotsView(),
                    client.player, true, true) == null, "最大填充请求未接通");
                client.screen.onClose();
                advance(11);
            }
            case 11 -> {
                if (TerminalJeiStorageCache.isBusy() || client.player.containerMenu.getSlot(0).getItem().getCount() != 64) return;
                capture(client, "maximum", 12);
            }
            case 12 -> {
                client.getSingleplayerServer().execute(() -> openCrafting(client));
                advance(13);
            }
            case 13 -> {
                if (!(client.player.containerMenu instanceof CraftingMenu)) return;
                showCrafting("anvilcraft:port_terminal_water");
                advance(14);
            }
            case 14 -> {
                require(active(client), "空桶和端口水应使流体容器配方可填充");
                clickTransfer(client);
                advance(15);
            }
            case 15 -> {
                if (TerminalJeiStorageCache.isBusy() || !client.player.containerMenu.getSlot(0).getItem().is(Items.CLAY_BALL)) return;
                require(client.player.containerMenu.getSlot(1).getItem().is(Items.WATER_BUCKET), "实际工作台未收到水桶");
                capture(client, "fluid", 16);
            }
            case 16 -> {
                var targets = TerminalJeiStorageCache.boundStorages(client.player);
                var first = TerminalJeiStorageCache.ensure(targets);
                require(first == TerminalJeiStorageCache.ensure(targets), "同目标查询应合并在途请求");
                long oldEpoch = TerminalJeiStorageCache.begin();
                TerminalJeiStorageCache.clear();
                require(!TerminalJeiStorageCache.isBusy() && !TerminalJeiStorageCache.isCurrent(oldEpoch), "清理应重置补库状态");
                first.whenCompleteAsync((result, error) -> {
                    try {
                        require(error == null && TerminalJeiStorageCache.get(targets) == null, "清理前的迟到响应不能恢复缓存");
                        cacheCleared = true;
                    } catch (Throwable problem) {
                        failure = problem;
                    }
                }, client);
                advance(17);
            }
            case 17 -> {
                if (!cacheCleared) return;
                AnvilCraft.LOGGER.info(
                    "PORT_TERMINAL_JEI_PASSED: native button, variants, missing hint, furnace max, fluid, cache lifecycle");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown terminal JEI stage " + stage);
        }
    }

    private static void openCrafting(Minecraft client) {
        var server = client.getSingleplayerServer();
        var player = server.getPlayerList().getPlayers().getFirst();
        server.overworld().setBlock(TABLE, Blocks.CRAFTING_TABLE.defaultBlockState(), Block.UPDATE_ALL);
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
            new CraftingMenu(id, inventory, ContainerLevelAccess.create(server.overworld(), TABLE)),
            Component.literal("Terminal crafting check")));
    }

    private static void showCrafting(String id) {
        var runtime = Internal.getJeiRuntime();
        var manager = runtime.getRecipeManager();
        var recipe = manager.createRecipeLookup(RecipeTypes.CRAFTING).get()
            .filter(holder -> holder.id().identifier().toString().equals(id)).findFirst().orElseThrow();
        runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.CRAFTING), List.of(recipe), List.of());
    }

    private static IRecipeLayoutWithButtons<?> layout(Minecraft client) {
        var layouts = (List<?>) field(field(client.screen, "layouts"), "recipeLayoutsWithButtons");
        return (IRecipeLayoutWithButtons<?>) layouts.getFirst();
    }

    private static IconButton button(Minecraft client) {
        return (IconButton) ((List<?>) field(layout(client), "buttons")).getFirst();
    }

    private static boolean active(Minecraft client) {
        return button(client).isVisible() && ((AbstractWidget) field(button(client), "button")).active;
    }

    private static void clickTransfer(Minecraft client) {
        var area = button(client).getArea();
        var screen = client.screen;
        var event = new MouseButtonEvent(area.getX() + area.getWidth() / 2, area.getY() + area.getHeight() / 2, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
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

    private static void moveMouse(Minecraft client, int x, int y) {
        try {
            var window = client.getWindow();
            var mx = MouseHandler.class.getDeclaredField("xpos");
            var my = MouseHandler.class.getDeclaredField("ypos");
            mx.setAccessible(true);
            my.setAccessible(true);
            mx.setDouble(client.mouseHandler, (double) x * window.getScreenWidth() / window.getGuiScaledWidth());
            my.setDouble(client.mouseHandler, (double) y * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_JEI_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-jei-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
