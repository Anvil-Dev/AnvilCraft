package dev.dubhe.anvilcraft.event.anvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.GiantAnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class GiantAnvilMultiblockCraftingEventListener {
    /**
     * 侦听大铁砧铁砧击中多方块合成事件
     *
     * @param event 铁砧落地事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onLand(@NotNull GiantAnvilFallOnLandEvent event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos().below();
        AnvilCraftingContext context = new AnvilCraftingContext(level, pos, event.getEntity());
        Optional<AnvilRecipe> optional = AnvilRecipeManager.getAnvilRecipeList().stream()
                .filter(recipe -> recipe.getAnvilRecipeType().equals(AnvilRecipeType.MULTIBLOCK_CRAFTING)
                && recipe.matches(context, level))
                .findFirst();
        optional.ifPresent(anvilRecipe -> anvilProcess(anvilRecipe, context));
    }

    private void anvilProcess(AnvilRecipe recipe, AnvilCraftingContext context) {
        int counts = 0;
        while (counts < AnvilCraft.config.anvilEfficiency) {
            if (!recipe.craft(context)) break;
            counts++;
        }
    }
}
