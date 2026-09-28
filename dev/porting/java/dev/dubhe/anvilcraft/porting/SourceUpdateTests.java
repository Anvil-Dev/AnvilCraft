package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class SourceUpdateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_source_update_environment", SourceUpdateTests::environment,
        "port_source_update_food_scope", SourceUpdateTests::food,
        "port_source_update_enderman", SourceUpdateTests::enderman
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_source_update"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 120, 0, true))));
    }

    private static void environment(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var pos = new BlockPos(6, 3, 6);
            helper.setBlock(pos, Blocks.LAVA);
            var absolute = helper.absolutePos(pos);
            player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
            player.baseTick();
            helper.assertTrue(player.isInLava(), "Fixture is inside lava");
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.RUBY_AMULET.asStack());
            AmuletAbilitiesEventListener.onInventoryTick(new EntityTickEvent.Post(player));
            helper.assertTrue(player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() == 210, "Ruby refreshes fire resistance in lava");
            helper.setBlock(pos, Blocks.WATER);
            player.baseTick();
            helper.assertTrue(player.isInWater(), "Fixture is inside water");
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.SAPPHIRE_AMULET.asStack());
            AmuletAbilitiesEventListener.onInventoryTick(new EntityTickEvent.Post(player));
            helper.assertTrue(player.getEffect(MobEffects.CONDUIT_POWER).getDuration() == 210, "Sapphire refreshes conduit power in water");
            player.removeEffect(MobEffects.CONDUIT_POWER);
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 190));
            AmuletAbilitiesEventListener.onInventoryTick(new EntityTickEvent.Post(player));
            helper.assertTrue(player.getEffect(MobEffects.CONDUIT_POWER).getDuration() == 210, "Refresh avoids the flashing threshold");
            new ItemStack(Items.MILK_BUCKET).finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(!player.hasEffect(MobEffects.CONDUIT_POWER), "Native milk removal still applies to refreshed effects");
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            new ItemStack(Items.HONEY_BOTTLE).finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(!player.hasEffect(MobEffects.POISON), "Native honey removal retains poison-specific behavior");
        }
        helper.succeed();
    }

    static ItemStack stew() {
        var stew = new ItemStack(Items.SUSPICIOUS_STEW);
        stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewEffects(List.of(
            new SuspiciousStewEffects.Entry(MobEffects.POISON, 200),
            new SuspiciousStewEffects.Entry(MobEffects.REGENERATION, 200))));
        return stew;
    }

    private static void food(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true);
             var other = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.ABNORMAL_AMULET.asStack());
            other.player().setItemInHand(InteractionHand.OFF_HAND, ModItems.ABNORMAL_AMULET.asStack());
            stew().finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(!player.hasEffect(MobEffects.POISON) && player.hasEffect(MobEffects.REGENERATION),
                "Suspicious stew listener keeps positive effects and blocks harmful food effects");
            helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "Food scope ends after consumption");
            player.removeEffect(MobEffects.POISON);
            var instant = stew();
            instant.get(DataComponents.CONSUMABLE).onConsume(helper.getLevel(), player, instant);
            helper.assertTrue(!player.hasEffect(MobEffects.POISON), "Direct component consumption also carries food context");
            AmuletAbilitiesEventListener.withFoodConsumption(player, stew(), () -> {
                helper.assertTrue(other.player().addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "Food context is entity-local");
                AmuletAbilitiesEventListener.withFoodConsumption(player, new ItemStack(Items.POTION), () -> {
                    helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "Nested non-food remains non-food");
                    return ItemStack.EMPTY;
                });
                player.removeEffect(MobEffects.POISON);
                helper.assertTrue(!player.addEffect(new MobEffectInstance(MobEffects.POISON, 200)),
                    "Nested scope restores outer food context");
                return ItemStack.EMPTY;
            });
            try {
                AmuletAbilitiesEventListener.withFoodConsumption(player, stew(), () -> {
                    throw new IllegalStateException("expected consumption failure");
                });
            } catch (IllegalStateException expected) {
                helper.assertTrue(expected.getMessage().equals("expected consumption failure"), "Expected fixture exception");
            }
            helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.POISON, 200)),
                "Exceptional consumption cannot leak immunity");
        }
        helper.succeed();
    }

    private static void enderman(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            var mob = helper.spawn(EntityType.ENDERMAN, new BlockPos(12, 8, 16));
            mob.setNoAi(true);
            var pos = helper.absolutePos(new BlockPos(12, 8, 12));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            var direction = mob.getEyePosition().subtract(player.getEyePosition());
            float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
            helper.assertTrue(stared(mob, player), "Ordinary gaze: look=" + player.getViewVector(1)
                + ", direction=" + direction.normalize() + ", los=" + player.hasLineOfSight(mob)
                + ", helmet=" + player.getItemBySlot(EquipmentSlot.HEAD) + ", player=" + player.position() + ", mob=" + mob.position());
            player.setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack());
            helper.assertTrue(stared(mob, player), "Breathing helmet alone does not protect against staring");
            var helmet = ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack();
            helmet.set(ModComponents.NIGHT_VISION_ENABLED, false);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            helper.assertTrue(!stared(mob, player), "Weatherproof helmet protects even with night vision disabled");
            Difficulty previous = helper.getLevel().getDifficulty();
            try {
                helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
                player.setInvulnerable(false);
                player.getAbilities().invulnerable = false;
                mob.setTarget(player);
                helper.assertTrue(mob.getTarget() == player, "Protection does not remove existing aggression");
            } finally {
                helper.getLevel().getServer().setDifficulty(previous, true);
            }
        }
        helper.succeed();
    }

    private static boolean stared(EnderMan mob, Player player) {
        try {
            var method = EnderMan.class.getDeclaredMethod("isBeingStaredBy", Player.class);
            method.setAccessible(true);
            return (boolean) method.invoke(mob, player);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
