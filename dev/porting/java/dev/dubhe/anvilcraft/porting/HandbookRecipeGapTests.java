package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.VanillaRecipesWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class HandbookRecipeGapTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_handbook_gap_crafting", HandbookRecipeGapTests::crafting,
        "port_handbook_gap_charging", HandbookRecipeGapTests::charging,
        "port_handbook_gap_smithing", HandbookRecipeGapTests::smithing,
        "port_handbook_gap_superheat", HandbookRecipeGapTests::superheat,
        "port_handbook_gap_canonical_ids", HandbookRecipeGapTests::canonicalIds
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_handbook_gap"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    private static Recipe<?> recipe(GameTestHelper helper, String name) {
        var holder = helper.getLevel().getServer().getRecipeManager().recipeMap()
            .byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
        if (holder == null) throw new IllegalStateException("Missing source recipe " + name);
        return holder.value();
    }

    private static void crafting(GameTestHelper helper) {
        var experience = (CraftingRecipe) recipe(helper, "anvilcraft:exp_collector_alt");
        var slots = new ArrayList<>(List.of(ModItems.ROYAL_STEEL_INGOT.asStack(), ModBlocks.PUMP.asStack(),
            ModItems.ROYAL_STEEL_INGOT.asStack(), ModBlocks.PUMP.asStack(), ModBlocks.ITEM_COLLECTOR.asStack(),
            ModBlocks.PUMP.asStack(), ModItems.ROYAL_STEEL_INGOT.asStack(), ModBlocks.FLUID_TANK.asStack(),
            ModItems.ROYAL_STEEL_INGOT.asStack()));
        var input = CraftingInput.of(3, 3, slots);
        helper.assertTrue(experience.matches(input, helper.getLevel()) && experience.assemble(input).is(ModBlocks.EXP_COLLECTOR.asItem()),
            "Alternative collector consumes the source pump/collector/tank pattern");
        slots.set(4, new ItemStack(Items.HOPPER));
        helper.assertTrue(!experience.matches(CraftingInput.of(3, 3, slots), helper.getLevel()), "Incorrect centre is rejected");
        var infinite = (CraftingRecipe) recipe(helper, "anvilcraft:infinite_collector");
        input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, ModBlocks.CHARGE_COLLECTOR.asStack(), ItemStack.EMPTY,
            ModBlocks.CHARGE_COLLECTOR.asStack(), ModBlocks.HEAT_COLLECTOR.asStack(), ModBlocks.CHARGE_COLLECTOR.asStack(),
            ModItems.TRANSCENDIUM_INGOT.asStack(), ModItems.TRANSCENDIUM_INGOT.asStack(), ModItems.TRANSCENDIUM_INGOT.asStack()));
        helper.assertTrue(infinite.matches(input, helper.getLevel()) && infinite.assemble(input).is(ModBlocks.INFINITE_COLLECTOR.asItem()),
            "Infinite collector is craftable from three charge collectors, heat collector and three ingots");
        helper.succeed();
    }

    private static void charging(GameTestHelper helper) {
        var recipe = (ChargerChargingRecipe) recipe(helper, "anvilcraft:charger_charging/magnet_block");
        var input = new SingleRecipeInput(new ItemStack(Items.IRON_BLOCK));
        helper.assertTrue(recipe.matches(input, helper.getLevel()) && recipe.assemble(input).is(ModBlocks.MAGNET_BLOCK.asItem())
            && recipe.power() == -20 && recipe.time() == 80 && recipe.getProcessingBlock() == ModBlocks.CHARGER.get(),
            "Iron-block magnetization uses 20 kW for 80 ticks in the charger");
        helper.assertTrue(!recipe.matches(new SingleRecipeInput(new ItemStack(Items.IRON_INGOT)), helper.getLevel()),
            "Block charging does not replace the separate ingot recipe");
        helper.succeed();
    }

    private static void smithing(GameTestHelper helper) {
        var recipe = (SmithingTransformRecipe) recipe(helper, "anvilcraft:smithing/frost_dragon_rod");
        var input = new SmithingRecipeInput(ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE.asStack(),
            ModItems.ROYAL_DRAGON_ROD.asStack(), ModBlocks.FROST_METAL_BLOCK.asStack());
        helper.assertTrue(recipe.matches(input, helper.getLevel()) && recipe.assemble(input).is(ModItems.FROST_DRAGON_ROD),
            "Royal dragon rod upgrades through the frost template and frost block");
        input = new SmithingRecipeInput(ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE.asStack(),
            ModItems.ROYAL_DRAGON_ROD.asStack(), ModBlocks.FROST_METAL_BLOCK.asStack());
        helper.assertTrue(!recipe.matches(input, helper.getLevel()), "Different upgrade template is rejected");
        helper.assertTrue(recipe(helper, "anvilcraft:frost_dragon_rod") instanceof CraftingRecipe,
            "Existing direct hammer/devourer crafting route remains available");
        helper.succeed();
    }

    private static void superheat(GameTestHelper helper) {
        var recipe = (SuperHeatingRecipe) recipe(helper, "anvilcraft:super_heating/heated_netherite_block");
        helper.assertTrue(recipe.getHasCauldron().fluid().equals(Identifier.withDefaultNamespace("lava"))
            && recipe.getHasCauldron().consume() == 250, "Recipe retains the source lava requirement");
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 15, 3));
        level.setBlockAndUpdate(pos.below(), Blocks.LAVA_CAULDRON.defaultBlockState());
        level.setBlockAndUpdate(pos.below(2), ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false));
        var item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() - 0.375, pos.getZ() + 0.5, new ItemStack(Items.NETHERITE_BLOCK));
        level.addFreshEntity(item);
        var anvil = new FallingBlockEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Blocks.ANVIL.defaultBlockState());
        var context = new InWorldRecipeContext(level, Vec3.atBottomCenterOf(pos), anvil);
        helper.assertTrue(recipe.matches(context, level), "Actual heater/cauldron/input scene matches");
        recipe.assemble(context);
        context.accept();
        int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
            .filter(entity -> entity.getItem().is(ModBlocks.HEATED_NETHERITE_BLOCK.asItem()))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(count == 1 && (item.isRemoved() || item.getItem().isEmpty()),
            "Super-heating consumes netherite and emits one heated block");
        anvil.discard();
        helper.succeed();
    }

    private static void canonicalIds(GameTestHelper helper) {
        var disk = (CraftingRecipe) recipe(helper, "anvilcraft:disk_to_structure_disk");
        var input = CraftingInput.of(1, 1, List.of(ModItems.DISK.asStack()));
        helper.assertTrue(disk.matches(input, helper.getLevel()) && disk.assemble(input).is(ModItems.STRUCTURE_DISK),
            "Source disk conversion ID retains its one-disk input and structure-disk output");
        var furnace = (SmeltingRecipe) recipe(helper, "minecraft:netherrack");
        var flesh = new SingleRecipeInput(ModBlocks.ROTTEN_FLESH_BLOCK.asStack());
        helper.assertTrue(furnace.matches(flesh, helper.getLevel()) && furnace.assemble(flesh).is(Items.NETHERRACK),
            "Source namespace identifies the rotten-flesh-block furnace recipe");
        var fast = (FastCookingRecipe) recipe(helper, "anvilcraft:smoking_warp_dough_2_bread");
        helper.assertTrue(fast.getInputItems().getFirst().test(ModItems.DOUGH.asStack())
            && fast.getResultItems().getFirst().stack().is(Items.BREAD), "The published automatic dough recipe ID is loaded");
        var previous = VanillaRecipesWrap.recipes;
        try {
            VanillaRecipesWrap.recipes = new ArrayList<>();
            VanillaRecipesWrap.wrap((SmokingRecipe) recipe(helper, "anvilcraft:smoking_bread"));
            helper.assertTrue(VanillaRecipesWrap.recipes.size() == 1
                && VanillaRecipesWrap.recipes.getFirst().id().identifier().equals(AnvilCraft.of("smoking_warp_dough_2_bread")),
                "Tag-based wrapper generates the source item-derived ID on rebuild");
        } finally {
            VanillaRecipesWrap.recipes = previous;
        }
        helper.succeed();
    }
}
