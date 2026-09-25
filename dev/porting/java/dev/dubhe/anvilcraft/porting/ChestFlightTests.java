package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.energy.ItemFEStorage;
import dev.dubhe.anvilcraft.api.item.ICapacitorChargeable;
import dev.dubhe.anvilcraft.api.item.IFullCapacitor;
import dev.dubhe.anvilcraft.api.power.DynamicPowerComponent;
import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.armor.WeatherproofChestplateItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ChestFlightTests {
    private static LimitedChargeItem limitedItem;
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_chest_grid_tiers", ChestFlightTests::tiers,
        "port_chest_grid_sharing", ChestFlightTests::sharing,
        "port_chest_grid_changes", ChestFlightTests::changes,
        "port_chest_grid_charge", ChestFlightTests::gridCharge,
        "port_chest_capacitor_transactions", ChestFlightTests::capacitors,
        "port_backpack_grid_flight", ChestFlightTests::gridFlight,
        "port_chest_stored_flight", ChestFlightTests::storedFlight,
        "port_chest_full_suit", ChestFlightTests::fullSuit
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
        event.register(Registries.ITEM, registry -> {
            var id = ResourceKey.create(Registries.ITEM, Identifier.parse("anvilcraft_porting:limited_charge"));
            limitedItem = new LimitedChargeItem(new Item.Properties().setId(id).stacksTo(1)
                .component(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY));
            registry.register(id.identifier(), limitedItem);
        });
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_chest_flight"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void tiers(GameTestHelper helper) {
        try (var fixture = new GridFixture(helper)) {
            int[][] cases = {{0, 0}, {63, 0}, {64, 64}, {127, 64}, {128, 128}, {255, 128}, {256, 256}, {511, 256}, {512, 512}, {4096, 512}};
            for (int[] entry : cases) {
                fixture.generator.output = entry[0];
                fixture.grid.flush();
                helper.assertTrue(fixture.grid.getConsume() == entry[1] && fixture.grid.isWorking(),
                    "胸甲必须只申请真实余电可承担的分档：" + entry[0]);
                for (int tick = 0; tick < 40; tick++) IonoCraftBackpackItem.refreshPower(fixture.player());
                helper.assertTrue(fixture.component().getPowerConsumption() == entry[1], "玩家 tick 不能重复申请或抬高充电档位");
            }
            energy(fixture.chest(), WeatherproofChestplateItem.MAX_ENERGY);
            fixture.grid.flush();
            helper.assertTrue(fixture.grid.getConsume() == 0, "满电胸甲必须释放充电需求");
        }
        helper.succeed();
    }

    private static void sharing(GameTestHelper helper) {
        try (var fixture = new GridFixture(helper); var second = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = second.player();
            player.setGameMode(GameType.SURVIVAL);
            player.setItemSlot(EquipmentSlot.CHEST, ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack());
            var component = component(player);
            component.switchTo(fixture.grid);
            try {
                fixture.generator.output = 512;
                var ordinary = new DynamicPowerComponent.PowerConsumption(100);
                fixture.component().getPowerConsumptions().add(ordinary);
                fixture.grid.flush();
                helper.assertTrue(fixture.grid.getConsume() == 356 && fixture.grid.isWorking()
                    && fixture.component().getPowerConsumption() == 228 && component.getPowerConsumption() == 128,
                    "应先保留普通动态负载，再按玩家人数分配余电");
                fixture.generator.output = 200;
                fixture.grid.flush();
                helper.assertTrue(fixture.grid.getConsume() == 100 && component.getPowerConsumption() == 0,
                    "人均余电低于 64 kW 时双方均不得充电");
            } finally {
                component.switchTo(null);
            }
        }
        helper.succeed();
    }

    private static void changes(GameTestHelper helper) {
        try (var fixture = new GridFixture(helper)) {
            fixture.generator.output = 512;
            fixture.grid.flush();
            fixture.generator.output = 63;
            fixture.grid.flush();
            helper.assertTrue(fixture.grid.getConsume() == 0 && fixture.grid.isWorking(), "余电下降时必须清理旧档位，不能过载振荡");
            fixture.player().setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            fixture.generator.output = 512;
            fixture.grid.flush();
            helper.assertTrue(fixture.grid.getConsume() == 0, "卸下胸甲必须停止充电需求");
            fixture.player().setItemSlot(EquipmentSlot.CHEST, ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack());
            fixture.grid.flush();
            fixture.player().setPos(fixture.player().getX() + 100, fixture.player().getY(), fixture.player().getZ());
            fixture.grid.flush();
            helper.assertTrue(fixture.component().getPowerGrid() == null && !fixture.player().getData(ModDataAttachments.IN_POWER_GRID),
                "离开范围必须解除组件所属网与同步状态，不能仅从旧网集合删除");
        }
        helper.succeed();
    }

    private static void gridCharge(GameTestHelper helper) {
        var fixture = new GridFixture(helper);
        fixture.generator.output = 64;
        fixture.grid.flush();
        helper.onEachTick(() -> {
            if (helper.getLevel().getGameTime() % PowerGrid.GRID_TICK != 0) return;
            try {
                WeatherproofChestplateItem.tickEnergy(fixture.player());
                helper.assertTrue(WeatherproofChestplateItem.getEnergyStored(fixture.chest()) == 120000,
                    "64 kW 档每个电网周期必须实际充入 120 kFE");
                helper.succeed();
            } finally {
                fixture.close();
            }
        });
    }

    private static void capacitors(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var player = fixture.player();
            var chest = ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack();
            var armor = ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.get();
            var capacitor = (IFullCapacitor) ModItems.CAPACITOR.get();
            energy(chest, 10000);
            helper.assertTrue(!armor.charge(chest, capacitor, ModItems.CAPACITOR.asStack()), "普通自动补能只能在低电量门槛触发");
            var full = ModItems.CAPACITOR.asStack(2);
            full.set(DataComponents.CUSTOM_NAME, Component.literal("named-capacitor"));
            var slot = new Slot(new SimpleContainer(chest), 0, 0, 0);
            helper.assertTrue(IFullCapacitor.tryForceChargeTarget(capacitor, full, slot, ClickAction.SECONDARY, player)
                && WeatherproofChestplateItem.getEnergyStored(chest) == 8010000 && full.getCount() == 1,
                "手动右键强制补能应实际提交能量并只消耗一个电容");
            helper.assertTrue(player.getInventory().countItem(ModItems.CAPACITOR_EMPTY.get()) == 1,
                "成功充电必须返还对应空电容");
            var limited = new ItemStack(limitedItem);
            helper.assertTrue(!limitedItem.chargeForce(limited, capacitor, full)
                && limited.get(ModComponents.STORED_ENERGY).value() == 0, "部分接受的事务必须完整回滚，不能免费增加能量");
            energy(chest, WeatherproofChestplateItem.MAX_ENERGY - 1);
            helper.assertTrue(armor.chargeForce(chest, capacitor, full)
                && WeatherproofChestplateItem.getEnergyStored(chest) == WeatherproofChestplateItem.MAX_ENERGY, "强制充电应允许截断至容量上限");
            helper.assertTrue(!armor.chargeForce(chest, capacitor, full), "满电时不得再次消耗电容");
        }
        helper.succeed();
    }

    private static void gridFlight(GameTestHelper helper) {
        try (var fixture = new GridFixture(helper)) {
            final var player = fixture.player();
            player.setItemSlot(EquipmentSlot.CHEST, ModItems.IONOCRAFT_BACKPACK.asStack());
            helper.assertTrue(!player.getItemBySlot(EquipmentSlot.CHEST).has(ModComponents.STORED_ENERGY)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.CHEST_ARMOR)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.DURABILITY_ENCHANTABLE),
                "普通背包应使用胸甲与耐久附魔标签，且不再携带默认储能");
            fixture.generator.output = 64;
            IonoCraftBackpackItem.refreshPower(player);
            fixture.grid.flush();
            IonoCraftBackpackItem.refreshFlight(player);
            helper.assertTrue(fixture.grid.getConsume() == 8 && player.getAbilities().mayfly, "普通背包应通过 8 kW 电网负载提供飞行");
            player.setOnGround(false);
            player.getAbilities().flying = true;
            fixture.component().switchTo(null);
            IonoCraftBackpackItem.refreshPower(player);
            IonoCraftBackpackItem.refreshFlight(player);
            player.setDeltaMovement(0, -2, 0);
            player.fallDistance = 10;
            IonoCraftBackpackItem.applySlowFalling(player);
            helper.assertTrue(!player.getAbilities().flying && IonoCraftBackpackItem.isSlowFalling(player)
                && player.getDeltaMovement().y == -0.5 && player.fallDistance == 0, "离网必须停止飞行并提供限速缓降");
            IonoCraftBackpackItem.toggleSlowFalling(player);
            helper.assertTrue(!IonoCraftBackpackItem.isSlowFalling(player), "缓降期间应支持关闭");
            fixture.component().switchTo(fixture.grid);
            IonoCraftBackpackItem.refreshPower(player);
            fixture.grid.flush();
            IonoCraftBackpackItem.refreshFlight(player);
            helper.assertTrue(player.getAbilities().flying && !IonoCraftBackpackItem.isSlowFalling(player), "空中重新入网应恢复飞行并移除缓降");
            fixture.component().switchTo(null);
            IonoCraftBackpackItem.refreshFlight(player);
            player.setOnGround(true);
            fixture.component().switchTo(fixture.grid);
            IonoCraftBackpackItem.refreshPower(player);
            fixture.grid.flush();
            IonoCraftBackpackItem.refreshFlight(player);
            helper.assertTrue(!player.getAbilities().flying, "触地和重新入网同时发生时不能自动起飞");
            var flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            var other = AnvilCraft.of("port_other_flight");
            flight.addTransientModifier(new AttributeModifier(other, 1, AttributeModifier.Operation.ADD_VALUE));
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            IonoCraftBackpackItem.refreshFlight(player);
            helper.assertTrue(flight.hasModifier(other) && player.getAbilities().mayfly, "卸下背包不能移除其它飞行能力");
        }
        helper.succeed();
    }

    private static void storedFlight(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            player.setItemSlot(EquipmentSlot.CHEST, ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack());
            var chest = player.getItemBySlot(EquipmentSlot.CHEST);
            energy(chest, 5000);
            IonoCraftBackpackItem.refreshFlight(player);
            player.setOnGround(false);
            player.getAbilities().flying = true;
            WeatherproofChestplateItem.tickEnergy(player);
            IonoCraftBackpackItem.refreshFlight(player);
            helper.assertTrue(WeatherproofChestplateItem.getEnergyStored(chest) == 0 && IonoCraftBackpackItem.isSlowFalling(player),
                "储能胸甲每 tick 消耗 5 kFE，耗尽时也应提供缓降");
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
            player.inventoryMenu.getSlot(46).set(ModItems.SUPER_CAPACITOR.asStack());
            WeatherproofChestplateItem.tickEnergy(player);
            IonoCraftBackpackItem.refreshFlight(player);
            helper.assertTrue(WeatherproofChestplateItem.getEnergyStored(chest) == WeatherproofChestplateItem.MAX_ENERGY
                && player.getAbilities().flying && player.getInventory().countItem(ModItems.SUPER_CAPACITOR_EMPTY.get()) == 1,
                "口袋电容应自动补能并在空中恢复飞行，返还超级空电容");
        }
        helper.succeed();
    }

    private static void fullSuit(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            player.setItemSlot(EquipmentSlot.HEAD, ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack());
            player.setItemSlot(EquipmentSlot.CHEST, ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack());
            player.setItemSlot(EquipmentSlot.LEGS, ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS.asStack());
            player.setItemSlot(EquipmentSlot.FEET, ModItems.WEATHERPROOF_SPACESUIT_BOOTS.asStack());
            player.connection.markClientLoaded();
            player.getAbilities().invulnerable = false;
            player.setInvulnerable(false);
            player.setHealth(20);
            player.hurtServer(helper.getLevel(), player.damageSources().inFire(), 5);
            helper.assertTrue(player.getHealth() == 20 && !EquipmentAbilities.isImmune(player, player.damageSources().fellOutOfWorld()),
                "整套应免疫环境伤害，但不能免疫虚空伤害本身");
            player.setRemainingFireTicks(100);
            player.setTicksFrozen(100);
            EquipmentAbilities.beforeTick(new PlayerTickEvent.Pre(player));
            helper.assertTrue(!player.isOnFire() && player.getTicksFrozen() == 0, "完整套装应清除燃烧与冰冻");
            int bottom = helper.getLevel().getMinY();
            BlockPos floor = player.blockPosition().atY(bottom);
            for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-1, 0, -1), floor.offset(1, 1, 1))) {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            player.setPos(player.getX(), bottom + 0.5, player.getZ());
            player.move(MoverType.SELF, new Vec3(0, -2, 0));
            helper.assertTrue(player.getY() == bottom, "实际碰撞移动必须被套装截停在虚空底面");
            player.setPos(player.getX(), bottom - 2, player.getZ());
            EquipmentAbilities.beforeTick(new PlayerTickEvent.Pre(player));
            helper.assertTrue(player.getDeltaMovement().y == 0.2, "已处于底面以下时应获得回升速度");
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            helper.assertTrue(!EquipmentAbilities.isVoidProtected(player), "缺少任一部件必须失去整套保护");
        }
        helper.succeed();
    }

    private static DynamicPowerComponent component(ServerPlayer player) {
        return IDynamicPowerComponentHolder.of(player).anvilcraft$getPowerComponent();
    }

    private static void energy(ItemStack stack, int value) {
        stack.set(ModComponents.STORED_ENERGY, new StoredEnergy(value));
    }

    private static final class LimitedChargeItem extends Item implements ICapacitorChargeable {
        private LimitedChargeItem(Properties properties) {
            super(properties);
        }

        @Override
        public EnergyHandler getEnergyStorage(ItemStack stack) {
            return new ItemFEStorage(ItemAccess.forStack(stack), 16000000, 4000, 0);
        }
    }

    private static final class GridFixture implements AutoCloseable {
        private final StorageFluidRpcTests.Fixture fixture;
        private final Generator generator;
        private final PowerGrid grid;

        private GridFixture(GameTestHelper helper) {
            this.fixture = new StorageFluidRpcTests.Fixture(helper, true);
            this.player().setGameMode(GameType.SURVIVAL);
            this.player().setItemSlot(EquipmentSlot.CHEST, ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack());
            this.grid = new PowerGrid(helper.getLevel());
            this.generator = new Generator(helper.getLevel(), this.player().blockPosition());
            this.grid.add(this.generator);
            this.component().switchTo(this.grid);
        }

        private ServerPlayer player() {
            return this.fixture.player();
        }

        private ItemStack chest() {
            return this.player().getItemBySlot(EquipmentSlot.CHEST);
        }

        private DynamicPowerComponent component() {
            return ChestFlightTests.component(this.player());
        }

        @Override
        public void close() {
            this.component().switchTo(null);
            this.fixture.close();
        }
    }

    private static final class Generator implements IPowerProducer {
        private final Level level;
        private final BlockPos pos;
        private @Nullable PowerGrid grid;
        private int output;

        private Generator(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public Level getCurrentLevel() {
            return this.level;
        }

        @Override
        public BlockPos getPos() {
            return this.pos;
        }

        @Override
        public int getRange() {
            return 8;
        }

        @Override
        public void setGrid(@Nullable PowerGrid grid) {
            this.grid = grid;
        }

        @Override
        public @Nullable PowerGrid getGrid() {
            return this.grid;
        }

        @Override
        public int getOutputPower() {
            return this.output;
        }
    }
}
