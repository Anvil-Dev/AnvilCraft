package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.client.gui.screen.StorageMenu;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.integration.jei.StorageJeiBridge;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

@EmiEntrypoint
public class AnvilCraftEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        StorageJeiBridge.setEmiRecipeParent(
            screen -> Minecraft.getInstance().screen != screen && EmiApi.getHandledScreen() == screen
        );
        StorageJeiBridge.setEmiRecipeOpener(stonecutter -> EmiApi.displayRecipeCategory(
            stonecutter ? VanillaEmiRecipeCategories.STONECUTTING : VanillaEmiRecipeCategories.CRAFTING
        ));
        registry.addScreenBoundsProvider(
            StorageScreen.class,
            screen -> screen.width > 0 && screen.height > 0
                ? new Bounds(screen.getLeftPos(), screen.getTopPos(), screen.getImageWidth(), screen.getImageHeight())
                : Bounds.EMPTY
        );
        registry.addStackProvider(StorageScreen.class, (screen, x, y) -> {
            ItemStack item = screen.getItemUnderMouse(x, y);
            if (item != null && !item.isEmpty()) {
                return new EmiStackInteraction(EmiStack.of(item.copyWithCount(1)), null, false);
            }
            FluidStack fluid = screen.getFluidUnderMouse(x, y);
            if (fluid != null && !fluid.isEmpty()) {
                return new EmiStackInteraction(NeoForgeEmiStack.of(fluid), null, false);
            }
            return EmiStackInteraction.EMPTY;
        });
        registry.addRecipeHandler(StorageMenu.TYPE, new StorageRecipeHandler());
    }
}
