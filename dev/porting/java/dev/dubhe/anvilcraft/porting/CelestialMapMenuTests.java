package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CelestialMapMenuTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> registry.register(AnvilCraft.of("port_cfa_map_transfer"),
            CelestialMapMenuTests::transfer));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_cfa_map"));
        event.registerTest(AnvilCraft.of("port_cfa_map_transfer"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_cfa_map_transfer")),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)));
    }

    private static void transfer(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortCfaMap"));
        var be = new CelestialForgingAnvilBlockEntity(ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
            BlockPos.ZERO, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState());
        var menu = new CelestialForgingAnvilMenu(null, 1, player.getInventory(), be);
        Item[] anvils = {ModBlocks.CONFINED_TIME_ANVILON.asItem(), ModBlocks.CONFINED_SPACE_ANVILON.asItem(),
            ModBlocks.CONFINED_MASS_ANVILON.asItem(), ModBlocks.CONFINED_ENERGY_ANVILON.asItem()};
        for (int slot = 0; slot < 4; slot++) {
            player.getInventory().clearContent();
            player.getInventory().setItem(9, new ItemStack(anvils[slot], 37));
            player.getInventory().setItem(10, new ItemStack(anvils[slot], 27));
            menu.setCarried(new ItemStack(Items.DIAMOND, 3));
            for (int desired : new int[]{1, 32, 64, 17, 0, 64, 64, 0}) {
                menu.clicked(slot, desired, ContainerInput.QUICK_CRAFT, player);
                helper.assertTrue(menu.getSlot(slot).getItem().getCount() == desired, "Set exact chart count " + desired);
                assertTotal(helper, menu, anvils[slot], 64);
                helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 3,
                    "Chart selection preserves carried stack");
            }
            player.getInventory().clearContent();
            player.getInventory().setItem(9, new ItemStack(anvils[slot], 7));
            menu.clicked(slot, 64, ContainerInput.QUICK_CRAFT, player);
            helper.assertTrue(menu.getSlot(slot).getItem().getCount() == 7, "Stop at available stock");
            for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            menu.clicked(slot, 0, ContainerInput.QUICK_CRAFT, player);
            helper.assertTrue(menu.getSlot(slot).getItem().getCount() == 7, "Full inventory stops removal");
            player.getInventory().setItem(8, new ItemStack(anvils[slot], 62));
            menu.clicked(slot, 0, ContainerInput.QUICK_CRAFT, player);
            helper.assertTrue(menu.getSlot(slot).getItem().getCount() == 5, "Partial room stops at remaining capacity");
            assertTotal(helper, menu, anvils[slot], 69);
            menu.setCarried(ItemStack.EMPTY);
            for (int invalid : new int[]{-1, 65, 127}) menu.clicked(slot, invalid, ContainerInput.QUICK_CRAFT, player);
            helper.assertTrue(menu.getSlot(slot).getItem().getCount() == 5, "Out-of-range counts do not transfer");
            menu.getSlot(slot).set(ItemStack.EMPTY);
        }
        player.getInventory().clearContent();
        menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        menu.clicked(-999, 0, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(6, 1, ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, 2, ContainerInput.QUICK_CRAFT, player);
        helper.assertTrue(menu.getSlot(6).getItem().is(Items.DIAMOND) && menu.getSlot(6).getItem().getCount() == 3,
            "Ordinary inventory quick-craft remains functional outside parameter slots");
        helper.succeed();
    }

    private static void assertTotal(GameTestHelper helper, CelestialForgingAnvilMenu menu, Item item, int expected) {
        int total = menu.slots.stream().mapToInt(slot -> slot.getItem().is(item) ? slot.getItem().getCount() : 0).sum();
        helper.assertTrue(total == expected, "Conserve anvil inventory: " + total + " != " + expected);
    }
}
