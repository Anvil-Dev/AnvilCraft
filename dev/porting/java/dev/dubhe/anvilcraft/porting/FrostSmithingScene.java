package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import dev.dubhe.anvilcraft.recipe.frost.CustomFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public final class FrostSmithingScene {
    private static final BlockPos TABLE = new BlockPos(20, 81, 4);
    private static boolean started;
    private static volatile Throwable failure;
    private static volatile boolean prepared;
    private static int stage;
    private static int cycle;
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
        if (failure != null) throw new IllegalStateException("浮霜客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("浮霜客户端超时：" + cycle + "/" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                prepared = false;
                client.getSingleplayerServer().execute(() -> prepare(client));
                advance(1);
            }
            case 1 -> {
                if (!prepared || !(client.screen instanceof AbstractContainerScreen<?>)) return;
                if (cycle != 2 && (!(client.player.containerMenu instanceof FrostSmithingMenu menu)
                    || menu.getAdjacentTemplates().isEmpty())) {
                    return;
                }
                if (cycle == 2 && !(client.player.containerMenu instanceof TranscendenceSmithingMenu)) return;
                var runtime = Internal.getJeiRuntime();
                var manager = runtime.getRecipeManager();
                var display = manager.createRecipeLookup(AnvilCraftJeiPlugin.FROST_SMITHING).get()
                    .filter(page -> cycle == 1
                        ? page.input().is(ModItems.EMBER_METAL_SWORD.get()) && page.material() instanceof CustomFrostMaterialPredicate
                        : page.recipe() instanceof DeformationRecipe && page.input().is(Items.IRON_HELMET))
                    .findFirst().orElseThrow();
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(AnvilCraftJeiPlugin.FROST_SMITHING),
                    List.of(display), List.of());
                advance(2);
            }
            case 2 -> {
                if (!SmithingJeiScene.active(client)) return;
                capture(client, "jei", 3);
            }
            case 3 -> {
                SmithingJeiScene.clickTransfer(client);
                advance(4);
            }
            case 4 -> {
                int result = cycle == 2 ? TranscendenceSmithingMenu.ROYAL_FROST_RESULT_SLOT : 3;
                if (TerminalJeiStorageCache.isBusy()
                    || !client.player.containerMenu.getSlot(result).getItem().is(
                        cycle == 1 ? ModItems.FROST_METAL_SWORD.get() : Items.IRON_CHESTPLATE)) {
                    return;
                }
                var menu = client.player.containerMenu;
                int equipment = cycle == 2 ? 0 : 1;
                int material = cycle == 2 ? 1 : 2;
                require(menu.getSlot(equipment).hasItem() && menu.getSlot(material).getItem().getCount() == (cycle == 1 ? 3 : 4),
                    "JEI 必须填入实际消耗数量：盔甲四锭、嬗变三粒");
                if (cycle != 2) {
                    require(((AdjacentSmithingMenu) menu).isBorrowedTemplate(menu.getSlot(0).getItem()), "必须借用相邻实体模板");
                }
                capture(client, "menu", 5);
            }
            case 5 -> {
                if (cycle == 0) {
                    var screen = (AbstractContainerScreen<?>) client.screen;
                    SmithingJeiScene.click(client, screen.getLeftPos() + 104, screen.getTopPos() + 36);
                    advance(6);
                } else {
                    advance(6);
                }
            }
            case 6 -> {
                int result = cycle == 2 ? TranscendenceSmithingMenu.ROYAL_FROST_RESULT_SLOT : 3;
                if (cycle == 0) {
                    require(client.player.containerMenu.getSlot(result).getItem().is(Items.IRON_BOOTS),
                        "真实左箭头应切换到铁靴并同步服务端");
                }
                SmithingJeiScene.takeAndPark(client, result);
                advance(7);
            }
            case 7 -> {
                var menu = client.player.containerMenu;
                require(!menu.getSlot(cycle == 2 ? 0 : 1).hasItem() && !menu.getSlot(cycle == 2 ? 1 : 2).hasItem(),
                    "取出结果后必须恰好耗尽本次装备和材料");
                if (cycle != 2) require(menu.getSlot(0).hasItem(), "取出结果不能消耗模板");
                client.screen.onClose();
                advance(8);
            }
            case 8 -> {
                if (cycle == 2) {
                    AnvilCraft.LOGGER.info("PORT_FROST_SMITHING_PASSED: JEI quantities, borrowed templates, arrows, take, transcendence");
                    client.stop();
                } else {
                    cycle++;
                    advance(0);
                }
            }
            default -> throw new IllegalStateException("Unknown frost stage " + stage);
        }
    }

    private static void prepare(Minecraft client) {
        try {
            var server = client.getSingleplayerServer();
            var player = server.getPlayerList().getPlayers().getFirst();
            var terminal = player.getInventory().getItem(0).copy();
            final var storage = Storages.get().get(terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow()).orElseThrow();
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.getInventory().setItem(0, terminal);
            var items = storage.getItems();
            try (Transaction transaction = Transaction.openRoot()) {
                for (int slot = 0; slot < items.size(); slot++) {
                    if (!items.getResource(slot).isEmpty()) items.extract(slot, items.getResource(slot), Integer.MAX_VALUE, transaction);
                }
                items.insert(ItemResource.of(cycle == 1 ? ModItems.EMBER_METAL_SWORD.get() : Items.IRON_HELMET), 1, transaction);
                items.insert(ItemResource.of(cycle == 1 ? ModItems.FROST_METAL_NUGGET.get() : ModItems.FROST_METAL_INGOT.get()),
                    cycle == 1 ? 3 : 4, transaction);
                transaction.commit();
            }
            var level = server.overworld();
            if (cycle > 0) {
                var oldChest = (ChestBlockEntity) level.getBlockEntity(TABLE.east());
                require(oldChest.getItem(0).getCount() == 1, "关闭浮霜台后借用模板必须归还");
            }
            level.setBlock(TABLE, cycle == 2 ? ModBlocks.TRANSCENDENCE_SMITHING_TABLE.getDefaultState()
                : ModBlocks.FROST_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
            level.setBlock(TABLE.east(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
            ((ChestBlockEntity) level.getBlockEntity(TABLE.east())).setItem(0,
                cycle == 1 ? ModItems.PERMUTATION_TEMPLATE.asStack() : ModItems.DEFORMATION_TEMPLATE.asStack());
            player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> cycle == 2
                ? new TranscendenceSmithingMenu(id, inventory, ContainerLevelAccess.create(level, TABLE))
                : new FrostSmithingMenu(id, inventory, ContainerLevelAccess.create(level, TABLE)), Component.literal("Frost smithing")));
            prepared = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_FROST_STAGE: {}/{} -> {}", cycle, stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "frost-smithing-26.1-" + cycle + "-" + name + ".png",
            client.getMainRenderTarget(), 1, message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
