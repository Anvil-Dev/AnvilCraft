package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HypercubeBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class HypercubeTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_hypercube_crafting", HypercubeTests::crafting,
        "port_hypercube_block", HypercubeTests::block,
        "port_hypercube_terminal", HypercubeTests::terminal
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_hypercube"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void crafting(GameTestHelper h) {
        final var computer = Multiblock4DTests.setup(h);
        Multiblock4DTests.fill(h, ModBlocks.TEMPERING_GLASS.get());
        h.setBlock(Multiblock4DTests.CENTER.below(), Blocks.AIR);
        Multiblock4DTests.land(h);
        h.assertTrue(computer.getProcessingProgress() == 0, "Missing glass rejects the entire first structure");
        for (int step = 1; step <= 3; step++) {
            Multiblock4DTests.fill(h, ModBlocks.TEMPERING_GLASS.get());
            Multiblock4DTests.land(h);
            h.assertTrue(h.getBlockState(Multiblock4DTests.CENTER.below()).isAir(), "Each stage consumes 27 glass blocks");
            h.assertTrue(computer.getProcessingProgress() == (step == 3 ? 0 : step), "Three-stage progress");
            h.assertTrue(Multiblock4DTests.dropped(h, ModBlocks.HYPERCUBE.asItem()) == (step == 3 ? 1 : 0),
                "Only the third complete structure produces one hypercube");
        }
        h.assertTrue(Multiblock4DTests.dropped(h, ModBlocks.TEMPERING_GLASS.asItem()) == 0, "Completed material inputs are not refunded");
        h.succeed();
    }

    private static void block(GameTestHelper h) {
        h.setBlock(Multiblock4DTests.CENTER, ModBlocks.HYPERCUBE.get());
        h.getBlockEntity(Multiblock4DTests.CENTER, HypercubeBlockEntity.class);
        var state = h.getBlockState(Multiblock4DTests.CENTER);
        var pos = h.absolutePos(Multiblock4DTests.CENTER);
        h.assertTrue(state.getRenderShape() == RenderShape.INVISIBLE, "Static chunk geometry must not duplicate the transparent BER");
        h.assertTrue(!state.isRedstoneConductor(h.getLevel(), pos) && !state.isSuffocating(h.getLevel(), pos),
            "Source glass-like properties");
        h.assertTrue(state.getCollisionShape(h.getLevel(), pos).bounds().getXsize() == 1, "Source full block collision");
        h.assertTrue(ModBlocks.HYPERCUBE.get().getExplosionResistance() == 1200, "Source explosion resistance");
        h.assertTrue(ModBlocks.SPACETIME_SUPERCOMPUTER.asStack().getRarity() == Rarity.EPIC, "Supercomputer source rarity");
        h.succeed();
    }

    private static void terminal(GameTestHelper h) {
        var items = NonNullList.withSize(9, ItemStack.EMPTY);
        items.set(1, ModBlocks.SINGULARITY_CRYSTAL.asStack());
        items.set(3, ModItems.TRANSCENDIUM_INGOT.asStack());
        items.set(5, ModItems.TRANSCENDIUM_INGOT.asStack());
        var terminal = ModItems.SHULKER_TERMINAL.asStack();
        terminal.set(ModComponents.CRAFTING, CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.DIAMOND, 2)));
        items.set(4, terminal);
        items.set(7, ModBlocks.HYPERCUBE.asStack());
        var input = CraftingInput.of(3, 3, items);
        var recipe = (CraftingRecipe) h.getLevel().getServer().getRecipeManager().recipeMap()
            .byKey(ResourceKey.create(Registries.RECIPE, AnvilCraft.of("hyperdimension_terminal"))).value();
        h.assertTrue(recipe.matches(input, h.getLevel()), "Source terminal ingredient pattern");
        var output = recipe.assemble(input);
        h.assertTrue(output.is(ModItems.HYPERDIMENSION_TERMINAL), "Hypercube upgrades the terminal");
        h.assertTrue(output.get(ModComponents.CRAFTING).craftingInput().getFirst().getCount() == 2,
            "Native terminal upgrade preserves existing crafting contents");
        h.succeed();
    }
}
