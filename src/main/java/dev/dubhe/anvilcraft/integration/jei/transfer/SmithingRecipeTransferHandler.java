package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import dev.dubhe.anvilcraft.rpc.SmithingServerStub;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public final class SmithingRecipeTransferHandler<C extends AbstractContainerMenu, R>
    implements IRecipeTransferHandler<C, R> {
    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<C, R> materials;
    @Nullable
    private final IRecipeTransferHandler<C, R> inventoryTransfer;
    private final boolean templateIsInput;
    private final BiPredicate<R, ItemStack> templateMatches;

    private SmithingRecipeTransferHandler(
        IRecipeTransferRegistration registration,
        Class<C> menuClass,
        MenuType<C> menuType,
        RecipeType<R> recipeType,
        int inputStart,
        int inputCount,
        int inventoryStart,
        boolean templateIsInput,
        BiPredicate<R, ItemStack> templateMatches
    ) {
        this.helper = registration.getTransferHelper();
        this.materials = this.helper.createUnregisteredRecipeTransferHandler(this.helper.createBasicRecipeTransferInfo(
            menuClass, menuType, recipeType, inputStart, inputCount, inventoryStart, 36
        ));
        this.inventoryTransfer = menuClass == RoyalSmithingMenu.class
            ? this.helper.createUnregisteredRecipeTransferHandler(this.helper.createBasicRecipeTransferInfo(
                menuClass, menuType, recipeType, 0, 3, inventoryStart, 36
            )) : null;
        this.templateIsInput = templateIsInput;
        this.templateMatches = templateMatches;
    }

    public static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new SmithingRecipeTransferHandler<>(
            registration, RoyalSmithingMenu.class, ModMenuTypes.ROYAL_SMITHING.get(),
            RecipeTypes.SMITHING, 1, 2, 4, true,
            (recipe, stack) -> recipe.value().isTemplateIngredient(stack)
        ), RecipeTypes.SMITHING);
        registration.addRecipeTransferHandler(new SmithingRecipeTransferHandler<>(
            registration, EmberSmithingMenu.class, ModMenuTypes.EMBER_SMITHING.get(),
            AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING, 1, 9, 11, false,
            (recipe, stack) -> recipe.value().isTemplateIngredient(stack)
        ), AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING);
        registration.addRecipeTransferHandler(new SmithingRecipeTransferHandler<>(
            registration, FrostSmithingMenu.class, ModMenuTypes.FROST_SMITHING.get(),
            AnvilCraftJeiPlugin.FROST_SMITHING, 1, 2, 4, false,
            (recipe, stack) -> recipe.recipe().isTemplate(stack)
        ), AnvilCraftJeiPlugin.FROST_SMITHING);
        registration.addRecipeTransferHandler(new SmithingRecipeTransferHandler<>(
            registration, TranscendenceSmithingMenu.class, ModMenuTypes.TRANSCENDENCE_SMITHING.get(),
            RecipeTypes.SMITHING, 0, 2, 13, true,
            (recipe, stack) -> recipe.value().isTemplateIngredient(stack)
        ), RecipeTypes.SMITHING);
        registration.addRecipeTransferHandler(new SmithingRecipeTransferHandler<>(
            registration, TranscendenceSmithingMenu.class, ModMenuTypes.TRANSCENDENCE_SMITHING.get(),
            AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING, 2, 9, 13, false,
            (recipe, stack) -> recipe.value().isTemplateIngredient(stack)
        ), AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING);
        registration.addRecipeTransferHandler(new SmithingRecipeTransferHandler<>(
            registration, TranscendenceSmithingMenu.class, ModMenuTypes.TRANSCENDENCE_SMITHING.get(),
            AnvilCraftJeiPlugin.FROST_SMITHING, 0, 2, 13, false,
            (recipe, stack) -> recipe.recipe().isTemplate(stack)
        ), AnvilCraftJeiPlugin.FROST_SMITHING);
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return this.materials.getContainerClass();
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return this.materials.getMenuType();
    }

    @Override
    public RecipeType<R> getRecipeType() {
        return this.materials.getRecipeType();
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
        C menu, R recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer
    ) {
        List<IRecipeSlotView> templateViews = slots.getSlotViews(
            this.templateIsInput ? RecipeIngredientRole.INPUT : RecipeIngredientRole.CATALYST
        );
        if (templateViews.isEmpty()) return this.helper.createInternalError();
        IRecipeSlotView templateView = templateViews.getFirst();
        ItemStack template = availableTemplates(menu, player)
            .filter(stack -> !stack.isEmpty() && this.templateMatches.test(recipe, stack))
            .findFirst().map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
        if (template.isEmpty()) {
            if (this.inventoryTransfer != null && menu instanceof AdjacentSmithingMenu smithing
                && !smithing.isBorrowedTemplate(menu.getSlot(0).getItem())) {
                return this.inventoryTransfer.transferRecipe(menu, recipe, slots, player, maxTransfer, doTransfer);
            }
            return this.helper.createUserErrorForMissingSlots(
                Component.translatable("jei.tooltip.error.recipe.transfer.missing"), List.of(templateView)
            );
        }
        IRecipeSlotsView materialViews = this.helper.createRecipeSlotsView(slots.getSlotViews().stream()
            .filter(slot -> slot != templateView).toList());
        IRecipeTransferError error = this.materials.transferRecipe(menu, recipe, materialViews, player, maxTransfer, false);
        if (error != null || !doTransfer) return error;
        if (ItemStack.isSameItemSameComponents(selectedTemplate(menu), template)) {
            return this.materials.transferRecipe(menu, recipe, materialViews, player, maxTransfer, true);
        }
        RPC.invoke(RpcTarget.server(), SmithingServerStub::selectTemplate, player.getUUID(), menu.containerId, template)
            .thenAccept(selected -> Minecraft.getInstance().execute(() -> {
                if (!selected || Minecraft.getInstance().player != player || player.containerMenu != menu) return;
                if (!ItemStack.isSameItemSameComponents(selectedTemplate(menu), template)) return;
                this.materials.transferRecipe(menu, recipe, materialViews, player, maxTransfer, true);
            }));
        return null;
    }

    private static Stream<ItemStack> availableTemplates(AbstractContainerMenu menu, Player player) {
        if (menu instanceof AdjacentSmithingMenu smithing) {
            return Stream.concat(Stream.of(selectedTemplate(menu)), Stream.concat(
                smithing.getAdjacentTemplates().stream(), player.getInventory().items.stream()
            ));
        }
        if (menu instanceof TranscendenceSmithingMenu smithing) {
            return Stream.concat(Stream.of(smithing.getSelectedTemplate()), smithing.getTemplates().stream());
        }
        return Stream.empty();
    }

    private static ItemStack selectedTemplate(AbstractContainerMenu menu) {
        if (menu instanceof TranscendenceSmithingMenu smithing) return smithing.getSelectedTemplate();
        return menu.getSlot(0).getItem();
    }
}
