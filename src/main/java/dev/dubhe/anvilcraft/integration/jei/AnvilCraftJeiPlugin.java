package dev.dubhe.anvilcraft.integration.jei;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.util.Lazy;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.BaseChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.BatchCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ControlValveScreen;
import dev.dubhe.anvilcraft.client.gui.screen.FilterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemDetectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.JewelCraftingScreen;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.category.AnvilCollisionCraftCategory;
import dev.dubhe.anvilcraft.integration.jei.category.BeaconConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.ChargerChargingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.JewelCraftingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.MobTransformCategory;
import dev.dubhe.anvilcraft.integration.jei.category.MobTransformWithItemCategory;
import dev.dubhe.anvilcraft.integration.jei.category.MultipleToOneSmithingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.PortalConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.ProceduralProcessCategory;
import dev.dubhe.anvilcraft.integration.jei.category.TranscendiumRecipeCategory;
import dev.dubhe.anvilcraft.integration.jei.category.VoidDecayCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockCompressCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockCrushCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockSmearCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BoilingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BulgingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.CementStainingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ConcreteCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.CookingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemCompressCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemCrushCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemInjectCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.MassInjectCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.MeshRecipeCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.NeutronIrradiationCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.SqueezingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.StampingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.SuperHeatingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.TimeWarpCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.UnpackCategory;
import dev.dubhe.anvilcraft.integration.jei.category.extension.CanningFoodExtension;
import dev.dubhe.anvilcraft.integration.jei.category.extension.PillRecipeExtension;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlockConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlockCraftingCategory;
import dev.dubhe.anvilcraft.integration.jei.handlers.GhostIngredientHandler;
import dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.CementStainingRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.ColoredConcreteRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.MeshRecipeGroup;
import dev.dubhe.anvilcraft.integration.jei.recipe.TranscendiumRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.VoidDecayRecipe;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BaseStampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

@JeiPlugin
public class AnvilCraftJeiPlugin implements IModPlugin {
    public static final Lazy<ImmutableList<ItemLike>> ANVIL_PROCESSING_CATALYSTS = new Lazy<>(() -> ImmutableList.of(
        Items.ANVIL,
        ModBlocks.ROYAL_ANVIL,
        ModBlocks.EMBER_ANVIL,
        ModBlocks.FROST_ANVIL,
        ModBlocks.TRANSCENDENCE_ANVIL,
        ModBlocks.SPECTRAL_ANVIL,
        ModBlocks.GIANT_ANVIL,
        ModItems.ANVIL_HAMMER,
        ModItems.ROYAL_ANVIL_HAMMER,
        ModItems.EMBER_ANVIL_HAMMER,
        ModItems.TRANSCENDENCE_ANVIL_HAMMER
    ));
    public static final ImmutableList<ItemLike> CAULDRON_CATALYSTS = ImmutableList.of(
        Items.CAULDRON,
        ModBlocks.FISH_TANK
    );

    public static final IRecipeType<MeshRecipeGroup> MESH = createRecipeType("mesh", MeshRecipeGroup.class);
    public static final IRecipeType<CementStainingRecipe> CEMENT_STAINING =
        createRecipeType("cement_staining", CementStainingRecipe.class);
    public static final IRecipeType<ColoredConcreteRecipe> COLORED_CONCRETE =
        createRecipeType("colored_concrete", ColoredConcreteRecipe.class);
    public static final IRecipeType<BeaconConversionRecipe> BEACON_CONVERSION =
        createRecipeType("beacon_conversion", BeaconConversionRecipe.class);
    public static final IRecipeType<VoidDecayRecipe> VOID_DECAY =
        createRecipeType("void_decay", VoidDecayRecipe.class);
    public static final IRecipeType<TranscendiumRecipe> TRANSCENDIUM_RECIPE =
        createRecipeType("transcendium", TranscendiumRecipe.class);

