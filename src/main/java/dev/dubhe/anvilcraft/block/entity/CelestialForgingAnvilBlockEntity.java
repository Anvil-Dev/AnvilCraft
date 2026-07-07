package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceGenerator;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyRecipe;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExcavatorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.PenroseSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import dev.dubhe.anvilcraft.item.utility.DiskItem;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.GravityManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CelestialForgingAnvilBlockEntity extends BlockEntity
    implements MenuProvider, IPowerConsumer, IPowerProducer, IDiskCloneable {

    // === Megastructure delegation ===
    @Getter
    private final CfaMegastructureManager megastructureManager = new CfaMegastructureManager();

    @Getter
    private float preRotation = 0;
    @Getter
    private float rotation = 0;

    @Getter
    private boolean isAmplify = false;

    /**
     * Get the strongest redstone signal (0–15) received by the CFA's 3×2×3 structure.
     * Scans all 18 block positions in the structure bounding box, taking the max of each
     * block's best neighbor signal. Cached for {@code REDSTONE_SIGNAL_CACHE_TICKS} ticks,
     * recomputed on expiry or when {@link #markRedstoneSignalDirty()} is called.
     */
    public int getRedstoneSignal() {
        if (level == null) return 0;
        long now = level.getGameTime();
        if (this.redstoneSignalCacheTick >= 0 && now - this.redstoneSignalCacheTick < REDSTONE_SIGNAL_CACHE_TICKS) {
            return this.cachedRedstoneSignal;
        }
        int signal = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos partPos = worldPosition.offset(dx, dy, dz);
                    signal = Math.max(signal, level.getBestNeighborSignal(partPos));
                }
            }
        }
        this.cachedRedstoneSignal = Math.min(signal, 15);
        this.redstoneSignalCacheTick = now;
        return this.cachedRedstoneSignal;
    }

    /**
     * Invalidate the redstone-signal cache; called from the block's neighborChanged callback.
     */
    public void markRedstoneSignalDirty() {
        this.redstoneSignalCacheTick = -1;
    }

    private int cachedRedstoneSignal = 0;
    private long redstoneSignalCacheTick = -1;
    private static final int REDSTONE_SIGNAL_CACHE_TICKS = 5;

    @Getter
    @Setter
    @Nullable
    private CelestialBodyData celestialBodyData = null;

    @Getter
    @Setter
    private long bodySeed = 0;

    /**
     * Mass anvil count at time of body matching, for gravity calculation.
     */
    @Getter
    @Setter
    private int stellarMass = 0;

    /**
     * Age anvil count (time slot) stored for resource generation.
     */
    @Getter
    @Setter
    private int ageAnvilCount = 0;

    /**
     * Resources generated for the matched celestial body.
     */
    @Getter
    @Setter
    @Nullable
    private PlanetaryResourceSet planetaryResourceSet = null;

    /**
     * Index of the currently built megastructure (refactor option), or -1 if none.
     * Delegates to CfaMegastructureManager.
     */
    public int getActiveMegastructureIndex() {
        return this.megastructureManager.getActiveIndex();
    }

    /**
     * Whether the excavator has valid laser input (for model switching).
     * Delegates to ExcavatorHandler.
     */
    public boolean isExcavatorLaserActive() {
        ExcavatorHandler h = this.megastructureManager.findHandler(ExcavatorHandler.class);
        return h != null && h.isLaserActive();
    }

    /**
     * Whether the Penrose Sphere has valid laser input/output pairs (for model switching).
     * Delegates to PenroseSphereHandler.
     */
    public boolean isPenroseSphereLaserActive() {
        PenroseSphereHandler h = this.megastructureManager.findHandler(PenroseSphereHandler.class);
        return h != null && h.isLaserActive();
    }

    // === Wormhole Stabilizer state ===
    /**
     * Hash of the black hole parameters, computed when the stabilizer is built.
     */
    @Nullable
    public UUID getWormholeParamsHash() {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        return wh.getBodyUuid();
    }

    /**
     * Tracked chunk-loaded connected CFAs, keyed by dimension + position.
     */
    private final Map<WormholeChunkLoadKey, LoadChuckData> wormholeLoadedChunks = new HashMap<>();

    private record WormholeChunkLoadKey(Identifier dimension, BlockPos pos) {}

    // === Temple state ===
    /**
     * Current position in the 3-day cycle: 0=blessing, 1=blessing, 2=punishment.
     */
    @Getter
    private int templeCycleDay = 0;
    /**
     * Last MC day when the demand was refreshed.
     */
    private long templeLastDay = -1;
    /**
     * The currently demanded item type (count=1, identity only; synced to client for tooltip).
     */
    @Getter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    /**
     * Total count required for the current demand.
     */
    @Getter
    private int templeDemandCount = 0;
    /**
     * Cumulative count of items already offered toward the current demand.
     * Resets when a new demand is picked or the demand is satisfied.
     */
    @Getter
    private int templeDemandProgress = 0;
    /**
     * Whether the current day's demand has been satisfied.
     */
    @Getter
    private boolean templeDemandSatisfied = false;

    // === Stellar Evolution Accelerator delegation ===
    public int getAcceleratorStage() {
        return this.megastructureManager.getAcceleratorHandler().getStage();
    }

    public int getAcceleratorTicksRemaining() {
        return this.megastructureManager.getAcceleratorHandler().getTicksRemaining();
    }

    public int getAcceleratorTicksTotal() {
        return this.megastructureManager.getAcceleratorHandler().getTicksTotal();
    }

    public int getSupernovaFlashTicks() {
        return this.megastructureManager.getAcceleratorHandler().getSupernovaFlashTicks();
    }

    public int getCollapseAnimTicks() {
        return this.megastructureManager.getAcceleratorHandler().getCollapseAnimTicks();
    }

    /**
     * Whether the stellar evolution accelerator is active (any stage 1-4).
     */
    public boolean isAcceleratorActive() {
        return this.megastructureManager.getAcceleratorHandler().isActive();
    }

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === IPowerConsumer ===

    @Override
    public int getInputPower() {
        if (this.searching && this.searchTicksRemaining > 0) {
            return this.isAmplify ? 4000 : 1000;
        }
        return this.megastructureManager.getInputPower(this);
    }

    @Override
    public int getOutputPower() {
        return this.megastructureManager.getOutputPower(this);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public int getRange() {
        return 1;
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        return this.megastructureManager.getComponentType(this);
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        PowerComponentType type = this.getComponentType();
        return new PowerComponentInfo(
            this.getPos(),
            this.getInputPower(),
            this.getOutputPower(),
            0, 0,
            this.getRange(),
            getShape(),
            type
        );
    }

    @Override
    public void gridTick() {
        this.megastructureManager.gridTick(this);
    }

    private boolean hasEnoughPower() {
        if (this.grid == null) return false;
        int required = this.getInputPower();
        return required <= 0 || this.grid.isWorking();
    }

    @Getter
    private int bodyRotation = 0;

    // === Celestial body animation (client-side only, not persisted) ===
    @Getter
    private int animationTicks = 0;
    @Getter
    private boolean animationForward = true;
    @Nullable
    @Getter
    private CelestialBodyData animationPreviousBodyData = null;
    private static final int ANIMATION_DURATION_TICKS = 20; // 1 second at 20 TPS

    /**
     * Get the effective celestial body data for rendering, accounting for reverse animation.
     * During reverse animation, the actual celestialBodyData is already null (server cleared it),
     * so we use the cached previous data to keep rendering the shrinking body.
     */
    @Nullable
    public CelestialBodyData getEffectiveBodyDataForRendering() {
        if (this.celestialBodyData != null) return this.celestialBodyData;
        if (this.animationTicks > 0 && !this.animationForward && this.animationPreviousBodyData != null) {
            return this.animationPreviousBodyData;
        }
        return null;
    }

    /**
     * Get animation progress from 0 (hidden) to 1 (fully visible).
     * Uses ease-in-out cubic interpolation.
     */
    public float getAnimationProgress(float partialTick) {
        if (this.animationTicks <= 0) return this.animationForward ? 1.0f : 0.0f;
        float t = (ANIMATION_DURATION_TICKS - this.animationTicks + partialTick) / (float) ANIMATION_DURATION_TICKS;
        float eased = easeInOutCubic(t);
        return this.animationForward ? eased : (1.0f - eased);
    }

    /**
     * Get rotation speed multiplier during animation.
     * Starts fast (5x) and decays to 1x as animation progresses.
     */
    public float getAnimationRotationBoost(float partialTick) {
        float progress = this.getAnimationProgress(partialTick);
        return 1.0f + 4.0f * (1.0f - progress);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3) / 2.0f;
    }

    // === Supernova flash (synced to client, rendering only) ===
    /** Body visual-center world Y captured at trigger time (flash center, independent of the remnant). */
    @Getter
    private double supernovaCenterY = 0;
    /** Body scale ratio (relative to full redstone-15 scale) captured at trigger, so the flash tracks the body scale. */
    @Getter
    private float supernovaScale = 1.0f;
    /** Total supernova flash duration (ticks); matches AcceleratorHandler.triggerSupernova initial value. */
    public static final int SUPERNOVA_FLASH_TICKS = 10;

    /**
     * Trigger the supernova flash on the server and sync to the client. Called by the AcceleratorHandler
     * during the supernova stage. MUST be called before the remnant replaces the body data, so the
     * exploding star's center and scale can be captured.
     */
    public void startSupernovaFlash() {
        this.megastructureManager.getAcceleratorHandler().setSupernovaFlashTicks(SUPERNOVA_FLASH_TICKS);
        this.supernovaCenterY = this.getBodyCenterWorldY();
        // Scale ratio = current body scale / base (no-redstone) body scale: 1 with no redstone
        // (baseline 16×16), larger as redstone increases so the flash grows with the body.
        if (this.celestialBodyData != null) {
            float redstoneFactor = this.getRedstoneSignal() / 5.0f;
            float rawBodyScale = this.celestialBodyData.bodyScale();
            float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
            float bodyScaleMultiplier = rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor;
            this.supernovaScale = rawBodyScale > 1e-6f ? bodyScaleMultiplier / rawBodyScale : 1.0f;
        } else {
            this.supernovaScale = 1.0f;
        }
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            this.syncToClient();
        }
    }

    /**
     * Compute the world Y of the body visual center at the current redstone signal.
     * Matches the renderer centerY: linear interpolation from baseCenterY to the full dynamicCenterY.
     */
    public double getBodyCenterWorldY() {
        float redstoneFactor = this.getRedstoneSignal() / 5.0f;
        float fullCenterY = CelestialBodyData.dynamicCenterY(this.celestialBodyData, this.isAmplify);
        float baseCenterY = this.isAmplify ? 6.5f : 4.5f;
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;
        return worldPosition.getY() + centerY;
    }

    // === Render smoothing (client-only, not persisted / synced) ===
    // Frame-rate-independent exponential approach for ring scale, body-center height, body scale, beam
    // height, so redstone-driven size/height changes glide smoothly instead of snapping each tick.
    @Getter
    private float smoothRingScale;
    @Getter
    private float smoothCenterY;
    @Getter
    private float smoothBodyScale;
    @Getter
    private float smoothBeamHeight;
    private boolean smoothInitialized = false;
    private long lastSmoothNanos = 0L;
    /** Exponential approach time constant (seconds). Smaller = catches up faster. */
    private static final float SMOOTH_TAU = 0.18f;

    /** Advance one frame of smoothing, returning the frame-rate-independent approach factor. */
    private float advanceSmoothFactor() {
        long now = Util.getNanos();
        if (!this.smoothInitialized) {
            this.lastSmoothNanos = now;
            return 1.0f;
        }
        float dt = (now - this.lastSmoothNanos) / 1.0e9f;
        this.lastSmoothNanos = now;
        if (dt <= 0f) return 0f;
        if (dt > 0.25f) dt = 0.25f; // guard against jumps after lag/pause
        return 1.0f - (float) Math.exp(-dt / SMOOTH_TAU);
    }

    /** Update the smoothed render scale/height values. Called each frame by the renderer with the current targets. */
    public void updateRenderSmoothing(float targetRingScale, float targetCenterY, float targetBodyScale, float targetBeamHeight) {
        float f = this.advanceSmoothFactor();
        if (!this.smoothInitialized) {
            this.smoothRingScale = targetRingScale;
            this.smoothCenterY = targetCenterY;
            this.smoothBodyScale = targetBodyScale;
            this.smoothBeamHeight = targetBeamHeight;
            this.smoothInitialized = true;
            return;
        }
        this.smoothRingScale += (targetRingScale - this.smoothRingScale) * f;
        this.smoothCenterY += (targetCenterY - this.smoothCenterY) * f;
        this.smoothBodyScale += (targetBodyScale - this.smoothBodyScale) * f;
        this.smoothBeamHeight += (targetBeamHeight - this.smoothBeamHeight) * f;
    }

    @Getter
    @Setter
    private boolean locked = false;

    /**
     * Whether the amplifier multiblock is physically formed.
     */
    @Getter
    @Setter
    private boolean amplifierPresent = false;

    // Material slot filter (set when a refactor option is selected)
    @Getter
    @Setter
    private ItemStack materialFilter = new ItemStack(Items.BARRIER);
    @Getter
    @Setter
    private int materialLimit = 0;

    @Getter
    private final SimpleContainer materialContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            CelestialForgingAnvilBlockEntity.this.setChanged();
        }
    };

    /**
     * Configure the material slot for a given refactor option.
     * Called on the server when the player selects a refactor option.
     */
    public void configureMaterialSlot(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (this.celestialBodyData == null) return;
        List<CelestialRefactorOption> options = this.getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            setMaterialFilter(new ItemStack(Items.BARRIER));
            setMaterialLimit(0);
        } else {
            CelestialRefactorOption opt = options.get(optionIndex);
            if (opt.needsMaterial()) {
                setMaterialFilter(opt.material().copy());
                setMaterialLimit(opt.materialCount());
            } else {
                setMaterialFilter(new ItemStack(Items.BARRIER));
                setMaterialLimit(0);
            }
        }
        this.setChanged();
        this.syncToClient();
    }

    // Search timer
    @Getter
    private int searchTicksRemaining = 0;
    @Getter
    private boolean searching = false;
    @Getter
    @Setter
    private boolean searchFailed = false;
    @Getter
    private boolean powerInsufficient = false;
    private static final int SEARCH_TICKS = 200; // 10 second

    // Track the seed item consumed when the search started (for special body matching)
    @Nullable
    private Item lastConsumedSeedItem = null;
    @Nullable
    private CompoundTag lastConsumedSeedNbt = null;

    // Power grid
    @Nullable
    private PowerGrid grid;

    // Gravity source state
    private boolean gravitySourceActive = false;
    private double currentGravityStrength = 0;
    private int currentGravitySize = 0;
    /**
     * Y-offset from controller block to the rendered star center.
     */
    private static final int GRAVITY_CENTER_Y_OFFSET = 6;
    /**
     * Gravity influence radius (blocks), covers the Ring6 7×7×7 area.
     * Represents ~2× the largest stellar radius (red supergiant ~2580 R☉).
     */
    private static final int GRAVITY_RADIUS = 4;
    /**
     * Unified reference physical radius for all bodies' gravity calculation.
     * 5000 × R☉, and R☉/R⊕ = 109, so R_ref/R⊕ = 545,000.
     */
    private static final double GRAVITY_REFERENCE_RADIUS_RATIO = 5000.0 * 109.0;
    /**
     * Gameplay multiplier to make gravity perceptible at the block scale.
     */
    private static final double GRAVITY_STRENGTH_MULTIPLIER = 10000000.0;

    public void startSearch() {
        this.searchFailed = false;
        this.powerInsufficient = false;

        // Check if seed item is present (for pre-check skip and consumption)
        ItemStack seedStack = this.anvilInventory.getItem(4);
        boolean hasSeedItem = !seedStack.isEmpty();

        // Server-side parameter pre-check (skip when seed item is present)
        if (level != null && !level.isClientSide()) {
            if (!hasSeedItem) {
                var preCheck = CelestialBodyMatcher.match(
                    this.getAnvilCount(0),
                    this.getAnvilCount(1),
                    this.getAnvilCount(2),
                    this.getAnvilCount(3),
                    this.isAmplify,
                    level.getRandom()
                );
                if (preCheck == null) {
                    this.searchFailed = true;
                    this.searching = false;
                    this.searchTicksRemaining = 0;
                    setChanged();
                    this.syncToClient();
                    return;
                }
            }
        }

        // Check power availability
        if (!this.hasEnoughPower()) {
            this.powerInsufficient = true;
            this.searching = false;
            this.searchTicksRemaining = 0;
            setChanged();
            if (level != null && !level.isClientSide()) {
                this.syncToClient();
            }
            return;
        }

        // Capture seed item data but don't consume yet (consumed on successful match)
        if (hasSeedItem) {
            this.lastConsumedSeedItem = seedStack.getItem();
            this.lastConsumedSeedNbt = extractSnapshot(seedStack);
        } else {
            this.lastConsumedSeedItem = null;
            this.lastConsumedSeedNbt = null;
        }

        // Only clear the old body once we know the search will actually start
        this.setCelestialBodyData(null);
        // Start search
        this.searchTicksRemaining = SEARCH_TICKS;
        this.searching = true;
        setChanged();
        if (level != null && !level.isClientSide()) {
            this.syncToClient();
        }
    }

    public void serverTick() {
        // Continuous power state refresh — clears stale powerInsufficient when grid recovers
        boolean hasEnoughPower = this.hasEnoughPower();
        if (!hasEnoughPower && !this.powerInsufficient) {
            this.powerInsufficient = true;
            setChanged();
            this.syncToClient();
        } else if (hasEnoughPower && this.powerInsufficient) {
            this.powerInsufficient = false;
            setChanged();
            this.syncToClient();
        }
        if (this.searchTicksRemaining > 0) {
            // Check if power is still sufficient during search
            if (!hasEnoughPower) {
                this.searching = false;
                this.searchTicksRemaining = 0;
                this.powerInsufficient = true;
                setChanged();
                if (level != null) {
                    this.syncToClient();
                }
            } else {
                this.searchTicksRemaining--;
                if (this.searchTicksRemaining == 0) {
                    this.searching = false;
                    this.tryMatchCelestialBody();
                    if (this.celestialBodyData == null) {
                        this.searchFailed = true;
                    }
                    setChanged();
                    if (level != null) {
                        this.syncToClient();
                    }
                }
            }
        }

        // Manage stellar gravity source
        this.updateGravitySource();

        // Destroy entities at the gravity center
        if (this.gravitySourceActive && level != null) {
            this.destroyEntitiesAtCenter();
        }

        // Megastructure logic (delegated to handler classes)
        this.megastructureManager.serverTick(this);

        // Supernova flash timer
        var accel = this.megastructureManager.getAcceleratorHandler();
        if (accel.getSupernovaFlashTicks() > 0) {
            accel.setSupernovaFlashTicks(accel.getSupernovaFlashTicks() - 1);
        }
    }

    /**
     * Update the gravity source for the current celestial body.
     *
     * <p>All bodies share a unified reference radius (5000 R☉ ≈ 2 × red supergiant radius)
     * that corresponds to the {@link #GRAVITY_RADIUS} boundary in blocks.
     * Gravity falls off as 1/r² from the source center.
     *
     * <p>Strength = gravity at the unified reference radius, in multiples of g⊕:
     * <ul>
     *   <li>Mass: M/M⊕ = 2^((massAnvilCount - 12) / 2)</li>
     *   <li>Reference radius: R_ref/R⊕ = 5000 × 109 = 545,000</li>
     *   <li>Strength = (M/M⊕) / (R_ref/R⊕)²</li>
     * </ul>
     */
    private void updateGravitySource() {
        if (level == null || level.isClientSide()) return;

        boolean shouldHaveGravity = this.amplifierPresent
            && this.celestialBodyData instanceof StarData
            && this.stellarMass > 0
            && this.celestialBodyData.size() > 0;

        double newStrength = 0;
        if (shouldHaveGravity) {
            double massRatio = Math.pow(2, (this.stellarMass - 12) / 2.0);
            newStrength = massRatio * GRAVITY_STRENGTH_MULTIPLIER
                / (GRAVITY_REFERENCE_RADIUS_RATIO * GRAVITY_REFERENCE_RADIUS_RATIO);
        }
        int newSize = shouldHaveGravity ? this.celestialBodyData.size() : 0;

        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);

        if (shouldHaveGravity) {
            if (!this.gravitySourceActive || newStrength != this.currentGravityStrength || newSize != this.currentGravitySize) {
                // Remove old source if strength/size changed
                if (this.gravitySourceActive) {
                    GravityManager.GravitySourceManager.removeSource(level, centerPos);
                }
                // Add new/updated source
                GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(newStrength, GRAVITY_RADIUS);
                GravityManager.GravitySourceManager.addSource(level, centerPos, type);
                this.gravitySourceActive = true;
                this.currentGravityStrength = newStrength;
                this.currentGravitySize = newSize;
            }
        } else if (this.gravitySourceActive) {
            GravityManager.GravitySourceManager.removeSource(level, centerPos);
            this.gravitySourceActive = false;
            this.currentGravityStrength = 0;
            this.currentGravitySize = 0;
        }
    }

    /**
     * Force remove the gravity source. Called when the amplifier is dismantled
     * to ensure gravity disappears immediately rather than waiting for next tick.
     */
    public void removeGravitySource() {
        if (level == null || level.isClientSide()) return;
        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
        GravityManager.GravitySourceManager.removeSource(level, centerPos);
        this.gravitySourceActive = false;
        this.currentGravityStrength = 0;
        this.currentGravitySize = 0;
    }

    private void destroyEntitiesAtCenter() {
        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
        AABB centerBox = new AABB(centerPos);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, centerBox);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                if (this.celestialBodyData instanceof StarData star
                    && star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                    // noinspection deprecation
                    living.hurtOrSimulate(ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
                } else {
                    // noinspection deprecation
                    living.hurtOrSimulate(level.damageSources().inFire(), 1.0E12f);
                }
            } else {
                entity.discard();
            }
        }
    }

    /**
     * Search history, max 10 entries. Index 0 = newest.
     */
    @Getter
    private final List<SearchHistoryEntry> searchHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    /**
     * A search history entry bundling a celestial body with its generated resources.
     */
    public record SearchHistoryEntry(CelestialBodyData body, @Nullable PlanetaryResourceSet resources) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.put("body", this.body.toTag());
            if (this.resources != null) {
                tag.put("resources", this.resources.toTag());
            }
            return tag;
        }

        public static SearchHistoryEntry fromTag(CompoundTag tag) {
            CelestialBodyData body = CelestialBodyData.fromTag(tag.getCompoundOrEmpty("body"));
            PlanetaryResourceSet resources = null;
            if (tag.contains("resources")) {
                resources = PlanetaryResourceSet.fromTag(tag.getCompoundOrEmpty("resources"));
            }
            return new SearchHistoryEntry(body, resources);
        }
    }

    /**
     * Browsing index into searchHistory: 0 = showing locked body, 1+ = browsing.
     */
    @Getter
    private int historyBrowseIndex = 0;
    @Nullable
    private SearchHistoryEntry historyOriginalEntry;

    @Getter
    private final SimpleContainer anvilInventory = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            CelestialForgingAnvilBlockEntity.this.setChanged();
        }
    };

    public void tick() {
        if (this.rotation >= 360) this.rotation -= 360;
        this.preRotation = this.rotation;
        // 红石信号越大星环越大 → 转速越慢
        float rotationSpeed = 3.0f / (1.0f + this.getRedstoneSignal() * 0.4f);
        this.rotation += rotationSpeed;
        this.bodyRotation += 1;

        // Animation tick (client-side only)
        if (this.animationTicks > 0) {
            this.animationTicks--;
            if (this.animationTicks == 0 && !this.animationForward) {
                this.animationPreviousBodyData = null;
            }
        }
        // Supernova flash countdown (client-side, for rendering)
        var accel = this.megastructureManager.getAcceleratorHandler();
        if (accel.getSupernovaFlashTicks() > 0) {
            accel.setSupernovaFlashTicks(accel.getSupernovaFlashTicks() - 1);
        }
        // Collapse animation — during accelerator stage 3, the server syncs every tick
        // so the client should NOT independently decrement to avoid desync.
        // Outside stage 3, the client decrements independently as a fallback.
        if (accel.getCollapseAnimTicks() > 0 && accel.getStage() != 3) {
            accel.setCollapseAnimTicks(accel.getCollapseAnimTicks() - 1);
        }
    }

    public void setAmplify(boolean amplify) {
        if (this.isAmplify != amplify) {
            this.isAmplify = amplify;
            if (level != null && !level.isClientSide()) {
                if (this.celestialBodyData instanceof StarData) {
                    if (!amplify) {
                        this.locked = true; // Lock when amplifier removed with stellar body
                    }
                }
            }
            this.setChanged();
            if (level != null) {
                this.syncToClient();
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && !PowerGrid.isServerClosing) {
            if (this.gravitySourceActive) {
                BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
                GravityManager.GravitySourceManager.removeSource(level, centerPos);
                this.gravitySourceActive = false;
            }
            // Unregister wormhole and clear megastructures so connected portals close.
            // Skip during server shutdown to avoid accessing saved data during save.
            this.megastructureManager.clearAllMegastructures(this);
        }
    }

    /**
     * Get a reproducible ±5% random offset percentage derived from bodySeed.
     * Used only for UI display of age/radius/mass values.
     *
     * @param index 0=age(time), 1=radius(space), 2=mass
     * @return offset in [-0.05, +0.05]
     */
    public float getDisplayOffset(int index) {
        if (this.bodySeed == 0) return 0f;
        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(this.bodySeed + index * 7919L);
        return (rand.nextFloat() - 0.5f) * 0.1f;
    }

    public void tryMatchCelestialBody() {
        if (level == null) return;
        int time = this.getAnvilCount(0);
        final int space = this.getAnvilCount(1);
        int mass = this.getAnvilCount(2);
        final int energy = this.getAnvilCount(3);
        this.ageAnvilCount = time;
        this.bodySeed = level.getRandom().nextLong();
        this.stellarMass = mass;

        // Verify seed item is still present — if player removed it during the search,
        // clear captured data so we fall through to normal matching instead of granting
        // a special planet without deducting the seed item.
        if (this.lastConsumedSeedItem != null || this.lastConsumedSeedNbt != null) {
            ItemStack seedStack = this.anvilInventory.getItem(4);
            if (seedStack.isEmpty()) {
                this.lastConsumedSeedItem = null;
                this.lastConsumedSeedNbt = null;
            }
        }

        // First: check for seed item snapshot (disk / singularity crystal)
        if (this.lastConsumedSeedNbt != null && this.lastConsumedSeedNbt.contains("celestialBody")) {
            this.applySnapshot(this.lastConsumedSeedNbt);
            this.consumeSeedItem();
            if (!level.isClientSide()) {
                this.setChanged();
                this.syncToClient();
            }
            return;
        }

        // Second: check for a player-head seed item → player-head celestial body
        if (this.lastConsumedSeedItem == Items.PLAYER_HEAD) {
            ItemStack headSeedStack = this.anvilInventory.getItem(4);
            CompoundTag profileTag = extractProfileNbt(headSeedStack);
            if (profileTag != null) {
                this.celestialBodyData = SpecialCelestialBodyData.fromPlayerHead(profileTag, space);
                this.planetaryResourceSet = new PlanetaryResourceSet();
                this.addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
                this.consumeSeedItem();
                if (!level.isClientSide()) {
                    this.setChanged();
                    this.syncToClient();
                }
                return;
            }
        }

        // Second: check for special celestial body discovery via seed item
        if (this.lastConsumedSeedItem != null) {
            SpecialCelestialBodyData specialBody = this.tryMatchSpecialCelestialBody(
                time,
                space,
                mass,
                energy,
                this.lastConsumedSeedItem,
                ((ServerLevel) level).getSeed()
            );
            if (specialBody != null) {
                this.celestialBodyData = specialBody;
                if (!level.isClientSide()) {
                    Identifier recipeId = Identifier.parse(specialBody.recipeId());
                    ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key =
                        ResourceKey.create(Registries.RECIPE, recipeId);
                    net.minecraft.world.item.crafting.RecipeHolder<?> holder =
                        RecipesRecord.getRecipes(this.level).byKey(key);
                    if (holder != null && holder.value() instanceof SpecialCelestialBodyRecipe recipe) {
                        this.planetaryResourceSet = recipe.generateResources();
                    }
                }
                this.addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
                this.consumeSeedItem();
                if (!level.isClientSide()) {
                    this.setChanged();
                    this.syncToClient();
                }
                return;
            }
        }

        // Fall back to normal three-step matching
        this.celestialBodyData = CelestialBodyMatcher.match(time, space, mass, energy, this.isAmplify, level.getRandom());
        if (this.celestialBodyData != null) {
            // Assign a UUID derived from bodySeed for wormhole identity
            if (this.celestialBodyData instanceof StarData star && star.bodyUuid() == null) {
                this.celestialBodyData = star.withBodyUuid(StarData.uuidFromBodySeed(this.bodySeed));
            }
            // Generate planetary resources
            if (!level.isClientSide()) {
                this.planetaryResourceSet = PlanetResourceGenerator.generate(
                    this.celestialBodyData,
                    this.ageAnvilCount,
                    level,
                    this.bodySeed
                );
            }
            this.addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        } else {
            this.planetaryResourceSet = null;
            this.searchTicksRemaining = 0; // Stop timer on failure
        }
        this.consumeSeedItem();

        if (!level.isClientSide()) {
            this.setChanged();
            this.syncToClient();
        }
    }

    /**
     * Try to match a special (hidden) celestial body based on anvil parameters
     * and the consumed seed item. The seed item must be THE effective item for
     * this world seed (using the same pattern as RoyalPreference).
     */
    private void consumeSeedItem() {
        if (level == null || level.isClientSide()) return;
        ItemStack seed = this.anvilInventory.getItem(4);
        if (!seed.isEmpty()) {
            this.anvilInventory.setItem(4, ItemStack.EMPTY);
        }
    }

    /**
     * Extract the resolvable-profile NBT from a player-head stack, or null if it has none.
     */
    @Nullable
    private static CompoundTag extractProfileNbt(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) return null;
        net.minecraft.world.item.component.ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return null;
        return (CompoundTag) net.minecraft.world.item.component.ResolvableProfile.CODEC
            .encodeStart(NbtOps.INSTANCE, profile)
            .getOrThrow();
    }

    @Nullable
    private SpecialCelestialBodyData tryMatchSpecialCelestialBody(
        int time,
        int space,
        int mass,
        int energy,
        Item consumedSeedItem,
        long worldSeed
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        List<SpecialCelestialBodyRecipe> recipes = RecipesRecord.getRecipes(serverLevel)
            .byType(ModRecipeTypes.SPECIAL_CELESTIAL_BODY.get())
            .stream().map(RecipeHolder::value).toList();
        for (SpecialCelestialBodyRecipe recipe : recipes) {
            if (recipe.time() == time && recipe.space() == space
                && recipe.mass() == mass && recipe.energy() == energy
                && recipe.isEffectiveSeedItem(consumedSeedItem, worldSeed)
            ) {
                // Find the recipe holder to get the full ID
                return RecipesRecord.getRecipes(serverLevel)
                    .byType(ModRecipeTypes.SPECIAL_CELESTIAL_BODY.get())
                    .stream()
                    .filter(h -> h.value() == recipe)
                    .findFirst()
                    .map(h -> SpecialCelestialBodyData.fromRecipe(recipe, h.id().identifier().toString()))
                    .orElse(null);
            }
        }
        return null;
    }

    /**
     * Load a celestial body from a snapshot (disk / singularity crystal seed item).
     * The snapshot contains all parameters — anvil counts are ignored for matching.
     */
    private void applySnapshot(CompoundTag tag) {
        if (level == null) return;
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompoundOrEmpty("celestialBody"));
        }
        this.bodySeed = tag.getLongOr("bodySeed", 0);
        this.ageAnvilCount = tag.getIntOr("ageAnvilCount", 0);
        this.stellarMass = tag.getIntOr("stellarMass", 0);
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompoundOrEmpty("planetaryResources"));
        }
        this.addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        if (!level.isClientSide()) {
            this.setChanged();
            this.syncToClient();
        }
    }

    // === IDiskCloneable ===

    @Override
    public void storeDiskData(ValueOutput output) {
        if (this.celestialBodyData != null) {
            output.store("celestialBody", CompoundTag.CODEC, this.celestialBodyData.toTag());
            output.putLong("bodySeed", this.bodySeed);
            output.putInt("ageAnvilCount", this.ageAnvilCount);
            output.putInt("stellarMass", this.stellarMass);
            output.putIntArray(
                "anvilCounts", new int[]{
                    this.getAnvilCount(0),
                    this.getAnvilCount(1),
                    this.getAnvilCount(2),
                    this.getAnvilCount(3)
                }
            );
            output.putBoolean("isAmplify", this.isAmplify);
            if (this.planetaryResourceSet != null) {
                output.store("planetaryResources", CompoundTag.CODEC, this.planetaryResourceSet.toTag());
            }
        }
    }

    @Override
    public void applyDiskData(ValueInput input) {
        // Disk data is only applied via the seed slot, not via right-click.
    }

    @Override
    public InteractionResult useDisk(Level level, Player player, InteractionHand hand, ItemStack itemStack, BlockHitResult hitResult) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (itemStack.is(ModItems.DISK.get())) {
            // Only allow storing, not applying
            if (!DiskItem.hasDataStored(itemStack)) {
                // Extreme bodies (black hole / neutron star) require a singularity crystal
                if (this.celestialBodyData instanceof StarData star && star.bodyClass().isExtreme()) {
                    player.sendSystemMessage(
                        Component.translatable("message.anvilcraft.disk.extreme_body_requires_crystal")
                            .withStyle(ChatFormatting.RED)
                    );
                    return InteractionResult.FAIL;
                }
                // Redirect hit to main block position so DiskItem.useOn finds the BlockEntity
                BlockHitResult mainHit = new BlockHitResult(
                    hitResult.getLocation(),
                    hitResult.getDirection(),
                    this.getBlockPos(),
                    hitResult.isInside()
                );
                return itemStack.useOn(new UseOnContext(level, player, hand, itemStack, mainHit));
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * Extract a celestial snapshot from a seed item stack.
     */
    @Nullable
    public static CompoundTag extractSnapshot(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            return DiskItem.getData(stack).copy();
        }
        return loadSnapshotFromStack(stack);
    }

    /**
     * Load a celestial snapshot from a disk or singularity crystal.
     */
    @Nullable
    public static CompoundTag loadSnapshotFromStack(ItemStack stack) {
        // Disk
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            CompoundTag data = DiskItem.getData(stack);
            if (data.contains("celestialBody")) return data.copy();
        }
        // Singularity crystal
        if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            if (!tag.isEmpty() && tag.contains("celestialSnapshot")) {
                CompoundTag snapshot = tag.getCompoundOrEmpty("celestialSnapshot");
                if (!snapshot.isEmpty() && snapshot.contains("celestialBody")) return snapshot.copy();
            }
        }
        return null;
    }

    /**
     * Save a snapshot into a disk or singularity crystal.
     */
    public static void saveSnapshotToStack(ItemStack stack, CompoundTag snapshot) {
        if (stack.getItem() instanceof DiskItem) {
            // Extreme bodies (black hole / neutron star) cannot be stored on disks
            if (snapshot.contains("celestialBody")) {
                CompoundTag bodyTag = snapshot.getCompoundOrEmpty("celestialBody");
                String bodyClass = bodyTag.getStringOr("bodyClass", "");
                if ("BLACK_HOLE".equals(bodyClass) || "NEUTRON_STAR".equals(bodyClass)) {
                    return; // silently reject — extreme bodies require singularity crystal
                }
            }
            CompoundTag diskTag = DiskItem.hasDataStored(stack)
                ? DiskItem.getData(stack).copy()
                : new CompoundTag();
            diskTag.merge(snapshot);
            stack.set(ModComponents.DISK_DATA, new DiskData(diskTag));
        } else if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            CustomData oldCustom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag updated = oldCustom.copyTag();
            updated.put("celestialSnapshot", snapshot.copy());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(updated));
        }
    }

    // === CFA block interaction ===

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            // Re-register with power grid to ensure CFA is in both producer and consumer sets
            PowerGrid.addComponent(this);
            // Re-register with wormhole network if wormhole stabilizer is active
            // Delegated to handler's onBuild which handles re-registration
            WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
            if (this.megastructureManager.getActiveIndex() >= 0 && this.getActiveMegastructureOption() != null
                && "wormhole_stabilizer".equals(this.getActiveMegastructureOption().megastructure())) {
                wh.onBuild(this);
            }
            this.setChanged();
            this.syncToClient();
        }
    }

    // === NBT persistence (26.1: ValueOutput/ValueInput for disk) ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("amplified", this.isAmplify);
        output.putLong("bodySeed", this.bodySeed);
        output.putInt("stellarMass", this.stellarMass);

        output.putBoolean("locked", this.locked);
        output.putBoolean("amplifierPresent", this.amplifierPresent);
        output.putBoolean("searching", this.searching);
        output.putInt("searchTicks", this.searchTicksRemaining);
        output.putBoolean("searchFailed", this.searchFailed);
        output.putBoolean("powerInsufficient", this.powerInsufficient);
        if (this.celestialBodyData != null) {
            output.store("celestialBody", CompoundTag.CODEC, this.celestialBodyData.toTag());
        }
        // Search history
        CompoundTag histTag = new CompoundTag();
        histTag.putInt("size", Math.min(this.searchHistory.size(), MAX_HISTORY));
        for (int i = 0; i < Math.min(this.searchHistory.size(), MAX_HISTORY); i++) {
            histTag.put("h" + i, this.searchHistory.get(i).toTag());
        }
        output.store("searchHistory", CompoundTag.CODEC, histTag);
        // Anvil inventory
        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = this.anvilInventory.getItem(i);
            if (!stack.isEmpty()) {
                invTag.put("s" + i, ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
            }
        }
        output.store("anvils", CompoundTag.CODEC, invTag);
        // Material slot
        if (!this.materialFilter.isEmpty()) {
            output.store("materialFilter", ItemStack.OPTIONAL_CODEC, this.materialFilter);
        }
        output.putInt("materialLimit", this.materialLimit);
        output.putInt("ageAnvilCount", this.ageAnvilCount);
        if (this.planetaryResourceSet != null) {
            output.store("planetaryResources", CompoundTag.CODEC, this.planetaryResourceSet.toTag());
        }
        // Portals are persisted by WormholeStabilizerHandler via megastructureManager
        // Temple state
        output.putInt("templeCycleDay", this.templeCycleDay);
        output.putLong("templeLastDay", this.templeLastDay);
        if (!this.templeDemandItem.isEmpty()) {
            output.store("templeDemand", ItemStack.OPTIONAL_CODEC, this.templeDemandItem);
        }
        output.putInt("templeDemandCount", this.templeDemandCount);
        output.putInt("templeDemandProgress", this.templeDemandProgress);
        output.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        output.putInt("historyBrowseIndex", this.historyBrowseIndex);
        // Delegate megastructure NBT to manager
        this.megastructureManager.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.isAmplify = input.getBooleanOr("amplified", false);
        this.stellarMass = input.getIntOr("stellarMass", 0);
        this.locked = input.getBooleanOr("locked", false);
        this.amplifierPresent = input.getBooleanOr("amplifierPresent", false);
        this.searching = input.getBooleanOr("searching", false);
        this.searchTicksRemaining = input.getIntOr("searchTicks", 0);
        this.searchFailed = input.getBooleanOr("searchFailed", false);
        this.powerInsufficient = input.getBooleanOr("powerInsufficient", false);
        // If searching was true but no timer was saved (old data or newly placed),
        // reset the flag to prevent stuck searching state
        if (this.searching && this.searchTicksRemaining <= 0) {
            this.searching = false;
        }
        this.bodySeed = input.getLongOr("bodySeed", 0);
        // Capture old body data for animation transition detection
        CelestialBodyData oldBodyData = this.celestialBodyData;
        this.celestialBodyData = input.read("celestialBody", CompoundTag.CODEC)
            .map(CelestialBodyData::fromTag).orElse(null);
        // Detect transitions for animation (client-side only, e.g. singleplayer chunk load)
        // Skip animation during accelerator evolution or supernova flash
        boolean skipAnimLoad = this.getAcceleratorStage() >= 1 || this.getSupernovaFlashTicks() > 0;
        if (level != null && level.isClientSide() && !skipAnimLoad) {
            this.detectAnimationTransition(oldBodyData, this.celestialBodyData);
        }
        // Search history
        input.read("searchHistory", CompoundTag.CODEC).ifPresent(this::loadSearchHistory);
        // Inventory
        input.read("anvils", CompoundTag.CODEC).ifPresent(this::loadInventoryFromTag);
        // Material filter
        this.materialFilter = input.read("materialFilter", ItemStack.OPTIONAL_CODEC)
            .orElse(new ItemStack(Items.BARRIER));
        this.materialLimit = input.getIntOr("materialLimit", 0);
        this.ageAnvilCount = input.getIntOr("ageAnvilCount", 0);
        // Planetary resources
        this.planetaryResourceSet = input.read("planetaryResources", CompoundTag.CODEC)
            .map(PlanetaryResourceSet::fromTag).orElse(null);
        // Portals are loaded by WormholeStabilizerHandler via megastructureManager
        // Temple state
        this.templeCycleDay = input.getIntOr("templeCycleDay", 0);
        this.templeLastDay = input.getLongOr("templeLastDay", -1);
        this.templeDemandItem = input.read("templeDemand", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.templeDemandCount = input.getIntOr("templeDemandCount", 0);
        this.templeDemandProgress = input.getIntOr("templeDemandProgress", 0);
        this.templeDemandSatisfied = input.getBooleanOr("templeDemandSatisfied", false);
        // Collider runtime state is not persisted — always start clean on load
        this.historyBrowseIndex = input.getIntOr("historyBrowseIndex", 0);
        // Supernova flash sync (runtime path uses loadAdditional; guarded so disk loads are ignored).
        // Only restart if the incoming tick count is larger, to keep the client's smooth countdown.
        input.getInt("supernovaFlashTicks").ifPresent(incomingFlash -> {
            var accel = this.megastructureManager.getAcceleratorHandler();
            if (incomingFlash > accel.getSupernovaFlashTicks()) {
                accel.setSupernovaFlashTicks(incomingFlash);
            }
            this.supernovaCenterY = input.getDoubleOr("supernovaCenterY", 0);
            this.supernovaScale = input.getFloatOr("supernovaScale", 1.0f);
        });
        // Delegate megastructure NBT to manager (must be last so managers overwrite BE fields)
        this.megastructureManager.loadAdditional(input);
        if (level != null && !level.isClientSide()) {
            this.syncToClient();
        }
    }

    /**
     * Detect body transition and trigger animation on the client side.
     */
    private void detectAnimationTransition(@Nullable CelestialBodyData oldBody, @Nullable CelestialBodyData newBody) {
        if (level == null || !level.isClientSide()) return;
        boolean hadBody = oldBody != null;
        boolean hasBody = newBody != null;
        if (!hadBody && hasBody) {
            // Body appeared — start forward (grow-in) animation
            this.animationTicks = ANIMATION_DURATION_TICKS;
            this.animationForward = true;
            this.animationPreviousBodyData = null;
        } else if (hadBody && !hasBody) {
            // Body disappeared — start reverse (shrink-out) animation
            this.animationTicks = ANIMATION_DURATION_TICKS;
            this.animationForward = false;
            this.animationPreviousBodyData = oldBody;
        } else if (hadBody && !oldBody.toTag().equals(newBody.toTag())) {
            // Body changed to a different type — animate transition
            this.animationTicks = ANIMATION_DURATION_TICKS;
            this.animationForward = true;
            this.animationPreviousBodyData = oldBody;
        }
    }

    private void loadInventoryFromTag(CompoundTag invTag) {
        for (int i = 0; i < 5; i++) {
            String key = "s" + i;
            if (invTag.contains(key)) {
                ItemStack stack = ItemStack.CODEC.parse(NbtOps.INSTANCE, invTag.get(key))
                    .result().orElse(ItemStack.EMPTY);
                this.anvilInventory.setItem(i, stack);
            } else {
                this.anvilInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    // === Network sync (26.1: getUpdateTag returns CompoundTag, client receives via loadAdditional) ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putLong("bodySeed", this.bodySeed);
        tag.putInt("stellarMass", this.stellarMass);

        tag.putBoolean("locked", this.locked);
        tag.putBoolean("amplifierPresent", this.amplifierPresent);
        tag.putBoolean("searching", this.searching);
        tag.putInt("searchTicks", this.searchTicksRemaining);
        tag.putBoolean("searchFailed", this.searchFailed);
        tag.putBoolean("powerInsufficient", this.powerInsufficient);
        if (this.celestialBodyData != null) {
            tag.put("celestialBody", this.celestialBodyData.toTag());
        }
        // Search history
        CompoundTag histTag = new CompoundTag();
        histTag.putInt("size", Math.min(this.searchHistory.size(), MAX_HISTORY));
        for (int i = 0; i < Math.min(this.searchHistory.size(), MAX_HISTORY); i++) {
            histTag.put("h" + i, this.searchHistory.get(i).toTag());
        }
        tag.put("searchHistory", histTag);
        // Anvil inventory (sync all 5 slots for client display)
        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = this.anvilInventory.getItem(i);
            if (!stack.isEmpty()) {
                invTag.put("s" + i, ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).getOrThrow());
            }
        }
        tag.put("anvils", invTag);
        // Material filter sync
        if (!this.materialFilter.isEmpty()) {
            tag.put("materialFilter", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.materialFilter).getOrThrow());
        }
        tag.putInt("materialLimit", this.materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (this.planetaryResourceSet != null) {
            tag.put("planetaryResources", this.planetaryResourceSet.toTag());
        }
        // Portals are synced by WormholeStabilizerHandler via megastructureManager
        // Temple state (client sync)
        tag.putInt("templeCycleDay", this.templeCycleDay);
        tag.putLong("templeLastDay", this.templeLastDay);
        if (!this.templeDemandItem.isEmpty()) {
            tag.put("templeDemand", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.templeDemandItem).getOrThrow());
        }
        tag.putInt("templeDemandCount", this.templeDemandCount);
        tag.putInt("templeDemandProgress", this.templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        // Collider runtime state not synced to client
        tag.putInt("historyBrowseIndex", this.historyBrowseIndex);
        // Supernova flash (synced for rendering)
        tag.putInt("supernovaFlashTicks", this.getSupernovaFlashTicks());
        tag.putDouble("supernovaCenterY", this.supernovaCenterY);
        tag.putFloat("supernovaScale", this.supernovaScale);
        // Delegate megastructure NBT to manager
        this.megastructureManager.writeUpdateTag(tag, registries);
        return tag;
    }

    public int getAnvilCount(int slot) {
        return this.anvilInventory.getItem(slot).getCount();
    }

    public void addToSearchHistory(CelestialBodyData data, @Nullable PlanetaryResourceSet resources) {
        // Dedup: don't add if it's already the most recent entry
        if (!this.searchHistory.isEmpty()) {
            SearchHistoryEntry latest = this.searchHistory.getFirst();
            if (latest.body().toTag().toString().equals(data.toTag().toString())) return;
        }
        this.searchHistory.addFirst(new SearchHistoryEntry(data, resources));
        while (this.searchHistory.size() > MAX_HISTORY) {
            this.searchHistory.removeLast();
        }
    }

    private void loadSearchHistory(CompoundTag tag) {
        this.searchHistory.clear();
        int size = Math.min(tag.getIntOr("size", 0), MAX_HISTORY);
        for (int i = 0; i < size; i++) {
            if (tag.contains("h" + i)) {
                CompoundTag entryTag = tag.getCompoundOrEmpty("h" + i);
                if (entryTag.contains("body")) {
                    // New format: SearchHistoryEntry
                    this.searchHistory.add(SearchHistoryEntry.fromTag(entryTag));
                } else {
                    // Old format: bare CelestialBodyData (no resources saved)
                    CelestialBodyData body = CelestialBodyData.fromTag(entryTag);
                    this.searchHistory.add(new SearchHistoryEntry(body, null));
                }
            }
        }
    }

    // === History browsing (server-side) ===

    public boolean hasPreviousHistory() {
        int sz = this.searchHistory.size();
        return sz > 1 && this.historyBrowseIndex < sz;
    }

    public boolean hasNextHistory() {
        return this.historyBrowseIndex > 0;
    }

    public void browseHistoryPrev() {
        if (level == null || level.isClientSide()) return;
        int sz = this.searchHistory.size();
        // Need at least 2 entries: index 0 is the current locked body
        if (sz <= 1 || this.historyBrowseIndex >= sz) return;
        if (this.historyBrowseIndex == 0) {
            this.historyOriginalEntry = new SearchHistoryEntry(this.celestialBodyData, this.planetaryResourceSet);
            this.historyBrowseIndex = 1; // skip the current-body entry
        }
        this.historyBrowseIndex++;
        if (this.historyBrowseIndex > sz) return;
        this.applyHistoryEntry();
    }

    public void browseHistoryNext() {
        if (level == null || level.isClientSide()) return;
        if (this.historyBrowseIndex <= 0) return;
        this.historyBrowseIndex--;
        if (this.historyBrowseIndex == 0) {
            if (this.historyOriginalEntry != null) {
                this.celestialBodyData = this.historyOriginalEntry.body();
                this.planetaryResourceSet = this.historyOriginalEntry.resources();
                this.historyOriginalEntry = null;
            }
            setChanged();
            this.syncToClient();
        } else {
            this.applyHistoryEntry();
        }
    }

    private void applyHistoryEntry() {
        if (this.historyBrowseIndex > 0 && this.historyBrowseIndex <= this.searchHistory.size()) {
            SearchHistoryEntry entry = this.searchHistory.get(this.historyBrowseIndex - 1);
            this.celestialBodyData = entry.body();
            this.planetaryResourceSet = entry.resources();
        }
        setChanged();
        this.syncToClient();
    }

    public void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = this.getUpdatePacket();
            for (ServerPlayer serverPlayer : serverLevel.getChunkSource().chunkMap.getPlayers(
                serverLevel.getChunkAt(worldPosition)
                    .getPos(), false
            )) {
                serverPlayer.connection.send(packet);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.celestial_forging_anvil");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.level == null || player.isSpectator()) return null;
        return new CelestialForgingAnvilMenu(ModMenuTypes.CELESTIAL_FORGING_ANVIL.get(), containerId, inventory, this);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === Megastructure ===

    /**
     * Toggle the lock state. Called from the server when the player clicks the lock button.
     */
    public void toggleLocked() {
        if (level == null || level.isClientSide()) return;
        if (this.isAcceleratorActive()) {
            // Cannot unlock during stellar evolution
            return;
        }
        this.locked = !this.locked;
        if (!this.locked) {
            // Unlocking: clear megastructure and accelerator to revert to restriction ring
            this.clearMegastructure();
            this.clearAcceleratorState();
        }
        this.setChanged();
        this.syncToClient();
    }

    private void clearAcceleratorState() {
        this.megastructureManager.getAcceleratorHandler().onClear(this);
    }

    /**
     * Clear the active megastructure and all related state, reverting to the restriction ring.
     */
    private void clearMegastructure() {
        this.megastructureManager.clearMegastructure(this);
        // Clear material filter (still owned by BE)
        this.materialFilter = new ItemStack(Items.BARRIER);
        this.materialLimit = 0;
        // Re-register with power grid to restore CONSUMER type
        PowerGrid.addComponent(this);
    }

    /**
     * Get the option list matching what the client sees (applies the same filtering).
     * When a megastructure is already built, only the accelerator is visible.
     */
    public List<CelestialRefactorOption> getClientVisibleOptions() {
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            this.celestialBodyData,
            this.isAmplify,
            this.planetaryResourceSet
        );
        if (this.megastructureManager.getActiveIndex() >= 0) {
            options = options.stream().filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())).toList();
        }
        return options;
    }

    /**
     * Get the currently active megastructure option, or null if none is built.
     */
    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() {
        return this.megastructureManager.getActiveOption(this);
    }

    /**
     * Get the portals placed on this CFA's sides (unmodifiable).
     */
    public Map<Cube323PartHalf, BlockPos> getPortals() {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        return wh.getPortals();
    }

    /**
     * Attempt to build a megastructure. Called from the server when the player clicks "Start Refactoring".
     *
     * @param optionIndex the selected refactor option index
     */
    public void buildMegastructure(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (this.celestialBodyData == null) return;
        List<CelestialRefactorOption> options = this.getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        CelestialRefactorOption option = options.get(optionIndex);

        // Check materials first
        if (option.needsMaterial()) {
            ItemStack contained = this.materialContainer.getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(contained, required) || contained.getCount() < required.getCount()) {
                return;
            }
            contained.shrink(required.getCount());
        }

        // Delegate to megastructure manager
        this.megastructureManager.buildMegastructure(optionIndex, this);

        // Re-register with power grid so the component type change takes effect
        PowerGrid.addComponent(this);
        this.setChanged();
        this.syncToClient();
    }

    // === Wormhole interface scanning ===

    /**
     * Get all laser interfaces mapped by relative offset from this CFA's controller.
     */
    public Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> getLaserInterfacesMap() {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilLaserInterfaceBlockEntity.class, level, worldPosition);
    }

    /**
     * Get all logistics interfaces mapped by relative offset from this CFA's controller.
     */
    public Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> getLogisticsInterfacesMap() {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilLogisticsInterfaceBlockEntity.class, level, worldPosition);
    }

    /**
     * Get all fluid interfaces mapped by relative offset from this CFA's controller.
     */
    public Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> getFluidInterfacesMap() {
        return CfaInterfaceScanner.getInterfacesMap(
            CelestialForgingAnvilFluidInterfaceBlockEntity.class, level, worldPosition);
    }

    // === Wormhole content syncing ===

    /**
     * Called immediately when a player inserts/removes items in a logistics interface.
     * Delegates to the wormhole stabilizer handler to push the change to canonical
     * state and to all connected CFAs in the same tick.
     */
    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot) {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        wh.syncLogisticsOnChange(interfacePos, changedSlot, this);
    }

    /**
     * Register a portal on a specific side of the CFA.
     *
     * @return true if successful, false if side already has a portal or invalid side
     */
    public boolean addPortal(Cube323PartHalf side, BlockPos portalPos) {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        return wh.addPortal(side, portalPos, this);
    }

    /**
     * Unregister a portal from a specific side.
     */
    public void removePortal(Cube323PartHalf side) {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        wh.removePortal(side, this);
    }
}
