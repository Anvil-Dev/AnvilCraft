package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;

/** Matches seed items against special celestial-body recipes. */
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

    public static @Nullable Result match(
        ServerLevel level,
        int time,
        int space,
        int mass,
        int energy,
        Item seedItem
    ) {
        for (RecipeHolder<SpecialCelestialBodyRecipe> holder : level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get())) {
            SpecialCelestialBodyRecipe recipe = holder.value();
            if (recipe.time() != time
                || recipe.space() != space
                || recipe.mass() != mass
                || recipe.energy() != energy
                || !recipe.isEffectiveSeedItem(seedItem, level.getSeed())) {
                continue;
            }
            SpecialCelestialBodyData body = SpecialCelestialBodyData.fromRecipe(
                recipe,
                holder.id().toString()
            );
            return new Result(body, recipe.generateResources());
        }
        return null;
    }

    public record Result(SpecialCelestialBodyData body, PlanetaryResourceSet resources) {
    }
}
