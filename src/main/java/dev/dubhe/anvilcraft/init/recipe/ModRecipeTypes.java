package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.StampingUniqueItemsRecipe;
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
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> DF = DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockCrushRecipe>> BLOCK_CRUSH = registerType("block_crush");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemCrushRecipe>> ITEM_CRUSH = registerType("item_crush");
    public static final DeferredHolder<RecipeType<?>, RecipeType<UnpackRecipe>> UNPACK = registerType("unpack");
    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockCompressRecipe>> BLOCK_COMPRESS = registerType("block_compress");
    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockSmearRecipe>> BLOCK_SMEAR = registerType("block_smear");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemCompressRecipe>> ITEM_COMPRESS = registerType("item_compress");
    public static final DeferredHolder<RecipeType<?>, RecipeType<StampingRecipe>> STAMPING = registerType("stamping");
    public static final DeferredHolder<RecipeType<?>, RecipeType<StampingUniqueItemsRecipe>> STAMPING_UNIQUE_ITEMS = registerType(
        "stamping_unique_items"
    );

    public static final DeferredHolder<RecipeType<?>, RecipeType<SuperHeatingRecipe>> SUPER_HEATING = registerType("super_heating");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemInjectRecipe>> ITEM_INJECT = registerType("item_inject");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MassInjectRecipe>> MASS_INJECT = registerType("mass_inject");
    public static final DeferredHolder<RecipeType<?>, RecipeType<SqueezingRecipe>> SQUEEZING = registerType("squeezing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingRecipe>> COOKING = registerType("cooking");
    public static final DeferredHolder<RecipeType<?>, RecipeType<BoilingRecipe>> BOILING = registerType("boiling");
    public static final DeferredHolder<RecipeType<?>, RecipeType<BulgingRecipe>> BULGING = registerType("bulging");
    public static final DeferredHolder<RecipeType<?>, RecipeType<NeutronIrradiationRecipe>> NEUTRON_IRRADIATION = registerType(
        "neutron_irradiation"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<TimeWarpRecipe>> TIME_WARP = registerType("time_warp");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MeshRecipe>> MESH = registerType("mesh");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MobTransformRecipe>> MOB_TRANSFORM = registerType("mob_transform");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MobTransformWithItemRecipe>> MOB_TRANSFORM_WITH_ITEM = registerType(
        "mob_transform_with_item"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<MultiblockRecipe>> MULTIBLOCK = registerType("multiblock");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MultiblockConversionRecipe>> MULTIBLOCK_CONVERSION = registerType(
        "multiblock_conversion"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<MineralFountainRecipe>> MINERAL_FOUNTAIN = registerType(
        "mineral_fountain"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<MineralFountainChanceRecipe>> MINERAL_FOUNTAIN_CHANCE = registerType(
        "mineral_fountain_chance"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<JewelCraftingRecipe>> JEWEL_CRAFTING = registerType("jewel_crafting");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ChargerChargingRecipe>> CHARGER_CHARGING = registerType(
        "charger_charging"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<BaseMultipleToOneSmithingRecipe>> MULTIPLE_TO_ONE_SMITHING = registerType(
        "multiple_to_one_smithing"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<PermutationRecipe>> PERMUTATION = registerType("permutation");
    public static final DeferredHolder<RecipeType<?>, RecipeType<DeformationRecipe>> DEFORMATION = registerType("deformation");
    public static final DeferredHolder<RecipeType<?>, RecipeType<EnergyWeaponMakeRecipe>> ENERGY_WEAPON_MAKE = registerType(
        "energy_weapon_make"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<AnvilCollisionCraftRecipe>> ANVIL_COLLISION_CRAFT = registerType(
        "anvil_collision"
    );
    public static final DeferredHolder<RecipeType<?>, RecipeType<PortalConversionRecipe>> PORTAL_CONVERSION = registerType(
        "portal_conversion"
    );

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String name) {
        return DF.register(name, RecipeType::simple);
    }

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