    public static final IRecipeHolderType<BlockCompressRecipe> BLOCK_COMPRESS = createHolderType("block_compress");
    public static final IRecipeHolderType<BlockCrushRecipe> BLOCK_CRUSH = createHolderType("block_crush");
    public static final IRecipeHolderType<BlockSmearRecipe> BLOCK_SMEAR = createHolderType("block_smear");
    public static final IRecipeHolderType<ItemCrushRecipe> ITEM_CRUSH = createHolderType("item_crush");
    public static final IRecipeHolderType<ItemInjectRecipe> ITEM_INJECT = createHolderType("item_inject");
    public static final IRecipeHolderType<MassInjectRecipe> MASS_INJECT = createHolderType("mass_inject");
    public static final IRecipeHolderType<ItemCompressRecipe> ITEM_COMPRESS = createHolderType("item_compress");
    public static final IRecipeHolderType<UnpackRecipe> UNPACK = createHolderType("unpack");
    public static final IRecipeHolderType<CookingRecipe> COOKING = createHolderType("cooking");
    public static final IRecipeHolderType<BoilingRecipe> BOILING = createHolderType("boiling");
    public static final IRecipeHolderType<BaseStampingRecipe<?>> STAMPING = createHolderType("stamping");
    public static final IRecipeHolderType<SuperHeatingRecipe> SUPER_HEATING = createHolderType("super_heating");
    public static final IRecipeHolderType<SqueezingRecipe> SQUEEZING = createHolderType("squeezing");
    public static final IRecipeHolderType<BulgingRecipe> BULGING = createHolderType("bulging");
    public static final IRecipeHolderType<TimeWarpRecipe> TIME_WARP = createHolderType("time_warp");
    public static final IRecipeHolderType<NeutronIrradiationRecipe> NEUTRON_IRRADIATION = createHolderType("neutron_irradiation");

    public static final IRecipeHolderType<MultiblockRecipe> MULTIBLOCK_CRAFTING = createHolderType("multiblock");
    public static final IRecipeHolderType<MultiblockConversionRecipe> MULTIBLOCK_CONVERSION = createHolderType("multiblock_conversion");

    public static final IRecipeHolderType<JewelCraftingRecipe> JEWEL_CRAFTING = createHolderType("jewel_crafting");
    public static final IRecipeHolderType<ChargerChargingRecipe> CHARGER_CHARGING = createHolderType("charger_charging");
    public static final IRecipeHolderType<BaseMultipleToOneSmithingRecipe> MULTIPLE_TO_ONE_SMITHING = createHolderType(
        "multiple_to_one_smithing"
    );
    public static final IRecipeHolderType<PortalConversionRecipe> PORTAL_CONVERSION = createHolderType("portal_conversion");

    public static final IRecipeHolderType<MobTransformRecipe> MOB_TRANSFORM = createHolderType("mob_transform");
    public static final IRecipeHolderType<MobTransformWithItemRecipe> MOB_TRANSFORM_WITH_ITEM = createHolderType("mob_transform_with_item");

    public static final IRecipeHolderType<AnvilCollisionCraftRecipe> ANVIL_COLLISION = createHolderType("anvil_collision");
    public static final IRecipeHolderType<ProceduralProcessRecipe> PROCEDURAL_PROCESS = createHolderType("procedural_process");

