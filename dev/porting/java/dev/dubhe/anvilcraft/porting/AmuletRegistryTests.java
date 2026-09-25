package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.event.PlayerEventListener;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AmuletRegistryTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_amulet_registered_identity", AmuletRegistryTests::identity,
        "port_amulet_signature_codec", AmuletRegistryTests::codec,
        "port_amulet_component_weight", AmuletRegistryTests::weight,
        "port_amulet_comrade_signing", AmuletRegistryTests::signing,
        "port_amulet_comrade_damage", AmuletRegistryTests::damage,
        "port_amulet_registered_discount", AmuletRegistryTests::discount
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_amulet_registry"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void identity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var manager = AmuletManager.get(player.registryAccess());
            helper.assertTrue(ModAmulets.CAT.get() != ModAmulets.DOG.get()
                && manager.getAmulet(ModItems.CAT_AMULET.asStack()) == ModAmulets.CAT.get(), "相同实现类型的护符必须保持独立注册身份");
            player.getInventory().setItem(0, ModItems.GEM_AMULET.asStack());
            helper.assertTrue(manager.hasAmuletInInventory(player, ModAmulets.EMERALD.getKey())
                && manager.hasAmuletInInventory(player, ModAmulets.RUBY.getKey())
                && !manager.hasAmuletInInventory(player, ModAmulets.CAT.getKey()), "复合宝石护符必须按注册键提供对应成员能力");
            player.getInventory().setItem(0, ModItems.NATURE_AMULET.asStack());
            helper.assertTrue(manager.hasAmuletInInventory(player, ModAmulets.ARMADILLO.getKey())
                && !manager.hasAmuletInInventory(player, ModAmulets.FEATHER.getKey()), "自然护符包含犰狳但不能再充当羽毛护符");
            var unknown = ModItems.CAT_AMULET.asStack();
            unknown.set(ModComponents.AMULET, ResourceKey.create(ModRegistryKeys.AMULET, AnvilCraft.of("port_unknown")));
            helper.assertTrue(manager.getAmulet(unknown) == null, "未注册护符键应安全地没有行为");
        }
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        UUID signer = UUID.randomUUID();
        var stack = ModItems.EMERALD_AMULET.asStack();
        stack.set(ModComponents.AMULET, ModAmulets.COMRADE.getKey());
        stack.set(ModComponents.AMULET_WEIGHT, 4);
        stack.set(ModComponents.COMRADES, new Comrades(List.of(signer)));
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        helper.assertTrue(json.getAsJsonObject().getAsJsonObject("components").get("anvilcraft:amulet").getAsString()
            .equals("anvilcraft:comrade"), "持久化护符身份必须使用注册键字符串");
        var restored = ItemStack.CODEC.parse(ops, json).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var synced = ItemStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(ModAmulets.COMRADE.getKey().equals(synced.get(ModComponents.AMULET))
                && synced.getOrDefault(ModComponents.AMULET_WEIGHT, 0) == 4
                && synced.getOrDefault(ModComponents.COMRADES, Comrades.EMPTY).contains(signer),
                "身份、重量与签名必须独立保存并完整网络同步");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void weight(GameTestHelper helper) {
        helper.assertTrue(ModItems.CAT_AMULET.asStack().getOrDefault(ModComponents.AMULET_WEIGHT, 0) == 6
            && ModItems.GEM_AMULET.asStack().getOrDefault(ModComponents.AMULET_WEIGHT, 0) == 9, "大小护符默认重量应为六和九");
        var mutable = BoxContents.EMPTY.mutable();
        var cat = ModItems.CAT_AMULET.asStack();
        cat.set(ModComponents.AMULET_WEIGHT, 4);
        helper.assertTrue(mutable.tryInsert(cat).isPresent() && mutable.immutable().usage() == 4,
            "护符盒必须读取物品重量组件而非行为对象");
        helper.assertTrue(mutable.tryInsert(ModItems.DOG_AMULET.asStack()).isPresent()
            && mutable.immutable().usage() == 10, "不同注册身份的小护符可共同装入");
        for (int index = 0; index < 6; index++) {
            helper.assertTrue(mutable.tryInsert(new ItemStack(Items.TOTEM_OF_UNDYING)).isPresent(), "图腾仍占一个容量");
        }
        helper.assertTrue(mutable.immutable().usage() == 16
            && mutable.tryInsert(new ItemStack(Items.TOTEM_OF_UNDYING)).isEmpty(), "满盒不得再插入图腾");
        mutable.select(0);
        helper.assertTrue(mutable.pop().is(ModItems.CAT_AMULET.get()) && mutable.immutable().usage() == 12,
            "取出时应按原物品重量扣减容量");
        helper.succeed();
    }

    private static void signing(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var stack = ModItems.COMRADE_AMULET.asStack();
            player.getInventory().setItem(0, stack);
            var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            PlayerEventListener.onPlayerUse(event);
            helper.assertTrue(event.getCancellationResult() == InteractionResult.SUCCESS_SERVER
                && stack.getOrDefault(ModComponents.COMRADES, Comrades.EMPTY).players().equals(List.of(player.getUUID()))
                && stack.get(ModComponents.AMULET).equals(ModAmulets.COMRADE.getKey()), "签名只应新增签名组件，不得改变护符身份");
            var repeated = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
            PlayerEventListener.onPlayerUse(repeated);
            helper.assertTrue(repeated.getCancellationResult() == InteractionResult.FAIL
                && stack.get(ModComponents.COMRADES).players().size() == 1, "重复签名不得重复写入同一玩家");
        }
        helper.succeed();
    }

    private static void damage(GameTestHelper helper) {
        var victim = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortVictim")) {
            @Override
            public boolean canHarmPlayer(Player target) {
                // FakePlayer disables PvP independently of game rules; allow the normal damage pipeline here.
                return true;
            }
        };
        var attacker = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortSigner"));
        var signed = ModItems.COMRADE_AMULET.asStack();
        signed.set(ModComponents.COMRADES, Comrades.EMPTY.sign(attacker));
        victim.getInventory().setItem(0, signed);
        victim.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        victim.setInvulnerable(false);
        victim.getAbilities().invulnerable = false;
        victim.setHealth(20);
        victim.connection.markClientLoaded();
        victim.hurtServer(helper.getLevel(), victim.damageSources().playerAttack(attacker), 4);
        helper.assertTrue(victim.getHealth() == 20, "手中生效的已签名护符必须阻止签名玩家伤害");
        var contents = BoxContents.EMPTY.mutable();
        contents.tryInsert(signed.copy());
        var box = ModItems.AMULET_BOX.asStack();
        box.set(ModComponents.BOX_CONTENTS, contents.immutable());
        victim.getInventory().setItem(0, box);
        victim.hurtServer(helper.getLevel(), victim.damageSources().playerAttack(attacker), 4);
        helper.assertTrue(victim.getHealth() == 20, "护符盒内的签名应同样参与伤害判定");
        victim.getInventory().setItem(0, ModItems.COMRADE_AMULET.asStack());
        var source = victim.damageSources().playerAttack(attacker);
        helper.assertTrue(!AmuletManager.get(victim.registryAccess()).shouldImmune(victim, source), "未签名护符不能授予免疫");
        victim.hurtServer(helper.getLevel(), source, 4);
        helper.assertTrue(victim.getHealth() < 20, "未签名护符不能阻止玩家伤害");
        helper.succeed();
    }

    private static void discount(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            fixture.player().getInventory().setItem(0, ModItems.GEM_AMULET.asStack());
            var villager = new Villager(EntityType.VILLAGER, helper.getLevel());
            var offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), new ItemStack(Items.BREAD), 10, 0, 0);
            villager.getOffers().clear();
            villager.getOffers().add(offer);
            try {
                var update = Villager.class.getDeclaredMethod("updateSpecialPrices", Player.class);
                update.setAccessible(true);
                update.invoke(villager, fixture.player());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
            helper.assertTrue(offer.getSpecialPriceDiff() == -3, "复合护符应解析注册键并应用三成交易折扣");
        }
        helper.succeed();
    }

}
