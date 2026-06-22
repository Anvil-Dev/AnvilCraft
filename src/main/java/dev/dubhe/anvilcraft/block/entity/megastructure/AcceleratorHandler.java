package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AcceleratorHandler extends BaseMegastructureHandler {

    @Getter
    private int stage = 0;
    @Getter
    private int ticksRemaining = 0;
    @Getter
    private int ticksTotal = 0;
    private int originalMass = 0;
    private int originalEnergy = 0;
    private int originalSize = 0;
    private boolean dysonDestroyed = false;
    private long dysonDestroyTick = -1;
    @Setter @Getter
    private int supernovaFlashTicks = 0;
    @Setter @Getter
    private int collapseAnimTicks = 0;

    @Override
    public String name() {
        return "stellar_evolution_accelerator";
    }

    public boolean isActive() {
        return stage >= 1 && stage <= 4;
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        if (!be.isAmplifierPresent()) return;
        if (stage < 1 || stage > 4) return;

        switch (stage) {
            case 1 -> tickStage1(be);
            case 2 -> tickStage2(be);
            case 3 -> tickStage3(be);
            case 4 -> tickStage4(be);
            default -> {}
        }
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;

        CelestialBodyClass cls = star.bodyClass();
        int ageX = CelestialBodyMatcher.toX(be.getAgeAnvilCount());
        int energyY = CelestialBodyMatcher.toY(star.energy());

        this.originalMass = be.getStellarMass();
        this.originalEnergy = star.energy();
        this.originalSize = star.size();
        this.dysonDestroyed = false;
        this.dysonDestroyTick = -1;

        if (cls.isMainSequence()) {
            int pixelsRight = CelestialBodyMatcher.countPixelsRightInAgeTemp(ageX, energyY);
            this.stage = 1;
            this.ticksRemaining = pixelsRight * 2400;
            this.ticksTotal = ticksRemaining;
        } else {
            initGiantPhase(be, ageX, energyY);
        }

        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void initGiantPhase(CelestialForgingAnvilBlockEntity be, int ageX, int energyY) {
        int pixelsDown = CelestialBodyMatcher.countPixelsDownInAgeTempSp(ageX, energyY);
        int totalPixels = CelestialBodyMatcher.countTotalColoredPixelsInAgeTempSpColumn(ageX, energyY);
        if (totalPixels <= 0) totalPixels = 1;
        float fraction = (float) pixelsDown / totalPixels;
        this.stage = 2;
        this.ticksRemaining = Math.max((int) (fraction * 2400), 1);
        this.ticksTotal = ticksRemaining;

        if (isDysonSphereBuilt(be) && ticksRemaining > 20) {
            long startTick = be.getLevel().getGameTime();
            long range = ticksRemaining / 2;
            if (range > 0) {
                this.dysonDestroyTick = startTick + be.getLevel().getRandom().nextInt((int) range);
            }
        }

        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private boolean isDysonSphereBuilt(CelestialForgingAnvilBlockEntity be) {
        if (be.getActiveMegastructureIndex() < 0) return false;
        var option = be.getActiveMegastructureOption();
        if (option == null) return false;
        String name = option.megastructure();
        return "dyson_sphere_small".equals(name) || "dyson_sphere_large".equals(name);
    }

    private void tickStage1(CelestialForgingAnvilBlockEntity be) {
        ticksRemaining--;
        if (ticksRemaining % 20 == 0) syncToClient(be);
        if (ticksRemaining <= 0) {
            if (be.getCelestialBodyData() instanceof StarData star && star.bodyClass() == CelestialBodyClass.M_MAIN) {
                transitionToStage4(be);
            } else {
                transitionToStage2(be);
            }
        }
    }

    private void tickStage2(CelestialForgingAnvilBlockEntity be) {
        ticksRemaining--;
        updateGiantPhaseVisuals(be);
        if (ticksRemaining % 20 == 0) syncToClient(be);
        if (!dysonDestroyed && dysonDestroyTick >= 0 && be.getLevel().getGameTime() >= dysonDestroyTick) {
            destroyDysonSphere(be);
        }
        if (ticksRemaining <= 0) {
            transitionToStage3(be);
        }
    }

    private void tickStage3(CelestialForgingAnvilBlockEntity be) {
        if (collapseAnimTicks > 0) {
            collapseAnimTicks--;
            ticksRemaining--;
            updateCollapseColor(be);
            if (collapseAnimTicks == 5) {
                be.getLevel().explode(null,
                    be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 4.0, be.getBlockPos().getZ() + 0.5,
                    6.0f, Level.ExplosionInteraction.BLOCK);
            }
            if (collapseAnimTicks > 0) syncToClient(be);
        } else {
            triggerSupernova(be);
        }
    }

    private void tickStage4(CelestialForgingAnvilBlockEntity be) {
        ticksRemaining--;
        if (ticksRemaining <= 0) completeMStarEvolution(be);
    }

    private void transitionToStage2(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        int ageX = CelestialBodyMatcher.toX(be.getAgeAnvilCount());
        int energyY = CelestialBodyMatcher.toY(star.energy());
        initGiantPhase(be, ageX, energyY);
    }

    private void transitionToStage3(CelestialForgingAnvilBlockEntity be) {
        this.stage = 3;
        this.collapseAnimTicks = 10;
        this.ticksRemaining = 10;
        this.ticksTotal = 10;
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void transitionToStage4(CelestialForgingAnvilBlockEntity be) {
        this.stage = 4;
        this.ticksRemaining = 2400;
        this.ticksTotal = 2400;
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void triggerSupernova(CelestialForgingAnvilBlockEntity be) {
        createRemnant(be);
        be.getMegastructureManager().clearAllMegastructures(be);
        this.supernovaFlashTicks = 10;
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void createRemnant(CelestialForgingAnvilBlockEntity be) {
        int mass = originalMass;
        if (mass < 55) createWhiteDwarfRemnant(be);
        else if (mass <= 58) createNeutronStarRemnant(be);
        else createBlackHoleRemnant(be);
        finishAccelerator();
    }

    private void completeMStarEvolution(CelestialForgingAnvilBlockEntity be) {
        createWhiteDwarfRemnant(be);
        finishAccelerator();
    }

    private void createWhiteDwarfRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        int wdMassAnvil = originalMass <= 30 ? 48 : originalMass <= 42 ? 49 : 50;
        int wdSpaceAnvil = originalMass <= 30 ? 11 : originalMass <= 42 ? 10 : 9;
        int wdEnergy = 47;
        int[] rgb = CelestialBodyMatcher.getStarColor(wdEnergy);
        int newMag = Math.min(star.magneticFieldStrength() + 1, 5);
        int newRotation = Math.min(star.rotationSpeed() + 1, 5);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(wdMassAnvil);
        be.setCelestialBodyData(new StarData(CelestialBodyClass.WHITE_DWARF, wdSpaceAnvil,
            rgb[0], rgb[1], rgb[2], star.axialTilt(), newRotation, newMag, wdEnergy, star.bodyUuid()));
        be.setPlanetaryResourceSet(null);
    }

    private void createNeutronStarRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        int neutronMass = originalMass <= 55 ? 50 : originalMass <= 56 ? 51 : 52;
        int newMag = Math.min(star.magneticFieldStrength() + 2, 6);
        int newRotation = Math.min(star.rotationSpeed() + 2, 5);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(neutronMass);
        be.setCelestialBodyData(new StarData(CelestialBodyClass.NEUTRON_STAR, 1,
            255, 255, 255, star.axialTilt(), newRotation, newMag, 64, star.bodyUuid()));
        be.setPlanetaryResourceSet(null);
    }

    private void createBlackHoleRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        int bhMass = Math.clamp(53 + (originalMass - 59), 53, 59);
        int newMag = Math.min(star.magneticFieldStrength() + 2, 6);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(bhMass);
        be.setCelestialBodyData(new StarData(CelestialBodyClass.BLACK_HOLE, 1,
            0, 0, 0, star.axialTilt(), 1, newMag, 64, star.bodyUuid()));
        be.setPlanetaryResourceSet(null);
    }

    private void finishAccelerator() {
        this.stage = 0;
        this.ticksRemaining = 0;
        this.ticksTotal = 0;
        this.dysonDestroyed = false;
        this.dysonDestroyTick = -1;
    }

    private void destroyDysonSphere(CelestialForgingAnvilBlockEntity be) {
        if (dysonDestroyed) return;
        dysonDestroyed = true;
        be.getMegastructureManager().clearMegastructure(be);
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void syncToClient(CelestialForgingAnvilBlockEntity be) {
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void updateGiantPhaseVisuals(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        if (be.getLevel().getGameTime() % 20 != 0) return;
        float progress = ticksTotal > 0 ? (float) ticksRemaining / ticksTotal : 0f;
        float t = 1.0f - progress;
        int newSize = originalSize + Math.round((64 - originalSize) * t);
        newSize = Math.clamp(newSize, 1, 64);
        float floatEnergy = Math.clamp(originalEnergy + (38 - originalEnergy) * t, 38, 64);
        int[] rgb = getBlendedStarColor(floatEnergy);
        be.setCelestialBodyData(new StarData(star.bodyClass(), newSize,
            rgb[0], rgb[1], rgb[2], star.axialTilt(), star.rotationSpeed(), star.magneticFieldStrength(), star.energy(), star.bodyUuid()));
    }

    private void updateCollapseColor(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        int collapseEnergy = switch (collapseAnimTicks) {
            case 10 -> 38; case 9 -> 40; case 8 -> 42; case 7 -> 44; case 6 -> 46;
            case 5 -> 48; case 4 -> 50; case 3 -> 53; case 2 -> 56; case 1 -> 59; default -> 62;
        };
        int[] rgb = CelestialBodyMatcher.getStarColor(collapseEnergy);
        float startScale = visualScale(star.size());
        float endScale = visualScale(9);
        float progress = Math.clamp((10.0f - collapseAnimTicks) / 9.0f, 0.0f, 1.0f);
        float targetScale = startScale + (endScale - startScale) * progress;
        int collapseSize = Math.max(9, sizeForVisualScale(targetScale));
        be.setCelestialBodyData(new StarData(star.bodyClass(), collapseSize,
            rgb[0], rgb[1], rgb[2], star.axialTilt(), star.rotationSpeed(), star.magneticFieldStrength(), star.energy(), star.bodyUuid()));
    }

    private static float visualScale(int size) {
        if (size <= 20) return 1.5f * (0.2f + (size - 1) * 0.8f / 19f);
        else { float t2 = (size - 20) / 44f; return 1.5f * (1.0f + t2 * t2 * 1.63f); }
    }

    private static int sizeForVisualScale(float scale) {
        if (scale >= 1.5f) { float t2 = (float) Math.sqrt((scale / 1.5f - 1.0f) / 1.63f); return Math.round(20f + 44f * t2); }
        else return Math.round(1f + (scale / 1.5f - 0.2f) * 19f / 0.8f);
    }

    private static int[] getBlendedStarColor(float energy) {
        int low = (int) Math.floor(energy);
        int high = Math.min(low + 1, 64);
        float frac = energy - low;
        int[] rgbLow = CelestialBodyMatcher.getStarColor(low);
        int[] rgbHigh = CelestialBodyMatcher.getStarColor(high);
        return new int[]{
            Math.round(rgbLow[0] + (rgbHigh[0] - rgbLow[0]) * frac),
            Math.round(rgbLow[1] + (rgbHigh[1] - rgbLow[1]) * frac),
            Math.round(rgbLow[2] + (rgbHigh[2] - rgbLow[2]) * frac)
        };
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.stage = 0;
        this.ticksRemaining = 0;
        this.ticksTotal = 0;
        this.dysonDestroyed = false;
        this.dysonDestroyTick = -1;
        this.collapseAnimTicks = 0;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putInt("acceleratorStage", stage);
        output.putInt("acceleratorTicksRemaining", ticksRemaining);
        output.putInt("acceleratorTicksTotal", ticksTotal);
        output.putInt("acceleratorOriginalMass", originalMass);
        output.putInt("acceleratorOriginalEnergy", originalEnergy);
        output.putInt("acceleratorOriginalSize", originalSize);
        output.putBoolean("acceleratorDysonDestroyed", dysonDestroyed);
        output.putLong("acceleratorDysonDestroyTick", dysonDestroyTick);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.stage = input.getIntOr("acceleratorStage", 0);
        this.ticksRemaining = input.getIntOr("acceleratorTicksRemaining", 0);
        this.ticksTotal = input.getIntOr("acceleratorTicksTotal", 0);
        this.originalMass = input.getIntOr("acceleratorOriginalMass", 0);
        this.originalEnergy = input.getIntOr("acceleratorOriginalEnergy", 0);
        this.originalSize = input.getIntOr("acceleratorOriginalSize", 0);
        this.dysonDestroyed = input.getBooleanOr("acceleratorDysonDestroyed", false);
        this.dysonDestroyTick = input.getLongOr("acceleratorDysonDestroyTick", -1);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("acceleratorStage", stage);
        tag.putInt("acceleratorTicksRemaining", ticksRemaining);
        tag.putInt("acceleratorTicksTotal", ticksTotal);
        tag.putInt("supernovaFlashTicks", supernovaFlashTicks);
        tag.putInt("collapseAnimTicks", collapseAnimTicks);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.stage = tag.getIntOr("acceleratorStage", 0);
        this.ticksRemaining = tag.getIntOr("acceleratorTicksRemaining", 0);
        this.ticksTotal = tag.getIntOr("acceleratorTicksTotal", 0);
        this.supernovaFlashTicks = tag.getIntOr("supernovaFlashTicks", 0);
        this.collapseAnimTicks = tag.getIntOr("collapseAnimTicks", 0);
    }
}
