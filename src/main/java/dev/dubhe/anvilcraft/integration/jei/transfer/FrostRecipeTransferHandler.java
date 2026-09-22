package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.integration.jei.category.FrostSmithingCategory;
import dev.dubhe.anvilcraft.inventory.FrostSmithingTransfer;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput;
import dev.dubhe.anvilcraft.rpc.SmithingServerStub;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class FrostRecipeTransferHandler<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
    private final Class<C> menuClass;
    private final MenuType<C> menuType;
    private final IRecipeType<R> recipeType;
    private final IRecipeTransferHandlerHelper errors;

    public FrostRecipeTransferHandler(Class<C> menuClass, MenuType<C> menuType, IRecipeType<R> recipeType,
                                     IRecipeTransferHandlerHelper errors) {
        this.menuClass = menuClass;
        this.menuType = menuType;
        this.recipeType = recipeType;
        this.errors = errors;
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return this.menuClass;
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return Optional.of(this.menuType);
    }

    @Override
    public IRecipeType<R> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots, Player player,
                                                        boolean maximum, boolean transfer) {
        if (!(recipe instanceof FrostSmithingCategory.Display display) || player.containerMenu != menu) {
            return this.errors.createInternalError();
        }
        List<UUID> targets = TerminalJeiStorageCache.boundStorages(player);
        StorageServerStub.TerminalSnapshot snapshot = targets.isEmpty()
            ? new StorageServerStub.TerminalSnapshot(List.of(), List.of(), true) : TerminalJeiStorageCache.get(targets);
        if (!targets.isEmpty()) TerminalJeiStorageCache.ensure(targets);
        Choice choice = choose(player, menu, display, snapshot, maximum);
        if (choice == null && snapshot != null && snapshot.complete()) {
            return this.errors.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                slots.getSlotViews(RecipeIngredientRole.INPUT));
        }
        if (!transfer || TerminalJeiStorageCache.isBusy()) return null;
        final var client = Minecraft.getInstance();
        final long epoch = TerminalJeiStorageCache.begin();
        CompletableFuture<StorageServerStub.TerminalSnapshot> ready = targets.isEmpty()
            ? CompletableFuture.completedFuture(snapshot) : TerminalJeiStorageCache.ensure(targets);
        ready.thenComposeAsync(loaded -> {
            if (!TerminalJeiStorageCache.isCurrent(epoch) || client.player != player || player.containerMenu != menu) {
                return CompletableFuture.completedFuture(false);
            }
            Choice selected = choose(player, menu, display, loaded, maximum);
            if (selected == null) return CompletableFuture.completedFuture(false);
            return RPC.invoke(RpcTarget.server(), SmithingServerStub::transferFrost,
                player.getUUID(), menu.containerId, selected.equipment(), selected.material(), targets);
        }, client).whenCompleteAsync((result, error) -> {
            if (TerminalJeiStorageCache.isCurrent(epoch)) TerminalJeiStorageCache.invalidate(targets);
            TerminalJeiStorageCache.finish(epoch);
        }, client);
        return null;
    }

    private static @Nullable Choice choose(Player player, AbstractContainerMenu menu, FrostSmithingCategory.Display display,
                                           StorageServerStub.@Nullable TerminalSnapshot snapshot, boolean maximum) {
        List<ItemStack> pool = FrostSmithingTransfer.available(player, menu);
        if (snapshot != null) pool.addAll(snapshot.items());
        for (ItemStack candidate : pool) {
            if (candidate.isEmpty() || !candidate.is(display.input().getItem()) || !display.recipe().isInput(candidate)) continue;
            long equipmentCount = FrostSmithingTransfer.count(pool, candidate, player.registryAccess());
            ItemStack equipment = candidate.copyWithCount(maximum ? (int) Math.min(equipmentCount, candidate.getMaxStackSize()) : 1);
            if (display.material().test(display.recipe(), new FrostSmithingRecipeInput(ItemStack.EMPTY, equipment, ItemStack.EMPTY))) {
                return new Choice(equipment, ItemStack.EMPTY);
            }
            for (ItemStack material : pool) {
                if (material.isEmpty()) continue;
                long amount = FrostSmithingTransfer.count(pool, material, player.registryAccess());
                if (FrostSmithingTransfer.same(equipment, material, player.registryAccess())) amount -= equipment.getCount();
                if (amount <= 0) continue;
                ItemStack available = material.copyWithCount((int) Math.min(amount, material.getMaxStackSize()));
                var input = new FrostSmithingRecipeInput(ItemStack.EMPTY, equipment, available);
                if (!display.material().test(display.recipe(), input)) continue;
                int cost = display.material().cost(display.recipe(), input);
                return new Choice(equipment, available.copyWithCount(maximum ? available.getCount() : Math.max(1, cost)));
            }
        }
        return null;
    }

    private record Choice(ItemStack equipment, ItemStack material) {
    }
}
