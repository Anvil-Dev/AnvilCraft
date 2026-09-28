package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.FishTankEvent;
import dev.dubhe.anvilcraft.api.event.LargeCauldronEvent;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CauldronEventTests {
    private static final BlockPos POS = new BlockPos(4, 4, 4);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_cauldron_events_fish_tick", CauldronEventTests::fishTick,
        "port_cauldron_events_large_tick", CauldronEventTests::largeTick,
        "port_cauldron_events_inside", CauldronEventTests::inside,
        "port_cauldron_events_use", CauldronEventTests::use,
        "port_cauldron_events_impact", CauldronEventTests::impact,
        "port_cauldron_events_fish_damage", CauldronEventTests::fishDamage,
        "port_cauldron_events_large_damage", CauldronEventTests::largeDamage,
        "port_cauldron_events_mix", h -> mixing(h, false, false),
        "port_cauldron_events_mix_full", h -> mixing(h, true, false),
        "port_cauldron_events_mix_empty", h -> mixing(h, true, true)
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_cauldron_events"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static FishTankBlockEntity fish(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.FISH_TANK.get());
        return h.getBlockEntity(POS, FishTankBlockEntity.class);
    }

    private static LargeCauldronBlockEntity large(GameTestHelper h) {
        var bottom = POS.below();
        var state = ModBlocks.LARGE_CAULDRON.getDefaultState();
        h.setBlock(bottom, state);
        ModBlocks.LARGE_CAULDRON.get().setPlacedBy(h.getLevel(), h.absolutePos(bottom), state, null, ModBlocks.LARGE_CAULDRON.asStack());
        return h.getBlockEntity(POS, LargeCauldronBlockEntity.class);
    }

    private static Pig pig(GameTestHelper h) {
        var pig = new Pig(EntityType.PIG, h.getLevel());
        var pos = h.absolutePos(POS);
        pig.setPos(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        h.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static void fishTick(GameTestHelper h) {
        var tank = fish(h);
        tank.getFluidHandler().set(FluidResource.of(Fluids.WATER), 1000);
        int[] calls = {0};
        Consumer<FishTankEvent.ServerTick> listener = event -> {
            if (event.getTank() != tank) return;
            h.assertTrue(event.getLevel() == h.getLevel() && event.getServerLevel() == h.getLevel()
                && event.getPos().equals(tank.getBlockPos()), "Fish tick context");
            h.assertTrue(tank.getFluidHandler().getResource(0).is(Fluids.WATER), "Non-lava contents must reach the event");
            calls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            FishTankBlockEntity.serverTick(h.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
            h.assertTrue(calls[0] == 1, "Fish server tick dispatched once before the lava guard");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    private static void largeTick(GameTestHelper h) {
        var tank = large(h);
        h.setBlock(POS.above(2), Blocks.WATER);
        int[] calls = {0};
        Consumer<LargeCauldronEvent.ServerTick> listener = event -> {
            if (event.getCauldron() != tank) return;
            h.assertTrue(event.getServerLevel() == h.getLevel() && event.getPos().equals(tank.getBlockPos()), "Large tick context");
            h.assertTrue(tank.getFluids().getTotalAmount() == 1000, "Source intake must happen before the tick event");
            calls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            LargeCauldronBlockEntity.serverTick(h.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
            var child = h.getBlockEntity(POS.below(), LargeCauldronBlockEntity.class);
            LargeCauldronBlockEntity.serverTick(h.getLevel(), child.getBlockPos(), child.getBlockState(), child);
            h.assertTrue(calls[0] == 1, "Only the main part publishes the tick event");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    private static void inside(GameTestHelper h) {
        var tank = fish(h);
        var pig = pig(h);
        int[] calls = {0, 0};
        Consumer<FishTankEvent.EntityInside> fishListener = event -> {
            if (event.getTank() != tank) return;
            h.assertTrue(event.getEntity() == pig && event.getState() == tank.getBlockState(), "Fish collision context");
            calls[0]++;
        };
        Consumer<LargeCauldronEvent.EntityInside> largeListener = event -> {
            if (!event.getPos().equals(h.absolutePos(POS))) return;
            h.assertTrue(event.getEntity() == pig && event.getLevel() == h.getLevel(), "Large collision context");
            calls[1]++;
        };
        NeoForge.EVENT_BUS.addListener(fishListener);
        NeoForge.EVENT_BUS.addListener(largeListener);
        try {
            tank.getBlockState().entityInside(h.getLevel(), tank.getBlockPos(), pig, InsideBlockEffectApplier.NOOP, true);
            var cauldron = large(h);
            cauldron.getBlockState().entityInside(h.getLevel(), cauldron.getBlockPos(), pig, InsideBlockEffectApplier.NOOP, true);
            h.assertTrue(calls[0] == 1 && calls[1] == 1, "Both collision hooks fire for non-item entities");
        } finally {
            NeoForge.EVENT_BUS.unregister(fishListener);
            NeoForge.EVENT_BUS.unregister(largeListener);
            pig.discard();
        }
        h.succeed();
    }

    private static void use(GameTestHelper h) {
        var tank = large(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        var hit = new BlockHitResult(tank.getBlockPos().getCenter(), Direction.UP, tank.getBlockPos(), false);
        int[] calls = {0};
        Consumer<LargeCauldronEvent.UseItem> listener = event -> {
            if (!event.getPos().equals(tank.getBlockPos())) return;
            h.assertTrue(event.getPlayer() == player && event.getHand() == InteractionHand.MAIN_HAND
                && event.getHit() == hit && event.getStack() == player.getMainHandItem(), "Interaction context");
            event.setResult(InteractionResult.FAIL);
            event.setCanceled(true);
            calls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var result = tank.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(result == InteractionResult.FAIL && calls[0] == 1, "Canceled interaction returns the listener result");
            h.assertTrue(tank.getFluids().getTotalAmount() == 0 && player.getMainHandItem().is(Items.WATER_BUCKET),
                "Canceled interaction must not transfer fluid or consume its container");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    private static void impact(GameTestHelper h) {
        var tank = large(h);
        var pos = tank.getBlockPos().above(3);
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.GIANT_ANVIL.getDefaultState());
        var anvil = new FallingGiantAnvilEntity(ModEntities.FALLING_GIANT_ANVIL.get(), h.getLevel());
        var landing = new AnvilEvent.OnLand(h.getLevel(), pos, anvil, 4);
        int[] calls = {0};
        Consumer<LargeCauldronEvent.GiantAnvilImpact> listener = event -> {
            if (event.getCauldron() != tank) return;
            h.assertTrue(event.getLanding() == landing && event.getPos().equals(pos)
                && event.getLandedAnvilState().is(ModBlocks.GIANT_ANVIL.get()), "Impact context before validation");
            event.setLandedAnvilState(Blocks.AIR.defaultBlockState());
            calls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.assertTrue(tank.handleGiantAnvilImpact(landing) && calls[0] == 1, "Listener replaces the state used for impact processing");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    private static void fishDamage(GameTestHelper h) {
        var tank = fish(h);
        tank.getFluidHandler().set(FluidResource.of(ModFluids.OIL.get()), 1000);
        tank.setIgnited(true);
        var pig = pig(h);
        pig.setRemainingFireTicks(-1);
        float before = pig.getHealth();
        var effects = new InsideBlockEffectApplier.StepBasedCollector();
        tank.entityInsideFluidContent(h.getLevel(), tank.getBlockPos(), pig, effects);
        h.assertTrue(pig.getHealth() == before, "Native collision effects must be deferred");
        effects.applyAndClear(pig);
        h.assertTrue(before - pig.getHealth() == 4 && pig.isOnFire(), "Default source burning fluid deals four damage and ignites");
        pig.invulnerableTime = 0;
        before = pig.getHealth();
        int[] calls = {0};
        Consumer<FishTankEvent.FluidDamage> listener = event -> {
            if (event.getTank() != tank) return;
            h.assertTrue(event.getFluid().is(ModFluids.OIL.get()) && event.getDamage() == 4, "Fish burning-fluid event context");
            event.setDamage(2);
            calls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            tank.entityInsideFluidContent(h.getLevel(), tank.getBlockPos(), pig, effects);
            effects.applyAndClear(pig);
            h.assertTrue(calls[0] == 1 && before - pig.getHealth() == 2, "Fish damage must use the listener-modified amount");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            pig.discard();
        }
        h.succeed();
    }

    private static void largeDamage(GameTestHelper h) {
        var tank = large(h);
        tank.getFluids().setFluids(List.of(new FluidStack(ModFluids.OIL.get(), 64000)));
        tank.setIgnited(true);
        var pig = pig(h);
        pig.setPos(pig.getX(), tank.getBlockPos().getY() - 0.4, pig.getZ());
        final float before = pig.getHealth();
        int[] calls = {0};
        Consumer<LargeCauldronEvent.FluidDamage> listener = event -> {
            if (event.getCauldron() != tank) return;
            h.assertTrue(event.getFluid().is(ModFluids.OIL.get()) && event.getDamage() == 4, "Large burning-fluid event context");
            event.setDamage(2);
            calls[0]++;
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            LargeCauldronBlockEntity.serverTick(h.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
            h.assertTrue(calls[0] == 1 && before - pig.getHealth() == 2, "Large cauldron damage must use the listener-modified amount");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            pig.discard();
        }
        h.succeed();
    }

    private static void mixing(GameTestHelper h, boolean full, boolean empty) {
        var tank = large(h);
        tank.getFluids().setFluids(List.of(new FluidStack(Fluids.WATER, 250)));
        if (full) {
            for (int slot = 0; slot < tank.getOutputHandler().size(); slot++) {
                tank.getOutputHandler().set(slot, ItemResource.of(Items.DIAMOND), 64);
            }
        }
        var recipe = new FluidMixingRecipe(List.of(SizedFluidIngredient.of(Fluids.WATER, 250)),
            List.of(ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.DIAMOND))), List.of(), false);
        var manager = h.getLevel().getServer().getRecipeManager();
        final var previous = manager.recipes;
        var seenAmounts = new ArrayList<Integer>();
        Consumer<LargeCauldronEvent.MixingOutput> listener = event -> {
            if (event.getCauldron() != tank) return;
            h.assertTrue(event.getResult().is(Items.DIAMOND) && event.getResult().getCount() == 1,
                "Each event receives a fresh recipe result");
            seenAmounts.add(tank.getFluids().getTotalAmount());
            event.setResult(empty ? ItemStack.EMPTY : new ItemStack(Items.EMERALD, 2));
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            manager.recipes = RecipeMap.create(List.of(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, AnvilCraft.of("port_event_mix")), recipe)));
            var method = LargeCauldronBlockEntity.class.getDeclaredMethod("tryProcessFluidMixingRecipe", ServerLevel.class);
            method.setAccessible(true);
            boolean success = (boolean) method.invoke(tank, h.getLevel());
            boolean expected = !full || empty;
            h.assertTrue(success == expected && tank.getFluids().getTotalAmount() == (expected ? 0 : 250),
                "Mix capacity is tested after mutation");
            h.assertTrue(seenAmounts.equals(expected ? List.of(250, 0) : List.of(250)), "Source simulation and commit event ordering");
            h.assertTrue(recipe.getItemResults().getFirst().is(Items.DIAMOND), "Handlers must not mutate the stored recipe");
            if (!full) {
                h.assertTrue(tank.getOutputHandler().getResource(0).is(Items.EMERALD)
                    && tank.getOutputHandler().getAmountAsInt(0) == 2, "Committed output follows the event");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        } finally {
            manager.recipes = previous;
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }
}
