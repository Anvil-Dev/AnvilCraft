package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** Power production and primordial-matter processing for all Dyson spheres. */
public class DysonSphereHandler extends BaseMegastructureHandler {
    private static final String BROWN_DWARF_NAME = "dyson_sphere_brown_dwarf";
    private static final String SMALL_NAME = "dyson_sphere_small";
    private static final int STABLE_SUPPLY_TICKS = 40;
    private static final int MATTER_PER_BUCKET = 1_000;
    private static final int BROWN_DWARF_TIER_1 = 250;
    private static final int BROWN_DWARF_TIER_2 = 500;
    private static final int BROWN_DWARF_TIER_3 = MATTER_PER_BUCKET;
    private static final int BROWN_DWARF_TIER_4 = 2 * MATTER_PER_BUCKET;
    private static final long RED_DWARF_MATTER = 12_800L * MATTER_PER_BUCKET;

    private int cachedGridConsumption;
    private final String name;
    private int stableSupplyTicks;
    private int stableSupplyTier;
    private long accumulatedExcessMatter;

    public DysonSphereHandler(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        resetSupplyState();
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        resetSupplyState();
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) return;

        boolean brownDwarfSphere = isBrownDwarfSphere();
        boolean smallDwarfSphere = isSmallDwarfSphere(be);
        if (!brownDwarfSphere && !smallDwarfSphere) {
            updateSupplyState(0, be);
            return;
        }

