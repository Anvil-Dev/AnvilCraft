package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSeedMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSnapshotCodec;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceGenerator;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 天体搜索状态机，负责种子物品优先级、参数匹配和供电中断处理。
 */
final class CfaSearchController {
    private static final int SEARCH_DURATION = 200;
    private static final int SEED_SLOT = 4;

    private int ticksRemaining;
    private boolean searching;
    private boolean failed;
    private boolean powerInsufficient;
    private @Nullable Item capturedSeedItem;
    private @Nullable CompoundTag capturedSnapshot;

    void start(CelestialForgingAnvilBlockEntity owner) {
        this.failed = false;
        this.powerInsufficient = false;
        Level level = owner.getLevel();
        ItemStack seedStack = owner.getAnvilInventory().getItem(SEED_SLOT);
        boolean hasSeed = !seedStack.isEmpty();

        if (level != null && !level.isClientSide() && !hasSeed && CelestialBodyMatcher.match(
            owner.getAnvilCount(0),
            owner.getAnvilCount(1),
            owner.getAnvilCount(2),
            owner.getAnvilCount(3),
            owner.isAmplify(),
            level.getRandom()
        ) == null) {
            this.stop(true, false);
            sync(owner);
            return;
        }

        if (!hasEnoughPower(owner)) {
            this.stop(false, true);
            sync(owner);
            return;
        }

        this.capturedSeedItem = hasSeed ? seedStack.getItem() : null;
        this.capturedSnapshot = hasSeed ? CelestialSnapshotCodec.extract(seedStack) : null;
        owner.setCelestialBodyData(null);
        this.ticksRemaining = SEARCH_DURATION;
        this.searching = true;
        sync(owner);
    }

    void serverTick(CelestialForgingAnvilBlockEntity owner) {
        boolean enoughPower = hasEnoughPower(owner);
        if (enoughPower == this.powerInsufficient) {
            this.powerInsufficient = !enoughPower;
            sync(owner);
        }
        if (this.ticksRemaining <= 0) return;
        if (!enoughPower) {
            this.stop(false, true);
            sync(owner);
            return;
        }
        this.ticksRemaining--;
        if (this.ticksRemaining == 0) {
            this.searching = false;
            this.match(owner);
            this.failed = owner.getCelestialBodyData() == null;
            sync(owner);
        }
    }

    void match(CelestialForgingAnvilBlockEntity owner) {
        Level level = owner.getLevel();
        if (level == null) return;
        int time = owner.getAnvilCount(0);
        final int space = owner.getAnvilCount(1);
        int mass = owner.getAnvilCount(2);
        final int energy = owner.getAnvilCount(3);
        owner.setAgeAnvilCount(time);
        owner.setBodySeed(level.getRandom().nextLong());
        owner.setStellarMass(mass);
        this.validateCapturedSeed(owner);

        if (this.capturedSnapshot != null && this.capturedSnapshot.contains("celestialBody")) {
            applySnapshot(owner, this.capturedSnapshot);
            consumeSeed(owner);
            sync(owner);
            return;
        }

        if (this.capturedSeedItem == Items.PLAYER_HEAD) {
            SpecialCelestialBodyData playerHead = CelestialSeedMatcher.fromPlayerHead(
                owner.getAnvilInventory().getItem(SEED_SLOT), space
            );
            if (playerHead != null) {
                applyResult(owner, playerHead, new PlanetaryResourceSet());
                consumeSeed(owner);
                sync(owner);
                return;
            }
        }

        if (this.capturedSeedItem != null && level instanceof ServerLevel serverLevel) {
            CelestialSeedMatcher.Result special = CelestialSeedMatcher.match(
                serverLevel, time, space, mass, energy, this.capturedSeedItem
            );
            if (special != null) {
                applyResult(owner, special.body(), special.resources());
                consumeSeed(owner);
                sync(owner);
                return;
            }
        }

        CelestialBodyData body = CelestialBodyMatcher.match(
            time, space, mass, energy, owner.isAmplify(), level.getRandom()
        );
        if (body instanceof StarData star && star.bodyUuid() == null) {
            body = star.withBodyUuid(StarData.uuidFromBodySeed(owner.getBodySeed()));
        }
        PlanetaryResourceSet resources = body == null || level.isClientSide()
            ? null
            : PlanetResourceGenerator.generate(
                body,
                time,
                level,
                owner.getBodySeed(),
                this.capturedSeedItem == null ? null : BuiltInRegistries.ITEM.getKey(this.capturedSeedItem)
            );
        owner.setCelestialBodyData(body);
        owner.setPlanetaryResourceSet(resources);
        if (body != null) {
            owner.addToSearchHistory(body, resources);
        } else {
            this.ticksRemaining = 0;
        }
        consumeSeed(owner);
        sync(owner);
    }

