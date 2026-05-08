package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingDiffRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.EightToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.FourToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> DF = DeferredRegister.create(
        Registries.RECIPE_SERIALIZER,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCrushRecipe>> BLOCK_CRUSH = DF.register(
        "block_crush",
        () -> BlockCrushRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemCrushRecipe>> ITEM_CRUSH = DF.register(
        "item_crush",
        () -> ItemCrushRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UnpackRecipe>> UNPACK = DF.register(
        "unpack",
        () -> UnpackRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCompressRecipe>> BLOCK_COMPRESS = DF.register(
        "block_compress",
        () -> BlockCompressRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockSmearRecipe>> BLOCK_SMEAR = DF.register(
        "block_smear",
        () -> BlockSmearRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemCompressRecipe>> ITEM_COMPRESS = DF.register(
        "item_compress",
        () -> ItemCompressRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StampingRecipe>> STAMPING = DF.register(
        "stamping",
        () -> StampingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StampingDiffRecipe>> STAMPING_DIFF = DF.register(
        "stamping_diff",
        () -> StampingDiffRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SuperHeatingRecipe>> SUPER_HEATING = DF.register(
        "super_heating",
        () -> SuperHeatingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemInjectRecipe>> ITEM_INJECT = DF.register(
        "item_inject",
        () -> ItemInjectRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MassInjectRecipe>> MASS_INJECT = DF.register(
        "mass_inject",
        () -> MassInjectRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SqueezingRecipe>> SQUEEZING = DF.register(
        "squeezing",
        () -> SqueezingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CookingRecipe>> COOKING = DF.register(
        "cooking",
        () -> CookingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BoilingRecipe>> BOILING = DF.register(
        "boiling",
        () -> BoilingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BulgingRecipe>> BULGING = DF.register(
        "bulging",
        () -> BulgingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NeutronIrradiationRecipe>> NEUTRON_IRRADIATION = DF.register(
        "neutron_irradiation",
        () -> NeutronIrradiationRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TimeWarpRecipe>> TIME_WARP = DF.register(
        "time_warp",
        () -> TimeWarpRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MeshRecipe>> MESH = DF.register(
        "mesh",
        () -> MeshRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MobTransformRecipe>> MOB_TRANSFORM = DF.register(
        "mob_transform",
        () -> MobTransformRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MobTransformWithItemRecipe>> MOB_TRANSFORM_WITH_ITEM = DF
        .register("mob_transform_with_item", () -> MobTransformWithItemRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MultiblockRecipe>> MULTIBLOCK = DF.register(
        "multiblock",
        () -> MultiblockRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MultiblockConversionRecipe>> MULTIBLOCK_CONVERSION = DF
        .register("multiblock_conversion", () -> MultiblockConversionRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MineralFountainRecipe>> MINERAL_FOUNTAIN = DF.register(
        "mineral_fountain",
        () -> MineralFountainRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MineralFountainChanceRecipe>> MINERAL_FOUNTAIN_CHANCE = DF
        .register("mineral_fountain_chance", () -> MineralFountainChanceRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<JewelCraftingRecipe>> JEWEL_CRAFTING = DF.register(
        "jewel_crafting",
        () -> JewelCraftingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CanningFoodRecipe>> CANNING_FOOD = DF.register(
        "canning_food",
        () -> CanningFoodRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PillRecipe>> PILL = DF.register(
        "pill_recipe",
        () -> PillRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChargerChargingRecipe>> CHARGER_CHARGING = DF.register(
        "charger_charging",
        () -> ChargerChargingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TwoToOneSmithingRecipe>> _221 = DF.register(
        "two_to_one_smithing",
        () -> TwoToOneSmithingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FourToOneSmithingRecipe>> _421 = DF.register(
        "four_to_one_smithing",
        () -> FourToOneSmithingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EightToOneSmithingRecipe>> _821 = DF.register(
        "eight_to_one_smithing",
        () -> EightToOneSmithingRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PermutationRecipe>> PERMUTATION = DF.register(
        "permutation",
        () -> PermutationRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DeformationRecipe>> DEFORMATION = DF.register(
        "deformation",
        () -> DeformationRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnergyWeaponMakeRecipe>> ENERGY_WEAPON_MAKE = DF.register(
        "energy_weapon_make",
        () -> EnergyWeaponMakeRecipe.SERIALIZER
    );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnvilCollisionCraftRecipe>> ANVIL_COLLISION_CRAFT = DF
        .register("anvil_collision", () -> AnvilCollisionCraftRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PortalConversionRecipe>> PORTAL_CONVERSION = DF.register(
        "portal_conversion",
        () -> PortalConversionRecipe.SERIALIZER
    );

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