        int supplied = consumePrimordialMatter(be);
        if (brownDwarfSphere) {
            updateSupplyState(supplyTier(supplied), be);
            if (supplied > BROWN_DWARF_TIER_4
                && be.getCelestialBodyData() instanceof GiantPlanetData brown
                && brown.brownDwarf()) {
                this.accumulatedExcessMatter += supplied - BROWN_DWARF_TIER_4;
                be.setChanged();
                if (this.accumulatedExcessMatter >= RED_DWARF_MATTER) {
                    transformToRedDwarf(be, brown);
                }
            }
        } else if (isSmallDwarfSphere(be)) {
            updateSupplyState(supplied >= BROWN_DWARF_TIER_4 ? 1 : 0, be);
        } else {
            updateSupplyState(0, be);
        }
    }

    @Override
    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        boolean specialBrownStar = false;
        boolean brownDwarf = false;
        if (isBrownDwarfSphere()) {
            specialBrownStar = be.getCelestialBodyData() instanceof StarData star && star.specialRedDwarf();
            brownDwarf = be.getCelestialBodyData() instanceof GiantPlanetData brown && brown.brownDwarf();
            if (!brownDwarf && !specialBrownStar) return 0;
        } else if (!(be.getCelestialBodyData() instanceof StarData)) {
            return 0;
        }
        if (!brownDwarf && !specialBrownStar && !be.isAmplifierPresent()) return 0;

        if (be.isAcceleratorActive() && be.getAcceleratorStage() == 1 && be.isAmplifierPresent()) {
            return Math.max(cachedGridConsumption * 2, cachedGridConsumption + 1);
        }

        int energy;
        int radius;
        int denominator;
        if (brownDwarf) {
            GiantPlanetData brown = (GiantPlanetData) be.getCelestialBodyData();
            energy = effectiveEnergy(brown.energy(), be);
            radius = Math.clamp(brown.size(), 0, 64);
            denominator = 1_600;
        } else {
            StarData star = (StarData) be.getCelestialBodyData();
            energy = Math.clamp(star.energy(), 0, 64);
            radius = Math.clamp(star.size(), 0, 64);
            denominator = 800;
        }
        if (energy <= 0 || radius <= 0) return 0;

        long basePower = ((long) energy * radius * radius / denominator) * 1_000L;
        long boostedPower = applyBoost(basePower, be);
        return (int) Math.min(boostedPower, Integer.MAX_VALUE);
    }

    @Override
    public PowerComponentType getComponentType() {
        return PowerComponentType.PRODUCER;
    }

    @Override
    public void gridTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getGrid() != null
            && be.isAcceleratorActive()
            && be.getAcceleratorStage() == 1
            && be.hasActiveMegastructure()) {
            this.cachedGridConsumption = be.getGrid().getConsume();
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeState(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readState(tag);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeUpdateTag(tag, registries);
        writeState(tag);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.readUpdateTag(tag, registries);
        readState(tag);
    }

    private void writeState(CompoundTag tag) {
        String prefix = statePrefix();
        tag.putInt(prefix + "StableTicks", stableSupplyTicks);
        tag.putInt(prefix + "StableTier", stableSupplyTier);
        tag.putLong(prefix + "ExcessMatter", accumulatedExcessMatter);
    }

    private void readState(CompoundTag tag) {
        String prefix = statePrefix();
        stableSupplyTicks = Math.clamp(tag.getInt(prefix + "StableTicks"), 0, STABLE_SUPPLY_TICKS);
        stableSupplyTier = Math.clamp(tag.getInt(prefix + "StableTier"), 0, 4);
        accumulatedExcessMatter = Math.max(tag.getLong(prefix + "ExcessMatter"), 0L);
    }

    private String statePrefix() {
        return "dysonSphere_" + name + "_";
    }

    private boolean isBrownDwarfSphere() {
        return BROWN_DWARF_NAME.equals(name);
    }

    private boolean isSmallDwarfSphere(CelestialForgingAnvilBlockEntity be) {
        if (!SMALL_NAME.equals(name) || !(be.getCelestialBodyData() instanceof StarData star)) return false;
        return star.size() < 48
            && (star.bodyClass() == CelestialBodyClass.M_MAIN
                || star.bodyClass() == CelestialBodyClass.K_MAIN
                || star.bodyClass() == CelestialBodyClass.G_MAIN);
    }

    private static int supplyTier(int amount) {
        if (amount >= BROWN_DWARF_TIER_4) return 4;
        if (amount >= BROWN_DWARF_TIER_3) return 3;
        if (amount >= BROWN_DWARF_TIER_2) return 2;
        if (amount >= BROWN_DWARF_TIER_1) return 1;
        return 0;
    }

    private void updateSupplyState(int tier, CelestialForgingAnvilBlockEntity be) {
        int oldActiveTier = activeSupplyTier();
        int oldStableTicks = stableSupplyTicks;
        int oldStableTier = stableSupplyTier;
        if (tier <= 0) {
            stableSupplyTicks = 0;
            stableSupplyTier = 0;
        } else if (tier != stableSupplyTier) {
            stableSupplyTier = tier;
            stableSupplyTicks = 1;
        } else {
            stableSupplyTicks = Math.min(stableSupplyTicks + 1, STABLE_SUPPLY_TICKS);
        }
        if (oldStableTicks != stableSupplyTicks || oldStableTier != stableSupplyTier) {
            be.setChanged();
        }
        if (oldActiveTier != activeSupplyTier()) {
            markPowerStateChanged(be);
        }
    }

    private int activeSupplyTier() {
        return stableSupplyTicks >= STABLE_SUPPLY_TICKS ? stableSupplyTier : 0;
    }

    private long applyBoost(long basePower, CelestialForgingAnvilBlockEntity be) {
        int tier = activeSupplyTier();
        if (tier == 0) return basePower;

        if (isBrownDwarfSphere() && be.getCelestialBodyData() instanceof StarData star
            && star.specialRedDwarf()) {
            return basePower * 2L;
        }
        if (isBrownDwarfSphere()) {
            return switch (tier) {
                case 1 -> basePower * 3L / 2L;
                case 2 -> basePower * 2L;
                case 3 -> basePower * 3L;
                default -> basePower * 5L;
            };
        }
        if (!isSmallDwarfSphere(be) || !(be.getCelestialBodyData() instanceof StarData star)) return basePower;
        return switch (star.bodyClass()) {
            case M_MAIN -> basePower * 2L;
            case K_MAIN -> basePower * 3L / 2L;
            case G_MAIN -> basePower * 5L / 4L;
            default -> basePower;
        };
    }

    private static int effectiveEnergy(int storedEnergy, CelestialForgingAnvilBlockEntity be) {
        if (storedEnergy > 0) return Math.clamp(storedEnergy, 0, 64);
        return Math.clamp(be.getAnvilCount(3), 0, 64);
    }

    private void transformToRedDwarf(CelestialForgingAnvilBlockEntity be, GiantPlanetData brown) {
        int energy = effectiveEnergy(brown.energy(), be);
        int[] rgb = CelestialBodyMatcher.getStarColor(energy);
        be.setCelestialBodyData(new StarData(
            CelestialBodyClass.M_MAIN,
            Math.clamp(brown.size(), 1, 64),
            rgb[0],
            rgb[1],
            rgb[2],
            brown.axialTilt(),
            brown.rotationSpeed(),
            brown.magneticFieldStrength(),
            energy,
            StarData.uuidFromBodySeed(be.getBodySeed()),
            true
        ));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
        accumulatedExcessMatter = 0L;
        stableSupplyTicks = 0;
        stableSupplyTier = 0;
        markPowerStateChanged(be);
    }

    private void resetSupplyState() {
        cachedGridConsumption = 0;
        stableSupplyTicks = 0;
        stableSupplyTier = 0;
        accumulatedExcessMatter = 0L;
    }

    private static void markPowerStateChanged(CelestialForgingAnvilBlockEntity be) {
        be.setChanged();
        if (be.getGrid() != null) be.getGrid().markChanged();
        Level level = be.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }
}
