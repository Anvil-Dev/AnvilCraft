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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** State machine for the ten-second CFA celestial search. */
final class CfaSearchController {
    private static final int SEARCH_DURATION = 200;
    private static final int SEED_SLOT = 4;
    private static final String CAPTURED_SEED_KEY = "searchCapturedSeed";
    private static final String SEED_STATE_KNOWN_KEY = "searchSeedStateKnown";

    private int ticksRemaining;
    private boolean searching;
    private boolean failed;
    private boolean powerInsufficient;
    private @Nullable Item capturedSeedItem;
    private @Nullable ItemStack capturedSeedStack;
    private @Nullable CompoundTag capturedSnapshot;

    void start(CelestialForgingAnvilBlockEntity owner) {
        this.failed = false;
        this.powerInsufficient = false;

        Level level = owner.getLevel();
        ItemStack seedStack = owner.getAnvilInventory().getItem(SEED_SLOT);
        boolean hasSeed = !seedStack.isEmpty();
        if (level != null && !level.isClientSide() && !hasSeed
            && CelestialBodyMatcher.match(
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
        this.capturedSeedStack = hasSeed ? seedStack.copy() : null;
        this.capturedSnapshot = hasSeed ? CelestialSnapshotCodec.extract(seedStack) : null;
        owner.setCelestialBodyData(null);
        this.ticksRemaining = SEARCH_DURATION;
        this.searching = true;
        sync(owner);
    }

    void serverTick(CelestialForgingAnvilBlockEntity owner) {
        boolean enoughPower = hasEnoughPower(owner);
        if (enoughPower != !this.powerInsufficient) {
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

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    void match(CelestialForgingAnvilBlockEntity owner) {
        Level level = owner.getLevel();
        if (level == null) return;

        int time = owner.getAnvilCount(0);
        int space = owner.getAnvilCount(1);
        int mass = owner.getAnvilCount(2);
        int energy = owner.getAnvilCount(3);
        owner.setAgeAnvilCount(time);
        owner.setBodySeed(level.getRandom().nextLong());
        owner.setStellarMass(mass);

        ItemStack currentSeed = owner.getAnvilInventory().getItem(SEED_SLOT);
        boolean capturedSeedMatches = this.capturedSeedStack != null
            && !currentSeed.isEmpty()
            && ItemStack.isSameItemSameComponents(currentSeed, this.capturedSeedStack);
        if ((this.capturedSeedItem != null || this.capturedSnapshot != null)
            && !capturedSeedMatches) {
            this.capturedSeedItem = null;
            this.capturedSeedStack = null;
            this.capturedSnapshot = null;
        }

        if (this.capturedSnapshot != null && this.capturedSnapshot.contains("celestialBody")) {
            applySnapshot(owner, this.capturedSnapshot);
            consumeSeed(owner, capturedSeedMatches);
            sync(owner);
            return;
        }

        if (this.capturedSeedItem == Items.PLAYER_HEAD) {
            SpecialCelestialBodyData playerHead = CelestialSeedMatcher.fromPlayerHead(currentSeed, space);
            if (playerHead != null) {
                applyResult(owner, playerHead, new PlanetaryResourceSet());
                consumeSeed(owner, capturedSeedMatches);
                sync(owner);
                return;
            }
        }

        if (this.capturedSeedItem != null && level instanceof ServerLevel serverLevel) {
            CelestialSeedMatcher.Result special = CelestialSeedMatcher.match(
                serverLevel,
                time,
                space,
                mass,
                energy,
                this.capturedSeedItem
            );
            if (special != null) {
                applyResult(owner, special.body(), special.resources());
                consumeSeed(owner, capturedSeedMatches);
                sync(owner);
                return;
            }
        }

        CelestialBodyData body = CelestialBodyMatcher.match(
            time,
            space,
            mass,
            energy,
            owner.isAmplify(),
            level.getRandom()
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
                this.capturedSeedItem == null
                    ? null
                    : BuiltInRegistries.ITEM.getKey(this.capturedSeedItem)
            );
        owner.setCelestialBodyData(body);
        owner.setPlanetaryResourceSet(resources);
        if (body != null) {
            owner.addToSearchHistory(body, resources);
        } else {
            this.ticksRemaining = 0;
        }
        consumeSeed(owner, capturedSeedMatches);
        sync(owner);
    }

    void loadPersistent(
        boolean searching,
        int ticksRemaining,
        boolean failed,
        boolean powerInsufficient,
        CompoundTag tag,
        HolderLookup.Provider registries
    ) {
        this.loadState(searching, ticksRemaining, failed, powerInsufficient);
        if (!this.searching) return;
        if (!(tag.get(CAPTURED_SEED_KEY) instanceof CompoundTag seedTag)) {
            if (!tag.getBoolean(SEED_STATE_KNOWN_KEY)) {
                // Older saves did not persist the seed used by an in-flight search.
                // Stop instead of granting a result without safely consuming that seed.
                this.stop(false, false);
            }
            return;
        }
        ItemStack.parse(registries, seedTag).ifPresent(stack -> {
            this.capturedSeedStack = stack;
            this.capturedSeedItem = stack.getItem();
            this.capturedSnapshot = CelestialSnapshotCodec.extract(stack);
        });
        if (this.capturedSeedStack == null || this.capturedSeedStack.isEmpty()) {
            this.stop(false, false);
        }
    }

    void loadSynced(
        boolean searching,
        int ticksRemaining,
        boolean failed,
        boolean powerInsufficient
    ) {
        this.loadState(searching, ticksRemaining, failed, powerInsufficient);
    }

    private void loadState(
        boolean searching,
        int ticksRemaining,
        boolean failed,
        boolean powerInsufficient
    ) {
        this.searching = searching && ticksRemaining > 0;
        this.ticksRemaining = Math.max(0, ticksRemaining);
        this.failed = failed;
        this.powerInsufficient = powerInsufficient;
        this.capturedSeedItem = null;
        this.capturedSeedStack = null;
        this.capturedSnapshot = null;
    }

    void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.remove(CAPTURED_SEED_KEY);
        tag.remove(SEED_STATE_KNOWN_KEY);
        if (this.searching) {
            tag.putBoolean(SEED_STATE_KNOWN_KEY, true);
        }
        if (this.searching && this.capturedSeedStack != null) {
            tag.put(CAPTURED_SEED_KEY, this.capturedSeedStack.save(registries));
        }
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

    void setFailed(boolean failed) {
        this.failed = failed;
    }

    boolean isPowerInsufficient() {
        return this.powerInsufficient;
    }

    private static void applySnapshot(CelestialForgingAnvilBlockEntity owner, CompoundTag tag) {
        owner.setCelestialBodyData(CelestialBodyData.fromTag(tag.getCompound("celestialBody")));
        owner.setBodySeed(tag.getLong("bodySeed"));
        owner.setAgeAnvilCount(tag.getInt("ageAnvilCount"));
        owner.setStellarMass(tag.getInt("stellarMass"));
        PlanetaryResourceSet resources = tag.contains("planetaryResources")
            ? PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"))
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

    private static void consumeSeed(CelestialForgingAnvilBlockEntity owner, boolean capturedSeedMatches) {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide() || !capturedSeedMatches) return;
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
        this.capturedSeedItem = null;
        this.capturedSeedStack = null;
        this.capturedSnapshot = null;
    }

    private static void sync(CelestialForgingAnvilBlockEntity owner) {
        owner.markSearchStateChanged();
    }
}
