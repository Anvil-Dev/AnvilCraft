package dev.dubhe.anvilcraft.event.giantanvil;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.GiantAnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
        BlockPos craftingTablePos = pos.below();
        for (int dx = -1; dx < 1; dx++) {
            for (int dz = -1; dz < 1; dz++) {
                BlockState bs = level.getBlockState(craftingTablePos.offset(dx, 0, dz));
                if (!bs.is(ModBlockTags.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) return;
            }
        }
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
            if (!recipe.craft(context.clearData())) break;
            counts++;
        }
        context.spawnItemEntity();
    }
}
