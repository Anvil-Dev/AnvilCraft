package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * 使用种子物品匹配隐藏天体及其资源。
 */
public final class CelestialSeedMatcher {
    private CelestialSeedMatcher() {
    }

    public static @Nullable SpecialCelestialBodyData fromPlayerHead(ItemStack stack, int space) {
        if (!stack.is(Items.PLAYER_HEAD)) return null;
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return null;
        CompoundTag profileTag = (CompoundTag) ResolvableProfile.CODEC
            .encodeStart(NbtOps.INSTANCE, profile)
            .getOrThrow();
        return SpecialCelestialBodyData.fromPlayerHead(profileTag, space);
    }

    /** 查找首个同时匹配砧子参数和世界种子物品的特殊天体配方。 */
    public static @Nullable Result match(
        ServerLevel level,
        int time,
        int space,
        int mass,
        int energy,
        Item seedItem
    ) {
        Collection<RecipeHolder<SpecialCelestialBodyRecipe>> recipes = RecipesRecord.getRecipes(level)
            .byType(ModRecipeTypes.SPECIAL_CELESTIAL_BODY.get());
        for (RecipeHolder<SpecialCelestialBodyRecipe> holder : recipes) {
            SpecialCelestialBodyRecipe recipe = holder.value();
            if (recipe.time() == time
                && recipe.space() == space
                && recipe.mass() == mass
                && recipe.energy() == energy
                && recipe.isEffectiveSeedItem(seedItem, level.getSeed())) {
                SpecialCelestialBodyData body = SpecialCelestialBodyData.fromRecipe(
                    recipe, holder.id().identifier().toString()
                );
                return new Result(body, recipe.generateResources());
            }
        }
        return null;
    }

    /** 同一配方匹配得到的天体和资源。 */
    public record Result(SpecialCelestialBodyData body, PlanetaryResourceSet resources) {
    }
}