    void load(boolean searching, int ticksRemaining, boolean failed, boolean powerInsufficient) {
        this.searching = searching && ticksRemaining > 0;
        this.ticksRemaining = Math.max(0, ticksRemaining);
        this.failed = failed;
        this.powerInsufficient = powerInsufficient;
        this.capturedSeedItem = null;
        this.capturedSnapshot = null;
    }

    int ticksRemaining() {
        return this.ticksRemaining;
    }

    boolean isSearching() {
        return this.searching;
    }

    boolean hasFailed() {
        return this.failed;
    }

    boolean isPowerInsufficient() {
        return this.powerInsufficient;
    }

    private void validateCapturedSeed(CelestialForgingAnvilBlockEntity owner) {
        if (this.capturedSeedItem == null && this.capturedSnapshot == null) return;
        if (owner.getAnvilInventory().getItem(SEED_SLOT).isEmpty()) {
            this.capturedSeedItem = null;
            this.capturedSnapshot = null;
        }
    }

    private static void applySnapshot(CelestialForgingAnvilBlockEntity owner, CompoundTag tag) {
        if (tag.contains("celestialBody")) {
            owner.setCelestialBodyData(CelestialBodyData.fromTag(tag.getCompoundOrEmpty("celestialBody")));
        }
        owner.setBodySeed(tag.getLongOr("bodySeed", 0));
        owner.setAgeAnvilCount(tag.getIntOr("ageAnvilCount", 0));
        owner.setStellarMass(tag.getIntOr("stellarMass", 0));
        PlanetaryResourceSet resources = tag.contains("planetaryResources")
            ? PlanetaryResourceSet.fromTag(tag.getCompoundOrEmpty("planetaryResources"))
            : null;
        owner.setPlanetaryResourceSet(resources);
        if (owner.getCelestialBodyData() != null) {
            owner.addToSearchHistory(owner.getCelestialBodyData(), resources);
        }
    }

    private static void applyResult(
        CelestialForgingAnvilBlockEntity owner,
        CelestialBodyData body,
        @Nullable PlanetaryResourceSet resources
    ) {
        owner.setCelestialBodyData(body);
        owner.setPlanetaryResourceSet(resources);
        owner.addToSearchHistory(body, resources);
    }

    private static void consumeSeed(CelestialForgingAnvilBlockEntity owner) {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide()) return;
        if (!owner.getAnvilInventory().getItem(SEED_SLOT).isEmpty()) {
            owner.getAnvilInventory().setItem(SEED_SLOT, ItemStack.EMPTY);
        }
    }

    private static boolean hasEnoughPower(CelestialForgingAnvilBlockEntity owner) {
        PowerGrid grid = owner.getGrid();
        return grid != null && (owner.getInputPower() <= 0 || grid.isWorking());
    }

    private void stop(boolean failed, boolean powerInsufficient) {
        this.failed = failed;
        this.powerInsufficient = powerInsufficient;
        this.searching = false;
        this.ticksRemaining = 0;
    }

    private static void sync(CelestialForgingAnvilBlockEntity owner) {
        owner.setChanged();
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide()) {
            owner.syncToClient();
        }
    }
}
