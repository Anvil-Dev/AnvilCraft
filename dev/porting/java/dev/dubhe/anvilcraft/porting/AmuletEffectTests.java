package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContextKey;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneFriendlyDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AmuletEffectTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_amulet_context", AmuletEffectTests::context,
        "port_amulet_dedup", AmuletEffectTests::dedup,
        "port_amulet_cache_mutation", AmuletEffectTests::cache,
        "port_amulet_non_player", AmuletEffectTests::nonPlayer,
        "port_amulet_mask_cleanup", AmuletEffectTests::masks,
        "port_amulet_legacy_component", AmuletEffectTests::legacy,
        "port_amulet_ai_hooks", AmuletEffectTests::ai,
        "port_amulet_curios", AmuletEffectTests::curios
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_amulet_effects"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void context(GameTestHelper helper) {
        var ctx = new AmuletEffectContext();
        var key = AmuletEffectContextKey.ofBool(AnvilCraft.of("test_context"));
        ctx.set(key, true);
        helper.assertTrue(ctx.getOrThrow(AmuletEffectContextKey.ofBool(key.id())), "Equivalent typed context keys must share values");
        helper.assertTrue(ctx.get(AmuletEffectContextKey.ofInt(key.id())).isEmpty(), "Distinct value types must not collide");
        helper.assertTrue(ctx.remove(key) && !ctx.getOrDefault(key, false), "Removing a context must restore its default");
        boolean missing = false;
        try {
            ctx.getOrThrow(key);
        } catch (NullPointerException expected) {
            missing = expected.getMessage().contains(key.id().toString());
        }
        helper.assertTrue(missing, "Required context errors must identify the key");
        helper.succeed();
    }

    private static void dedup(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.GEM_AMULET.asStack());
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.EMERALD_AMULET.asStack());
            var manager = AmuletManager.get(player.registryAccess());
            var active = manager.getActiveEffects(player);
            for (var effect : ModAmulets.EMERALD.get().getFlattenEffects()) {
                helper.assertTrue(active.get(effect).size() == 1, "Wrapped and direct amulets must share one effect invocation");
            }
            var ctx = new AmuletEffectContext();
            manager.trigger(player, ctx);
            helper.assertTrue(Math.abs(ctx.getOrDefault(ModAmuletEffectContextKeys.DISCOUNT_RATE, 0F) - 0.3F) < 0.0001F,
                "Gem and emerald must not double the trading discount");
        }
        helper.succeed();
    }

    private static void cache(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var first = ModItems.COMRADE_AMULET.asStack();
            var second = ModItems.COMRADE_AMULET.asStack();
            first.set(ModComponents.COMRADES, new Comrades(List.of(UUID.randomUUID())));
            second.set(ModComponents.COMRADES, new Comrades(List.of(UUID.randomUUID())));
            player.setItemInHand(InteractionHand.MAIN_HAND, first);
            player.setItemInHand(InteractionHand.OFF_HAND, second);
            var manager = AmuletManager.get(player.registryAccess());
            var before = manager.getActiveEffects(player);
            helper.assertTrue(before.get(ImmuneFriendlyDamageAmuletEffect.INSTANCE).size() == 2,
                "Two signed amulets must retain their separate signatures");
            helper.assertTrue(before == manager.getActiveEffects(player), "Unchanged components should hit the cache");
            second.remove(ModComponents.COMRADES);
            var after = manager.getActiveEffects(player);
            helper.assertTrue(before != after && after.get(ImmuneFriendlyDamageAmuletEffect.INSTANCE).size() == 1,
                "In-place component changes must invalidate cached repetition rules");
            manager.clear(player.getUUID());
            helper.assertTrue(after != manager.getActiveEffects(player), "Entity cleanup must discard its cached result");
        }
        helper.succeed();
    }

    private static void nonPlayer(GameTestHelper helper) {
        var entity = new Zombie(EntityType.ZOMBIE, helper.getLevel());
        var manager = AmuletManager.get(entity.registryAccess());
        helper.assertTrue(manager.findAmulets(entity) == null, "Non-player entities must opt in");
        Consumer<AmuletEvent.EntityCheck> check = event -> {
            if (event.getEntity() == entity) event.pass();
        };
        Consumer<AmuletEvent.Find> find = event -> {
            if (event.getEntity() == entity) event.provide(ModItems.ANVIL_AMULET.asStack());
        };
        NeoForge.EVENT_BUS.addListener(check);
        NeoForge.EVENT_BUS.addListener(find);
        try {
            var ctx = new AmuletEffectContext();
            manager.trigger(entity, ctx);
            helper.assertTrue(ctx.getOrDefault(ModAmuletEffectContextKeys.IGNORE_GRAVITY, false)
                && entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0, "Queries must not apply lifecycle modifiers");
            AmuletAbilitiesEventListener.onInventoryTick(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1, "Opt-in entities receive lifecycle effects");
            NeoForge.EVENT_BUS.unregister(find);
            AmuletAbilitiesEventListener.onInventoryTick(new EntityTickEvent.Post(entity));
            helper.assertTrue(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0, "Removing the source removes its modifier");
        } finally {
            NeoForge.EVENT_BUS.unregister(find);
            NeoForge.EVENT_BUS.unregister(check);
            manager.clear(entity.getUUID());
        }
        helper.succeed();
    }

    private static void masks(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.NATURE_AMULET.asStack());
            var manager = AmuletManager.get(player.registryAccess());
            var ctx = new AmuletEffectContext();
            ctx.set(ModAmuletEffectContextKeys.LIVING_ENTITY_CLASS, Cat.class);
            ctx.set(ModAmuletEffectContextKeys.SIMULATE, true);
            manager.trigger(player, ctx);
            helper.assertTrue(ctx.getOrDefault(ModAmuletEffectContextKeys.MASK_VALID, false)
                && ctx.get(ModAmuletEffectContextKeys.TO_AVOID_ENTITY).isEmpty(), "Simulation validates a mask without creating an entity");
            ctx.set(ModAmuletEffectContextKeys.SIMULATE, false);
            manager.trigger(player, ctx);
            var first = ctx.getOrThrow(ModAmuletEffectContextKeys.TO_AVOID_ENTITY);
            helper.assertTrue(first instanceof DummyCat && first.position().equals(player.position()), "A cat mask follows its wearer");
            DummyCat.clear(player);
            manager.trigger(player, ctx);
            helper.assertTrue(first != ctx.getOrThrow(ModAmuletEffectContextKeys.TO_AVOID_ENTITY), "Entity cleanup invalidates the mask");
            DummyCat.clear(player);
        }
        helper.succeed();
    }

    private static void legacy(GameTestHelper helper) {
        var json = new JsonObject();
        json.addProperty("id", "anvilcraft:feather_amulet");
        json.addProperty("count", 1);
        var components = new JsonObject();
        components.addProperty("anvilcraft:amulet", "anvilcraft:feather");
        json.add("components", components);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var stack = ItemStack.CODEC.parse(ops, json).getOrThrow();
        helper.assertTrue(stack.get(ModComponents.AMULET).equals(ModAmulets.FEATHER.getKey())
            && AmuletManager.get(helper.getLevel().registryAccess()).getAmulet(stack) == ModAmulets.FEATHER.get(),
            "Existing component IDs must still resolve the refactored amulet");
        helper.succeed();
    }

    private static void ai(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.CAT_AMULET.asStack());
            player.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(2, 10, 2)).getCenter());
            helper.getLevel().addNewPlayer(player);
            try {
                var mob = new net.minecraft.world.entity.monster.Creeper(EntityType.CREEPER, helper.getLevel());
                mob.setPos(player.getX() + 2, player.getY(), player.getZ());
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                    mob.getBoundingBox().inflate(6)).contains(player), "The wearer must be in the actual entity query");
                helper.assertTrue(mob.hasLineOfSight(DummyCat.fromEntity(player)),
                    "The test wearer must not be hidden by the storage core");
                var avoid = new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(mob, Cat.class, 6, 1, 1.2);
                avoid.canUse();
                var avoided = net.minecraft.world.entity.ai.goal.AvoidEntityGoal.class.getDeclaredField("toAvoid");
                avoided.setAccessible(true);
                helper.assertTrue(avoided.get(avoid) instanceof DummyCat, "The actual avoidance goal must discover the wearer as a cat");
                mob.setTarget(player);
                helper.assertTrue(mob.getTarget() == null, "Cat impersonation must also stop hostile targeting");
                player.setItemInHand(InteractionHand.OFF_HAND, ModItems.EMERALD_AMULET.asStack());
                var golem = new net.minecraft.world.entity.animal.golem.IronGolem(EntityType.IRON_GOLEM, helper.getLevel());
                var goal = new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    golem, net.minecraft.world.entity.player.Player.class, false);
                var target = net.minecraft.world.entity.ai.goal.target.TargetGoal.class.getDeclaredField("targetMob");
                target.setAccessible(true);
                target.set(goal, player);
                helper.assertTrue(!goal.canContinueToUse() && target.get(goal) == null,
                    "An already cached target must be discarded after amulet protection becomes active");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            } finally {
                player.discard();
            }
        }
        helper.succeed();
    }

    private static void curios(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
            helper.succeed();
            return;
        }
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow();
            handler.reset();
            var slots = java.util.Objects.requireNonNull(handler.getCurios().get("charm"), "Missing charm slots").getStacks();
            var previous = slots.getStackInSlot(0);
            try {
                slots.setStackInSlot(0, ModItems.TOPAZ_AMULET.asStack());
                helper.assertTrue(handler.findCurios(stack -> stack.is(ModItems.TOPAZ_AMULET)).size() == 1,
                    "The fixture must equip one real Curios stack before discovery");
                var manager = AmuletManager.get(player.registryAccess());
                helper.assertTrue(manager.findAmulets(player).stream().anyMatch(stack -> stack.is(ModItems.TOPAZ_AMULET)),
                    "Curios must provide amulets through the new living-entity discovery event");
                slots.setStackInSlot(0, ItemStack.EMPTY);
                helper.assertTrue(manager.findAmulets(player).stream().noneMatch(stack -> stack.is(ModItems.TOPAZ_AMULET)),
                    "Removing a Curios amulet must remove it from discovery");
                AnvilCraft.LOGGER.info("PORT_AMULET_CURIOS_PASSED");
            } finally {
                slots.setStackInSlot(0, previous);
            }
        }
        helper.succeed();
    }

}
