package dev.dubhe.anvilcraft.integration.jei;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.BaseChuteScreen;
import dev.dubhe.anvilcraft.client.gui.screen.BatchCrafterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ControlValveScreen;
import dev.dubhe.anvilcraft.client.gui.screen.FilterScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemCollectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.ItemDetectorScreen;
import dev.dubhe.anvilcraft.client.gui.screen.JewelCraftingScreen;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.category.AnvilCollisionCraftCategory;
import dev.dubhe.anvilcraft.integration.jei.category.BeaconConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.ChargerChargingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.ContainerUpgradeCategory;
import dev.dubhe.anvilcraft.integration.jei.category.DecayCategory;
import dev.dubhe.anvilcraft.integration.jei.category.EnergyWeaponCategory;
import dev.dubhe.anvilcraft.integration.jei.category.FluidReactionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.JewelCraftingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.MineralFountainCategory;
import dev.dubhe.anvilcraft.integration.jei.category.MobTransformCategory;
import dev.dubhe.anvilcraft.integration.jei.category.MultipleToOneSmithingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.PortalConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.ProceduralProcessCategory;
import dev.dubhe.anvilcraft.integration.jei.category.SolidLiquidCategory;
import dev.dubhe.anvilcraft.integration.jei.category.UseItemOnBlockCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockCompressCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockCrushCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.BlockSmearCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemCompressCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemCrushCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.ItemInjectCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.MassInjectCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.MeshRecipeCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.SqueezingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.StampingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.UnpackCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid.FastCookingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid.NeutronIrradiationCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid.SuperHeatingCategory;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid.TimeWarpCategory;
import dev.dubhe.anvilcraft.integration.jei.category.extension.CanningFoodExtension;
import dev.dubhe.anvilcraft.integration.jei.category.extension.PillRecipeExtension;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlock4DCategory;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlockConversionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.multiblock.MultiBlockCraftingCategory;
import dev.dubhe.anvilcraft.integration.jei.handlers.GhostIngredientHandler;
import dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.ContainerUpgradeRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.DecayRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.MeshRecipeGroup;
import dev.dubhe.anvilcraft.integration.jei.recipe.MineralFountainJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.MobTransformJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.recipe.UseItemOnBlockRecipe;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.recipe.CanningFoodRecipe;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.PillRecipe;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.NeutronIrradiationRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SqueezingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;

@JeiPlugin
public class AnvilCraftJeiPlugin implements IModPlugin {

    public static final ImmutableList<ItemLike> ANVIL_PROCESSING_CATALYSTS = ImmutableList.of(
        Items.ANVIL,
        ModBlocks.ROYAL_ANVIL,
        ModBlocks.EMBER_ANVIL,
        ModBlocks.FROST_ANVIL,
        ModBlocks.TRANSCENDENCE_ANVIL,
        ModBlocks.SPECTRAL_ANVIL,
        ModBlocks.GIANT_ANVIL
    );

    public static final ImmutableList<ItemLike> CAULDRON_PROCESSING_CATALYSTS = ImmutableList.of(
        Items.CAULDRON,
        ModBlocks.FISH_TANK,
        ModBlocks.LARGE_CAULDRON
    );

    public static final RecipeType<MeshRecipeGroup> MESH = createRecipeType("mesh", MeshRecipeGroup.class);
    public static final RecipeType<BeaconConversionRecipe> BEACON_CONVERSION =
        createRecipeType("beacon_conversion", BeaconConversionRecipe.class);
    public static final RecipeType<DecayRecipe> DECAY = createRecipeType("decay", DecayRecipe.class);
    public static final RecipeType<ContainerUpgradeRecipe> CONTAINER_UPGRADE =
        createRecipeType("container_upgrade", ContainerUpgradeRecipe.class);

