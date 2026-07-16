package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExcavatorHandler extends BaseMegastructureHandler {
    private static final int LASER_THRESHOLD = 16;
    private static final int MAX_LASERS = 4;

    @Getter
    private boolean laserActive = false;
    private int logisticsRoundRobin = 0;
    private final Map<Item, Optional<BlockState>> rawMaterialOres = new IdentityHashMap<>();

    @Override
    public String name() {
        return "planet_excavator";
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) {
            laserActive = false;
            return;
        }
        if (be.getPlanetaryResourceSet() == null) return;

        List<CelestialForgingAnvilLaserInterfaceBlockEntity> validLasers = findValidLasers(be);
        boolean hasValidLaser = !validLasers.isEmpty();
        if (laserActive != hasValidLaser) {
            laserActive = hasValidLaser;
            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }

        if (!hasValidLaser) return;

        List<PlanetaryResourceSet.WeightedItemStack> miningPool = new ArrayList<>();
        miningPool.addAll(be.getPlanetaryResourceSet().getMinerals());
        miningPool.addAll(be.getPlanetaryResourceSet().getWastelandItems());
        if (miningPool.isEmpty()) return;

        int totalWeight = miningPool.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        if (totalWeight <= 0) return;

        int roll = be.getLevel().getRandom().nextInt(totalWeight);
        int cumulative = 0;
        ResourceLocation chosenItem = null;
        for (PlanetaryResourceSet.WeightedItemStack mineral : miningPool) {
            cumulative += mineral.weight();
            if (roll < cumulative) {
                chosenItem = mineral.itemId();
                break;
            }
        }
        if (chosenItem == null) chosenItem = miningPool.getFirst().itemId();

        Item item = BuiltInRegistries.ITEM.get(chosenItem);
        if (item == Items.AIR) return;

        List<ItemStack> outputs = new ArrayList<>();
        Block resourceBlock = Block.byItem(item);
        BlockState resourceState = resourceBlock == Blocks.AIR ? null : resourceBlock.defaultBlockState();
        if (resourceState == null && be.getLevel() instanceof ServerLevel serverLevel) {
            resourceState = rawMaterialOres.computeIfAbsent(
                item,
                ignored -> BreakBlockUtil.findOreForRawMaterial(serverLevel, be.getBlockPos(), item.getDefaultInstance())
            ).orElse(null);
        }
        if (resourceState != null && be.getLevel() instanceof ServerLevel serverLevel) {
            for (CelestialForgingAnvilLaserInterfaceBlockEntity laser : validLasers) {
                outputs.addAll(BreakBlockUtil.dropVirtualForLaser(
                    serverLevel,
                    be.getBlockPos(),
                    resourceState,
                    laser.getReceivedMiningEffect()
                ));
            }
        } else {
            for (int i = 0; i < validLasers.size(); i++) {
                outputs.add(item.getDefaultInstance());
            }
        }
        if (outputs.isEmpty()) return;

        var logistics = findOutputLogisticsInterfaces(be);
        if (logistics.size() == 0) return;

        for (ItemStack output : outputs) {
            ItemOutputResult result = insertOutputItem(logistics, output, logisticsRoundRobin);
            if (result.remainder().getCount() < output.getCount()) {
                logisticsRoundRobin = result.nextIndex();
            }
        }
    }

    private List<CelestialForgingAnvilLaserInterfaceBlockEntity> findValidLasers(
        CelestialForgingAnvilBlockEntity be
    ) {
        return findLaserInterfaces(be)
            .stream()
            .filter(l -> l.getReceivedLaserLevel() >= LASER_THRESHOLD)
            .limit(MAX_LASERS)
            .toList();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("excavatorLaserActive", laserActive);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.laserActive = tag.getBoolean("excavatorLaserActive");
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("excavatorLaserActive", laserActive);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.laserActive = tag.getBoolean("excavatorLaserActive");
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.laserActive = false;
        this.logisticsRoundRobin = 0;
    }
}
