package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import dev.dubhe.anvilcraft.inventory.FrostSmithingTransfer;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class FrostSmithingTests {
    private static final BlockPos TABLE = new BlockPos(1, 2, 1);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_frost_deformation_menu", FrostSmithingTests::deformation,
        "port_frost_permutation_menu", FrostSmithingTests::permutation,
        "port_frost_free_bow", FrostSmithingTests::bow,
        "port_frost_heavy_quantity", FrostSmithingTests::heavy,
        "port_frost_transcendence", FrostSmithingTests::transcendence,
        "port_frost_recipe_codec", FrostSmithingTests::codec,
        "port_frost_transfer_inventory", FrostSmithingTests::transferInventory,
        "port_frost_transfer_terminal", FrostSmithingTests::transferTerminal,
        "port_armadillo_abilities", FrostSmithingTests::armadillo
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_frost_smithing"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static FrostSmithingMenu menu(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture, boolean deformation) {
        helper.getLevel().setBlock(helper.absolutePos(TABLE), ModBlocks.FROST_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
        var menu = new FrostSmithingMenu(7, fixture.player().getInventory(),
            ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TABLE)));
        fixture.player().containerMenu = menu;
        menu.getSlot(0).set(deformation ? ModItems.DEFORMATION_TEMPLATE.asStack() : ModItems.PERMUTATION_TEMPLATE.asStack());
        return menu;
    }

    private static void deformation(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = menu(helper, fixture, true);
            helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_SWORD))
                && !menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_INGOT)), "第一输入槽必须接受装备并拒绝材料");
            menu.getSlot(1).set(new ItemStack(Items.IRON_HELMET));
            helper.assertTrue(!menu.getSlot(2).mayPlace(new ItemStack(Items.IRON_INGOT))
                && menu.getSlot(2).mayPlace(new ItemStack(Items.IRON_INGOT, 2)), "盔甲维修材料需要两个");
            menu.getSlot(2).set(ModItems.FROST_METAL_INGOT.asStack(3));
            helper.assertTrue(!menu.getSlot(3).hasItem(), "三个通用材料不足以支付盔甲形变");
            menu.getSlot(2).set(ModItems.FROST_METAL_INGOT.asStack(5));
            helper.assertTrue(menu.getSlot(3).getItem().is(Items.IRON_CHESTPLATE), "形变选项顺序应从当前装备的下一个开始");
            menu.turn(true);
            helper.assertTrue(menu.getSlot(3).getItem().is(Items.IRON_BOOTS), "左箭头必须循环至最后一个结果");
            menu.clicked(3, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.getCarried().is(Items.IRON_BOOTS) && !menu.getSlot(1).hasItem()
                && menu.getSlot(2).getItem().getCount() == 1 && menu.getSlot(0).hasItem(),
                "取出后必须消耗一件装备、四个通用材料并保留模板");
            menu.setCarried(ItemStack.EMPTY);
            menu.getSlot(1).set(new ItemStack(Items.IRON_SWORD));
            menu.getSlot(2).set(ItemStack.EMPTY);
            helper.assertTrue(menu.getSlot(1).hasItem(), "取走材料不能把装备弹出或吞掉");
            menu.getSlot(0).set(ItemStack.EMPTY);
            helper.assertTrue(menu.getSlot(1).hasItem() && !menu.getSlot(3).hasItem(), "取走模板应保留输入且清空产物");
        }
        helper.succeed();
    }

    private static void permutation(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = menu(helper, fixture, false);
            var sword = ModItems.EMBER_METAL_SWORD.asStack();
            sword.set(DataComponents.CUSTOM_NAME, Component.literal("frost port"));
            menu.getSlot(1).set(sword);
            helper.assertTrue(menu.getSlot(3).getItem().is(ModItems.ROYAL_STEEL_SWORD.get()) && menu.results.size() == 2,
                "空材料应显示皇家钢和下界合金退化选项");
            menu.getSlot(2).set(ModItems.FROST_METAL_NUGGET.asStack(2));
            helper.assertTrue(!menu.getSlot(3).hasItem(), "两个浮霜粒不能执行三粒嬗变");
            menu.getSlot(2).set(ModItems.FROST_METAL_NUGGET.asStack(5));
            helper.assertTrue(menu.getSlot(3).getItem().is(ModItems.FROST_METAL_SWORD.get()) && menu.results.size() == 1,
                "材料必须筛选出对应的嬗变结果");
            menu.clicked(3, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.getCarried().getHoverName().getString().equals("frost port")
                && menu.getSlot(2).getItem().getCount() == 2, "嬗变必须保留输入名称并准确扣除三粒材料");
        }
        helper.succeed();
    }

    private static void bow(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = menu(helper, fixture, true);
            menu.getSlot(1).set(new ItemStack(Items.BOW));
            helper.assertTrue(menu.getSlot(3).getItem().is(Items.CROSSBOW), "弓与弩形变不需要材料");
            menu.getSlot(2).set(new ItemStack(Items.STRING));
            helper.assertTrue(!menu.getSlot(3).hasItem(), "无材料配方必须要求材料槽为空");
            menu.getSlot(2).set(ItemStack.EMPTY);
            menu.clicked(3, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.getCarried().is(Items.CROSSBOW) && menu.getSlot(0).hasItem(), "免费形变仍需消耗装备并保留模板");
        }
        helper.succeed();
    }

    private static void heavy(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = menu(helper, fixture, false);
            menu.getSlot(1).set(new ItemStack(ModBlocks.FROST_ANVIL.asItem(), 4));
            menu.getSlot(2).set(ModItems.EMBER_METAL_INGOT.asStack(8));
            helper.assertTrue(menu.getSlot(3).getItem().is(ModBlocks.EMBER_ANVIL.asItem())
                && menu.getSlot(3).getItem().getCount() == 1, "堆叠工作方块每次嬗变只能生成一件结果");
            menu.clicked(3, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.getSlot(1).getItem().getCount() == 3 && menu.getSlot(2).getItem().getCount() == 5,
                "重型物品每次嬗变只消耗一个装备和三个金属锭");
        }
        helper.succeed();
    }

    private static void transcendence(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = new TranscendenceSmithingMenu(8, fixture.player().getInventory(), ContainerLevelAccess.NULL);
            fixture.player().containerMenu = menu;
            helper.assertTrue(menu.selectTemplateForTransfer(fixture.player(), ModItems.DEFORMATION_TEMPLATE.asStack()), "超越台应支持内置形变模板");
            menu.getSlot(0).set(new ItemStack(Items.IRON_HELMET));
            menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 5));
            int resultSlot = TranscendenceSmithingMenu.ROYAL_FROST_RESULT_SLOT;
            helper.assertTrue(menu.getSlot(resultSlot).getItem().is(Items.IRON_CHESTPLATE), "超越台应使用装备在前、材料在后的顺序");
            menu.turnFrostResult(true);
            menu.clicked(resultSlot, 0, ContainerInput.PICKUP, fixture.player());
            helper.assertTrue(menu.getCarried().is(Items.IRON_BOOTS) && menu.getSlot(1).getItem().getCount() == 3,
                "超越台必须按选择结果消耗两个原生维修材料");
        }
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        var recipes = RecipesRecord.getRecipes(helper.getLevel());
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            for (var holder : recipes.byType(ModRecipeTypes.DEFORMATION.get())) {
                var original = holder.value();
                var json = DeformationRecipe.SERIALIZER.codec().codec().encodeStart(ops, original).getOrThrow();
                var restored = DeformationRecipe.SERIALIZER.codec().codec().parse(ops, json).getOrThrow();
                DeformationRecipe.SERIALIZER.streamCodec().encode(buffer, restored);
                var synced = DeformationRecipe.SERIALIZER.streamCodec().decode(buffer);
                helper.assertTrue(synced.inputs().size() == original.inputs().size(), "形变配方必须完整同步所有装备选项");
                buffer.clear();
            }
            for (var holder : recipes.byType(ModRecipeTypes.PERMUTATION.get())) {
                var original = holder.value();
                var json = PermutationRecipe.SERIALIZER.codec().codec().encodeStart(ops, original).getOrThrow();
                var restored = PermutationRecipe.SERIALIZER.codec().codec().parse(ops, json).getOrThrow();
                PermutationRecipe.SERIALIZER.streamCodec().encode(buffer, restored);
                var synced = PermutationRecipe.SERIALIZER.streamCodec().decode(buffer);
                helper.assertTrue(synced.options().size() == original.options().size(), "嬗变配方必须完整同步逐结果材料规则");
                buffer.clear();
            }
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void transferInventory(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = menu(helper, fixture, false);
            var player = fixture.player();
            player.getInventory().setItem(9, ModItems.EMBER_METAL_SWORD.asStack());
            player.getInventory().setItem(10, ModItems.FROST_METAL_NUGGET.asStack());
            player.getInventory().setItem(11, ModItems.FROST_METAL_NUGGET.asStack(2));
            helper.assertTrue(FrostSmithingTransfer.transfer(player, ModItems.EMBER_METAL_SWORD.asStack(),
                ModItems.FROST_METAL_NUGGET.asStack(3), List.of()), "转移必须合并背包分散的材料堆");
            helper.assertTrue(menu.getSlot(2).getItem().getCount() == 3 && menu.getSlot(3).hasItem()
                && player.getInventory().getItem(10).isEmpty() && player.getInventory().getItem(11).isEmpty(), "一次转移应填足三个材料");
            helper.assertTrue(!FrostSmithingTransfer.transfer(player, ModItems.EMBER_METAL_SWORD.asStack(),
                ModItems.FROST_METAL_NUGGET.asStack(4), List.of()) && menu.getSlot(2).getItem().getCount() == 3,
                "库存不足时不能部分清空或覆盖原输入");
            menu.getSlot(2).set(new ItemStack(Items.DIRT));
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            player.getInventory().setItem(10, ModItems.FROST_METAL_NUGGET.asStack(64));
            helper.assertTrue(!FrostSmithingTransfer.transfer(player, ModItems.EMBER_METAL_SWORD.asStack(),
                ModItems.FROST_METAL_NUGGET.asStack(3), List.of()) && menu.getSlot(2).getItem().is(Items.DIRT)
                && player.getInventory().getItem(10).getCount() == 64, "旧输入放不回满背包时必须完整保留原状态");
        }
        helper.succeed();
    }

    private static void transferTerminal(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var menu = menu(helper, fixture, true);
            var target = TerminalAccessTests.bound(fixture).get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
            fixture.stock(ItemResource.of(Items.IRON_HELMET), 1);
            fixture.stock(ItemResource.of(ModItems.FROST_METAL_INGOT.get()), 3);
            helper.assertTrue(!FrostSmithingTransfer.transfer(fixture.player(), new ItemStack(Items.IRON_HELMET),
                ModItems.FROST_METAL_INGOT.asStack(4), List.of(target)) && !menu.getSlot(1).hasItem()
                && fixture.count(ItemResource.of(Items.IRON_HELMET)) == 1
                && fixture.count(ItemResource.of(ModItems.FROST_METAL_INGOT.get())) == 3,
                "补库不足时必须归还本次已借出的装备和材料");
            fixture.stock(ItemResource.of(ModItems.FROST_METAL_INGOT.get()), 1);
            helper.assertTrue(FrostSmithingTransfer.transfer(fixture.player(), new ItemStack(Items.IRON_HELMET),
                ModItems.FROST_METAL_INGOT.asStack(4), List.of(target)) && menu.getSlot(3).getItem().is(Items.IRON_CHESTPLATE)
                && fixture.count(ItemResource.of(ModItems.FROST_METAL_INGOT.get())) == 0,
                "终端补库与菜单填料必须完成完整数量的一次操作");
        }
        helper.succeed();
    }

    private static void armadillo(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.getInventory().setItem(0, ModItems.ARMADILLO_AMULET.asStack());
            player.setShiftKeyDown(true);
            dev.dubhe.anvilcraft.item.AmuletAbilities.onTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(player.hasEffect(MobEffects.RESISTANCE) && player.getEffect(MobEffects.RESISTANCE).getAmplifier() == 1,
                "犰狳护符在潜行时必须提供抗性提升 II");
            var spider = new Spider(EntityType.SPIDER, helper.getLevel());
            spider.setTarget(player);
            helper.assertTrue(spider.getTarget() == null, "蜘蛛不能锁定持有犰狳护符的玩家");
            player.removeEffect(MobEffects.RESISTANCE);
            player.setShiftKeyDown(false);
            dev.dubhe.anvilcraft.item.AmuletAbilities.onTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(!player.hasEffect(MobEffects.RESISTANCE), "非潜行状态不能刷新犰狳抗性");
            player.getInventory().setItem(0, ModItems.NATURE_AMULET.asStack());
            player.setShiftKeyDown(true);
            dev.dubhe.anvilcraft.item.AmuletAbilities.onTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
            helper.assertTrue(player.hasEffect(MobEffects.RESISTANCE), "自然护符必须包含犰狳护符能力");
        }
        helper.succeed();
    }
}
