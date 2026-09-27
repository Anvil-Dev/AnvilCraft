package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cake.ShovelEatableCakeBlock;
import dev.dubhe.anvilcraft.block.cake.StepEffectBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ConfectioneryTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_confectionery_eating", ConfectioneryTests::eating,
        "port_confectionery_pillar", ConfectioneryTests::pillar,
        "port_confectionery_chocolate", ConfectioneryTests::chocolate,
        "port_confectionery_recipes", ConfectioneryTests::recipes
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_confectionery"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 150, 0, true))));
    }

    private static void eating(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        int index = 0;
        for (ShovelEatableCakeBlock block : List.of(ModBlocks.HONEY_CREAM_BLOCK.get(), ModBlocks.HONEY_CAKE_BLOCK.get(),
            ModBlocks.MATCHA_CREAM_BLOCK.get(), ModBlocks.MATCHA_CAKE_BLOCK.get())) {
            int food = new int[]{12, 20, 8, 14}[index];
            float saturation = new float[]{0.4F, 0.6F, 0.4F, 0.6F}[index++];
            helper.assertTrue(block.getFoodLevel() == food && block.getSaturationLevel() == saturation, "Source food values are preserved");
            for (InteractionHand hand : InteractionHand.values()) {
                helper.setBlock(POS, block);
                player.getFoodData().setFoodLevel(0);
                player.getFoodData().setSaturation(0);
                var hit = new BlockHitResult(helper.absolutePos(POS).getCenter(), Direction.UP, helper.absolutePos(POS), false);
                player.setItemInHand(hand, new ItemStack(Items.DIAMOND_SWORD));
                helper.assertTrue(helper.getBlockState(POS).useItemOn(player.getItemInHand(hand), helper.getLevel(), player, hand, hit)
                    == InteractionResult.PASS && helper.getBlockState(POS).is(block), "Unrelated tools cannot eat the block");
                var shovel = new ItemStack(Items.DIAMOND_SHOVEL);
                player.setItemInHand(hand, shovel);
                helper.assertTrue(helper.getBlockState(POS).useItemOn(shovel, helper.getLevel(), player, hand, hit).consumesAction(),
                    "Both shovel hands eat the block");
                helper.assertTrue(helper.getBlockState(POS).isAir() && shovel.getDamageValue() == 1
                    && player.getFoodData().getFoodLevel() == food
                    && Math.abs(player.getFoodData().getSaturationLevel() - Math.min(food, food * saturation * 2)) < 0.001,
                    "Eating removes one block, consumes one durability and applies exact food/saturation");
                helper.setBlock(POS, block);
                player.getFoodData().setFoodLevel(20);
                helper.assertTrue(!helper.getBlockState(POS).useItemOn(shovel, helper.getLevel(), player, hand, hit).consumesAction()
                    && helper.getBlockState(POS).is(block) && shovel.getDamageValue() == 1,
                    "Full hunger cannot consume food or durability");
            }
        }
        helper.succeed();
    }

    private static void pillar(GameTestHelper helper) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            var state = ModBlocks.COOKIE_PILLAR.getDefaultState().setValue(RotatedPillarBlock.AXIS, axis);
            var hole = switch (axis) {
                case X -> Block.box(0, 3, 3, 16, 13, 13);
                case Y -> Block.box(3, 0, 3, 13, 16, 13);
                case Z -> Block.box(3, 3, 0, 13, 13, 16);
            };
            var expected = Shapes.join(Shapes.block(), hole, BooleanOp.ONLY_FIRST);
            helper.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(helper.getLevel(), helper.absolutePos(POS)),
                expected, BooleanOp.NOT_SAME), "All three pillar axes retain the ten-pixel hollow collision");
            helper.assertTrue(!Shapes.joinIsNotEmpty(state.getOcclusionShape(), expected, BooleanOp.NOT_SAME),
                "Occlusion follows the hollow geometry");
            helper.assertTrue(!Shapes.joinIsNotEmpty(state.getShape(helper.getLevel(), helper.absolutePos(POS), CollisionContext.empty()),
                Shapes.block(), BooleanOp.NOT_SAME), "Selection remains a full cube");
        }
        helper.succeed();
    }

    private static void chocolate(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (helper.getLevel().getGameTime() % 80 != 0) {
            StepEffectBlock.stepOnBlackWhiteChocolateBlock(player);
            helper.assertTrue(player.getActiveEffects().isEmpty(), "Chocolate effects respect the eighty-tick cadence");
        }
        long delay = (80 - helper.getLevel().getGameTime() % 80) % 80;
        helper.runAfterDelay(delay, () -> {
            ModBlocks.BLACK_WHITE_CHOCOLATE_BLOCK.get().stepOn(helper.getLevel(), helper.absolutePos(POS),
                ModBlocks.BLACK_WHITE_CHOCOLATE_BLOCK.getDefaultState(), player);
            int index = 0;
            for (var effect : List.of(MobEffects.SPEED, MobEffects.HASTE, MobEffects.JUMP_BOOST)) {
                var active = player.getEffect(effect);
                helper.assertTrue(active != null && active.getAmplifier() == new int[]{4, 3, 5}[index++]
                    && active.getDuration() == 180 && active.isAmbient() && active.isVisible(),
                    "Source combined chocolate buffs are exact");
            }
            helper.succeed();
        });
    }

    private static Recipe<?> recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, AnvilCraft.of(path)))
            .orElseThrow().value();
    }

    private static void recipes(GameTestHelper helper) {
        for (Block block : List.<Block>of(ModBlocks.HONEY_CREAM_BLOCK.get(), ModBlocks.HONEY_CAKE_BLOCK.get(),
            ModBlocks.MATCHA_CREAM_BLOCK.get(), ModBlocks.MATCHA_CAKE_BLOCK.get(), ModBlocks.COOKIE_BLOCK.get(),
            ModBlocks.COOKIE_PILLAR.get(), ModBlocks.BLACK_WHITE_CHOCOLATE_BLOCK.get())) {
            helper.assertTrue(block.asItem().getDescriptionId().equals(block.getDescriptionId()),
                "Block items reuse the source block translation keys");
        }
        var cookie = (ShapedRecipe) recipe(helper, "cookie_block");
        helper.assertTrue(cookie.placementInfo().ingredients().size() == 9
            && cookie.placementInfo().ingredients().stream().allMatch(value -> value.test(new ItemStack(Items.COOKIE))),
            "Cookie block uses nine cookies");
        helper.assertTrue(((ShapelessRecipe) recipe(helper, "cookie_from_cookie_block")).result.count() == 9, "Cookie unpack returns nine");
        var pillar = (ShapedRecipe) recipe(helper, "cookie_pillar");
        helper.assertTrue(pillar.placementInfo().ingredients().size() == 6 && pillar.result.count() == 8,
            "Six blocks craft eight hollow pillars");
        helper.assertTrue(((ShapedRecipe) recipe(helper, "black_white_chocolate_block")).result.count() == 4,
            "Checkerboard crafts four blocks");
        for (String flavor : List.of("honey", "matcha")) {
            var block = (BlockCompressRecipe) recipe(helper, "block_compress/" + flavor + "_cake_block");
            helper.assertTrue(block.getInputBlocks().size() == 2, "Cream and cake base form the cake");
        }
        var matcha = (ItemCompressRecipe) recipe(helper, "item_compress/matcha_cream_block");
        helper.assertTrue(matcha.getInputItems().getFirst().count() == 4
            && matcha.getInputItems().getFirst().test(ModItems.CREAM.asStack(4))
            && matcha.getInputItems().get(2).test(new ItemStack(Items.AZALEA_LEAVES)), "Matcha uses four cream, sugar and azalea leaves");
        var honey = (SolidLiquidRecipe) recipe(helper, "solid_liquid/honey_cream_block");
        helper.assertTrue(honey.getHasCauldron().consume() == 250 && honey.getHasCauldron().fluid().equals(AnvilCraft.of("honey"))
            && honey.getInputItems().getFirst().count() == 4, "Honey cream consumes 250 mB with four cream");
        helper.assertTrue(((ItemCompressRecipe) recipe(helper, "item_compress/cookie_block")).getInputItems().getFirst().count() == 9,
            "Anvil cookie compression preserves nine-cookie input");
        helper.succeed();
    }
}
