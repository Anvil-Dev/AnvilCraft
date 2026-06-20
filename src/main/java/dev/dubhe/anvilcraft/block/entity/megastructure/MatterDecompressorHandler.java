package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class MatterDecompressorHandler extends BaseMegastructureHandler {
    private static final int NEUTRON_STAR_INTERVAL = 200;
    private int counter = 0;
    private int logisticsRoundRobin = 0;

    @Override
    public String name() {
        return "matter_decompressor";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        if (!be.isAmplifierPresent()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;

        CelestialBodyClass bodyClass = star.bodyClass();
        if (bodyClass != CelestialBodyClass.NEUTRON_STAR && bodyClass != CelestialBodyClass.BLACK_HOLE) return;

        int totalGammaLevel = 0;
        List<CelestialForgingAnvilLaserInterfaceBlockEntity> lasers = findLaserInterfaces(be);
        for (CelestialForgingAnvilLaserInterfaceBlockEntity laser : lasers) {
            if (laser.isReceivedGamma()) {
                totalGammaLevel += laser.getReceivedLaserLevel();
            }
        }

        if (totalGammaLevel <= 0) return;
        int efficiency = totalGammaLevel;

        if (bodyClass == CelestialBodyClass.BLACK_HOLE) {
            ItemLike voidMatter = dev.dubhe.anvilcraft.init.item.ModItems.VOID_MATTER.get();
            ItemStack output = new ItemStack(voidMatter, efficiency);
            List<IItemHandler> logistics = findLogisticsInterfaces(be);
            if (!logistics.isEmpty()) {
                int startIdx = logisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    IItemHandler handler = logistics.get(idx);
                    ItemStack remainder = insertIntoHandler(handler, output);
                    if (remainder.getCount() < output.getCount()) {
                        logisticsRoundRobin = (idx + 1) % logistics.size();
                        return;
                    }
                }
            }
        } else {
            counter++;
            int interval = NEUTRON_STAR_INTERVAL / efficiency;
            if (interval < 1) interval = 1;
            if (counter >= interval) {
                counter = 0;
                ItemLike neutroniumIngot = dev.dubhe.anvilcraft.init.item.ModItems.NEUTRONIUM_INGOT.get();
                ItemStack output = new ItemStack(neutroniumIngot, 1);
                List<IItemHandler> logistics = findLogisticsInterfaces(be);
                if (!logistics.isEmpty()) {
                    int startIdx = logisticsRoundRobin % logistics.size();
                    for (int attempt = 0; attempt < logistics.size(); attempt++) {
                        int idx = (startIdx + attempt) % logistics.size();
                        IItemHandler handler = logistics.get(idx);
                        ItemStack remainder = insertIntoHandler(handler, output);
                        if (remainder.getCount() < output.getCount()) {
                            logisticsRoundRobin = (idx + 1) % logistics.size();
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.counter = 0;
        this.logisticsRoundRobin = 0;
    }
}
