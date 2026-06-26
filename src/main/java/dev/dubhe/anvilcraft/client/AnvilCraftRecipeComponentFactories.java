package dev.dubhe.anvilcraft.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDAnvilCollisionCraftRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDChargerChargingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDJewelCraftingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDMultipleToOneSmithingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDPortalConversionRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDProceduralProcessRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBlockCompressRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBlockCrushRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBlockProcessingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBlockSmearRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBoilingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDBulgingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDCookingRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDItemCompressRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDItemCrushRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDItemInjectRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDMeshRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.anvil.MDNeutronIrradiationRecipeComponent;
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
            ModRecipeTypes.MESH_TYPE.get(),
            MDMeshRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BOILING = RECIPE_COMPONENT_FACTORIES.register(
        "boiling", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BOILING_TYPE.get(),
            MDBoilingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BULGING = RECIPE_COMPONENT_FACTORIES.register(
        "bulging", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BULGING_TYPE.get(),
            MDBulgingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        COOKING = RECIPE_COMPONENT_FACTORIES.register(
        "cooking", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.COOKING_TYPE.get(),
            MDCookingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ITEM_COMPRESS = RECIPE_COMPONENT_FACTORIES.register(
        "item_compress", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ITEM_COMPRESS_TYPE.get(),
            MDItemCompressRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ITEM_CRUSH = RECIPE_COMPONENT_FACTORIES.register(
        "item_crush", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ITEM_CRUSH_TYPE.get(),
            MDItemCrushRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        NEUTRON_IRRADIATION = RECIPE_COMPONENT_FACTORIES.register(
        "neutron_irradiation", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.NEUTRON_IRRADIATION_TYPE.get(),
            MDNeutronIrradiationRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        STAMPING = RECIPE_COMPONENT_FACTORIES.register(
        "stamping", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.STAMPING_TYPE.get(),
            MDStampingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        TIME_WARP = RECIPE_COMPONENT_FACTORIES.register(
        "time_warp", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.TIME_WARP_TYPE.get(),
            MDTimeWarpRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        SUPER_HEATING = RECIPE_COMPONENT_FACTORIES.register(
        "super_heating", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.SUPER_HEATING_TYPE.get(),
            MDSuperHeatingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        UNPACK = RECIPE_COMPONENT_FACTORIES.register(
        "unpack", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.UNPACK_TYPE.get(),
            MDUnpackRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        SQUEEZING = RECIPE_COMPONENT_FACTORIES.register(
        "squeezing", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.SQUEEZING_TYPE.get(),
            MDSqueezingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BLOCK_COMPRESS = RECIPE_COMPONENT_FACTORIES.register(
        "block_compress", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BLOCK_COMPRESS_TYPE.get(),
            MDBlockCompressRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BLOCK_CRUSH = RECIPE_COMPONENT_FACTORIES.register(
        "block_crush", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BLOCK_CRUSH_TYPE.get(),
            MDBlockCrushRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BLOCK_SMEAR = RECIPE_COMPONENT_FACTORIES.register(
        "block_smear", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BLOCK_SMEAR_TYPE.get(),
            MDBlockSmearRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        BLOCK_PROCESSING = RECIPE_COMPONENT_FACTORIES.register(
        "block_processing", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.BLOCK_PROCESSING_TYPE.get(),
            MDBlockProcessingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ITEM_INJECT = RECIPE_COMPONENT_FACTORIES.register(
        "item_inject", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.ITEM_INJECT_TYPE.get(),
            MDItemInjectRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        JEWEL_CRAFTING = RECIPE_COMPONENT_FACTORIES.register(
        "jewelcrafting", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.JEWEL_CRAFTING_TYPE.get(),
            MDJewelCraftingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        ANVIL_CRAFTING = RECIPE_COMPONENT_FACTORIES.register(
        "multiple_to_one_smithing", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get(),
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
            ModRecipeTypes.CHARGER_CHARGING_TYPE.get(),
            MDChargerChargingRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        PORTAL_CONVERSION = RECIPE_COMPONENT_FACTORIES.register(
        "portal_conversion", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.PORTAL_CONVERSION_TYPE.get(),
            MDPortalConversionRecipeComponent::new
        )
    );

    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>>
        PROCEDURAL_PROCESS = RECIPE_COMPONENT_FACTORIES.register(
        "procedural_process", () -> MDRecipeComponent.RecipeComponentFactory.create(
            ModRecipeTypes.PROCEDURAL_PROCESS.get(),
            MDProceduralProcessRecipeComponent::new
        )
    );

    private AnvilCraftRecipeComponentFactories() {
    }
}
