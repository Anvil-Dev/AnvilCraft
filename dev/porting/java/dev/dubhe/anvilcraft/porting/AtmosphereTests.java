package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModLevelKeys;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.Atmosphere;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AtmosphereTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_atmosphere_composition", AtmosphereTests::composition,
        "port_atmosphere_breathing", AtmosphereTests::breathing,
        "port_atmosphere_entity_drag", AtmosphereTests::entities,
        "port_atmosphere_projectile_drag", AtmosphereTests::projectiles,
        "port_atmosphere_flying_drag", AtmosphereTests::flying,
        "port_atmosphere_surface_friction", AtmosphereTests::surface
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_atmosphere"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void composition(GameTestHelper helper) {
        var atmosphere = new Atmosphere(Map.of(Atmosphere.OXYGEN, 0.3, Atmosphere.NITROGEN, 0.9));
        helper.assertTrue(Math.abs(atmosphere.pressure() - 1.2) < 0.000001 && atmosphere.gasAmount(Atmosphere.OXYGEN) == 0.3,
            "气体含量不能被归一化，总和必须保留为气压");
        helper.assertTrue(AtmosphereManager.getDimensionAirResistance(ModLevelKeys.MUN) == 0.1
            && AtmosphereManager.getDimensionAirResistance(ModLevelKeys.VOID_PLANET) == 0
            && AtmosphereManager.getDimensionAtmosphere(ModLevelKeys.MUN).gasAmount(Atmosphere.OXYGEN) == 0,
            "月球采用真空和最小阻力，虚空行星显式采用零阻力");
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            boolean rejected = false;
            try {
                new Atmosphere(Map.of(Atmosphere.OXYGEN, invalid));
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "非法气体含量必须拒绝");
        }
        withAtmosphere(helper, atmosphere, Double.NaN, () -> {
            helper.assertTrue(AtmosphereManager.getAtmosphere(helper.getLevel(), BlockPos.ZERO).equals(atmosphere)
                && Math.abs(AtmosphereManager.drag(helper.getLevel(), 0.91) - Math.pow(0.91, 1.2)) < 0.00000001,
                "位置入口应使用维度大气，阻力应为原版系数的气压次幂");
        });
        helper.succeed();
    }

    private static void breathing(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.setAirSupply(100);
            withAtmosphere(helper, Atmosphere.VACUUM, Double.NaN, () -> {
                helper.assertTrue(AtmosphereManager.isSuffocating(player), "无氧空气中的生存玩家必须窒息");
                player.baseTick();
                helper.assertTrue(player.getAirSupply() < 100, "真实实体呼吸逻辑必须消耗气息");
                int air = player.getAirSupply();
                player.setAirSupply(200);
                helper.assertTrue(player.getAirSupply() == air, "真空中不能绕过呼吸事件自行补气");
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200));
                helper.assertTrue(AtmosphereManager.isSuffocating(player), "水下呼吸药水不能让玩家在无氧空气中呼吸");
                for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(1, y, 1), Blocks.WATER);
                BlockPos water = helper.absolutePos(new BlockPos(1, 1, 1));
                player.setPos(water.getX() + 0.5, water.getY(), water.getZ() + 0.5);
                player.baseTick();
                helper.assertTrue(player.isEyeInFluid(FluidTags.WATER) && !AtmosphereManager.isSuffocating(player), "真空中的水与水下呼吸应允许呼吸");
                player.removeEffect(MobEffects.WATER_BREATHING);
                helper.assertTrue(AtmosphereManager.isSuffocating(player), "无药水的玩家仍会在真空水体中溺水");
                player.setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack());
                player.baseTick();
                helper.assertTrue(!AtmosphereManager.isSuffocating(player) && player.getAirSupply() == player.getMaxAirSupply(),
                    "呼吸头盔应覆盖无氧环境判定并实际补满气息");
                var pig = new Pig(EntityType.PIG, helper.getLevel());
                pig.setPos(player.getX(), player.getY() + 10, player.getZ());
                pig.setAirSupply(-19);
                float health = pig.getHealth();
                pig.baseTick();
                helper.assertTrue(pig.getAirSupply() == 0 && pig.getHealth() == health - 2,
                    "真空耗尽气息必须触发实际窒息伤害并重置气息计时：" + pig.getAirSupply() + "/" + pig.getHealth());
                var skeleton = new Skeleton(EntityType.SKELETON, helper.getLevel());
                var golem = new IronGolem(EntityType.IRON_GOLEM, helper.getLevel());
                var fish = new Cod(EntityType.COD, helper.getLevel());
                helper.assertTrue(!AtmosphereManager.requiresBreathing(skeleton) && !AtmosphereManager.requiresBreathing(golem)
                    && AtmosphereManager.requiresBreathing(fish), "不需呼吸生物和铁傀儡应豁免，水生生物仍需判断环境");
                fish.setAirSupply(30);
                helper.assertTrue(AtmosphereManager.isSuffocating(fish), "水生生物不能在真空空气中呼吸");
                fish.setAirSupply(300);
                helper.assertTrue(fish.getAirSupply() == 30, "水生生物的独立补气路径也必须受无氧条件约束");
            });
        }
        helper.succeed();
    }

    private static void entities(GameTestHelper helper) {
        for (double resistance : new double[]{0, 1, 2}) {
            withAtmosphere(helper, Atmosphere.OVERWORLD, resistance, () -> {
                BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
                var item = new ItemEntity(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.STONE));
                var orb = new ExperienceOrb(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), 1);
                var block = new FallingBlockEntity(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), Blocks.SAND.defaultBlockState());
                var pig = new Pig(EntityType.PIG, helper.getLevel());
                try {
                    for (Entity entity : List.of(item, orb, block, pig)) {
                        entity.setPos(pos.getX(), pos.getY(), pos.getZ());
                        entity.setNoGravity(true);
                        entity.setDeltaMovement(1, 1, 0);
                        if (entity == pig) pig.travel(Vec3.ZERO);
                        else entity.tick();
                        double expected = Math.pow(entity == pig ? 0.91F : 0.98F, resistance);
                        helper.assertTrue(Math.abs(entity.getDeltaMovement().x - expected) < 0.000001,
                            "不同实体的真实空气阻力必须生效：" + entity.getType());
                    }
                } finally {
                    for (Entity entity : List.of(item, orb, block, pig)) entity.discard();
                }
            });
        }
        helper.succeed();
    }

    private static void projectiles(GameTestHelper helper) {
        withAtmosphere(helper, Atmosphere.OVERWORLD, 0, () -> {
            BlockPos pos = helper.absolutePos(new BlockPos(1, 20, 1));
            var snowball = new Snowball(EntityType.SNOWBALL, helper.getLevel());
            var arrow = new Arrow(EntityType.ARROW, helper.getLevel());
            var fireball = new SmallFireball(EntityType.SMALL_FIREBALL, helper.getLevel());
            fireball.accelerationPower = 0;
            try {
                for (Entity entity : List.of(snowball, arrow, fireball)) {
                    entity.setPos(pos.getX(), pos.getY(), pos.getZ());
                    entity.setNoGravity(true);
                    entity.setDeltaMovement(1, 0, 0);
                    entity.tick();
                    helper.assertTrue(Math.abs(entity.getDeltaMovement().x - 1) < 0.000001, "零空气阻力不能继续衰减投射物：" + entity.getType());
                }
            } finally {
                for (Entity entity : List.of(snowball, arrow, fireball)) entity.discard();
            }
        });
        helper.succeed();
    }

    private static void flying(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            withAtmosphere(helper, Atmosphere.OVERWORLD, 0, () -> {
                player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
                player.getAbilities().flying = true;
                helper.assertTrue(AtmosphereManager.drag(player, 0.91F) == 0.91F, "创造飞行应保持原版阻力");
                player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
                player.getAbilities().flying = true;
                helper.assertTrue(AtmosphereManager.drag(player, 0.91F) == 1, "生存飞行仍需使用大气阻力");
                player.getAbilities().flying = false;
                player.setXRot(-30);
                player.setYRot(0);
                try {
                    Method movement = net.minecraft.world.entity.LivingEntity.class
                        .getDeclaredMethod("updateFallFlyingMovement", Vec3.class);
                    movement.setAccessible(true);
                    Vec3 result = (Vec3) movement.invoke(player, new Vec3(0, -0.1, 1));
                    helper.assertTrue(result.y < -0.1 && Math.abs(result.z - 1) < 0.000001,
                        "零阻力环境中的鞘翅不能凭无空气产生升力，但应保持前进惯性：" + result);
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
            });
        }
        helper.succeed();
    }

    private static void surface(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        var entity = new ItemEntity(helper.getLevel(), floor.getX() + 0.875, floor.getY() + 1, floor.getZ() + 0.5,
            new ItemStack(Items.STONE));
        try {
            entity.setNoGravity(true);
            helper.assertTrue(GravityManager.hasCustomSurfaceFriction(entity), "零重力接触应启用方向摩擦");
            Vec3 result = GravityManager.applySurfaceFriction(entity, new Vec3(1, 1, 1));
            helper.assertTrue(Math.abs(result.x - 0.6) < 0.000001 && Math.abs(result.y - 0.6) < 0.000001
                && Math.abs(result.z - 0.6) < 0.000001, "墙地角落同一切向分量只能乘一次摩擦系数");
        } finally {
            entity.discard();
        }
        helper.succeed();
    }

    private static void withAtmosphere(GameTestHelper helper, Atmosphere atmosphere, double resistance, Runnable action) {
        var dimension = helper.getLevel().dimension();
        Atmosphere previous = AtmosphereManager.getDimensionAtmosphere(dimension);
        double previousResistance = AtmosphereManager.getDimensionAirResistance(dimension);
        try {
            AtmosphereManager.registerDimensionAtmosphere(dimension, atmosphere);
            AtmosphereManager.clearDimensionAirResistance(dimension);
            if (!Double.isNaN(resistance)) AtmosphereManager.registerDimensionAirResistance(dimension, resistance);
            action.run();
        } finally {
            AtmosphereManager.registerDimensionAtmosphere(dimension, previous);
            AtmosphereManager.clearDimensionAirResistance(dimension);
            if (previousResistance != Math.max(AtmosphereManager.MIN_AIR_RESISTANCE, previous.pressure())) {
                AtmosphereManager.registerDimensionAirResistance(dimension, previousResistance);
            }
        }
    }
}
