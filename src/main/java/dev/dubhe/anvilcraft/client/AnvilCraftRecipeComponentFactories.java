package dev.dubhe.anvilcraft.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDAnvilCollisionCraftRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDChargerChargingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDEnergyWeaponMakeRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDJewelCraftingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDMultipleToOneSmithingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDPortalConversionRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBlockCompressRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBlockCrushRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDFastCookingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDItemCompressRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDItemCrushRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDItemInjectRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDMeshRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDNeutronIrradiationRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDSolidLiquidRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDSqueezingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDStampingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDSuperHeatingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDTimeWarpRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDUnpackRecipeComponent;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class AnvilCraftRecipeComponentFactories {
    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORIES = DeferredRegister.create(AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY_KEY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        MESH = RECIPE_COMPONENT_FACTORIES.register(
        "mesh", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.MESH.get(),
            MDMeshRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        SOLID_LIQUID = RECIPE_COMPONENT_FACTORIES.register(
        "solid_liquid", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.SOLID_LIQUID.get(),
            MDSolidLiquidRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        FAST_COOKING = RECIPE_COMPONENT_FACTORIES.register(
        "fast_cooking", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.FAST_COOKING.get(),
            MDFastCookingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ITEM_COMPRESS = RECIPE_COMPONENT_FACTORIES.register(
        "item_compress", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ITEM_COMPRESS.get(),
            MDItemCompressRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ITEM_CRUSH = RECIPE_COMPONENT_FACTORIES.register(
        "item_crush", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ITEM_CRUSH.get(),
            MDItemCrushRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        NEUTRON_IRRADIATION = RECIPE_COMPONENT_FACTORIES.register(
        "neutron_irradiation", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.NEUTRON_IRRADIATION.get(),
            MDNeutronIrradiationRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        STAMPING = RECIPE_COMPONENT_FACTORIES.register(
        "stamping", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.STAMPING.get(),
            MDStampingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        TIME_WARP = RECIPE_COMPONENT_FACTORIES.register(
        "time_warp", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.TIME_WARP.get(),
            MDTimeWarpRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        SUPER_HEATING = RECIPE_COMPONENT_FACTORIES.register(
        "super_heating", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.SUPER_HEATING.get(),
            MDSuperHeatingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        UNPACK = RECIPE_COMPONENT_FACTORIES.register(
        "unpack", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.UNPACK.get(),
            MDUnpackRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        SQUEEZING = RECIPE_COMPONENT_FACTORIES.register(
        "squeezing", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.SQUEEZING.get(),
            MDSqueezingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BLOCK_COMPRESS = RECIPE_COMPONENT_FACTORIES.register(
        "block_compress", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BLOCK_COMPRESS.get(),
            MDBlockCompressRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BLOCK_CRUSH = RECIPE_COMPONENT_FACTORIES.register(
        "block_crush", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BLOCK_CRUSH.get(),
            MDBlockCrushRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ITEM_INJECT = RECIPE_COMPONENT_FACTORIES.register(
        "item_inject", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ITEM_INJECT.get(),
            MDItemInjectRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        JEWEL_CRAFTING = RECIPE_COMPONENT_FACTORIES.register(
        "jewelcrafting", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.JEWEL_CRAFTING.get(),
            MDJewelCraftingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ANVIL_CRAFTING = RECIPE_COMPONENT_FACTORIES.register(
        "multiple_to_one_smithing", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get(),
            MDMultipleToOneSmithingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ANVIL_COLLISION_CRAFTING = RECIPE_COMPONENT_FACTORIES.register(
        "anvil_collision", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ANVIL_COLLISION_CRAFT.get(),
            MDAnvilCollisionCraftRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        CHARGER_CHARGING = RECIPE_COMPONENT_FACTORIES.register(
        "charger_charging", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.CHARGER_CHARGING.get(),
            MDChargerChargingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        PORTAL_CONVERSION = RECIPE_COMPONENT_FACTORIES.register(
        "portal_conversion", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.PORTAL_CONVERSION.get(),
            MDPortalConversionRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ENERGY_WEAPON_MAKE = RECIPE_COMPONENT_FACTORIES.register(
        "energy_weapon_make", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ENERGY_WEAPON_MAKE.get(),
            MDEnergyWeaponMakeRecipeComponent::new
        )
    );

    private AnvilCraftRecipeComponentFactories() {
    }
}