    public static final RecipeType<RecipeHolder<BlockCompressRecipe>> BLOCK_COMPRESS =
        createRecipeHolderType("block_compress");
    public static final RecipeType<RecipeHolder<BlockCrushRecipe>> BLOCK_CRUSH = createRecipeHolderType("block_crush");
    public static final RecipeType<RecipeHolder<BlockSmearRecipe>> BLOCK_SMEAR = createRecipeHolderType("block_smear");
    public static final RecipeType<RecipeHolder<ItemCrushRecipe>> ITEM_CRUSH = createRecipeHolderType("item_crush");
    public static final RecipeType<RecipeHolder<ItemInjectRecipe>> ITEM_INJECT = createRecipeHolderType("item_inject");
    public static final RecipeType<RecipeHolder<MassInjectRecipe>> MASS_INJECT = createRecipeHolderType("mass_inject");
    public static final RecipeType<RecipeHolder<ItemCompressRecipe>> ITEM_COMPRESS =
        createRecipeHolderType("item_compress");
    public static final RecipeType<RecipeHolder<UnpackRecipe>> UNPACK = createRecipeHolderType("unpack");
    public static final RecipeType<RecipeHolder<FastCookingRecipe>> FAST_COOKING =
        createRecipeHolderType("fast_cooking");
    public static final RecipeType<RecipeHolder<StampingRecipe>> STAMPING = createRecipeHolderType("stamping");
    public static final RecipeType<RecipeHolder<SuperHeatingRecipe>> SUPER_HEATING =
        createRecipeHolderType("super_heating");
    public static final RecipeType<RecipeHolder<SqueezingRecipe>> SQUEEZING = createRecipeHolderType("squeezing");
    public static final RecipeType<RecipeHolder<FluidMixingRecipe>> FLUID_REACTION =
        createRecipeHolderType("fluid_reaction");
    public static final RecipeType<RecipeHolder<FluidMixingRecipe>> SOLID_LIQUID =
        createRecipeHolderType("solid_liquid");
    public static final RecipeType<RecipeHolder<TimeWarpRecipe>> TIME_WARP = createRecipeHolderType("time_warp");
    public static final RecipeType<RecipeHolder<NeutronIrradiationRecipe>> NEUTRON_IRRADIATION =
        createRecipeHolderType("neutron_irradiation");

    public static final RecipeType<RecipeHolder<MultiblockRecipe>> MULTIBLOCK_CRAFTING =
        createRecipeHolderType("multiblock");
    public static final RecipeType<RecipeHolder<Multiblock4DRecipe>> MULTIBLOCK_4D =
        createRecipeHolderType("4d_multiblock");
    public static final RecipeType<RecipeHolder<MultiblockConversionRecipe>> MULTIBLOCK_CONVERSION =
        createRecipeHolderType("multiblock_conversion");

    public static final RecipeType<RecipeHolder<JewelCraftingRecipe>> JEWEL_CRAFTING =
        createRecipeHolderType("jewel_crafting");
    public static final RecipeType<RecipeHolder<ChargerChargingRecipe>> CHARGER_CHARGING =
        createRecipeHolderType("charger_charging");
    public static final RecipeType<RecipeHolder<BaseMultipleToOneSmithingRecipe>> MULTIPLE_TO_ONE_SMITHING =
        createRecipeHolderType("multiple_to_one_smithing");
    public static final RecipeType<RecipeHolder<PortalConversionRecipe>> PORTAL_CONVERSION =
        createRecipeHolderType("portal_conversion");

    public static final RecipeType<MobTransformJeiRecipe> MOB_TRANSFORM =
        createRecipeType("mob_transform", MobTransformJeiRecipe.class);

    public static final RecipeType<RecipeHolder<AnvilCollisionCraftRecipe>> ANVIL_COLLISION =
        createRecipeHolderType("anvil_collision");

    public static final RecipeType<UseItemOnBlockRecipe> USE_ITEM_ON_BLOCK =
        createRecipeType("use_item_on_block", UseItemOnBlockRecipe.class);

