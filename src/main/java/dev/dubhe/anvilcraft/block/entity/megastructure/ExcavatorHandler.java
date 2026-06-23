package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

public class ExcavatorHandler extends BaseMegastructureHandler {
    private static final int LASER_THRESHOLD = 16;
    private static final int MAX_LASERS = 4;

    @Getter
    private boolean laserActive = false;
    private int logisticsRoundRobin = 0;

    @Override
    public String name() {
        return "planet_excavator";
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !this.name().equals(option.megastructure())) {
            this.laserActive = false;
            return;
        }
        if (be.getPlanetaryResourceSet() == null) return;

        int laserCount = this.countValidLasersLocal(be);
        boolean hasValidLaser = laserCount > 0;
        if (this.laserActive != hasValidLaser) {
            this.laserActive = hasValidLaser;
            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }

        if (!hasValidLaser) return;
        int efficiency = Math.min(laserCount, MAX_LASERS);

        List<PlanetaryResourceSet.WeightedItemStack> miningPool = new ArrayList<>();
        miningPool.addAll(be.getPlanetaryResourceSet().getMinerals());
        miningPool.addAll(be.getPlanetaryResourceSet().getWastelandItems());
        if (miningPool.isEmpty()) return;

        int totalWeight = miningPool.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        if (totalWeight <= 0) return;

        int roll = be.getLevel().getRandom().nextInt(totalWeight);
        int cumulative = 0;
        Identifier chosenItem = null;
        for (PlanetaryResourceSet.WeightedItemStack mineral : miningPool) {
            cumulative += mineral.weight();
            if (roll < cumulative) {
                chosenItem = mineral.itemId();
                break;
            }
        }
        if (chosenItem == null) chosenItem = miningPool.getFirst().itemId();

        ItemLike item = BuiltInRegistries.ITEM.get(chosenItem)
            .map(h -> (net.minecraft.world.level.ItemLike) h.value()).orElse(Items.AIR);
        if (item.asItem() == Items.AIR) return;
        ItemStack output = new ItemStack(item, efficiency);

        List<ResourceHandler<ItemResource>> logistics = findLogisticsInterfaces(be);
        if (logistics.isEmpty()) return;

        int startIdx = this.logisticsRoundRobin % logistics.size();
        for (int attempt = 0; attempt < logistics.size(); attempt++) {
            int idx = (startIdx + attempt) % logistics.size();
            ResourceHandler<ItemResource> handler = logistics.get(idx);
            ItemStack remainder = insertIntoHandler(handler, output);
            if (remainder.getCount() < output.getCount()) {
                this.logisticsRoundRobin = (idx + 1) % logistics.size();
                return;
            }
        }
    }

    private int countValidLasersLocal(CelestialForgingAnvilBlockEntity be) {
        return (int) dev.dubhe.anvilcraft.block.entity.CfaInterfaceScanner.findLaserInterfaces(be.getLevel(), be.getBlockPos())
            .stream()
            .filter(l -> l.getReceivedLaserLevel() >= LASER_THRESHOLD)
            .count();
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putBoolean("excavatorLaserActive", this.laserActive);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.laserActive = input.getBooleanOr("excavatorLaserActive", false);
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.laserActive = false;
        this.logisticsRoundRobin = 0;
    }
}