    @Override
    public Identifier getPluginUid() {
        return AnvilCraft.of("jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MeshRecipeCategory.registerRecipes(registration);
        BlockCompressCategory.registerRecipes(registration);
        BlockCrushCategory.registerRecipes(registration);
        BlockSmearCategory.registerRecipes(registration);
        ItemCrushCategory.registerRecipes(registration);
        SqueezingCategory.registerRecipes(registration);
        ItemInjectCategory.registerRecipes(registration);
        MassInjectCategory.registerRecipes(registration);
        ItemCompressCategory.registerRecipes(registration);
        UnpackCategory.registerRecipes(registration);
        CookingCategory.registerRecipes(registration);
        BoilingCategory.registerRecipes(registration);
        StampingCategory.registerRecipes(registration);
        SuperHeatingCategory.registerRecipes(registration);
        CementStainingCategory.registerRecipes(registration);
        ConcreteCategory.registerRecipes(registration);
        BulgingCategory.registerRecipes(registration);
        TimeWarpCategory.registerRecipes(registration);
        NeutronIrradiationCategory.registerRecipes(registration);
        MultiBlockCraftingCategory.registerRecipes(registration);
        MultiBlockConversionCategory.registerRecipes(registration);
        JewelCraftingCategory.registerRecipes(registration);
        PortalConversionCategory.registerRecipes(registration);
        BeaconConversionCategory.registerRecipes(registration);
        VoidDecayCategory.registerRecipes(registration);
        ChargerChargingCategory.registerRecipes(registration);
        MultipleToOneSmithingCategory.registerRecipes(registration);
        MobTransformCategory.registerRecipes(registration);
        MobTransformWithItemCategory.registerRecipes(registration);
        AnvilCollisionCraftCategory.registerRecipes(registration);
        TranscendiumRecipeCategory.registerRecipes(registration);
        ProceduralProcessCategory.registerRecipes(registration);

        registration.addItemStackInfo(
            new ItemStack(ModItems.GEODE.get()),
            Component.translatable("jei.anvilcraft.info.geode_1"),
            Component.translatable("jei.anvilcraft.info.geode_2"),
            Component.translatable("jei.anvilcraft.info.geode_3"),
            Component.translatable("jei.anvilcraft.info.geode_4")
        );
        registration.addItemStackInfo(
            new ItemStack(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.get()),
            Component.translatable("jei.anvilcraft.info.royal_steel_upgrade_smithing_template_1"),
            Component.translatable("jei.anvilcraft.info.royal_steel_upgrade_smithing_template_2")
        );
        registration.addItemStackInfo(
            new ItemStack(ModItems.CRAB_CLAW.get()),
            Component.translatable("jei.anvilcraft.info.craw_claw")
        );
        registration.addItemStackInfo(
            new ItemStack(ModItems.CAPACITOR.get()),
            Component.translatable("jei.anvilcraft.info.capacitor")
        );
        registration.addItemStackInfo(
            ModBlocks.END_DUST.asStack(),
            Component.translatable("jei.anvilcraft.info.end_dust")
        );
        registration.addItemStackInfo(
            Items.ZOMBIE_SPAWN_EGG.getDefaultInstance(),
            Component.translatable("jei.anvilcraft.info.mob_transform_with_item")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        MeshRecipeCategory.registerRecipeCatalysts(registration);
        BlockCompressCategory.registerRecipeCatalysts(registration);
        BlockCrushCategory.registerRecipeCatalysts(registration);
        BlockSmearCategory.registerRecipeCatalysts(registration);
        ItemCrushCategory.registerRecipeCatalysts(registration);
        SqueezingCategory.registerRecipeCatalysts(registration);
        ItemInjectCategory.registerRecipeCatalysts(registration);
        MassInjectCategory.registerRecipeCatalysts(registration);
        ItemCompressCategory.registerRecipeCatalysts(registration);
        UnpackCategory.registerRecipeCatalysts(registration);
        CookingCategory.registerRecipeCatalysts(registration);
        BoilingCategory.registerRecipeCatalysts(registration);
        StampingCategory.registerRecipeCatalysts(registration);
        SuperHeatingCategory.registerRecipeCatalysts(registration);
        CementStainingCategory.registerRecipeCatalysts(registration);
        ConcreteCategory.registerRecipeCatalysts(registration);
        BulgingCategory.registerRecipeCatalysts(registration);
        TimeWarpCategory.registerRecipeCatalysts(registration);
        NeutronIrradiationCategory.registerRecipeCatalysts(registration);
        MultiBlockCraftingCategory.registerRecipeCatalysts(registration);
        MultiBlockConversionCategory.registerRecipeCatalysts(registration);
        JewelCraftingCategory.registerRecipeCatalysts(registration);
        PortalConversionCategory.registerRecipeCatalysts(registration);
        BeaconConversionCategory.registerRecipeCatalysts(registration);
        VoidDecayCategory.registerRecipeCatalysts(registration);
        ChargerChargingCategory.registerRecipeCatalysts(registration);
        MultipleToOneSmithingCategory.registerRecipeCatalysts(registration);
        MobTransformCategory.registerRecipeCatalysts(registration);
        MobTransformWithItemCategory.registerRecipeCatalysts(registration);
        AnvilCollisionCraftCategory.registerRecipeCatalysts(registration);
        TranscendiumRecipeCategory.registerRecipeCatalysts(registration);
        ProceduralProcessCategory.registerRecipeCatalysts(registration);

        registration.addCraftingStation(RecipeTypes.CRAFTING, new ItemStack(ModBlocks.BATCH_CRAFTER));

        registration.addCraftingStation(RecipeTypes.ANVIL, new ItemStack(ModBlocks.ROYAL_ANVIL));
        registration.addCraftingStation(RecipeTypes.ANVIL, new ItemStack(ModBlocks.EMBER_ANVIL));
        registration.addCraftingStation(RecipeTypes.ANVIL, new ItemStack(ModBlocks.GIANT_ANVIL));
        registration.addCraftingStation(RecipeTypes.ANVIL, new ItemStack(ModBlocks.SPECTRAL_ANVIL));

        registration.addCraftingStation(RecipeTypes.SMITHING, new ItemStack(ModBlocks.ROYAL_SMITHING_TABLE));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers jeiHelpers = registration.getJeiHelpers();
        IGuiHelper guiHelper = jeiHelpers.getGuiHelper();

        registration.addRecipeCategories(new MeshRecipeCategory(guiHelper));
        registration.addRecipeCategories(new BlockCompressCategory(guiHelper));
        registration.addRecipeCategories(new BlockCrushCategory(guiHelper));
        registration.addRecipeCategories(new BlockSmearCategory(guiHelper));
        registration.addRecipeCategories(new ItemCrushCategory(guiHelper));
        registration.addRecipeCategories(new SqueezingCategory(guiHelper));
        registration.addRecipeCategories(new ItemInjectCategory(guiHelper));
        registration.addRecipeCategories(new MassInjectCategory(guiHelper));
        registration.addRecipeCategories(new ItemCompressCategory(guiHelper));
        registration.addRecipeCategories(new UnpackCategory(guiHelper));
        registration.addRecipeCategories(new CookingCategory(guiHelper));
        registration.addRecipeCategories(new BoilingCategory(guiHelper));
        registration.addRecipeCategories(new StampingCategory(guiHelper));
        registration.addRecipeCategories(new SuperHeatingCategory(guiHelper));
        registration.addRecipeCategories(new CementStainingCategory(guiHelper));
        registration.addRecipeCategories(new ConcreteCategory(guiHelper));
        registration.addRecipeCategories(new BulgingCategory(guiHelper));
        registration.addRecipeCategories(new TimeWarpCategory(guiHelper));
        registration.addRecipeCategories(new NeutronIrradiationCategory(guiHelper));
        registration.addRecipeCategories(new MultiBlockCraftingCategory(guiHelper));
        registration.addRecipeCategories(new MultiBlockConversionCategory(guiHelper));
        registration.addRecipeCategories(new JewelCraftingCategory(guiHelper));
        registration.addRecipeCategories(new PortalConversionCategory(guiHelper));
        registration.addRecipeCategories(new BeaconConversionCategory(guiHelper));
        registration.addRecipeCategories(new VoidDecayCategory(guiHelper));
        registration.addRecipeCategories(new ChargerChargingCategory(guiHelper));
        registration.addRecipeCategories(new MultipleToOneSmithingCategory(guiHelper));
        registration.addRecipeCategories(new MobTransformCategory(guiHelper));
        registration.addRecipeCategories(new MobTransformWithItemCategory(guiHelper));
        registration.addRecipeCategories(new AnvilCollisionCraftCategory(guiHelper));
        registration.addRecipeCategories(new TranscendiumRecipeCategory(guiHelper));
        registration.addRecipeCategories(new ProceduralProcessCategory(guiHelper));
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            RoyalSmithingMenu.class,
            ModMenuTypes.ROYAL_SMITHING.get(),
            RecipeTypes.SMITHING,
            0, 3, 4, 36
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
            JewelCraftingScreen.class,
            100,
            53,
            30,
            13,
            JEWEL_CRAFTING
        );

        registration.addGhostIngredientHandler(
            FilterScreen.class,
            new GhostIngredientHandler<>()
        );
        registration.addGhostIngredientHandler(
            BaseChuteScreen.class,
            new GhostIngredientHandler<>()
        );
        registration.addGhostIngredientHandler(
            BatchCrafterScreen.class,
            new GhostIngredientHandler<>()
        );
        registration.addGhostIngredientHandler(
            ItemDetectorScreen.class,
            new GhostIngredientHandler<>()
        );
        registration.addGhostIngredientHandler(
            ItemCollectorScreen.class,
            new GhostIngredientHandler<>()
        );
        registration.addGhostIngredientHandler(
            ControlValveScreen.class,
            new GhostIngredientHandler<>()
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(CanningFoodRecipe.class, CanningFoodExtension.INSTANCE);
        registration.getCraftingCategory().addExtension(PillRecipe.class, new PillRecipeExtension());
    }

    public static <T> IRecipeType<T> createRecipeType(String name, Class<T> clazz) {
        return IRecipeType.create(AnvilCraft.of(name), clazz);
    }

    public static <R extends Recipe<?>> IRecipeHolderType<R> createHolderType(String name) {
        return IRecipeHolderType.create(AnvilCraft.of(name));
    }

    public static void addAnvilProcessingCatalysts(IRecipeCatalystRegistration registration, IRecipeType<?> recipeType) {
        AnvilCraftJeiPlugin.ANVIL_PROCESSING_CATALYSTS.get().forEach(item -> registration.addCraftingStation(recipeType, item));
    }

    public static void addCauldronCatalysts(IRecipeCatalystRegistration registration, IRecipeType<?> recipeType) {
        AnvilCraftJeiPlugin.CAULDRON_CATALYSTS.forEach(item -> registration.addCraftingStation(recipeType, item));
    }
}
