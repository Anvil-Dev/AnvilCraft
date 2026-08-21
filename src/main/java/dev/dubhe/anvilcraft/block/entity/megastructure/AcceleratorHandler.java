package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.init.ModMegastructures;
import dev.dubhe.anvilcraft.network.QuenchedOutMusicPacket;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

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
    @Setter
    @Getter
    private int collapseAnimTicks = 0;

    /// === 淬灭序曲（超新星前奏曲）===
    /// 曲目全长约 71.77 秒（≈1435 刻），最后一秒（20 刻）恰为超新星爆发开始。
    /// 规则：
    /// 1. 演化开始时若距超新星不足 1 分 12 秒（1440 刻），曲目无法完整播放则不播；
    /// 2. 否则在爆炸开始前 1415 刻开始播放，使曲目尾音对准爆炸开始；
    /// 3. 演化期间锻星砧或增幅器被破坏，音乐立即中断。
    private static final int QUENCHED_FULL_PLAY_TICKS = 1440;
    private static final int QUENCHED_EXPLOSION_LEAD_TICKS = 1415;
    private static final int SUPER_STAGE3_TICKS = 10;

    private boolean quenchedScheduled = false;
    private long quenchedStartTick = -1;
    private boolean quenchedStarted = false;
    private boolean quenchedCanceled = false;
    private boolean quenchedSupernovaFired = false;

    @Override
    public String name() {
        return "stellar_evolution_accelerator";
    }

    public boolean isActive() {
        return stage >= 1 && stage <= 4;
    }

    @Override
    public boolean isAuxiliaryActive(CelestialForgingAnvilBlockEntity be) {
        return this.isActive();
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        this.tickQuenchedOutMusic(be);
        if (!be.isAmplifierPresent()) return;
        if (stage < 1 || stage > 4) return;

        switch (stage) {
            case 1 -> tickStage1(be);
            case 2 -> tickStage2(be);
            case 3 -> tickStage3(be);
            case 4 -> tickStage4(be);
            default -> {
            }
        }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
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

        scheduleQuenchedOut(be, cls, ageX, energyY);

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

    /// 在演化开始时决定是否播放入场曲，并预定开始时刻。
    /// 仅当演化路径真的会走向超新星（非 M 型主序星），且距爆炸足以完整播放曲目时才会预定。
    private void scheduleQuenchedOut(CelestialForgingAnvilBlockEntity be, CelestialBodyClass cls, int ageX, int energyY) {
        quenchedScheduled = false;
        quenchedStartTick = -1;
        quenchedStarted = false;
        quenchedCanceled = false;
        if (cls == CelestialBodyClass.M_MAIN) {
            /// M 型主序星走阶段 4 白矮星路线，无超新星，不播。
            return;
        }
        int predicted = predictedTicksUntilSupernova(cls, ageX, energyY);
        if (predicted < QUENCHED_FULL_PLAY_TICKS) {
            /// 距爆发不足 1 分 12 秒，曲目无法完整播放，不播。
            return;
        }
        quenchedScheduled = true;
        quenchedStartTick = be.getLevel().getGameTime() + (long) predicted - QUENCHED_EXPLOSION_LEAD_TICKS;
    }

    /// 预测从演化开始到超新星爆发开始的总刻数（与各阶段实际计时的算法一致）。
    private int predictedTicksUntilSupernova(CelestialBodyClass cls, int ageX, int energyY) {
        int giantTicks = predictedGiantTicks(ageX, energyY);
        if (cls.isMainSequence() && cls != CelestialBodyClass.M_MAIN) {
            int mainTicks = CelestialBodyMatcher.countPixelsRightInAgeTemp(ageX, energyY) * 2400;
            return mainTicks + giantTicks + SUPER_STAGE3_TICKS;
        }
        return giantTicks + SUPER_STAGE3_TICKS;
    }

    /// 与 {@link #initGiantPhase} 相同的巨星级阶段时长计算。
    private int predictedGiantTicks(int ageX, int energyY) {
        int pixelsDown = CelestialBodyMatcher.countPixelsDownInAgeTempSp(ageX, energyY);
        int totalPixels = CelestialBodyMatcher.countTotalColoredPixelsInAgeTempSpColumn(ageX, energyY);
        if (totalPixels <= 0) totalPixels = 1;
        return Math.max((int) ((float) pixelsDown / totalPixels * 2400), 1);
    }

    /// 每服务器刻推进音乐状态：定时开始、增幅器缺失时立即中断。
    private void tickQuenchedOutMusic(CelestialForgingAnvilBlockEntity be) {
        if (stage < 1 || stage > 4) return;
        if (!be.isAmplifierPresent()) {
            /// 增幅器被破坏：已开播则立即中断，未开播则取消预定。
            if (quenchedStarted) {
                quenchedStarted = false;
                quenchedCanceled = true;
                sendQuenchedOutMusic(be, false);
            } else if (quenchedScheduled) {
                quenchedCanceled = true;
                quenchedScheduled = false;
            }
            return;
        }
        if (quenchedCanceled) return;
        if (quenchedScheduled && !quenchedStarted && be.getLevel().getGameTime() >= quenchedStartTick) {
            quenchedScheduled = false;
            quenchedStarted = true;
            sendQuenchedOutMusic(be, true);
        }
    }

    private void sendQuenchedOutMusic(CelestialForgingAnvilBlockEntity be, boolean start) {
        if (!(be.getLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            new ChunkPos(be.getBlockPos()),
            new QuenchedOutMusicPacket(be.getBlockPos(), start)
        );
    }

    private boolean isDysonSphereBuilt(CelestialForgingAnvilBlockEntity be) {
        return ModMegastructures.DYSON_SPHERE_SMALL.getId().equals(be.getActiveMegastructureId())
            || ModMegastructures.DYSON_SPHERE_LARGE.getId().equals(be.getActiveMegastructureId());
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
                /// 爆炸发生在天体当前视觉中心（随红石信号上下移动）。
                be.getLevel().explode(
                    null,
                    be.getBlockPos().getX() + 0.5,
                    be.getBodyCenterWorldY(),
                    be.getBlockPos().getZ() + 0.5,
                    10.0f,
                    Level.ExplosionInteraction.BLOCK
                );
            }
            if (collapseAnimTicks > 0) syncToClient(be);
        } else {
            triggerSupernova(be);
        }
    }

    private void tickStage4(CelestialForgingAnvilBlockEntity be) {
        ticksRemaining--;
        if (ticksRemaining <= 0) {
            completeMStarEvolution(be);
        }
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

    /// 超新星屏幕震动范围（格）。
    private static final float SUPERNOVA_SHAKE_RADIUS = 32.0f;

    private void triggerSupernova(CelestialForgingAnvilBlockEntity be) {
        /// 在生成残骸（替换天体数据）之前触发闪光，以捕获爆炸恒星的中心与缩放。
        be.startSupernovaFlash();
        /// 向附近玩家发送屏幕震动包（中心为天体视觉中心，半径 32 格）。
        if (be.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                new net.minecraft.world.level.ChunkPos(be.getBlockPos()),
                dev.dubhe.anvilcraft.network.ScreenShakePacket.of(
                    new net.minecraft.world.phys.Vec3(
                        be.getBlockPos().getX() + 0.5,
                        be.getBodyCenterWorldY(),
                        be.getBlockPos().getZ() + 0.5
                    ),
                    SUPERNOVA_SHAKE_RADIUS,
                    dev.dubhe.anvilcraft.network.ScreenShakePacket.ShakeType.SUPERNOVA
                )
            );
        }
        createRemnant(be);
        /// 超新星已开始：曲目最后一秒正在播放，onClear 时不再强制中断。
        this.quenchedSupernovaFired = true;
        /// 通过管理器清除所有巨构
        be.getMegastructureManager().clearAllMegastructures(be);
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void createRemnant(CelestialForgingAnvilBlockEntity be) {
        int mass = originalMass;
        if (mass < 55) {
            createWhiteDwarfRemnant(be);
        } else if (mass <= 58) {
            createNeutronStarRemnant(be);
        } else {
            createBlackHoleRemnant(be);
        }
        finishAccelerator();
    }

    private void completeMStarEvolution(CelestialForgingAnvilBlockEntity be) {
        createWhiteDwarfRemnant(be);
        finishAccelerator();
    }

    private void createWhiteDwarfRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;

        int wdMassAnvil;
        int wdSpaceAnvil;
        if (originalMass <= 30) {
            wdMassAnvil = 48;
            wdSpaceAnvil = 11;
        } else if (originalMass <= 42) {
            wdMassAnvil = 49;
            wdSpaceAnvil = 10;
        } else {
            wdMassAnvil = 50;
            wdSpaceAnvil = 9;
        }

        int wdEnergy = 47;
        int[] rgb = CelestialBodyMatcher.getStarColor(wdEnergy);
        int newMag = Math.min(star.magneticFieldStrength() + 1, 5);
        int newRotation = Math.min(star.rotationSpeed() + 1, 5);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(wdMassAnvil);

        be.setCelestialBodyData(new StarData(
            CelestialBodyClass.WHITE_DWARF,
            wdSpaceAnvil,
            rgb[0],
            rgb[1],
            rgb[2],
            star.axialTilt(),
            newRotation,
            newMag,
            wdEnergy,
            star.bodyUuid()
        ));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
    }

    private void createNeutronStarRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;

        int neutronMass;
        if (originalMass <= 55) {
            neutronMass = 50;
        } else if (originalMass <= 56) {
            neutronMass = 51;
        } else {
            neutronMass = 52;
        }

        int newMag = Math.min(star.magneticFieldStrength() + 2, 6);
        int newRotation = Math.min(star.rotationSpeed() + 2, 5);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(neutronMass);

        be.setCelestialBodyData(new StarData(
            CelestialBodyClass.NEUTRON_STAR,
            1,
            255,
            255,
            255,
            star.axialTilt(),
            newRotation,
            newMag,
            64,
            star.bodyUuid()
        ));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
    }

    private void createBlackHoleRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;

        int bhMass = Math.clamp(53 + (originalMass - 59), 53, 59);
        int newMag = Math.min(star.magneticFieldStrength() + 2, 6);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(bhMass);

        be.setCelestialBodyData(new StarData(CelestialBodyClass.BLACK_HOLE, 1, 0, 0, 0, star.axialTilt(), 1, newMag, 64, star.bodyUuid()));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
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

        int targetEnergy = 38;
        float floatEnergy = originalEnergy + (targetEnergy - originalEnergy) * t;
        floatEnergy = Math.clamp(floatEnergy, targetEnergy, 64);
        int[] rgb = getBlendedStarColor(floatEnergy);

        be.setCelestialBodyData(new StarData(
            star.bodyClass(),
            newSize,
            rgb[0],
            rgb[1],
            rgb[2],
            star.axialTilt(),
            star.rotationSpeed(),
            star.magneticFieldStrength(),
            star.energy(),
            star.bodyUuid()
        ));
    }

    private void updateCollapseColor(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        int collapseEnergy = switch (collapseAnimTicks) {
            case 10 -> 38;
            case 9 -> 40;
            case 8 -> 42;
            case 7 -> 44;
            case 6 -> 46;
            case 5 -> 48;
            case 4 -> 50;
            case 3 -> 53;
            case 2 -> 56;
            case 1 -> 59;
            default -> 62;
        };
        int[] rgb = CelestialBodyMatcher.getStarColor(collapseEnergy);
        float startScale = visualScale(star.size());
        float endScale = visualScale(9);
        float progress = Math.clamp((10.0f - collapseAnimTicks) / 9.0f, 0.0f, 1.0f);
        float targetScale = startScale + (endScale - startScale) * progress;
        int collapseSize = Math.max(9, sizeForVisualScale(targetScale));
        be.setCelestialBodyData(new StarData(
            star.bodyClass(),
            collapseSize,
            rgb[0],
            rgb[1],
            rgb[2],
            star.axialTilt(),
            star.rotationSpeed(),
            star.magneticFieldStrength(),
            star.energy(),
            star.bodyUuid()
        ));
    }

    private static float visualScale(int size) {
        if (size <= 20) {
            return 1.5f * (0.2f + (size - 1) * 0.8f / 19f);
        } else {
            float t2 = (size - 20) / 44f;
            return 1.5f * (1.0f + t2 * t2 * 1.63f);
        }
    }

    private static int sizeForVisualScale(float scale) {
        if (scale >= 1.5f) {
            float t2 = (float) Math.sqrt((scale / 1.5f - 1.0f) / 1.63f);
            return Math.round(20f + 44f * t2);
        } else {
            return Math.round(1f + (scale / 1.5f - 0.2f) * 19f / 0.8f);
        }
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
        if (quenchedSupernovaFired) {
            /// 超新星已开始：曲目最后一秒正在播放，让它自然播完。
            quenchedSupernovaFired = false;
        } else if (quenchedStarted) {
            /// 演化被中断（解锁、锻星砧被破坏等）：立即停止音乐。
            sendQuenchedOutMusic(be, false);
        }
        quenchedScheduled = false;
        quenchedStartTick = -1;
        quenchedStarted = false;
        quenchedCanceled = false;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("acceleratorStage", stage);
        tag.putInt("acceleratorTicksRemaining", ticksRemaining);
        tag.putInt("acceleratorTicksTotal", ticksTotal);
        tag.putInt("acceleratorOriginalMass", originalMass);
        tag.putInt("acceleratorOriginalEnergy", originalEnergy);
        tag.putInt("acceleratorOriginalSize", originalSize);
        tag.putBoolean("acceleratorDysonDestroyed", dysonDestroyed);
        tag.putLong("acceleratorDysonDestroyTick", dysonDestroyTick);
        tag.putBoolean("quenchedScheduled", quenchedScheduled);
        tag.putLong("quenchedStartTick", quenchedStartTick);
        tag.putBoolean("quenchedStarted", quenchedStarted);
        tag.putBoolean("quenchedCanceled", quenchedCanceled);
        tag.putBoolean("quenchedSupernovaFired", quenchedSupernovaFired);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.stage = tag.getInt("acceleratorStage");
        this.ticksRemaining = tag.getInt("acceleratorTicksRemaining");
        this.ticksTotal = tag.getInt("acceleratorTicksTotal");
        this.originalMass = tag.getInt("acceleratorOriginalMass");
        this.originalEnergy = tag.getInt("acceleratorOriginalEnergy");
        this.originalSize = tag.getInt("acceleratorOriginalSize");
        this.dysonDestroyed = tag.getBoolean("acceleratorDysonDestroyed");
        this.dysonDestroyTick = tag.getLong("acceleratorDysonDestroyTick");
        this.quenchedScheduled = tag.getBoolean("quenchedScheduled");
        this.quenchedStartTick = tag.getLong("quenchedStartTick");
        this.quenchedStarted = tag.getBoolean("quenchedStarted");
        this.quenchedCanceled = tag.getBoolean("quenchedCanceled");
        this.quenchedSupernovaFired = tag.getBoolean("quenchedSupernovaFired");
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("acceleratorStage", stage);
        tag.putInt("acceleratorTicksRemaining", ticksRemaining);
        tag.putInt("acceleratorTicksTotal", ticksTotal);
        tag.putInt("collapseAnimTicks", collapseAnimTicks);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.stage = tag.getInt("acceleratorStage");
        this.ticksRemaining = tag.getInt("acceleratorTicksRemaining");
        this.ticksTotal = tag.getInt("acceleratorTicksTotal");
        this.collapseAnimTicks = tag.getInt("collapseAnimTicks");
    }
}
