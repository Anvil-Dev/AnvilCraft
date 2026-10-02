package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.common.transfer.RecipeTransferUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TerminalJeiTransferSupport {
    private TerminalJeiTransferSupport() {
    }

    public static void handle(AbstractContainerMenu menu, IRecipeSlotsView slots, Player player, IStackHelper stackHelper,
                              IRecipeTransferHandlerHelper errors, List<Slot> crafting, List<Slot> inventory,
                              boolean maximum, boolean transfer, Supplier<@Nullable IRecipeTransferError> retry,
                              Consumer<@Nullable IRecipeTransferError> cancel) {
        if (TerminalJeiStorageCache.isRetrying() || Minecraft.getInstance().player != player || player.containerMenu != menu) return;
        if (!RecipeTransferUtil.validateSlots(player, List.of(), crafting, inventory)) return;
        List<UUID> targets = TerminalJeiStorageCache.boundStorages(player);
        if (targets.isEmpty()) return;
        var snapshot = TerminalJeiStorageCache.get(targets);
        TerminalJeiStorageCache.ensure(targets);
        if (transfer && TerminalJeiStorageCache.isBusy()) {
            cancel.accept(null);
            return;
        }
        if (snapshot != null) {
            var plan = TerminalJeiTransferPlan.create(player, crafting, inventory,
                slots.getSlotViews(RecipeIngredientRole.INPUT), stackHelper, snapshot, maximum);
            if (plan.desiredInventory().isEmpty() && plan.missing().isEmpty()) return;
            if (!plan.missing().isEmpty() && snapshot.complete()) {
                cancel.accept(errors.createUserErrorForMissingSlots(
                    Component.translatable("jei.tooltip.error.recipe.transfer.missing"), plan.missing()));
                return;
            }
        }
        cancel.accept(null);
        if (!transfer) return;
        final var client = Minecraft.getInstance();
        final long epoch = TerminalJeiStorageCache.begin();
        TerminalJeiStorageCache.ensure(targets).thenComposeAsync(loaded -> {
            if (!TerminalJeiStorageCache.isCurrent(epoch) || client.player != player || player.containerMenu != menu) {
                return CompletableFuture.completedFuture(StorageServerStub.TerminalRestock.EMPTY);
            }
            var plan = TerminalJeiTransferPlan.create(player, crafting, inventory,
                slots.getSlotViews(RecipeIngredientRole.INPUT), stackHelper, loaded, maximum);
            if (plan.desiredInventory().isEmpty()) return CompletableFuture.completedFuture(StorageServerStub.TerminalRestock.EMPTY);
            return StorageTerminalClientStub.restock(menu.containerId, targets, plan.desiredInventory());
        }, client).thenComposeAsync(restock -> {
            if (!TerminalJeiStorageCache.isCurrent(epoch) || client.player != player) return CompletableFuture.completedFuture(false);
            TerminalJeiStorageCache.invalidate(targets);
            try {
                if (player.containerMenu == menu) {
                    TerminalJeiStorageCache.setRetrying(true);
                    retry.get();
                }
            } catch (RuntimeException error) {
                AnvilCraft.LOGGER.warn("Unable to retry recipe transfer after terminal restock", error);
            } finally {
                TerminalJeiStorageCache.setRetrying(false);
            }
            if (restock.withdrawn().isEmpty()) return CompletableFuture.completedFuture(false);
            // JEI has queued its transfer packet; the following RPC observes the server's resulting inventory.
            return StorageTerminalClientStub.returnExcess(targets, restock);
        }, client).whenCompleteAsync((returned, error) -> {
            if (TerminalJeiStorageCache.isCurrent(epoch)) TerminalJeiStorageCache.invalidate(targets);
            TerminalJeiStorageCache.finish(epoch);
        }, client);
    }
}
