package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageTooltipTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_tooltip_usage", StorageTooltipTests::usage,
        "port_storage_tooltip_infinite", StorageTooltipTests::infinite,
        "port_storage_tooltip_access", StorageTooltipTests::access
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_tooltip"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static UUID id(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture) {
        return ((StorageBlockEntity) helper.getLevel().getBlockEntity(fixture.core())).getId();
    }

    private static void usage(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            for (int index = 0; index < 12; index++) {
                var stack = new ItemStack(Items.STONE);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("type " + index));
                fixture.stock(ItemResource.of(stack), index + 1);
            }
            var items = (TypeLimitItemStacksResourceHandler) fixture.items();
            items.set(11, ItemResource.EMPTY, 0);
            var usage = StorageServerStub.getStorageUsage(fixture.player().getUUID(), id(helper, fixture));
            helper.assertTrue(usage.usedTypes() == 11 && usage.typeLimit() == 1024 && usage.types().size() == 9,
                "Count nonempty component variants and limit preview to nine representatives");
            for (int index = 0; index < 9; index++) {
                var stack = usage.types().get(index);
                helper.assertTrue(stack.getCount() == 1 && stack.getHoverName().getString().equals("type " + index),
                    "Stable order and preserved item components");
            }
            items.addTypeLimit(limit -> limit * 2);
            helper.assertTrue(StorageServerStub.getStorageUsage(fixture.player().getUUID(), id(helper, fixture)).typeLimit() == 2048,
                "Usage exposes expanded type capacity");
        }
        helper.succeed();
    }

    private static void infinite(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            fixture.stock(ItemResource.of(Items.DIAMOND), 50000);
            fixture.stock(ItemResource.of(Items.EMERALD), 1000);
            var usage = StorageServerStub.getStorageUsage(fixture.player().getUUID(), id(helper, fixture));
            helper.assertTrue(usage.usedTypes() == 2 && usage.typeLimit() == 0, "Unlimited type capacity uses source infinity marker");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                StorageServerStub.StorageUsage.STREAM_CODEC.encode(buffer, usage);
                var copy = StorageServerStub.StorageUsage.STREAM_CODEC.decode(buffer);
                helper.assertTrue(copy.usedTypes() == 2 && copy.typeLimit() == 0 && copy.types().size() == 2
                    && copy.types().getFirst().is(Items.DIAMOND) && copy.types().getFirst().getCount() == 1,
                    "Usage and representative item codec roundtrip");
            } finally {
                buffer.release();
            }
        }
        var missing = StorageServerStub.getStorageUsage(UUID.randomUUID(), UUID.randomUUID());
        helper.assertTrue(missing.usedTypes() == 0 && missing.types().isEmpty(), "Missing storage remains empty");
        helper.succeed();
    }

    private static boolean authorized(StorageFluidRpcTests.Fixture fixture, UUID player, UUID storage) {
        try {
            return new StorageServerStub.StorageUsageValidator().validate(TerminalAccessTests.context(fixture),
                StorageServerStub.class.getMethod("getStorageUsage", UUID.class, UUID.class), new Object[]{player, storage});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void access(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            UUID playerId = player.getUUID();
            UUID storageId = id(helper, fixture);
            helper.assertTrue(!authorized(fixture, playerId, storageId), "Knowing a storage UUID does not authorize inspection");
            var container = ModBlocks.SHULKER_CONTAINER.asStack();
            container.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER, storageId));
            player.getInventory().setItem(2, container);
            helper.assertTrue(authorized(fixture, playerId, storageId), "Held storage reference permits inspection");
            helper.assertTrue(!authorized(fixture, UUID.randomUUID(), storageId), "Spoofed player is rejected");
            helper.assertTrue(!authorized(fixture, playerId, UUID.randomUUID()), "Unrelated storage is rejected");
            player.getInventory().setItem(2, ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.OFFHAND, container);
            helper.assertTrue(authorized(fixture, playerId, storageId), "Native equipment offhand preserves source inventory access");
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            player.containerMenu.setCarried(container);
            helper.assertTrue(!authorized(fixture, playerId, storageId), "Source reference access requires inventory ownership");
            var terminal = ModItems.HYPERDIMENSION_TERMINAL.asStack();
            terminal.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(storageId)));
            player.containerMenu.setCarried(terminal);
            helper.assertTrue(authorized(fixture, playerId, storageId), "A bound terminal on the cursor permits inspection");
        }
        helper.succeed();
    }
}
