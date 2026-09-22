package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.ReadOnlyItemResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.common.Internal;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.gui.recipes.IRecipeLayoutWithButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ReadOnlyStorageScene {
    private static final BlockPos HYPER = new BlockPos(20, 81, 0);
    private static final BlockPos SHULKER = new BlockPos(30, 81, 0);
    private static boolean started;
    private static volatile Throwable failure;
    private static CompletableFuture<Void> operation;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 120000;
        }
        if (failure != null) throw new IllegalStateException("只读仓储客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("只读仓储客户端超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                operation = server(client, () -> prepare(client, HYPER));
                advance(1);
            }
            case 1 -> {
                if (!operation.isDone() || !(client.player.containerMenu instanceof RoyalSmithingMenu menu)
                    || menu.getAdjacentTemplates().isEmpty()) return;
                operation.join();
                show();
                advance(2);
            }
            case 2 -> {
                clickTransfer(client);
                advance(3);
            }
            case 3 -> {
                if (!client.player.containerMenu.getSlot(3).getItem().is(Items.NETHERITE_SWORD)) return;
                require(((RoyalSmithingMenu) client.player.containerMenu)
                    .isBorrowedTemplate(client.player.containerMenu.getSlot(0).getItem()),
                    "大型仓储模板应以借用形式显示");
                capture(client, "hyper", 4);
            }
            case 4 -> {
                client.screen.onClose();
                advance(5);
            }
            case 5 -> {
                operation = server(client, () -> {
                    verifyReturned(client, HYPER);
                    prepare(client, SHULKER);
                });
                advance(6);
            }
            case 6 -> {
                if (!operation.isDone() || !(client.player.containerMenu instanceof RoyalSmithingMenu menu)
                    || menu.getAdjacentTemplates().isEmpty()) return;
                operation.join();
                show();
                advance(7);
            }
            case 7 -> {
                clickTransfer(client);
                advance(8);
            }
            case 8 -> {
                if (!client.player.containerMenu.getSlot(3).getItem().is(Items.NETHERITE_SWORD)) return;
                capture(client, "shulker", 9);
            }
            case 9 -> {
                client.screen.onClose();
                advance(10);
            }
            case 10 -> {
                operation = server(client, () -> verifyReturned(client, SHULKER));
                advance(11);
            }
            case 11 -> {
                if (!operation.isDone()) return;
                operation.join();
                AnvilCraft.LOGGER.info(
                    "PORT_READONLY_STORAGE_PASSED: non-main hyper/shulker access, JEI borrowing, return, automation refusal");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown read-only stage " + stage);
        }
    }

    private static void prepare(Minecraft client, BlockPos core) {
        var server = client.getSingleplayerServer();
        final var level = server.overworld();
        var player = server.getPlayerList().getPlayers().getFirst();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a " + (core.getX() + 0.5) + " 82 2.5");
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND_SWORD));
        player.getInventory().setItem(10, new ItemStack(Items.NETHERITE_INGOT));
        var storage = (StorageBlockEntity) level.getBlockEntity(core);
        var items = Storages.get().get(storage.getId()).orElseThrow().getItems();
        try (Transaction transaction = Transaction.openRoot()) {
            items.insert(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 1, transaction);
            transaction.commit();
        }
        var handler = level.getCapability(Capabilities.Item.BLOCK, core.west(), null);
        require(handler instanceof ReadOnlyItemResourceHandler, "非主方块没有暴露只读能力");
        try (Transaction transaction = Transaction.openRoot()) {
            require(handler.extract(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 1, transaction) == 0,
                "自动化不能从本体提取模板");
            transaction.commit();
        }
        BlockPos table = core.west(2);
        level.setBlock(table, ModBlocks.ROYAL_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
            new RoyalSmithingMenu(id, inventory, ContainerLevelAccess.create(level, table)),
            Component.literal("Read-only template check")));
    }

    private static void verifyReturned(Minecraft client, BlockPos core) {
        var level = client.getSingleplayerServer().overworld();
        var entity = (StorageBlockEntity) level.getBlockEntity(core);
        var items = Storages.get().get(entity.getId()).orElseThrow().getItems();
        long count = 0;
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.getResource(slot).equals(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))) {
                count += items.getAmountAsLong(slot);
            }
        }
        require(count == 1, "关闭锻造界面后模板没有完整归还原仓储");
    }

    private static CompletableFuture<Void> server(Minecraft client, Runnable task) {
        var result = new CompletableFuture<Void>();
        client.getSingleplayerServer().execute(() -> {
            try {
                task.run();
                result.complete(null);
            } catch (Throwable problem) {
                failure = problem;
                result.completeExceptionally(problem);
            }
        });
        return result;
    }

    private static void show() {
        var runtime = Internal.getJeiRuntime();
        var manager = runtime.getRecipeManager();
        var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.NETHERITE_INGOT));
        var recipe = manager.createRecipeLookup(RecipeTypes.SMITHING).get()
            .filter(holder -> holder.value().matches(input, Minecraft.getInstance().level)).findFirst().orElseThrow();
        runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(RecipeTypes.SMITHING), List.of(recipe), List.of());
    }

    private static void clickTransfer(Minecraft client) {
        var layouts = (List<?>) field(field(client.screen, "layouts"), "recipeLayoutsWithButtons");
        var layout = (IRecipeLayoutWithButtons<?>) layouts.getFirst();
        var button = (IconButton) ((List<?>) field(layout, "buttons")).getFirst();
        var area = button.getArea();
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_READONLY_STORAGE_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "readonly-storage-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
