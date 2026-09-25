package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ItemDetectorUiTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,
            registry -> registry.register(AnvilCraft.of("port_detector_menu_invert"), ItemDetectorUiTests::invert));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_detector_ui"));
        event.registerTest(AnvilCraft.of("port_detector_menu_invert"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_detector_menu_invert")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)));
    }

    private static void invert(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ITEM_DETECTOR.getDefaultState());
        var detector = (ItemDetectorBlockEntity) helper.getLevel().getBlockEntity(pos);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortDetector"));
        var menu = new ItemDetectorMenu(ModMenuTypes.ITEM_DETECTOR.get(), 37, player.getInventory(), detector);
        List<Integer> states = new ArrayList<>();
        menu.setSynchronizer(new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu container, List<ItemStack> items, ItemStack carried, int[] data) {
                helper.assertTrue(data.length == 3, "Initial synchronization includes the inversion slot");
                states.add(data[ItemDetectorBlockEntity.DATASLOT_ID_OUTPUT_INVERT]);
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu container, ItemStack carried) {
            }

            @Override
            public RemoteSlot createSlot() {
                return RemoteSlot.PLACEHOLDER;
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu container, int slotIndex, ItemStack stack) {
            }

            @Override
            public void sendDataChange(AbstractContainerMenu container, int id, int value) {
                if (id == ItemDetectorBlockEntity.DATASLOT_ID_OUTPUT_INVERT) states.add(value);
            }
        });
        helper.assertTrue(detector.getDataAccess().getCount() == 3 && states.equals(List.of(0)), "Initial inversion data must synchronize");
        helper.assertTrue(menu.clickMenuButton(player, 1) && detector.isOutputInvert(), "Enable inversion from menu action");
        menu.broadcastChanges();
        helper.assertTrue(states.equals(List.of(0, 1)), "Changed inversion must broadcast through the third data slot");
        helper.assertTrue(!menu.clickMenuButton(player, -1) && !menu.clickMenuButton(player, 2) && detector.isOutputInvert(),
            "Invalid action ids must leave inversion unchanged");
        helper.assertTrue(menu.clickMenuButton(player, 0) && !detector.isOutputInvert(), "Disable inversion from menu action");
        menu.broadcastChanges();
        helper.assertTrue(states.equals(List.of(0, 1, 0)), "Disable state must broadcast");
        detector.setOutputInvert(true);
        menu.broadcastChanges();
        helper.assertTrue(states.equals(List.of(0, 1, 0, 1)), "External configuration changes must update open menus");
        var reopened = new ItemDetectorMenu(ModMenuTypes.ITEM_DETECTOR.get(), 38, player.getInventory(), detector);
        reopened.setData(ItemDetectorBlockEntity.DATASLOT_ID_OUTPUT_INVERT, 0);
        helper.assertTrue(!detector.isOutputInvert(), "Received menu data must update client-side state through the same setter");
        helper.succeed();
    }
}
