package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class DatapackSyncEventListener {
    @SubscribeEvent
    public static void on(OnDatapackSyncEvent event) {
        event.sendRecipes(
            RecipeType.CRAFTING,
            RecipeType.STONECUTTING,
            ModRecipeTypes.ANVIL_COLLISION_CRAFT.get(),
            ModRecipeTypes.BLOCK_COMPRESS.get(),
            ModRecipeTypes.BLOCK_CRUSH.get(),
            ModRecipeTypes.BLOCK_SMEAR.get(),
            ModRecipeTypes.BOILING.get(),
            ModRecipeTypes.BULGING.get(),
            ModRecipeTypes.CHARGER_CHARGING.get(),
            ModRecipeTypes.COOKING.get(),
            ModRecipeTypes.DEFORMATION.get(),
            ModRecipeTypes.ENERGY_WEAPON_MAKE.get(),
            ModRecipeTypes.ITEM_COMPRESS.get(),
            ModRecipeTypes.ITEM_CRUSH.get(),
            ModRecipeTypes.ITEM_INJECT.get(),
            ModRecipeTypes.JEWEL_CRAFTING.get(),
            ModRecipeTypes.MASS_INJECT.get(),
            ModRecipeTypes.MESH.get(),
            ModRecipeTypes.MINERAL_FOUNTAIN.get(),
            ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get(),
            ModRecipeTypes.MOB_TRANSFORM.get(),
            ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM.get(),
            ModRecipeTypes.MULTIBLOCK.get(),
            ModRecipeTypes.MULTIBLOCK_CONVERSION.get(),
            ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get(),
            ModRecipeTypes.NEUTRON_IRRADIATION.get(),
            ModRecipeTypes.PERMUTATION.get(),
            ModRecipeTypes.PORTAL_CONVERSION.get(),
            ModRecipeTypes.STAMPING.get(),
            ModRecipeTypes.STAMPING_DIFF.get(),
            ModRecipeTypes.SQUEEZING.get(),
            ModRecipeTypes.SUPER_HEATING.get(),
            ModRecipeTypes.TIME_WARP.get(),
            ModRecipeTypes.UNPACK.get()
        );
    }
}
