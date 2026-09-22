package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.common.Internal;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public final class SmithingJeiScene {
    private static final BlockPos ROYAL = new BlockPos(20, 81, 3);
    private static final BlockPos EMBER = new BlockPos(23, 81, 3);
    private static final BlockPos TRANSCENDENCE = new BlockPos(26, 81, 3);
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static BaseStorage<?> storage;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 180000;
        }
        if (failure != null) throw new IllegalStateException("锻造 JEI 验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("锻造 JEI 超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        var terminal = player.getInventory().getItem(0).copy();
                        storage = Storages.get().get(terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow()).orElseThrow();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                        player.getInventory().setItem(0, terminal);
                        player.inventoryMenu.setCarried(ItemStack.EMPTY);
                        stock(false);
                        server.overworld().setBlock(ROYAL, ModBlocks.ROYAL_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
                        server.overworld().setBlock(EMBER, ModBlocks.EMBER_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
                        server.overworld().setBlock(TRANSCENDENCE,
                            ModBlocks.TRANSCENDENCE_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
                        chest(client, ROYAL.south(), new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                        chest(client, EMBER.south(), new ItemStack(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get()));
                        open(client, 0);
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !(client.player.containerMenu instanceof RoyalSmithingMenu menu)
                    || menu.getAdjacentTemplates().isEmpty()) return;
                show(false);
                advance(2);
            }
            case 2 -> {
                require(active(client), "相邻模板与终端材料应启用皇家锻造转移");
                clickTransfer(client);
                advance(3);
            }
            case 3 -> {
                if (TerminalJeiStorageCache.isBusy() || !client.player.containerMenu.getSlot(3).getItem().is(Items.NETHERITE_SWORD)) return;
                var menu = (AdjacentSmithingMenu) client.player.containerMenu;
                require(menu.isBorrowedTemplate(menu.getSlot(0).getItem()), "皇家模板必须标为借用且不能作为材料搬走");
                capture(client, "royal", 4);
            }
            case 4 -> {
                takeAndPark(client, 3);
                advance(5);
            }
            case 5 -> {
                require(client.player.containerMenu.getSlot(0).getItem().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                    "皇家取出产物不得消耗模板");
                client.getSingleplayerServer().execute(() -> {
                    stock(true);
                    open(client, 1);
                    var oldChest = (ChestBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(ROYAL.south());
                    if (!oldChest.getItem(0).is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                        failure = new IllegalStateException("关闭皇家台后借用模板没有归还");
                    }
                });
                advance(6);
            }
            case 6 -> {
                if (!(client.player.containerMenu instanceof EmberSmithingMenu menu) || menu.getAdjacentTemplates().isEmpty()) return;
                TerminalJeiStorageCache.clear();
                show(true);
                advance(7);
            }
            case 7 -> {
                require(active(client), "余烬转移应识别催化模板与三个实际输入");
                clickTransfer(client);
                advance(8);
            }
            case 8 -> {
                if (TerminalJeiStorageCache.isBusy()
                    || !client.player.containerMenu.getSlot(10).getItem().is(ModBlocks.TRANSCENDENCE_GRINDSTONE.asItem())) return;
                capture(client, "ember", 9);
            }
            case 9 -> {
                takeAndPark(client, 10);
                advance(10);
            }
            case 10 -> {
                require(client.player.containerMenu.getSlot(0).getItem().is(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE),
                    "余烬取出产物不得消耗模板");
                client.getSingleplayerServer().execute(() -> {
                    stock(false);
                    open(client, 2);
                    var oldChest = (ChestBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(EMBER.south());
                    if (!oldChest.getItem(0).is(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE)) {
                        failure = new IllegalStateException("关闭余烬台后借用模板没有归还");
                    }
                });
                advance(11);
            }
            case 11 -> {
                if (!(client.player.containerMenu instanceof TranscendenceSmithingMenu menu) || menu.getTemplates().isEmpty()) return;
                TerminalJeiStorageCache.clear();
                show(false);
                advance(12);
            }
            case 12 -> {
                require(active(client), "超越台内置皇家模板应可用");
                clickTransfer(client);
                advance(13);
            }
            case 13 -> {
                if (TerminalJeiStorageCache.isBusy()
                    || !client.player.containerMenu.getSlot(11).getItem().is(Items.NETHERITE_SWORD)) return;
                require(((TranscendenceSmithingMenu) client.player.containerMenu).getSelectedTemplate()
                    .is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), "未同步内置皇家模板选择");
                capture(client, "transcendence-royal", 14);
            }
            case 14 -> {
                takeAndPark(client, 11);
                client.getSingleplayerServer().execute(() -> stock(true));
                advance(15);
            }
            case 15 -> {
                TerminalJeiStorageCache.clear();
                show(true);
                advance(16);
            }
            case 16 -> {
                require(active(client), "超越台切换到内置余烬模板应可填充");
                clickTransfer(client);
                advance(17);
            }
            case 17 -> {
                if (TerminalJeiStorageCache.isBusy()
                    || !client.player.containerMenu.getSlot(12).getItem().is(ModBlocks.TRANSCENDENCE_GRINDSTONE.asItem())) return;
                require(((TranscendenceSmithingMenu) client.player.containerMenu).getMode() == TranscendenceSmithingMenu.Mode.EMBER,
                    "模板切换没有同步输入槽模式");
                capture(client, "transcendence-ember", 18);
            }
            case 18 -> {
                AnvilCraft.LOGGER.info(
                    "PORT_SMITHING_JEI_PASSED: borrowed templates, terminal materials, batch fill, take, builtin mode switch");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown smithing stage " + stage);
        }
    }

    private static void stock(boolean ember) {
        var items = storage.getItems();
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < items.size(); slot++) {
                if (!items.getResource(slot).isEmpty()) items.extract(slot, items.getResource(slot), Integer.MAX_VALUE, transaction);
            }
            if (ember) {
                items.insert(ItemResource.of(ModItems.MULTIPHASE_TRANSCENDIUM.get()), 1, transaction);
                items.insert(ItemResource.of(ModBlocks.EMBER_GRINDSTONE.asItem()), 1, transaction);
                items.insert(ItemResource.of(ModBlocks.FROST_GRINDSTONE.asItem()), 1, transaction);
            } else {
                items.insert(ItemResource.of(Items.DIAMOND_SWORD), 1, transaction);
                items.insert(ItemResource.of(Items.NETHERITE_INGOT), 1, transaction);
            }
            transaction.commit();
        }
    }

    private static void chest(Minecraft client, BlockPos pos, ItemStack template) {
        var level = client.getSingleplayerServer().overworld();
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        ((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, template);
    }

    private static void open(Minecraft client, int kind) {
        var server = client.getSingleplayerServer();
        var player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = kind == 0 ? ROYAL : kind == 1 ? EMBER : TRANSCENDENCE;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> switch (kind) {
            case 0 -> new RoyalSmithingMenu(id, inventory, ContainerLevelAccess.create(server.overworld(), pos));
            case 1 -> new EmberSmithingMenu(id, inventory, ContainerLevelAccess.create(server.overworld(), pos));
            default -> new TranscendenceSmithingMenu(id, inventory, ContainerLevelAccess.create(server.overworld(), pos));
        }, Component.literal("Smithing transfer check")));
    }

    private static void show(boolean ember) {
        var runtime = Internal.getJeiRuntime();
        var manager = runtime.getRecipeManager();
        if (ember) {
            var recipe = manager.createRecipeLookup(AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING).get()
                .filter(holder -> holder.id().identifier().toString().equals("anvilcraft:two_to_one_smithing/transcendence_grindstone"))
                .findFirst().orElseThrow();
            runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING),
                List.of(recipe), List.of());
        } else {
            var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.NETHERITE_INGOT));
            var recipe = manager.createRecipeLookup(RecipeTypes.SMITHING).get()
                .filter(holder -> holder.value().matches(input, Minecraft.getInstance().level)).findFirst().orElseThrow();
            runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.SMITHING), List.of(recipe), List.of());
        }
    }

    private static IconButton button(Minecraft client) {
        var layouts = (List<?>) field(field(client.screen, "layouts"), "recipeLayoutsWithButtons");
        var layout = (IRecipeLayoutWithButtons<?>) layouts.getFirst();
        return (IconButton) ((List<?>) field(layout, "buttons")).getFirst();
    }

    private static boolean active(Minecraft client) {
        return button(client).isVisible() && ((AbstractWidget) field(button(client), "button")).active;
    }

    private static void clickTransfer(Minecraft client) {
        var area = button(client).getArea();
        click(client, area.getX() + area.getWidth() / 2, area.getY() + area.getHeight() / 2);
    }

    private static void takeAndPark(Minecraft client, int resultSlot) {
        var screen = (AbstractContainerScreen<?>) client.screen;
        Slot result = screen.getMenu().getSlot(resultSlot);
        click(client, screen.getLeftPos() + result.x + 8, screen.getTopPos() + result.y + 8);
        Slot target = screen.getMenu().slots.stream()
            .filter(slot -> slot.container == client.player.getInventory() && !slot.hasItem()).findFirst().orElseThrow();
        click(client, screen.getLeftPos() + target.x + 8, screen.getTopPos() + target.y + 8);
    }

    private static void click(Minecraft client, int x, int y) {
        var screen = client.screen;
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_SMITHING_JEI_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "smithing-jei-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
