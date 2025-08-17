package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
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
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.EightToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.FourToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilCraft.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<InWorldRecipe>> IN_WORLD_RECIPE =
        registerType("in_world_recipe");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InWorldRecipe>> IN_WORLD_RECIPE_SERIALIZER =
        RECIPE_SERIALIZERS.register("in_world_recipe", InWorldRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockCrushRecipe>> BLOCK_CRUSH_TYPE =
        registerType("block_crush");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCrushRecipe>> BLOCK_CRUSH_SERIALIZER =
        RECIPE_SERIALIZERS.register("block_crush", BlockCrushRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemCrushRecipe>> ITEM_CRUSH_TYPE =
        registerType("item_crush");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemCrushRecipe>> ITEM_CRUSH_SERIALIZERS =
        RECIPE_SERIALIZERS.register("item_crush", ItemCrushRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<UnpackRecipe>> UNPACK_TYPE =
        registerType("unpack");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UnpackRecipe>> UNPACK_SERIALIZERS =
        RECIPE_SERIALIZERS.register("unpack", UnpackRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockCompressRecipe>> BLOCK_COMPRESS_TYPE =
        registerType("block_compress");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCompressRecipe>>
        BLOCK_COMPRESS_SERIALIZER =
        RECIPE_SERIALIZERS.register("block_compress", BlockCompressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockSmearRecipe>> BLOCK_SMEAR_TYPE =
        registerType("block_smear");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockSmearRecipe>>
        BLOCK_SMEAR_SERIALIZER =
        RECIPE_SERIALIZERS.register("block_smear", BlockSmearRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemCompressRecipe>> ITEM_COMPRESS_TYPE =
        registerType("item_compress");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemCompressRecipe>>
        ITEM_COMPRESS_SERIALIZER = RECIPE_SERIALIZERS.register("item_compress", ItemCompressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<StampingRecipe>> STAMPING_TYPE =
        registerType("stamping");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StampingRecipe>> STAMPING_SERIALIZER =
        RECIPE_SERIALIZERS.register("stamping", StampingRecipe.Serializer::new);


    public static final DeferredHolder<RecipeType<?>, RecipeType<StampingUniqueItemsRecipe>> STAMPING_UNIQUE_ITEMS_TYPE =
        registerType("stamping_unique_items");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StampingUniqueItemsRecipe>> STAMPING_UNIQUE_ITEMS_SERIALIZER =
        RECIPE_SERIALIZERS.register("stamping_unique_items", StampingUniqueItemsRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SuperHeatingRecipe>> SUPER_HEATING_TYPE =
        registerType("super_heating");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SuperHeatingRecipe>>
        SUPER_HEATING_SERIALIZER = RECIPE_SERIALIZERS.register("super_heating", SuperHeatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemInjectRecipe>> ITEM_INJECT_TYPE =
        registerType("item_inject");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemInjectRecipe>> ITEM_INJECT_SERIALIZER =
        RECIPE_SERIALIZERS.register("item_inject", ItemInjectRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MassInjectRecipe>> MASS_INJECT_TYPE =
        registerType("mass_inject");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MassInjectRecipe>> MASS_INJECT_SERIALIZER =
        RECIPE_SERIALIZERS.register("mass_inject", MassInjectRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SqueezingRecipe>> SQUEEZING_TYPE =
        registerType("squeezing");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SqueezingRecipe>> SQUEEZING_SERIALIZER =
        RECIPE_SERIALIZERS.register("squeezing", SqueezingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingRecipe>> COOKING_TYPE = registerType("cooking");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CookingRecipe>> COOKING_SERIALIZER =
        RECIPE_SERIALIZERS.register("cooking", CookingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BoilingRecipe>> BOILING_TYPE = registerType("boiling");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BoilingRecipe>> BOILING_SERIALIZER =
        RECIPE_SERIALIZERS.register("boiling", BoilingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BulgingRecipe>> BULGING_TYPE = registerType("bulging");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BulgingRecipe>> BULGING_SERIALIZER =
        RECIPE_SERIALIZERS.register("bulging", BulgingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<TimeWarpRecipe>> TIME_WARP_TYPE =
        registerType("time_warp");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TimeWarpRecipe>> TIME_WARP_SERIALIZER =
        RECIPE_SERIALIZERS.register("time_warp", TimeWarpRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MeshRecipe>> MESH_TYPE = registerType("mesh");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MeshRecipe>> MESH_SERIALIZER =
        RECIPE_SERIALIZERS.register("mesh", MeshRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MobTransformRecipe>> MOB_TRANSFORM_TYPE =
        registerType("mob_transform");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MobTransformRecipe>>
        MOB_TRANSFORM_SERIALIZER = RECIPE_SERIALIZERS.register("mob_transform", MobTransformRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<MobTransformWithItemRecipe>> MOB_TRANSFORM_WITH_ITEM_TYPE =
        registerType("mob_transform_with_item");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MobTransformWithItemRecipe>>
        MOB_TRANSFORM_WITH_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("mob_transform_with_item", MobTransformWithItemRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MultiblockRecipe>> MULTIBLOCK_TYPE =
        registerType("multiblock");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MultiblockConversionRecipe>> MULTIBLOCK_CONVERSION_TYPE =
        registerType("multiblock_conversion");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MultiblockRecipe>> MULTIBLOCK_SERIALIZER =
        RECIPE_SERIALIZERS.register("multiblock", MultiblockRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MultiblockConversionRecipe>>
        MULTIBLOCK_CONVERSION_SERIALIZER =
        RECIPE_SERIALIZERS.register("multiblock_conversion", MultiblockConversionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MineralFountainRecipe>> MINERAL_FOUNTAIN =
        registerType("mineral_fountain");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MineralFountainRecipe>>
        MINERAL_FOUNTAIN_SERIALIZER =
        RECIPE_SERIALIZERS.register("mineral_fountain", MineralFountainRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MineralFountainChanceRecipe>> MINERAL_FOUNTAIN_CHANCE =
        registerType("mineral_fountain_chance");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MineralFountainChanceRecipe>>
        MINERAL_FOUNTAIN_CHANCE_SERIALIZER =
        RECIPE_SERIALIZERS.register("mineral_fountain_chance", MineralFountainChanceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<JewelCraftingRecipe>> JEWEL_CRAFTING_TYPE = registerType("jewel_crafting");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<JewelCraftingRecipe>> JEWEL_CRAFTING_SERIALIZER =
        RECIPE_SERIALIZERS.register("jewel_crafting", JewelCraftingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CanningFoodRecipe>> CANNING_FOOD_SERIALIZER =
        RECIPE_SERIALIZERS.register("canning_food", () -> new SimpleCraftingRecipeSerializer<>(CanningFoodRecipe::new));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ChargerChargingRecipe>> CHARGER_CHARGING_TYPE =
        registerType("charger_charging");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChargerChargingRecipe>> CHARGER_CHARGING_SERIALIZER =
        RECIPE_SERIALIZERS.register("charger_charging", ChargerChargingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BaseMultipleToOneSmithingRecipe>> MULTIPLE_TO_ONE_SMITHING_TYPE =
        registerType("multiple_to_one_smithing");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<? extends TwoToOneSmithingRecipe>>
        TWO_TO_ONE_SMITHING_SERIALIZER =
        RECIPE_SERIALIZERS.register("two_to_one_smithing", TwoToOneSmithingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<? extends FourToOneSmithingRecipe>>
        FOUR_TO_ONE_SMITHING_SERIALIZER =
        RECIPE_SERIALIZERS.register("four_to_one_smithing", FourToOneSmithingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<? extends EightToOneSmithingRecipe>>
        EIGHT_TO_ONE_SMITHING_SERIALIZER =
        RECIPE_SERIALIZERS.register("eight_to_one_smithing", EightToOneSmithingRecipe.Serializer::new);

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String name) {
        return RECIPE_TYPES.register(name, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return AnvilCraft.of(name).toString();
            }
        });
    }

    public static final DeferredHolder<RecipeType<?>, RecipeType<AnvilCollisionCraftRecipe>> ANVIL_COLLISION_CRAFT =
        registerType("anvil_collision");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnvilCollisionCraftRecipe>> ANVIL_COLLISION_CRAFT_SERIALIZER =
        RECIPE_SERIALIZERS.register("anvil_collision", AnvilCollisionCraftRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
