package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.client.building.BlueprintClientFiles;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.StorageJeiSupport;
import dev.dubhe.anvilcraft.util.StructureScannerRecipes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;

import java.util.List;
import javax.annotation.Nullable;

public final class ScannerRecipeClientScene {
    private static final BlockPos SCANNER = new BlockPos(8, 81, 8);
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile boolean prepared;
    @Nullable private static volatile Throwable failure;
    @Nullable private static StructureSnapshot expected;
    private static boolean capturing;
    private static boolean opened;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Scanner recipe scene stage " + stage, failure);
        }
        if (capturing || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.setNoGravity(true);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 81 10.5");
                    player.level().setBlockAndUpdate(SCANNER, ModBlocks.STRUCTURE_SCANNER.getDefaultState());
                    var scanner = (StructureScannerBlockEntity) player.level().getBlockEntity(SCANNER);
                    scanner.setDiskStack(ModItems.STRUCTURE_DISK.asStack());
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        if (prepared && !opened) {
            if (!(client.level.getBlockEntity(SCANNER) instanceof StructureScannerBlockEntity)) return;
            opened = true;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.openMenu((StructureScannerBlockEntity) player.level().getBlockEntity(SCANNER), SCANNER);
            });
            next = System.currentTimeMillis() + 900;
            return;
        }
        if (!prepared || !(client.screen instanceof StructureScannerScreen screen)) return;
        client.options.guiScale().set(2);
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 1 -> {
                if (!screen.getMenu().getSlot(0).hasItem()) return;
                transfer(client, screen, AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
                stage = 2;
                next = System.currentTimeMillis() + 1000;
            }
            case 2 -> {
                if (BlueprintClientFiles.isBusy()) return;
                verify(screen);
                capture(client, "crafting", 3);
            }
            case 3 -> {
                transfer(client, screen, AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
                stage = 4;
                next = System.currentTimeMillis() + 1000;
            }
            case 4 -> {
                if (BlueprintClientFiles.isBusy()) return;
                verify(screen);
                capture(client, "conversion", 5);
            }
            case 5 -> {
                BlueprintClientFiles.requestRecipe(screen.getMenu().containerId, AnvilCraft.of("missing_recipe"));
                stage = 6;
            }
            case 6 -> {
                if (BlueprintClientFiles.isBusy()) return;
                verify(screen);
                AnvilCraft.LOGGER.info("PORT_SCANNER_RECIPE_CLIENT_PASSED: registered JEI handlers, dry run, busy guard, "
                    + "two recipe categories, native request/response, unchanged disk and invalid recipe rejection");
                client.stop();
                stage = 7;
            }
            default -> {
            }
        }
    }

    private static <R extends RecipeHolder<?>> void transfer(Minecraft client, StructureScannerScreen screen, IRecipeType<R> type) {
        IJeiRuntime runtime;
        try {
            var field = StorageJeiSupport.class.getDeclaredField("runtime");
            field.setAccessible(true);
            runtime = (IJeiRuntime) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        if (runtime == null) throw new IllegalStateException("JEI runtime unavailable");
        var manager = runtime.getRecipeManager();
        var recipe = manager.createRecipeLookup(type).get().findFirst().orElseThrow();
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(
            screen.getMenu(), manager.getRecipeCategory(type)).orElseThrow();
        check(handler.transferRecipe(screen.getMenu(), recipe, List::of, client.player, false, false) == null,
            "JEI dry run rejected a valid recipe");
        check(!BlueprintClientFiles.isBusy(), "JEI dry run sent a request");
        expected = StructureScannerRecipes.snapshot(recipe.value());
        check(handler.transferRecipe(screen.getMenu(), recipe, List::of, client.player, false, true) == null,
            "JEI transfer rejected a valid recipe");
        check(BlueprintClientFiles.isBusy(), "JEI transfer did not send a request");
        check(handler.transferRecipe(screen.getMenu(), recipe, List::of, client.player, false, true) != null,
            "Busy transfer was not rejected");
        AnvilCraft.LOGGER.info("PORT_SCANNER_RECIPE_TRANSFER: {}", recipe.id());
    }

    private static void verify(StructureScannerScreen screen) {
        var actual = screen.getMenu().getImportedStructure();
        check(expected != null && actual != null
            && StructureSnapshotCodec.write(expected).equals(StructureSnapshotCodec.write(actual.snapshot())),
            "Imported preview differs from recipe input");
        check(screen.getMenu().getSlot(0).hasItem() && !screen.getMenu().getSlot(1).hasItem(), "Recipe import consumed a disk");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void capture(Minecraft client, String label, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "scanner-recipe-26.1-" + label + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                stage = nextStage;
                next = System.currentTimeMillis() + 500;
            }));
    }
}
