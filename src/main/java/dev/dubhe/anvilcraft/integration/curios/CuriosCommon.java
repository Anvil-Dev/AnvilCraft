package dev.dubhe.anvilcraft.integration.curios;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.api.event.AmuletEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;

@Integration("curios")
public class CuriosCommon {
    public void apply() {
        IEventBus modEventBus = IntegrationHook.getModEventBus();
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(this::onPlayerWearAnvilHammerInCurioSlot);
        NeoForge.EVENT_BUS.addListener(this::findFromCurios);
    }

    private void setup(FMLCommonSetupEvent event) {
        AnvilHammerItem.addIsWearingPredicate(
            player -> CuriosApi.getCuriosInventory(player)
                .map(CuriosCommon::isAnvilHammerWearing)
                .orElse(false)
        );
        IonoCraftBackpackItem.addStackProvider(
            player -> CuriosApi.getCuriosInventory(player)
                .map(CuriosCommon::getIonocraftBackpackWearing)
                .orElse(ItemStack.EMPTY)
        );
        InventoryUtil.compatConsumer = InventoryUtil.compatConsumer.andThen(
            (items, living) -> CuriosApi.getCuriosInventory(living).ifPresent(
                handler -> handler.findCurios(stack -> true)
                    .forEach(result -> items.add(result.stack()))
            )
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
            CuriosCapability.ITEM,
            (stack, context) -> () -> stack,
            ModItems.ANVIL_HAMMER,
            ModItems.ROYAL_ANVIL_HAMMER,
            ModItems.FROST_ANVIL_HAMMER,
            ModItems.EMBER_ANVIL_HAMMER,
            ModItems.TRANSCENDENCE_ANVIL_HAMMER,
            ModItems.IONOCRAFT_BACKPACK
        );
    }

    private void onPlayerWearAnvilHammerInCurioSlot(CurioChangeEvent.Item event) {
        LivingEntity entity = event.getEntity();
        ItemStack eventTo = event.getTo();
        if (entity instanceof Player && eventTo.getItem() instanceof AnvilHammerItem) {
            Optional<PowerGrid> powerGrid = PowerGrid.findPowerGridContains(entity.level(), entity.position());
        }
    }

    private void findFromCurios(AmuletEvent.Find event) {
        Player player = event.getPlayer();
        if (CuriosApi.getCuriosInventory(player).isPresent()) {
            List<SlotResult> results = CuriosApi.getCuriosInventory(player).get()
                .findCurios(stack -> stack.is(ModItemTags.AMULET));
            for (SlotResult result : results) {
                event.provide(result.stack());
            }
        }
    }

    private static boolean isAnvilHammerWearing(ICuriosItemHandler handler) {
        return !handler.findCurios(it -> it.getItem() instanceof AnvilHammerItem).isEmpty();
    }

    private static ItemStack getIonocraftBackpackWearing(ICuriosItemHandler handler) {
        List<SlotResult> curios = handler.findCurios(it -> it.getItem() instanceof IonoCraftBackpackItem);
        if (!curios.isEmpty()) {
            return curios.getFirst().stack();
        }
        return ItemStack.EMPTY;
    }
}
