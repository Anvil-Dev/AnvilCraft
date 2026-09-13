package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.init.storage.ModCategories;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.AndCategory;
import dev.dubhe.anvilcraft.saved.storage.category.BlockCategory;
import dev.dubhe.anvilcraft.saved.storage.category.FluidCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.NamespaceCategory;
import dev.dubhe.anvilcraft.saved.storage.category.OrCategory;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageFluidCategoryTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_fluid_category_predicates", StorageFluidCategoryTests::predicates,
        "port_fluid_category_codecs", StorageFluidCategoryTests::codecs,
        "port_fluid_category_default", StorageFluidCategoryTests::defaults
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        final var environment = event.registerEnvironment(AnvilCraft.of("port_fluid_category"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void predicates(GameTestHelper helper) {
        final var water = new FluidStack(Fluids.WATER, 1000);
        final var honey = new FluidStack(ModFluids.HONEY.get(), 1000);
        final var namespace = new NamespaceCategory(Items.WATER_BUCKET, "minecraft");
        helper.assertTrue(FluidCategory.INSTANCE.testFluid(water) && !FluidCategory.INSTANCE.testFluid(FluidStack.EMPTY),
            "流体分类必须匹配非空流体，拒绝空栈");
        helper.assertTrue(!FluidCategory.INSTANCE.test(new UnlimitedItemStack(ItemResource.of(Items.WATER_BUCKET), 1)),
            "装有流体的桶仍是物品，不能混入流体分类");
        helper.assertTrue(!BlockCategory.INSTANCE.testFluid(water), "普通物品分类默认不能匹配流体");
        helper.assertTrue(namespace.testFluid(water) && !namespace.testFluid(honey),
            "命名空间分类必须按流体注册 ID 判断");
        helper.assertTrue(namespace.testFluid(FluidStack.EMPTY), "源版按注册 ID 判断，因此 minecraft:empty 也属于原版命名空间");
        final var and = new AndCategory(Items.WATER_BUCKET, AnvilCraft.of("fluid_test"), FluidCategory.INSTANCE, namespace);
        final var or = new OrCategory(Items.WATER_BUCKET, AnvilCraft.of("fluid_test"), BlockCategory.INSTANCE, FluidCategory.INSTANCE);
        helper.assertTrue(and.testFluid(water) && !and.testFluid(honey), "与分类必须满足每个子分类");
        helper.assertTrue(or.testFluid(honey) && !or.testFluid(FluidStack.EMPTY), "或分类匹配任一子分类即可");
        helper.assertTrue(new AndCategory(Items.WATER_BUCKET, AnvilCraft.of("empty")).testFluid(water), "空与分类沿用源版真值");
        helper.assertTrue(!new OrCategory(Items.WATER_BUCKET, AnvilCraft.of("empty")).testFluid(water), "空或分类沿用源版假值");
        helper.succeed();
    }

    private static void codecs(GameTestHelper helper) {
        final var lookup = helper.getLevel().registryAccess();
        final var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
        List<ICategory> categories = List.of(FluidCategory.INSTANCE,
            new AndCategory(Items.WATER_BUCKET, AnvilCraft.of("fluid_test"), FluidCategory.INSTANCE,
                new NamespaceCategory(Items.WATER_BUCKET, "minecraft")));
        for (ICategory category : categories) {
            final var encoded = ICategory.CODEC.encodeStart(ops, category).getOrThrow();
            final var decoded = ICategory.CODEC.parse(ops, encoded).getOrThrow();
            helper.assertTrue(decoded.equals(category), "分类保存必须保留流体类型和嵌套分类");
            if (category instanceof AndCategory) {
                var inline = ICategory.DIRECT_CODEC.encodeStart(ops, category).getOrThrow().getAsJsonObject();
                var name = ComponentSerialization.CODEC.encodeStart(ops, category.name()).getOrThrow();
                for (var form : List.of(name, new JsonPrimitive(name.toString()))) {
                    inline.add("name", form);
                    helper.assertTrue(ICategory.DIRECT_CODEC.parse(ops, inline).getOrThrow().equals(category),
                        "分类名称必须同时兼容旧 JSON 字符串和新组件对象");
                }
            }
            final var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), lookup);
            try {
                ICategory.STREAM_CODEC.encode(buffer, category);
                final var network = ICategory.STREAM_CODEC.decode(buffer);
                helper.assertTrue(network.equals(category) && network.testFluid(new FluidStack(Fluids.WATER, 1)),
                    "网络编解码必须保留流体判定");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static void defaults(GameTestHelper helper) {
        final var lookup = helper.getLevel().registryAccess();
        final var category = lookup.lookupOrThrow(ModRegistryKeys.CATEGORY).getOrThrow(ModCategories.FLUID).value();
        helper.assertTrue(category.equals(FluidCategory.INSTANCE), "数据包必须注册内置流体分类");
        final var setting = new PlayerSetting(lookup);
        helper.assertTrue(setting.listed().size() == 4 && setting.listed().getLast().getCategory().equals(category),
            "新玩家默认分类顺序应为原版、方块、不可堆叠和流体");
        helper.succeed();
    }
}
