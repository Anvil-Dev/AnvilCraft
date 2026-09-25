package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.common.Internal;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class StorageUnfilteredScene {
    private static final BlockPos CORE = new BlockPos(20, 81, 0);
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static BaseStorage<?> storage;
    private static StorageScreen screen;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;
    private static boolean versionChecked;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 150000;
        }
        if (failure != null) throw new IllegalStateException("未过滤快照客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("未过滤快照客户端超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                        player.inventoryMenu.setCarried(ItemStack.EMPTY);
                        player.inventoryMenu.broadcastChanges();
                        var core = (StorageBlockEntity) server.overworld().getBlockEntity(CORE);
                        storage = Storages.get().get(core.getId()).orElseThrow();
                        storage.setCrafting(CraftingStorage.EMPTY);
                        var items = storage.getItems();
                        try (Transaction transaction = Transaction.openRoot()) {
                            for (int slot = 0; slot < items.size(); slot++) {
                                if (!items.getResource(slot).isEmpty()) {
                                    items.extract(slot, items.getResource(slot), Integer.MAX_VALUE, transaction);
                                }
                            }
                            for (int i = 0; i < 300; i++) {
                                var stack = new ItemStack(Items.STONE);
                                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Unfiltered " + i));
                                items.insert(ItemResource.of(stack), 1, transaction);
                            }
                            items.insert(ItemResource.of(Items.DIAMOND), 8, transaction);
                            items.insert(ItemResource.of(Items.CRAFTING_TABLE), 1, transaction);
                            transaction.commit();
                        }
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                SettingClientStub.update(SearchMode.RETENTION);
                advance(1);
            }
            case 1 -> {
                if (!prepared || client.player.hasInfiniteMaterials()) return;
                screen = new StorageScreen(CORE);
                client.setScreen(screen);
                search().setValue("@missing_namespace");
                advance(2);
            }
            case 2 -> {
                if (!screen.canTransferRecipe() || !screen.hasUnfilteredContents()) return;
                require(display().isEmpty(), "页面搜索必须隐藏库存");
                require(screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) == 8,
                    "未过滤材料池必须包含第二页中的钻石");
                capture(client, "filtered", 3);
            }
            case 3 -> {
                showRecipe();
                advance(4);
            }
            case 4 -> {
                require(!active(client), "八颗钻石不足时不能放行九格配方");
                client.getSingleplayerServer().execute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        storage.getItems().insert(ItemResource.of(Items.DIAMOND), 1, transaction);
                        transaction.commit();
                    }
                });
                advance(5);
            }
            case 5 -> {
                if (!active(client)) return;
                require(screen.hasUnfilteredContents()
                    && screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) == 9,
                    "JEI 前台期间未刷新材料版本");
                require(search().getValue().equals("@missing_namespace") && display().isEmpty(), "刷新不能修改页面搜索和显示排序");
                capture(client, "live", 6);
            }
            case 6 -> {
                clickTransfer(client);
                advance(7);
            }
            case 7 -> {
                if (client.screen != screen || !screen.canTransferRecipe()
                    || !((ItemStack) field(screen, "craftingResult")).is(Items.DIAMOND_BLOCK)) return;
                require(search().getValue().equals("@missing_namespace") && display().isEmpty(), "完成转移不能清除搜索条件");
                clickWidget(client, (AbstractWidget) field(screen, "craftingClear"));
                advance(8);
            }
            case 8 -> {
                var crafting = (CraftingStorage) field(screen, "crafting");
                if (!screen.canTransferRecipe() || crafting.craftingInput().stream().anyMatch(stack -> !stack.isEmpty())) return;
                if (screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) != 9) return;
                search().setValue("");
                clickCategory(client);
                advance(9);
            }
            case 9 -> {
                require(SettingClientStub.listed().getFirst().getMode() == CategoryMode.ALLOWLIST, "第一次分类点击应设为允许列表");
                clickCategory(client);
                advance(10);
            }
            case 10 -> {
                if (!screen.canTransferRecipe() || !display().isEmpty()) return;
                require(SettingClientStub.listed().getFirst().getMode() == CategoryMode.BLOCKLIST && search().getValue().isEmpty(),
                    "应单独通过分类隐藏材料");
                require(screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) == 9,
                    "分类筛选不能从材料池移除钻石");
                showRecipe();
                advance(11);
            }
            case 11 -> {
                require(active(client), "分类隐藏材料后 JEI 应仍可转移");
                clickTransfer(client);
                advance(12);
            }
            case 12 -> {
                if (client.screen != screen || !screen.canTransferRecipe()
                    || !((ItemStack) field(screen, "craftingResult")).is(Items.DIAMOND_BLOCK)) return;
                require(display().isEmpty(), "分类过滤后的仓储区域应保持为空");
                capture(client, "category", 13);
            }
            case 13 -> {
                if (!screen.hasUnfilteredContents() || screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) != 9
                    || (boolean) field(screen, "metadataPending") || (boolean) field(screen, "contentsMetadataPending")) return;
                setField("metadataCooldown", Integer.MAX_VALUE);
                setField("nextContentsMetadata", Long.MAX_VALUE);
                var changed = new CompletableFuture<Void>();
                client.getSingleplayerServer().execute(() -> {
                    try (Transaction transaction = Transaction.openRoot()) {
                        storage.getItems().insert(ItemResource.of(Items.DIAMOND), 1, transaction);
                        transaction.commit();
                    }
                    changed.complete(null);
                });
                changed.thenComposeAsync(ignored -> StorageClientStub.sync(CORE, new IntArrayList()), client)
                    .whenCompleteAsync((result, error) -> {
                        try {
                            require(error == null, "模拟可见页同步应成功");
                            var method = StorageScreen.class.getDeclaredMethod("applySyncResult", StorageServerStub.SyncResult.class);
                            method.setAccessible(true);
                            method.invoke(screen, result);
                            require(!screen.hasUnfilteredContents(), "可见页版本推进后不能继续采用旧材料快照");
                            setField("metadataCooldown", 0);
                            versionChecked = true;
                        } catch (Throwable problem) {
                            failure = problem;
                        }
                    }, client);
                advance(14);
            }
            case 14 -> {
                if (!versionChecked || !screen.hasUnfilteredContents()
                    || screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) != 10) return;
                AnvilCraft.LOGGER.info(
                    "PORT_UNFILTERED_CONTENTS_PASSED: pagination, hidden materials, live JEI, category, transfer, sync version");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown unfiltered stage " + stage);
        }
    }

    private static EditBox search() {
        return (EditBox) field(screen, "search");
    }

    private static IntList display() {
        return (IntList) field(screen, "displayOrder");
    }

    private static void showRecipe() {
        var runtime = Internal.getJeiRuntime();
        var manager = runtime.getRecipeManager();
        var recipe = manager.createRecipeLookup(RecipeTypes.CRAFTING).get()
            .filter(holder -> holder.id().identifier().toString().equals("minecraft:diamond_block")).findFirst().orElseThrow();
        runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.CRAFTING), List.of(recipe), List.of());
    }

    private static IconButton transferButton(Minecraft client) {
        var layouts = (List<?>) field(field(client.screen, "layouts"), "recipeLayoutsWithButtons");
        var layout = (IRecipeLayoutWithButtons<?>) layouts.getFirst();
        return (IconButton) ((List<?>) field(layout, "buttons")).getFirst();
    }

    private static boolean active(Minecraft client) {
        var button = transferButton(client);
        return button.isVisible() && ((AbstractWidget) field(button, "button")).active;
    }

    private static void clickCategory(Minecraft client) {
        var categories = (CategoryList) field(screen, "categories");
        clickWidget(client, (AbstractWidget) fieldList(categories, "categoryButtons").getFirst());
    }

    private static List<?> fieldList(Object owner, String name) {
        return (List<?>) field(owner, name);
    }

    private static void clickWidget(Minecraft client, AbstractWidget widget) {
        click(client, widget.getX() + widget.getWidth() / 2, widget.getY() + widget.getHeight() / 2);
    }

    private static void clickTransfer(Minecraft client) {
        var area = transferButton(client).getArea();
        click(client, area.getX() + area.getWidth() / 2, area.getY() + area.getHeight() / 2);
    }

    private static void click(Minecraft client, int x, int y) {
        var target = client.screen;
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        target.mouseClicked(event, false);
        target.mouseReleased(event);
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

    private static void setField(String name, Object value) {
        try {
            var field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(screen, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_UNFILTERED_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-unfiltered-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
