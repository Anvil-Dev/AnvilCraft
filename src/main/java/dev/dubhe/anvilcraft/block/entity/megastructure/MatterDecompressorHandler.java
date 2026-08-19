package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

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
    public LaserRequirement getLaserRequirement() {
        return new LaserRequirement(1, true);
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
        int magneticField = star.magneticFieldStrength();

        if (bodyClass == CelestialBodyClass.BLACK_HOLE) {
            ItemLike voidMatter = dev.dubhe.anvilcraft.init.item.ModItems.VOID_MATTER.get();
            ItemStack output = new ItemStack(voidMatter, efficiency);
            tryInsert(be, output);

            // 激发态虚空物质：概率 = ((B-4)×2)%，最低为0，B是磁场强度
            if (magneticField > 4) {
                int chance = (magneticField - 4) * 2;
                if (be.getLevel().random.nextInt(100) < chance) {
                    ItemLike excitedVoidMatter = dev.dubhe.anvilcraft.init.item.ModItems.EXCITED_STATE_VOID_MATTER.get();
                    ItemStack specialOutput = new ItemStack(excitedVoidMatter, 1);
                    tryInsert(be, specialOutput);
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
                tryInsert(be, output);

                // 充能中子锭：概率 = ((B-3)^2)%，最低为0，B是磁场强度
                if (magneticField > 3) {
                    int chance = (magneticField - 3) * (magneticField - 3);
                    if (be.getLevel().random.nextInt(100) < chance) {
                        ItemLike chargedNeutronium = dev.dubhe.anvilcraft.init.item.ModItems.CHARGED_NEUTRONIUM_INGOT.get();
                        ItemStack specialOutput = new ItemStack(chargedNeutronium, 1);
                        tryInsert(be, specialOutput);
                    }
                }
            }
        }
    }

    private void tryInsert(CelestialForgingAnvilBlockEntity be, ItemStack output) {
        var logistics = findOutputLogisticsInterfaces(be);
        if (logistics.size() == 0) return;
        ItemOutputResult result = insertOutputItem(logistics, output, logisticsRoundRobin);
        if (result.remainder().getCount() < output.getCount()) {
            logisticsRoundRobin = result.nextIndex();
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.counter = 0;
        this.logisticsRoundRobin = 0;
    }
}
