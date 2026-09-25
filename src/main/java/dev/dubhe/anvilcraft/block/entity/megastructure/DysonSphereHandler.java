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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        return this.name;
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        this.resetSupplyState();
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.resetSupplyState();
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) return;

        boolean brownDwarfSphere = this.isBrownDwarfSphere();
        boolean smallDwarfSphere = this.isSmallDwarfSphere(be);
        if (!brownDwarfSphere && !smallDwarfSphere) {
            this.updateSupplyState(0, be);
            return;
        }

        int supplied = this.consumePrimordialMatter(be);
        if (brownDwarfSphere) {
            this.updateSupplyState(supplyTier(supplied), be);
            if (supplied > BROWN_DWARF_TIER_4
                && be.getCelestialBodyData() instanceof GiantPlanetData brown
                && brown.brownDwarf()) {
                this.accumulatedExcessMatter += supplied - BROWN_DWARF_TIER_4;
                be.setChanged();
                if (this.accumulatedExcessMatter >= RED_DWARF_MATTER) {
                    this.transformToRedDwarf(be, brown);
                }
            }
        } else if (this.isSmallDwarfSphere(be)) {
            this.updateSupplyState(supplied >= BROWN_DWARF_TIER_4 ? 1 : 0, be);
        } else {
            this.updateSupplyState(0, be);
        }
    }

    @Override
    public int getOutputPower(CelestialForgingAnvilBlockEntity be) {
        boolean brownDwarf = false;
        if (this.isBrownDwarfSphere()) {
            brownDwarf = be.getCelestialBodyData() instanceof GiantPlanetData brown && brown.brownDwarf();
            if (!brownDwarf) return 0;
        } else if (!(be.getCelestialBodyData() instanceof StarData)) {
            return 0;
        }
        if (!brownDwarf && !be.isAmplifierPresent()) return 0;

        if (be.isAcceleratorActive() && be.getAcceleratorStage() == 1 && be.isAmplifierPresent()) {
            return Math.max(this.cachedGridConsumption * 2, this.cachedGridConsumption + 1);
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
        long boostedPower = this.applyBoost(basePower, be);
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
    public void saveAdditional(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        this.writeState(tag);
        output.store(tag);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.stableSupplyTicks = Math.clamp(input.getIntOr(this.statePrefix() + "StableTicks", 0), 0, STABLE_SUPPLY_TICKS);
        this.stableSupplyTier = Math.clamp(input.getIntOr(this.statePrefix() + "StableTier", 0), 0, 4);
        this.accumulatedExcessMatter = Math.max(input.getLongOr(this.statePrefix() + "ExcessMatter", 0), 0L);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeUpdateTag(tag, registries);
        this.writeState(tag);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.readUpdateTag(tag, registries);
        this.readState(tag);
    }

    private void writeState(CompoundTag tag) {
        String prefix = this.statePrefix();
        tag.putInt(prefix + "StableTicks", this.stableSupplyTicks);
        tag.putInt(prefix + "StableTier", this.stableSupplyTier);
        tag.putLong(prefix + "ExcessMatter", this.accumulatedExcessMatter);
    }

    private void readState(CompoundTag tag) {
        String prefix = this.statePrefix();
        this.stableSupplyTicks = Math.clamp(tag.getIntOr(prefix + "StableTicks", 0), 0, STABLE_SUPPLY_TICKS);
        this.stableSupplyTier = Math.clamp(tag.getIntOr(prefix + "StableTier", 0), 0, 4);
        this.accumulatedExcessMatter = Math.max(tag.getLongOr(prefix + "ExcessMatter", 0), 0L);
    }

    private String statePrefix() {
        return "dysonSphere_" + this.name + "_";
    }

    private boolean isBrownDwarfSphere() {
        return BROWN_DWARF_NAME.equals(this.name);
    }

    private boolean isSmallDwarfSphere(CelestialForgingAnvilBlockEntity be) {
        if (!SMALL_NAME.equals(this.name) || !(be.getCelestialBodyData() instanceof StarData star)) return false;
        return !star.usesLargeStellarRings()
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
        int oldActiveTier = this.activeSupplyTier();
        int oldStableTicks = this.stableSupplyTicks;
        int oldStableTier = this.stableSupplyTier;
        if (tier <= 0) {
            this.stableSupplyTicks = 0;
            this.stableSupplyTier = 0;
        } else if (tier != this.stableSupplyTier) {
            this.stableSupplyTier = tier;
            this.stableSupplyTicks = 1;
        } else {
            this.stableSupplyTicks = Math.min(this.stableSupplyTicks + 1, STABLE_SUPPLY_TICKS);
        }
        if (oldStableTicks != this.stableSupplyTicks || oldStableTier != this.stableSupplyTier) {
            be.setChanged();
        }
        if (oldActiveTier != this.activeSupplyTier()) {
            markPowerStateChanged(be);
        }
    }

    private int activeSupplyTier() {
        return this.stableSupplyTicks >= STABLE_SUPPLY_TICKS ? this.stableSupplyTier : 0;
    }

    private long applyBoost(long basePower, CelestialForgingAnvilBlockEntity be) {
        int tier = this.activeSupplyTier();
        if (tier == 0) return basePower;

        if (this.isBrownDwarfSphere()) {
            return switch (tier) {
                case 1 -> basePower * 3L / 2L;
                case 2 -> basePower * 2L;
                case 3 -> basePower * 3L;
                default -> basePower * 5L;
            };
        }
        if (!this.isSmallDwarfSphere(be) || !(be.getCelestialBodyData() instanceof StarData star)) return basePower;
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
        be.normalizeRedDwarfState();
        markPowerStateChanged(be);
    }

    private void resetSupplyState() {
        this.cachedGridConsumption = 0;
        this.stableSupplyTicks = 0;
        this.stableSupplyTier = 0;
        this.accumulatedExcessMatter = 0L;
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
