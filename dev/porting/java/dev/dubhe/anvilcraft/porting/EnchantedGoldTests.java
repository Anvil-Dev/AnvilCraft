package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import dev.dubhe.anvilcraft.recipe.LiquidEnchantmentCauldronRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class EnchantedGoldTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_enchanted_gold_properties", EnchantedGoldTests::properties,
        "port_enchanted_gold_carrying", EnchantedGoldTests::carrying,
        "port_enchanted_gold_reactions", EnchantedGoldTests::reactions,
        "port_enchanted_gold_piglin", EnchantedGoldTests::piglins,
        "port_enchanted_gold_barter", EnchantedGoldTests::barter,
        "port_enchanted_gold_tracking", EnchantedGoldTests::tracking
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_enchanted_gold"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void properties(GameTestHelper helper) {
        for (var stack : List.of(ModItems.ENCHANTED_GOLD_INGOT.asStack(), ModItems.ENCHANTED_GOLD_NUGGET.asStack(),
            ModBlocks.ENCHANTED_GOLD_BLOCK.asStack())) {
            helper.assertTrue(stack.hasFoil() && stack.is(ModItemTags.ENCHANTED_GOLD), "All three forms retain foil and the common tag");
        }
        helper.assertTrue(ModItems.ENCHANTED_GOLD_INGOT.asStack().isPiglinCurrency(), "The ingot must be barter currency");
        helper.assertTrue(ModBlocks.ENCHANTED_GOLD_BLOCK.getDefaultState().getEnchantPowerBonus(helper.getLevel(), BlockPos.ZERO) == 3,
            "Enchanted gold must supply three enchanting power");
        helper.succeed();
    }

    private static void carrying(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.getInventory().clearContent();
            player.getAbilities().instabuild = false;
            player.getAbilities().invulnerable = false;
            var gold = ModItems.ENCHANTED_GOLD_NUGGET.asStack(63);
            player.setItemInHand(InteractionHand.MAIN_HAND, gold);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            gold.inventoryTick(helper.getLevel(), player, null);
            helper.assertTrue(!player.hasEffect(MobEffects.WEAKNESS) && !player.hasEffect(MobEffects.SLOWNESS)
                && !player.hasEffect(MobEffects.HUNGER) && player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.LUCK),
                "63 items clear only the three curse effects and do not grant luck");
            player.getInventory().setItem(1, ModBlocks.ENCHANTED_GOLD_BLOCK.asStack());
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.ABNORMAL_AMULET.asStack());
            gold.inventoryTick(helper.getLevel(), player, null);
            helper.assertTrue(IEnchantedGold.getEnchantedGoldCount(player) == 64 && player.hasEffect(MobEffects.LUCK),
                "Forms count as individual items, and the abnormal amulet must not suppress the benefit");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            ((ICursed) ModItems.CURSED_GOLD_INGOT.get()).addEffect(player);
            helper.assertTrue(!player.hasEffect(MobEffects.WEAKNESS), "Cursed gold must not reapply effects after the gold tick");
            player.getInventory().clearContent();
            ((ICursed) ModItems.CURSED_GOLD_INGOT.get()).addEffect(player);
            helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "Removing enchanted gold restores cursed effects");
        }
        helper.succeed();
    }

    private static void reactions(GameTestHelper helper) {
        var blank = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 16);
        var mending = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 4);
        mending.set(ModComponents.LIQUID_ENCHANTMENT, helper.getLevel().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING));
        var fortune = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1);
        fortune.set(ModComponents.LIQUID_ENCHANTMENT, helper.getLevel().registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE));
        for (var fluid : List.of(blank, mending, fortune)) {
            var result = LiquidEnchantmentCauldronRecipe.match(List.of(fluid), new ItemStack(Items.GOLD_INGOT), false).orElseThrow();
            helper.assertTrue(result.itemCost() == 1 && result.itemResult().is(ModItems.ENCHANTED_GOLD_INGOT)
                && result.fluids().stream().allMatch(FluidStack::isEmpty) && !result.consumesHeat(),
                "Each source route consumes exactly its fluid requirement");
            if (fluid.getAmount() > 1) {
                helper.assertTrue(LiquidEnchantmentCauldronRecipe.match(List.of(fluid.copyWithAmount(fluid.getAmount() - 1)),
                    new ItemStack(Items.GOLD_INGOT), false).isEmpty(), "Insufficient fluid must not create enchanted gold");
            }
        }
        var result = LiquidEnchantmentCauldronRecipe.match(List.of(blank, mending, fortune), new ItemStack(Items.GOLD_INGOT), true)
            .orElseThrow();
        helper.assertTrue(result.fluids().stream().mapToInt(FluidStack::getAmount).sum() == 5 && blank.getAmount() == 16,
            "Blank liquid has source priority, and matching must not mutate the input");
        helper.succeed();
    }

    private static void piglins(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.setPos(helper.absolutePos(new BlockPos(2, 10, 2)).getCenter());
            helper.getLevel().addNewPlayer(player);
            try {
                var piglin = new Piglin(EntityType.PIGLIN, helper.getLevel());
                var brute = new PiglinBrute(EntityType.PIGLIN_BRUTE, helper.getLevel());
                piglin.getBrain().setMemory(MemoryModuleType.UNIVERSAL_ANGER, true);
                piglin.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, player);
                brute.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, player);
                for (var mob : List.<AbstractPiglin>of(piglin, brute)) {
                    mob.setPos(player.getX() + 2, player.getY(), player.getZ());
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    helper.assertTrue(target(helper.getLevel(), mob).orElse(null) == player, "Fixture must expose an attack target");
                    player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.ENCHANTED_GOLD_INGOT.asStack());
                    helper.assertTrue(target(helper.getLevel(), mob).isEmpty(), "Holding enchanted gold protects from passive hostility");
                    mob.getBrain().setMemory(MemoryModuleType.ANGRY_AT, player.getUUID());
                    helper.assertTrue(target(helper.getLevel(), mob).orElse(null) == player, "Explicit anger must override the protection");
                }
            } finally {
                player.discard();
            }
        }
        helper.succeed();
    }

    private static Optional<?> target(ServerLevel level, AbstractPiglin piglin) {
        try {
            var owner = piglin instanceof Piglin ? PiglinAi.class : PiglinBruteAi.class;
            var type = piglin instanceof Piglin ? Piglin.class : AbstractPiglin.class;
            var method = owner.getDeclaredMethod("findNearestValidAttackTarget", ServerLevel.class, type);
            method.setAccessible(true);
            return (Optional<?>) method.invoke(null, level, piglin);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void barter(GameTestHelper helper) {
        var level = helper.getLevel();
        var piglin = new Piglin(EntityType.PIGLIN, level);
        var position = helper.absolutePos(new BlockPos(4, 10, 4)).getCenter();
        piglin.setPos(position);
        piglin.setBaby(false);
        try {
            var hold = PiglinAi.class.getDeclaredMethod("holdInOffhand", ServerLevel.class, Piglin.class, ItemStack.class);
            hold.setAccessible(true);
            var stop = PiglinAi.class.getDeclaredMethod("stopHoldingOffHandItem", ServerLevel.class, Piglin.class, boolean.class);
            stop.setAccessible(true);
            hold.invoke(null, level, piglin, ModItems.ENCHANTED_GOLD_INGOT.asStack());
            helper.assertTrue(piglin.getData(ModDataAttachments.ENCHANTED_GOLD_BARTER), "Currency capture must mark the next barter");
            stop.invoke(null, level, piglin, true);
            var drops = level.getEntitiesOfClass(ItemEntity.class, piglin.getBoundingBox().inflate(3));
            helper.assertTrue(drops.size() == 4 && !piglin.getData(ModDataAttachments.ENCHANTED_GOLD_BARTER),
                "Enchanted gold produces four independent loot rolls and consumes its marker");
            drops.forEach(ItemEntity::discard);
            hold.invoke(null, level, piglin, new ItemStack(Items.GOLD_INGOT));
            stop.invoke(null, level, piglin, true);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, piglin.getBoundingBox().inflate(3)).size() == 1,
                "Normal gold after an enchanted trade must return to one loot roll");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        helper.succeed();
    }

    private static void tracking(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var state = ModBlocks.ENCHANTED_GOLD_BLOCK.getDefaultState();
        var positions = dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions.getPositions();
        positions.clear();
        try {
            helper.getLevel().setBlockAndUpdate(pos, state);
            dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions.scanChunk(helper.getLevel().getChunkAt(pos));
            helper.assertTrue(positions.contains(pos), "Chunk scanning must discover existing gold blocks");
            dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions.unload(helper.getLevel().getChunkAt(pos).getPos());
            helper.assertTrue(!positions.contains(pos), "Chunk unload must remove tracked positions");
            var cursor = new BlockPos.MutableBlockPos(-1, 160, -1);
            dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions.onBlockChanged(cursor, net.minecraft.world.level.block.Blocks.AIR
                .defaultBlockState(), state);
            cursor.set(0, 160, 0);
            helper.assertTrue(positions.contains(new BlockPos(-1, 160, -1)), "Tracking must copy mutable coordinates");
            dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions.unload(new net.minecraft.world.level.ChunkPos(-1, -1));
            helper.assertTrue(positions.isEmpty(), "Negative chunk coordinates must unload correctly");
        } finally {
            positions.clear();
        }
        helper.succeed();
    }

}
