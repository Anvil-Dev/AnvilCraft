package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModMathFunctions;
import dev.dubhe.anvilcraft.recipe.frost.AndFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.CustomFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.EmptyFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.frost.IFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.frost.OrFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.RepairMaterialFrostMaterialPredicate;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class FrostMaterialTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_frost_repair_material", FrostMaterialTests::repair,
        "port_frost_custom_material", FrostMaterialTests::custom,
        "port_frost_composite_material", FrostMaterialTests::composite,
        "port_frost_material_expression", FrostMaterialTests::expression,
        "port_frost_material_codec", FrostMaterialTests::codec
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_frost_material"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static IFrostSmithingRecipe recipe() {
        return DeformationRecipe.builder().input(Items.IRON_SWORD).input(Items.IRON_AXE).buildRecipe();
    }

    private static FrostSmithingRecipeInput input(ItemStack equipment, ItemStack material) {
        return new FrostSmithingRecipeInput(ModItems.DEFORMATION_TEMPLATE.asStack(), material, equipment);
    }

    private static CustomFrostMaterialPredicate iron(int count) {
        return CustomFrostMaterialPredicate.of(ItemIngredientPredicate.of(Items.IRON_INGOT).withCount(count).build());
    }

    private static void repair(GameTestHelper helper) {
        var recipe = recipe();
        var predicate = RepairMaterialFrostMaterialPredicate.allowUniversal(3);
        var sword = new ItemStack(Items.IRON_SWORD);
        var ingots = new ItemStack(Items.IRON_INGOT, 3);
        helper.assertTrue(predicate.test(recipe, input(sword, ingots)) && predicate.cost(recipe, input(sword, ingots)) == 3,
            "装备维修材料应按原始数量计费");
        helper.assertTrue(!predicate.test(recipe, input(sword, ingots.copyWithCount(2))), "维修材料不足不能满足条件");
        helper.assertTrue(predicate.test(recipe, input(ItemStack.EMPTY, ingots)), "装备未放入时应允许配方候选装备的维修材料");
        helper.assertTrue(!predicate.test(recipe, input(new ItemStack(Items.DIAMOND_SWORD), ingots)),
            "已有装备时不能使用其它候选装备的维修材料");
        var frost = ModItems.FROST_METAL_INGOT.asStack(6);
        helper.assertTrue(predicate.test(recipe, input(sword, frost)) && predicate.cost(recipe, input(sword, frost)) == 6,
            "通用维修材料应按默认双倍数量计费");
        helper.assertTrue(!predicate.test(recipe, input(sword, frost.copyWithCount(5))), "通用材料不足不能满足条件");
        helper.assertTrue(!RepairMaterialFrostMaterialPredicate.disallowUniversal(3).test(recipe, input(sword, frost)),
            "禁止通用材料时必须拒绝浮霜锭");
        sword.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(
            BuiltInRegistries.ITEM.wrapAsHolder(ModItems.FROST_METAL_INGOT.get()))));
        helper.assertTrue(predicate.test(recipe, input(sword, frost.copyWithCount(3)))
            && predicate.cost(recipe, input(sword, frost)) == 3, "26.1 装备组件定义的维修材料优先于通用标签");
        sword.remove(DataComponents.REPAIRABLE);
        helper.assertTrue(!predicate.test(recipe, input(sword, ingots)), "移除维修组件后不能继续按物品默认材料匹配");
        helper.succeed();
    }

    private static void custom(GameTestHelper helper) {
        var recipe = recipe();
        var equipment = new ItemStack(Items.IRON_SWORD);
        var predicate = iron(4);
        helper.assertTrue(predicate.items().size() == 1 && predicate.items().getFirst().getCount() == 4,
            "26.1 原料模板必须转为带正确数量的显示物品");
        helper.assertTrue(predicate.test(recipe, input(equipment, new ItemStack(Items.IRON_INGOT, 4))), "自定义数量达到阈值应接受");
        helper.assertTrue(!predicate.test(recipe, input(equipment, new ItemStack(Items.IRON_INGOT, 3)))
            && predicate.cost(recipe, input(equipment, new ItemStack(Items.GOLD_INGOT, 4))) == 0, "不足数量或错误物品必须拒绝");
        var empty = new EmptyFrostMaterialPredicate();
        helper.assertTrue(empty.test(recipe, input(equipment, ItemStack.EMPTY))
            && empty.cost(recipe, input(equipment, ItemStack.EMPTY)) == 0, "空材料条件不能收取材料");
        helper.assertTrue(!empty.test(recipe, input(equipment, new ItemStack(Items.IRON_INGOT))), "空材料条件必须拒绝非空材料槽");
        helper.succeed();
    }

    private static void composite(GameTestHelper helper) {
        var recipe = recipe();
        var input = input(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_INGOT, 5));
        var and = AndFrostMaterialPredicate.of(iron(2), iron(4));
        helper.assertTrue(and.test(recipe, input) && and.cost(recipe, input) == 4, "AND 消耗取最大值而非相加");
        var or = OrFrostMaterialPredicate.of(iron(2), iron(4));
        helper.assertTrue(or.test(recipe, input) && or.cost(recipe, input) == 2, "OR 消耗必须取首个满足的条件");
        helper.assertTrue(OrFrostMaterialPredicate.of(iron(4), iron(2)).cost(recipe, input) == 4, "OR 必须保留配置顺序");
        var insufficient = input(input.input(), new ItemStack(Items.IRON_INGOT, 3));
        helper.assertTrue(!and.test(recipe, insufficient) && and.cost(recipe, insufficient) == 0, "AND 任一条件不满足则消耗为零");
        helper.assertTrue(AndFrostMaterialPredicate.of().test(recipe, input)
            && AndFrostMaterialPredicate.of().cost(recipe, input) == 0
            && !OrFrostMaterialPredicate.of().test(recipe, input), "空组合必须保留源版逻辑恒等值");
        helper.succeed();
    }

    private static void expression(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(LibRegistries.FUNCTION_KEY);
        helper.assertTrue(IExpression.of(registry.getOrThrow(ModMathFunctions.DOUBLE).value(), IExpression.of(3)).evaluate() == 6,
            "生成的 anvilcraft:double 数据包函数必须加载并可调用");
        for (String text : List.of("x*3+1", "$(cost)*3+1", "anvilcraft:double($(cost))+4")) {
            var predicate = RepairMaterialFrostMaterialPredicate.allowUniversal(3, IExpression.of(registry, text));
            helper.assertTrue(predicate.universalCostAmount() == 10, "位置变量、具名变量和数据包函数引用应计算相同消耗");
            helper.assertTrue(!predicate.test(recipe(), input(new ItemStack(Items.IRON_SWORD), ModItems.FROST_METAL_INGOT.asStack(9))),
                "表达式必须实际参与材料数量判定");
        }
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var functions = helper.getLevel().registryAccess().lookupOrThrow(LibRegistries.FUNCTION_KEY);
        var equipment = new ItemStack(Items.IRON_SWORD);
        var cases = List.<IFrostMaterialPredicate>of(
            new EmptyFrostMaterialPredicate(), iron(4), RepairMaterialFrostMaterialPredicate.allowUniversal(3),
            RepairMaterialFrostMaterialPredicate.disallowUniversal(3),
            RepairMaterialFrostMaterialPredicate.allowUniversal(3, IExpression.of(functions, "anvilcraft:double($(cost))+4")),
            AndFrostMaterialPredicate.of(iron(2), iron(4)),
            OrFrostMaterialPredicate.of(new EmptyFrostMaterialPredicate(), iron(2), RepairMaterialFrostMaterialPredicate.allowUniversal(3))
        );
        var inputs = List.of(ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT, 3), new ItemStack(Items.IRON_INGOT, 4),
            ModItems.FROST_METAL_INGOT.asStack(6), ModItems.FROST_METAL_INGOT.asStack(10));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            for (var predicate : cases) {
                var json = IFrostMaterialPredicate.CODEC.encodeStart(ops, predicate).getOrThrow();
                final var decoded = IFrostMaterialPredicate.CODEC.parse(ops, json).getOrThrow();
                buffer.clear();
                IFrostMaterialPredicate.STREAM_CODEC.encode(buffer, predicate);
                var synced = IFrostMaterialPredicate.STREAM_CODEC.decode(buffer);
                helper.assertTrue(!buffer.isReadable(), "网络判定应完整消费其编码");
                for (var material : inputs) {
                    var input = input(equipment, material);
                    for (var restored : List.of(decoded, synced)) {
                        helper.assertTrue(restored.type() == predicate.type()
                            && restored.test(recipe(), input) == predicate.test(recipe(), input)
                            && restored.cost(recipe(), input) == predicate.cost(recipe(), input), "数据及网络往返必须保留材料规则与消耗");
                    }
                }
            }
        } finally {
            buffer.release();
        }
        var defaults = IFrostMaterialPredicate.CODEC.parse(ops, JsonParser.parseString(
            "{\"type\":\"anvilcraft:repair_material\",\"cost\":3}"
        )).getOrThrow();
        helper.assertTrue(defaults.cost(recipe(), input(equipment, ModItems.FROST_METAL_INGOT.asStack(6))) == 6,
            "省略可选字段时应恢复允许通用材料和双倍消耗");
        helper.succeed();
    }
}