    public static final RecipeType<RecipeHolder<ProceduralProcessRecipe>> PROCEDURAL_PROCESS =
        createRecipeHolderType("procedural_process");
    public static final RecipeType<RecipeHolder<EnergyWeaponMakeRecipe>> ENERGY_WEAPON =
        createRecipeHolderType("energy_weapon");
    public static final RecipeType<MineralFountainJeiRecipe> MINERAL_FOUNTAIN =
        createRecipeType("mineral_fountain", MineralFountainJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
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
        FastCookingCategory.registerRecipes(registration);
        StampingCategory.registerRecipes(registration);
        SuperHeatingCategory.registerRecipes(registration);
        FluidReactionCategory.registerRecipes(registration);
        SolidLiquidCategory.registerRecipes(registration);
        TimeWarpCategory.registerRecipes(registration);
        NeutronIrradiationCategory.registerRecipes(registration);
        MultiBlockCraftingCategory.registerRecipes(registration);
        MultiBlock4DCategory.registerRecipes(registration);
        MultiBlockConversionCategory.registerRecipes(registration);
        JewelCraftingCategory.registerRecipes(registration);
        PortalConversionCategory.registerRecipes(registration);
        BeaconConversionCategory.registerRecipes(registration);
        DecayCategory.registerRecipes(registration);
        ChargerChargingCategory.registerRecipes(registration);
        MultipleToOneSmithingCategory.registerRecipes(registration);
        MobTransformCategory.registerRecipes(registration);
        AnvilCollisionCraftCategory.registerRecipes(registration);
        ProceduralProcessCategory.registerRecipes(registration);
        EnergyWeaponCategory.registerRecipes(registration);
        MineralFountainCategory.registerRecipes(registration);
        ContainerUpgradeCategory.registerRecipes(registration);
        UseItemOnBlockCategory.registerRecipes(registration);
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
        StampingCategory.registerRecipeCatalysts(registration);
        FastCookingCategory.registerRecipeCatalysts(registration);
        NeutronIrradiationCategory.registerRecipeCatalysts(registration);
        FluidReactionCategory.registerRecipeCatalysts(registration);
        SolidLiquidCategory.registerRecipeCatalysts(registration);
        SuperHeatingCategory.registerRecipeCatalysts(registration);
        TimeWarpCategory.registerRecipeCatalysts(registration);
        MultiBlockCraftingCategory.registerRecipeCatalysts(registration);
        MultiBlock4DCategory.registerRecipeCatalysts(registration);
        MultiBlockConversionCategory.registerRecipeCatalysts(registration);
        JewelCraftingCategory.registerRecipeCatalysts(registration);
        PortalConversionCategory.registerRecipeCatalysts(registration);
        BeaconConversionCategory.registerRecipeCatalysts(registration);
        DecayCategory.registerRecipeCatalysts(registration);
        ChargerChargingCategory.registerRecipeCatalysts(registration);
        MultipleToOneSmithingCategory.registerRecipeCatalysts(registration);
        MobTransformCategory.registerRecipeCatalysts(registration);
        AnvilCollisionCraftCategory.registerRecipeCatalysts(registration);
        ProceduralProcessCategory.registerRecipeCatalysts(registration);
        EnergyWeaponCategory.registerRecipeCatalysts(registration);
        MineralFountainCategory.registerRecipeCatalysts(registration);
        ContainerUpgradeCategory.registerRecipeCatalysts(registration);
        UseItemOnBlockCategory.registerRecipeCatalysts(registration);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BATCH_CRAFTER), RecipeTypes.CRAFTING);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), RecipeTypes.ANVIL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), RecipeTypes.ANVIL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FROST_ANVIL), RecipeTypes.ANVIL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), RecipeTypes.ANVIL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), RecipeTypes.ANVIL);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TRANSCENDENCE_ANVIL), RecipeTypes.ANVIL);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_SMITHING_TABLE), RecipeTypes.SMITHING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TRANSCENDENCE_SMITHING_TABLE), RecipeTypes.SMITHING);
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
        registration.addRecipeCategories(new FastCookingCategory(guiHelper));
        registration.addRecipeCategories(new StampingCategory(guiHelper));
        registration.addRecipeCategories(new SuperHeatingCategory(guiHelper));
        registration.addRecipeCategories(new FluidReactionCategory(guiHelper));
        registration.addRecipeCategories(new SolidLiquidCategory(guiHelper));
        registration.addRecipeCategories(new TimeWarpCategory(guiHelper));
        registration.addRecipeCategories(new NeutronIrradiationCategory(guiHelper));
        registration.addRecipeCategories(new MultiBlockCraftingCategory(guiHelper));
        registration.addRecipeCategories(new MultiBlock4DCategory(guiHelper));
        registration.addRecipeCategories(new MultiBlockConversionCategory(guiHelper));
        registration.addRecipeCategories(new JewelCraftingCategory(guiHelper));
        registration.addRecipeCategories(new PortalConversionCategory(guiHelper));
        registration.addRecipeCategories(new BeaconConversionCategory(guiHelper));
        registration.addRecipeCategories(new DecayCategory(guiHelper));
        registration.addRecipeCategories(new ChargerChargingCategory(guiHelper));
        registration.addRecipeCategories(new MultipleToOneSmithingCategory(guiHelper));
        registration.addRecipeCategories(new MobTransformCategory(guiHelper));
        registration.addRecipeCategories(new AnvilCollisionCraftCategory(guiHelper));
        registration.addRecipeCategories(new ProceduralProcessCategory(guiHelper));
        registration.addRecipeCategories(new EnergyWeaponCategory(guiHelper));
        registration.addRecipeCategories(new MineralFountainCategory(guiHelper));
        registration.addRecipeCategories(new ContainerUpgradeCategory(guiHelper));
        registration.addRecipeCategories(new UseItemOnBlockCategory(guiHelper));
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // 终端存储站补库由 JeiBasicRecipeTransferHandlerMixin 统一注入 JEI 转移流程处理
        registration.addRecipeTransferHandler(
            RoyalSmithingMenu.class,
            ModMenuTypes.ROYAL_SMITHING.get(),
            RecipeTypes.SMITHING,
            0, 3, 4, 36
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(
            StorageScreen.class,
            screen -> screen.width > 0 && screen.height > 0 ? new IGuiProperties() {
                    @Override
                    public Class<? extends Screen> screenClass() {
                        return StorageScreen.class;
                    }

                    @Override
                    public int guiLeft() {
                        return screen.getLeftPos();
                    }

                    @Override
                    public int guiTop() {
                        return screen.getTopPos();
                    }

                    @Override
                    public int guiXSize() {
                        return screen.getImageWidth();
                    }

                    @Override
                    public int guiYSize() {
                        return screen.getImageHeight();
                    }

                    @Override
                    public int screenWidth() {
                        return screen.width;
                    }

                    @Override
                    public int screenHeight() {
                        return screen.height;
                    }
                } : null
        );
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                IClickableIngredientFactory builder,
                double mouseX,
                double mouseY
            ) {
                if (!(Minecraft.getInstance().screen instanceof StorageScreen screen)) {
                    return Optional.empty();
                }
                ItemStack stack = screen.getItemUnderMouse(mouseX, mouseY);
                Rect2i area = screen.getItemArea(mouseX, mouseY);
                if (stack == null || stack.isEmpty() || area == null) {
                    return Optional.empty();
                }
                return builder.createBuilder(stack).buildWithArea(area);
            }
        });

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

    public static <T> RecipeType<T> createRecipeType(String name, Class<T> clazz) {
        return new RecipeType<>(AnvilCraft.of(name), clazz);
    }

    public static <R extends Recipe<?>> RecipeType<RecipeHolder<R>> createRecipeHolderType(String name) {
        return RecipeType.createRecipeHolderType(AnvilCraft.of(name));
    }

    public static void addAnvilProcessingCatalysts(IRecipeCatalystRegistration registration, RecipeType<?> recipeType) {
        ANVIL_PROCESSING_CATALYSTS.forEach(item ->
            registration.addRecipeCatalyst(new ItemStack(item), recipeType));
    }

    public static void addAnvilCauldronCatalysts(IRecipeCatalystRegistration registration, RecipeType<?> recipeType) {
        ANVIL_PROCESSING_CATALYSTS.forEach(item ->
            registration.addRecipeCatalyst(new ItemStack(item), recipeType));
        CAULDRON_PROCESSING_CATALYSTS.forEach(item ->
            registration.addRecipeCatalyst(new ItemStack(item), recipeType));
    }
}
