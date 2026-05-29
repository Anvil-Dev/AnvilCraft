package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeManagerEvent;
import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.VanillaRecipesWrap;
import dev.dubhe.anvilcraft.recipe.generate.MeshRecipeGeneratingCache;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class InWorldRecipeEventListener {
    @SubscribeEvent
    public static void inWorldRecipe(InWorldRecipeManagerEvent.Init event) {
        RecipeManager manager = event.getRecipeManager();
        List<RecipeHolder<InWorldRecipe>> init = VanillaRecipesWrap.init(
            manager.anvillib$getRegistries(),
            manager.getRecipes()
        );
        new MeshRecipeGeneratingCache(manager.anvillib$getRegistries())
            .buildRecipes()
            .ifPresent(recipeHolders -> {
                for (RecipeHolder<MeshRecipe> holder : recipeHolders) {
                    init.add(new RecipeHolder<>(holder.id(), holder.value()));
                }
            });
        manager.anvillib$addRecipes(init);
    }

    @SubscribeEvent
    public static void inWorldRecipe(InWorldRecipeEvent event) {
        RecipeType<? extends InWorldRecipe> recipeType = event.getRecipeType();
        ResourceLocation id = event.getId();
        InWorldRecipeContext context = event.getContext();
        ServerLevel level = context.getLevel();
        BlockPos pos = BlockPos.containing(context.getPos());
        TriggerUtil.inWorldRecipe(level, pos, ResourceLocation.parse(recipeType.toString()), id);
    }

    @SubscribeEvent
    public static void spawnItemEntity(ItemCacheEvent.SpawnItemEntity event) {
        ItemEntity entity = event.getEntity();
        entity.anvilcraft$setIsAdsorbable(false);
        entity.level().getBlockEntity(entity.blockPosition(), ModBlockEntities.FISH_TANK.get())
            .ifPresent(be -> be.getOutput().setStackInSlot(0, be.getOutput().getStackInSlot(0)));
    }
}
