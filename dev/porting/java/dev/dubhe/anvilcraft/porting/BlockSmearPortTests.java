package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.event.anvil.AnvilEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlockSmearPortTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_smear_catalogue", BlockSmearPortTests::catalogue,
        "port_smear_orientation_execution", BlockSmearPortTests::orientations,
        "port_smear_anvil_dispatch", BlockSmearPortTests::dispatch
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_smear"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    private static List<Block> grinders() {
        return List.of(Blocks.GRINDSTONE, ModBlocks.EMBER_GRINDSTONE.get(), ModBlocks.FROST_GRINDSTONE.get(),
            ModBlocks.ROYAL_GRINDSTONE.get(), ModBlocks.TRANSCENDENCE_GRINDSTONE.get());
    }

    private static BlockSmearRecipe recipe(GameTestHelper helper, String name) {
        var value = helper.getLevel().getServer().getRecipeManager().recipeMap()
            .byKey(ResourceKey.create(Registries.RECIPE, AnvilCraft.of("block_smear/" + name)));
        if (value == null || !(value.value() instanceof BlockSmearRecipe result)) throw new IllegalStateException("Missing smear " + name);
        return result;
    }

    private static void checkMapping(GameTestHelper helper, Block input, Block output, String suffix) {
        var recipe = recipe(helper, BuiltInRegistries.BLOCK.getKey(output).getPath() + "_" + suffix);
        helper.assertTrue(recipe.getInputBlocks().size() == 2 && recipe.getFirstResultBlock().state().is(output),
            "Exact input and output mapping for " + input);
        helper.assertTrue(recipe.getInputBlocks().get(1).constructStatesForRender().stream().allMatch(state -> state.is(input)),
            "Grinding mapping cannot accept a different input block");
        for (Block grinder : grinders()) {
            var state = grinder.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.CEILING);
            helper.assertTrue(recipe.getFirstInputBlock().test(helper.getLevel(), state, null), "Every inverted grinder variant matches");
        }
    }

    private static void catalogue(GameTestHelper helper) {
        int dewax = 0;
        int deoxidize = 0;
        int strip = 0;
        for (Block input : BuiltInRegistries.BLOCK) {
            var unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(input);
            if (unwaxed != null) {
                checkMapping(helper, input, unwaxed, "dewax");
                dewax++;
            }
            var previous = WeatheringCopper.getPrevious(input.defaultBlockState());
            if (previous.isPresent()) {
                checkMapping(helper, input, previous.get().getBlock(), "deoxidize");
                deoxidize++;
            }
            var stripped = AxeItem.getAxeStrippingState(input.defaultBlockState());
            if (stripped != null) {
                checkMapping(helper, input, stripped.getBlock(), "strip");
                strip++;
            }
        }
        helper.assertTrue(dewax > 36 && deoxidize > 27 && strip > 0, "Cover native added copper families and stripping mappings");
        var recipes = helper.getLevel().getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.BLOCK_SMEAR.get());
        helper.assertTrue(recipes.size() == 190, "All generated smear recipes are loaded");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().recipeMap().byKey(
            ResourceKey.create(Registries.RECIPE, AnvilCraft.of("block_smear/grass_block"))) == null,
            "The source-removed grass conversion is absent");
        AnvilCraft.LOGGER.info("PORT_SMEAR_CATALOGUE: dewax={}, deoxidize={}, strip={}, total={}", dewax, deoxidize, strip, recipes.size());
        helper.succeed();
    }

    private static void orientations(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 5, 4));
        List<Block> inputs = List.of(Blocks.WAXED_COPPER_BLOCK, Blocks.WEATHERED_COPPER, Blocks.OAK_LOG);
        List<Block> outputs = List.of(Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.STRIPPED_OAK_LOG);
        List<String> names = List.of("copper_block_dewax", "exposed_copper_deoxidize", "stripped_oak_log_strip");
        int checks = 0;
        for (int kind = 0; kind < names.size(); kind++) {
            var recipe = recipe(helper, names.get(kind));
            for (Block grinder : grinders()) {
                for (AttachFace face : AttachFace.values()) {
                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        BlockState state = grinder.defaultBlockState().setValue(GrindstoneBlock.FACE, face)
                            .setValue(GrindstoneBlock.FACING, facing);
                        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        level.setBlock(pos.below(), inputs.get(kind).defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        var anvil = new FallingBlockEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                            Blocks.ANVIL.defaultBlockState());
                        var context = new InWorldRecipeContext(level, pos.above().getBottomCenter(), anvil);
                        boolean matched = recipe.matches(context, level);
                        helper.assertTrue(matched == (face == AttachFace.CEILING),
                            "Only ceiling attachment matches, independent of facing");
                        if (matched) {
                            recipe.assemble(context);
                            context.accept();
                            helper.assertTrue(level.getBlockState(pos.below()).is(outputs.get(kind)),
                                "Apply the grinding result below the tool");
                            helper.assertTrue(level.getBlockState(pos).equals(state),
                                "Grinding must preserve the tool and its orientation");
                        } else {
                            helper.assertTrue(level.getBlockState(pos.below()).is(inputs.get(kind)),
                                "Rejected orientation does not consume inputs");
                        }
                        anvil.discard();
                        checks++;
                    }
                }
            }
        }
        helper.assertTrue(checks == 180, "Complete tool/orientation/operation matrix");
        helper.succeed();
    }

    private static void dispatch(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(4, 5, 4));
        var grinder = Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.CEILING);
        level.setBlock(pos, grinder, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(pos.below(), Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        var anvil = new FallingBlockEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, Blocks.ANVIL.defaultBlockState());
        AnvilEventListener.onLand(new AnvilEvent.OnLand(level, pos.above(), anvil, 1));
        helper.assertTrue(level.getBlockState(pos.below()).is(Blocks.OXIDIZED_COPPER), "First landing removes wax, not oxidation");
        AnvilEventListener.onLand(new AnvilEvent.OnLand(level, pos.above(), anvil, 1));
        helper.assertTrue(level.getBlockState(pos.below()).is(Blocks.WEATHERED_COPPER), "Next landing removes one oxidation stage");
        helper.assertTrue(level.getBlockState(pos).equals(grinder), "Anvil event dispatch retains the ceiling grinder");
        anvil.discard();
        helper.succeed();
    }
}
