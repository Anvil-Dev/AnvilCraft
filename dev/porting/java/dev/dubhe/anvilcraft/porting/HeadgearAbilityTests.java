package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.AmuletAbilities;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import dev.dubhe.anvilcraft.network.SwitchEquipmentAbilityPacket;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class HeadgearAbilityTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_headgear_attributes", HeadgearAbilityTests::attributes,
        "port_headgear_breathing", HeadgearAbilityTests::breathing,
        "port_headgear_mining", HeadgearAbilityTests::mining,
        "port_headgear_night_vision", HeadgearAbilityTests::nightVision,
        "port_headgear_recipe", HeadgearAbilityTests::recipe,
        "port_amulet_effects", HeadgearAbilityTests::effects,
        "port_amulet_food_effects", HeadgearAbilityTests::food,
        "port_amulet_taming_golem", HeadgearAbilityTests::taming,
        "port_amulet_celestial_gravity", HeadgearAbilityTests::gravity
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_headgear"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static double attribute(ItemStack stack, Holder<Attribute> attribute) {
        return stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
            .filter(entry -> entry.attribute().equals(attribute)).mapToDouble(entry -> entry.modifier().amount()).sum();
    }

    private static void attributes(GameTestHelper helper) {
        var basic = ModItems.BREATHING_HELMET.asStack();
        var weatherproof = ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack();
        helper.assertTrue(basic.getMaxDamage() == 165 && attribute(basic, Attributes.ARMOR) == 2
            && attribute(basic, Attributes.ARMOR_TOUGHNESS) == 0, "呼吸头盔应保留铁盔属性与 165 耐久");
        helper.assertTrue(weatherproof.getMaxDamage() == 814 && attribute(weatherproof, Attributes.ARMOR) == 5
            && attribute(weatherproof, Attributes.ARMOR_TOUGHNESS) == 4 && attribute(weatherproof, Attributes.KNOCKBACK_RESISTANCE) == 0.1,
            "全天候头盔属性必须与源版完全一致");
        helper.assertTrue(basic.isValidRepairItem(new ItemStack(Items.IRON_INGOT))
            && !basic.isValidRepairItem(ModItems.MULTIPHASE_MATTER.asStack())
            && weatherproof.isValidRepairItem(ModItems.MULTIPHASE_MATTER.asStack())
            && !weatherproof.isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT)), "维修材料应分别为铁锭和多相物质");
        helper.assertTrue(weatherproof.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.HEAD
            && weatherproof.get(DataComponents.DAMAGE_RESISTANT) != null
            && weatherproof.getOrDefault(ModComponents.NIGHT_VISION_ENABLED, false), "穿戴部位、防火与默认夜视组件必须齐全");
        helper.succeed();
    }

    private static void breathing(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setAirSupply(30);
            player.setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack());
            player.setAirSupply(20);
            helper.assertTrue(player.getAirSupply() == 30, "呼吸装备应阻止原版及外部路径减少气息");
            var event = new LivingBreatheEvent(player, false, 1, 0);
            EquipmentAbilities.breathe(event);
            helper.assertTrue(event.canBreathe() && event.getConsumeAirAmount() == 0
                && event.getRefillAirAmount() == player.getMaxAirSupply(), "呼吸事件应补满气息并停止耗气");
            player.setInvulnerable(false);
            player.getAbilities().invulnerable = false;
            player.connection.markClientLoaded();
            player.setHealth(20);
            player.hurtServer(helper.getLevel(), player.damageSources().drown(), 4);
            helper.assertTrue(player.getHealth() == 20, "头盔应取消实际溺水伤害");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.hurtServer(helper.getLevel(), player.damageSources().drown(), 4);
            helper.assertTrue(player.getHealth() < 20, "取下头盔后应恢复溺水伤害");
        }
        helper.succeed();
    }

    private static void mining(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(1, y, 1), Blocks.WATER);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            player.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
            player.baseTick();
            player.setOnGround(false);
            helper.assertTrue(player.isInWater(), "挖掘测试必须真实浸水");
            float bare = player.getDestroySpeed(Blocks.STONE.defaultBlockState(), pos);
            player.setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack());
            float equipped = player.getDestroySpeed(Blocks.STONE.defaultBlockState(), pos);
            helper.assertTrue(equipped >= bare * 24.99F && equipped > 1, "头盔必须同时移除水下和水中悬浮挖掘惩罚");
        }
        helper.succeed();
    }

    private static void nightVision(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var helmet = ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, helmet);
            new SwitchEquipmentAbilityPacket(InteractionHand.MAIN_HAND, EquipmentSlot.FEET, false).handleOnServer(player);
            helper.assertTrue(helmet.get(ModComponents.NIGHT_VISION_ENABLED), "错部位切换请求应拒绝");
            new SwitchEquipmentAbilityPacket(InteractionHand.MAIN_HAND, EquipmentSlot.HEAD, false).handleOnServer(player);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            EquipmentAbilities.afterTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "关闭后不能自动提供夜视");
            helmet.set(ModComponents.NIGHT_VISION_ENABLED, true);
            EquipmentAbilities.afterTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "穿戴开启的全天候头盔应提供夜视");
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            EquipmentAbilities.afterTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "卸下时应移除头盔自己提供的夜视");
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            EquipmentAbilities.afterTick(new PlayerTickEvent.Post(player));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 1));
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            EquipmentAbilities.afterTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 1200
                && player.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 1, "卸下头盔不得删除外部药水夜视");
        }
        helper.succeed();
    }

    private static void recipe(GameTestHelper helper) {
        var first = ModItems.BREATHING_HELMET.asStack();
        first.set(DataComponents.CUSTOM_NAME, Component.literal("Headgear port"));
        var second = new ItemStack(Items.NETHERITE_HELMET);
        var unbreaking = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        first.enchant(unbreaking, 1);
        second.enchant(unbreaking, 2);
        var input = new MultipleToOneSmithingRecipeInput(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.asStack(),
            ModItems.WEATHERPROOF_CORE.asStack(), List.of(first, second));
        var result = RecipesRecord.getRecipes(helper.getLevel()).getRecipesFor(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get(),
            input, helper.getLevel()).findFirst().orElseThrow().value().assemble(input, helper.getLevel());
        helper.assertTrue(result.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET) && result.getHoverName().getString().equals("Headgear port")
            && result.get(DataComponents.ENCHANTMENTS).getLevel(unbreaking) == 2, "升级配方必须保留名称并合并两个头盔的附魔");
        helper.succeed();
    }

    private static void effects(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.getInventory().setItem(0, ModItems.GEM_AMULET.asStack());
            player.setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack());
            AmuletAbilities.onTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.hasEffect(MobEffects.HASTE) && player.hasEffect(MobEffects.STRENGTH)
                && player.hasEffect(MobEffects.RESISTANCE), "宝石复合护符应提供急迫、力量及呼吸装备联动抗性");
            player.igniteForSeconds(10);
            AmuletAbilities.onTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.getEffect(MobEffects.STRENGTH).getAmplifier() == 1, "燃烧时红宝石力量应升为 II");
            player.getInventory().setItem(0, ModItems.FEATHER_AMULET.asStack());
            AmuletAbilities.onTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(player.hasEffect(MobEffects.SLOW_FALLING), "羽毛护符应提供缓降");
            player.setShiftKeyDown(true);
            AmuletAbilities.onTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.hasEffect(MobEffects.SLOW_FALLING)
                && !player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100)), "潜行时应移除并拒绝缓降");
            player.getInventory().setItem(0, ModItems.ANVIL_AMULET.asStack());
            AmuletAbilities.onTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100)), "铁砧护符应拒绝漂浮");
            var knockback = new LivingKnockBackEvent(player, 1, 1, 0);
            AmuletAbilities.onKnockback(knockback);
            helper.assertTrue(knockback.isCanceled() && player.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
                .hasModifier(AnvilCraft.of("anvil_amulet_knockback_resistance")), "铁砧护符应同时拦截击退并提供属性");
            var explosion = new ServerExplosion(helper.getLevel(), null, null, null, player.position(), 1, false,
                Explosion.BlockInteraction.KEEP);
            var blast = new ExplosionKnockbackEvent(helper.getLevel(), explosion, player, new Vec3(1, 1, 1), List.of());
            AmuletAbilities.onExplosionKnockback(blast);
            helper.assertTrue(blast.getKnockbackVelocity().equals(Vec3.ZERO), "爆炸击退也必须被铁砧护符归零");
            player.getInventory().setItem(0, ModItems.SILENCE_AMULET.asStack());
            AmuletAbilities.onTick(new PlayerTickEvent.Post(player));
            helper.assertTrue(!player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100))
                && !player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).hasModifier(AnvilCraft.of("anvil_amulet_knockback_resistance")),
                "寂静护符应拒绝黑暗，移除铁砧后应清理其属性");
        }
        helper.succeed();
    }

    private static void food(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.ABNORMAL_AMULET.asStack());
            player.getFoodData().setFoodLevel(10);
            new ItemStack(Items.PUFFERFISH).finishUsingItem(helper.getLevel(), player);
            helper.assertTrue(!player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.HUNGER)
                && !player.hasEffect(MobEffects.NAUSEA), "真实食物消费路径中的负面效果应被异常护符过滤");
            helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.POISON, 200)), "普通来源的负面效果不能被异常护符泛化免疫");
            try {
                AmuletAbilities.consumeFood(player, () -> {
                    throw new IllegalStateException("fixture");
                });
            } catch (IllegalStateException expected) {
                helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200)), "消费异常后不得泄露线程局部免疫状态");
            }
        }
        helper.succeed();
    }

    private static void taming(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.CAT_AMULET.asStack());
            var cat = new Cat(EntityType.CAT, helper.getLevel());
            var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, cat);
            AmuletAbilities.onInteract(event);
            helper.assertTrue(event.isCanceled() && cat.isTame() && cat.isOrderedToSit()
                && cat.getOwnerReference().getUUID().equals(player.getUUID()), "空手一次交互应驯服野猫并设置正确主人");
            var wolf = new Wolf(EntityType.WOLF, helper.getLevel());
            AmuletAbilities.onInteract(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, wolf));
            helper.assertTrue(!wolf.isTame(), "猫护符不能驯服狼");
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.DOG_AMULET.asStack());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            AmuletAbilities.onInteract(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, wolf));
            helper.assertTrue(!wolf.isTame(), "非空手不能触发免费驯服");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            AmuletAbilities.onInteract(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, wolf));
            helper.assertTrue(wolf.isTame() && wolf.getOwnerReference().getUUID().equals(player.getUUID()), "狗护符应一次驯服野狼");
            player.setItemInHand(InteractionHand.OFF_HAND, ModItems.EMERALD_AMULET.asStack());
            var golem = new IronGolem(EntityType.IRON_GOLEM, helper.getLevel());
            golem.setLastHurtByMob(player);
            helper.assertTrue(golem.getLastHurtByMob() == null, "绿宝石护符持有者不能成为铁傀儡的复仇目标");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            golem.setLastHurtByMob(player);
            helper.assertTrue(golem.getLastHurtByMob() == player, "移除护符后应恢复正常复仇记录");
        }
        helper.succeed();
    }

    private static void gravity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            BlockPos body = helper.absolutePos(new BlockPos(1, 3, 1));
            BlockPos field = body.offset(0, 1, 0);
            Vec3 center = player.getBoundingBox().getCenter().add(3, 0, 0);
            try {
                GravityManager.GravitySourceManager.upsertSource(helper.getLevel(), body, center,
                    new GravityManager.GravitySourceType(10, 8, 0.5));
                final Vec3 normal = GravityManager.getGravityVector(player, 0.08);
                player.getInventory().setItem(0, ModItems.ANVIL_AMULET.asStack());
                Vec3 protectedValue = GravityManager.getGravityVector(player, 0.08);
                player.noPhysics = false;
                helper.assertTrue(GravityManager.applyMovementEffects(player, new Vec3(6, 0, 0)).x < 3,
                    "引力豁免不得移除天体实体碰撞");
                helper.assertTrue(normal.distanceToSqr(protectedValue) > 0.000001, "铁砧护符必须排除天体引力");
                GravityManager.GravitySourceManager.upsertSource(helper.getLevel(), field, center,
                    new GravityManager.GravitySourceType(1, 8));
                Vec3 regularField = GravityManager.getGravityVector(player, 0.08);
                helper.assertTrue(regularField.distanceToSqr(protectedValue) > 0.00000001, "普通非天体引力不能被一并清除");
                player.getInventory().setItem(0, ItemStack.EMPTY);
                player.setShiftKeyDown(true);
                helper.assertTrue(GravityManager.getGravityVector(player, 0.08).distanceToSqr(regularField) < 0.00000001,
                    "潜行应按源版相同规则忽略天体引力");
                GravityManager.GravitySourceManager.removeSource(helper.getLevel(), field);
                player.setShiftKeyDown(false);
                player.getInventory().setItem(0, ItemStack.EMPTY);
                player.setPos(center.x - 10, player.getY(), center.z);
                player.noPhysics = true;
                Vec3 swept = GravityManager.applyMovementEffects(player, new Vec3(20, 0, 0));
                player.getInventory().setItem(0, ModItems.ANVIL_AMULET.asStack());
                Vec3 protectedSweep = GravityManager.applyMovementEffects(player, new Vec3(20, 0, 0));
                helper.assertTrue(swept.distanceToSqr(protectedSweep) > 0.0000000001
                    && protectedSweep.distanceToSqr(new Vec3(20, 0, 0)) < 0.0000000001,
                    "完整穿过天体场的扫掠积分也必须遵循护符豁免");
            } finally {
                GravityManager.GravitySourceManager.removeSource(helper.getLevel(), body);
                GravityManager.GravitySourceManager.removeSource(helper.getLevel(), field);
            }
        }
        helper.succeed();
    }
}
