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
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
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
import java.util.EnumMap;
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
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
    @Setter
    private boolean isAmplify = false;

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
        return megastructureManager.getActiveIndex();
    }

    /**
     * Whether the excavator has valid laser input (for model switching).
     * Delegates to ExcavatorHandler.
     */
    public boolean isExcavatorLaserActive() {
        // TODO Phase 9: implement excavator laser state tracking
        return false;
    }

    /**
     * Whether the Penrose Sphere has valid laser input/output pairs (for model switching).
     * Delegates to PenroseSphereHandler.
     */
    public boolean isPenroseSphereLaserActive() {
        // TODO Phase 9: implement penrose sphere laser state tracking
        return false;
    }

    // === Wormhole Stabilizer state ===
    /**
     * Hash of the black hole parameters, computed when the stabilizer is built.
     */
    @Nullable
    public UUID getWormholeParamsHash() {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
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
        return megastructureManager.getAcceleratorHandler().getStage();
    }

    public int getAcceleratorTicksRemaining() {
        return megastructureManager.getAcceleratorHandler().getTicksRemaining();
    }

    public int getAcceleratorTicksTotal() {
        return megastructureManager.getAcceleratorHandler().getTicksTotal();
    }

    public int getSupernovaFlashTicks() {
        return megastructureManager.getAcceleratorHandler().getSupernovaFlashTicks();
    }

    public int getCollapseAnimTicks() {
        return megastructureManager.getAcceleratorHandler().getCollapseAnimTicks();
    }

    /**
     * Whether the stellar evolution accelerator is active (any stage 1-4).
     */
    public boolean isAcceleratorActive() {
        return megastructureManager.getAcceleratorHandler().isActive();
    }

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === IPowerConsumer ===

    @Override
    public int getInputPower() {
        if (searching && searchTicksRemaining > 0) {
            return isAmplify ? 4000 : 1000;
        }
        return megastructureManager.getInputPower(this);
    }

    @Override
    public int getOutputPower() {
        return megastructureManager.getOutputPower(this);
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
        return megastructureManager.getComponentType(this);
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        PowerComponentType type = getComponentType();
        return new PowerComponentInfo(
            getPos(),
            getInputPower(),
            getOutputPower(),
            0, 0,
            getRange(),
            getShape(),
            type
        );
    }

    @Override
    public void gridTick() {
        megastructureManager.gridTick(this);
    }

    private boolean hasEnoughPower() {
        if (grid == null) return false;
        int required = getInputPower();
        return required <= 0 || grid.isWorking();
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
        if (celestialBodyData != null) return celestialBodyData;
        if (animationTicks > 0 && !animationForward && animationPreviousBodyData != null) {
            return animationPreviousBodyData;
        }
        return null;
    }

    /**
     * Get animation progress from 0 (hidden) to 1 (fully visible).
     * Uses ease-in-out cubic interpolation.
     */
    public float getAnimationProgress(float partialTick) {
        if (animationTicks <= 0) return animationForward ? 1.0f : 0.0f;
        float t = (ANIMATION_DURATION_TICKS - animationTicks + partialTick) / (float) ANIMATION_DURATION_TICKS;
        float eased = easeInOutCubic(t);
        return animationForward ? eased : (1.0f - eased);
    }

    /**
     * Get rotation speed multiplier during animation.
     * Starts fast (5x) and decays to 1x as animation progresses.
     */
    public float getAnimationRotationBoost(float partialTick) {
        float progress = getAnimationProgress(partialTick);
        return 1.0f + 4.0f * (1.0f - progress);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3) / 2.0f;
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
        if (celestialBodyData == null) return;
        List<CelestialRefactorOption> options = getClientVisibleOptions();
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
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
    @Setter
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
                    getAnvilCount(0),
                    getAnvilCount(1),
                    getAnvilCount(2),
                    getAnvilCount(3),
                    this.isAmplify,
                    level.getRandom()
                );
                if (preCheck == null) {
                    this.searchFailed = true;
                    this.searching = false;
                    this.searchTicksRemaining = 0;
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    return;
                }
            }
        }

        // Check power availability
        if (!hasEnoughPower()) {
            this.powerInsufficient = true;
            this.searching = false;
            this.searchTicksRemaining = 0;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void serverTick() {
        // Continuous power state refresh — clears stale powerInsufficient when grid recovers
        boolean hasEnoughPower = hasEnoughPower();
        if (!hasEnoughPower && !this.powerInsufficient) {
            this.powerInsufficient = true;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else if (hasEnoughPower && this.powerInsufficient) {
            this.powerInsufficient = false;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (searchTicksRemaining > 0) {
            // Check if power is still sufficient during search
            if (!hasEnoughPower) {
                this.searching = false;
                this.searchTicksRemaining = 0;
                this.powerInsufficient = true;
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            } else {
                searchTicksRemaining--;
                if (searchTicksRemaining == 0) {
                    this.searching = false;
                    tryMatchCelestialBody();
                    if (celestialBodyData == null) {
                        this.searchFailed = true;
                    }
                    setChanged();
                    if (level != null) {
                        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    }
                }
            }
        }

        // Manage stellar gravity source
        updateGravitySource();

        // Destroy entities at the gravity center
        if (gravitySourceActive && level != null) {
            destroyEntitiesAtCenter();
        }

        // Megastructure logic (delegated to handler classes)
        megastructureManager.serverTick(this);

        // Supernova flash timer
        var accel = megastructureManager.getAcceleratorHandler();
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

        boolean shouldHaveGravity = amplifierPresent
            && celestialBodyData instanceof StarData
            && stellarMass > 0
            && celestialBodyData.size() > 0;

        double newStrength = 0;
        if (shouldHaveGravity) {
            double massRatio = Math.pow(2, (stellarMass - 12) / 2.0);
            newStrength = massRatio * GRAVITY_STRENGTH_MULTIPLIER
                / (GRAVITY_REFERENCE_RADIUS_RATIO * GRAVITY_REFERENCE_RADIUS_RATIO);
        }
        int newSize = shouldHaveGravity ? celestialBodyData.size() : 0;

        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);

        if (shouldHaveGravity) {
            if (!gravitySourceActive || newStrength != currentGravityStrength || newSize != currentGravitySize) {
                // Remove old source if strength/size changed
                if (gravitySourceActive) {
                    GravityManager.GravitySourceManager.removeSource(level, centerPos);
                }
                // Add new/updated source
                GravityManager.GravitySourceType type = new GravityManager.GravitySourceType(newStrength, GRAVITY_RADIUS);
                GravityManager.GravitySourceManager.addSource(level, centerPos, type);
                gravitySourceActive = true;
                currentGravityStrength = newStrength;
                currentGravitySize = newSize;
            }
        } else if (gravitySourceActive) {
            GravityManager.GravitySourceManager.removeSource(level, centerPos);
            gravitySourceActive = false;
            currentGravityStrength = 0;
            currentGravitySize = 0;
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
        gravitySourceActive = false;
        currentGravityStrength = 0;
        currentGravitySize = 0;
    }

    private void destroyEntitiesAtCenter() {
        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
        AABB centerBox = new AABB(centerPos);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, centerBox);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                if (celestialBodyData instanceof StarData star
                    && star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                    living.hurt(ModDamageTypes.lostInTime(level), Float.MAX_VALUE);
                } else {
                    living.hurt(level.damageSources().inFire(), 1.0E12f);
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
            tag.put("body", body.toTag());
            if (resources != null) {
                tag.put("resources", resources.toTag());
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
        if (this.rotation == 360) this.rotation = 0;
        this.preRotation = this.rotation;
        this.rotation += 3;
        this.bodyRotation += 1;

        // Animation tick (client-side only)
        if (animationTicks > 0) {
            animationTicks--;
            if (animationTicks == 0 && !animationForward) {
                animationPreviousBodyData = null;
            }
        }
        // Supernova flash countdown (client-side, for rendering)
        var accel = megastructureManager.getAcceleratorHandler();
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
                if (celestialBodyData instanceof StarData) {
                    if (!amplify) {
                        this.locked = true; // Lock when amplifier removed with stellar body
                    }
                }
            }
            this.setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && !PowerGrid.isServerClosing) {
            if (gravitySourceActive) {
                BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
                GravityManager.GravitySourceManager.removeSource(level, centerPos);
                gravitySourceActive = false;
            }
            // Unregister wormhole and clear megastructures so connected portals close.
            // Skip during server shutdown to avoid accessing saved data during save.
            megastructureManager.clearAllMegastructures(this);
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
        if (bodySeed == 0) return 0f;
        net.minecraft.util.RandomSource rand = net.minecraft.util.RandomSource.create(bodySeed + index * 7919L);
        return (rand.nextFloat() - 0.5f) * 0.1f;
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void tryMatchCelestialBody() {
        if (level == null) return;
        int time = getAnvilCount(0);
        int space = getAnvilCount(1);
        int mass = getAnvilCount(2);
        int energy = getAnvilCount(3);
        this.ageAnvilCount = time;
        this.bodySeed = level.getRandom().nextLong();
        this.stellarMass = mass;

        // Verify seed item is still present — if player removed it during the search,
        // clear captured data so we fall through to normal matching instead of granting
        // a special planet without deducting the seed item.
        if (lastConsumedSeedItem != null || lastConsumedSeedNbt != null) {
            ItemStack seedStack = this.anvilInventory.getItem(4);
            if (seedStack.isEmpty()) {
                this.lastConsumedSeedItem = null;
                this.lastConsumedSeedNbt = null;
            }
        }

        // First: check for seed item snapshot (disk / singularity crystal)
        if (lastConsumedSeedNbt != null && lastConsumedSeedNbt.contains("celestialBody")) {
            applySnapshot(lastConsumedSeedNbt);
            consumeSeedItem();
            return;
        }

        // Second: check for special celestial body discovery via seed item
        if (lastConsumedSeedItem != null) {
            SpecialCelestialBodyData specialBody = tryMatchSpecialCelestialBody(
                time,
                space,
                mass,
                energy,
                lastConsumedSeedItem,
                ((ServerLevel) level).getSeed()
            );
            if (specialBody != null) {
                this.celestialBodyData = specialBody;
                if (!level.isClientSide()) {
                    Identifier recipeId = Identifier.parse(specialBody.recipeId());
                    ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key =
                        ResourceKey.create(Registries.RECIPE, recipeId);
                    net.minecraft.world.item.crafting.RecipeHolder<?> holder =
                        RecipesRecord.getRecipes((ServerLevel) level).byKey(key);
                    if (holder != null && holder.value() instanceof SpecialCelestialBodyRecipe recipe) {
                        this.planetaryResourceSet = recipe.generateResources();
                    }
                }
                addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
                consumeSeedItem();
                if (!level.isClientSide()) {
                    this.setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
            addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        } else {
            this.planetaryResourceSet = null;
            this.searchTicksRemaining = 0; // Stop timer on failure
        }
        consumeSeedItem();

        if (!level.isClientSide()) {
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        if (!level.isClientSide()) {
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // === IDiskCloneable ===

    @Override
    public void storeDiskData(ValueOutput output) {
        if (celestialBodyData != null) {
            output.store("celestialBody", CompoundTag.CODEC, celestialBodyData.toTag());
            output.putLong("bodySeed", this.bodySeed);
            output.putInt("ageAnvilCount", this.ageAnvilCount);
            output.putInt("stellarMass", this.stellarMass);
            output.putIntArray(
                "anvilCounts", new int[]{
                    getAnvilCount(0),
                    getAnvilCount(1),
                    getAnvilCount(2),
                    getAnvilCount(3)
                }
            );
            output.putBoolean("isAmplify", this.isAmplify);
            if (planetaryResourceSet != null) {
                output.store("planetaryResources", CompoundTag.CODEC, planetaryResourceSet.toTag());
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
                if (celestialBodyData instanceof StarData star && star.bodyClass().isExtreme()) {
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
            WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
            if (megastructureManager.getActiveIndex() >= 0 && getActiveMegastructureOption() != null
                && "wormhole_stabilizer".equals(getActiveMegastructureOption().megastructure())) {
                wh.onBuild(this);
            }
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        if (celestialBodyData != null) {
            output.store("celestialBody", CompoundTag.CODEC, celestialBodyData.toTag());
        }
        // Search history
        CompoundTag histTag = new CompoundTag();
        histTag.putInt("size", Math.min(searchHistory.size(), MAX_HISTORY));
        for (int i = 0; i < Math.min(searchHistory.size(), MAX_HISTORY); i++) {
            histTag.put("h" + i, searchHistory.get(i).toTag());
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
        if (!materialFilter.isEmpty()) {
            output.store("materialFilter", ItemStack.OPTIONAL_CODEC, materialFilter);
        }
        output.putInt("materialLimit", materialLimit);
        output.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            output.store("planetaryResources", CompoundTag.CODEC, planetaryResourceSet.toTag());
        }
        // Portals are persisted by WormholeStabilizerHandler via megastructureManager
        // Temple state
        output.putInt("templeCycleDay", templeCycleDay);
        output.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            output.store("templeDemand", ItemStack.OPTIONAL_CODEC, templeDemandItem);
        }
        output.putInt("templeDemandCount", templeDemandCount);
        output.putInt("templeDemandProgress", templeDemandProgress);
        output.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        output.putInt("historyBrowseIndex", historyBrowseIndex);
        // Delegate megastructure NBT to manager
        megastructureManager.saveAdditional(output);
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
        boolean skipAnimLoad = getAcceleratorStage() >= 1 || getSupernovaFlashTicks() > 0;
        if (level != null && level.isClientSide() && !skipAnimLoad) {
            detectAnimationTransition(oldBodyData, this.celestialBodyData);
        }
        // Search history
        input.read("searchHistory", CompoundTag.CODEC).ifPresent(this::loadSearchHistory);
        // Inventory
        input.read("anvils", CompoundTag.CODEC).ifPresent(invTag -> loadInventoryFromTag(invTag));
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
        // Delegate megastructure NBT to manager (must be last so managers overwrite BE fields)
        megastructureManager.loadAdditional(input);
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
        // Search history
        CompoundTag histTag = new CompoundTag();
        histTag.putInt("size", Math.min(searchHistory.size(), MAX_HISTORY));
        for (int i = 0; i < Math.min(searchHistory.size(), MAX_HISTORY); i++) {
            histTag.put("h" + i, searchHistory.get(i).toTag());
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
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, materialFilter).getOrThrow());
        }
        tag.putInt("materialLimit", materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            tag.put("planetaryResources", planetaryResourceSet.toTag());
        }
        // Portals are synced by WormholeStabilizerHandler via megastructureManager
        // Temple state (client sync)
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, templeDemandItem).getOrThrow());
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putInt("templeDemandProgress", templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        // Collider runtime state not synced to client
        tag.putInt("historyBrowseIndex", historyBrowseIndex);
        // Delegate megastructure NBT to manager
        megastructureManager.writeUpdateTag(tag, registries);
        return tag;
    }

    public int getAnvilCount(int slot) {
        return this.anvilInventory.getItem(slot).getCount();
    }

    public void addToSearchHistory(CelestialBodyData data, @Nullable PlanetaryResourceSet resources) {
        // Dedup: don't add if it's already the most recent entry
        if (!searchHistory.isEmpty()) {
            SearchHistoryEntry latest = searchHistory.getFirst();
            if (latest.body().toTag().toString().equals(data.toTag().toString())) return;
        }
        searchHistory.addFirst(new SearchHistoryEntry(data, resources));
        while (searchHistory.size() > MAX_HISTORY) {
            searchHistory.removeLast();
        }
    }

    private void loadSearchHistory(CompoundTag tag) {
        searchHistory.clear();
        int size = Math.min(tag.getIntOr("size", 0), MAX_HISTORY);
        for (int i = 0; i < size; i++) {
            if (tag.contains("h" + i)) {
                CompoundTag entryTag = tag.getCompoundOrEmpty("h" + i);
                if (entryTag.contains("body")) {
                    // New format: SearchHistoryEntry
                    searchHistory.add(SearchHistoryEntry.fromTag(entryTag));
                } else {
                    // Old format: bare CelestialBodyData (no resources saved)
                    CelestialBodyData body = CelestialBodyData.fromTag(entryTag);
                    searchHistory.add(new SearchHistoryEntry(body, null));
                }
            }
        }
    }

    // === History browsing (server-side) ===

    public boolean hasPreviousHistory() {
        int sz = searchHistory.size();
        return sz > 1 && historyBrowseIndex < sz;
    }

    public boolean hasNextHistory() {
        return historyBrowseIndex > 0;
    }

    public void browseHistoryPrev() {
        if (level == null || level.isClientSide()) return;
        int sz = searchHistory.size();
        // Need at least 2 entries: index 0 is the current locked body
        if (sz <= 1 || historyBrowseIndex >= sz) return;
        if (historyBrowseIndex == 0) {
            historyOriginalEntry = new SearchHistoryEntry(celestialBodyData, planetaryResourceSet);
            historyBrowseIndex = 1; // skip the current-body entry
        }
        historyBrowseIndex++;
        if (historyBrowseIndex > sz) return;
        applyHistoryEntry();
    }

    public void browseHistoryNext() {
        if (level == null || level.isClientSide()) return;
        if (historyBrowseIndex <= 0) return;
        historyBrowseIndex--;
        if (historyBrowseIndex == 0) {
            if (historyOriginalEntry != null) {
                celestialBodyData = historyOriginalEntry.body();
                planetaryResourceSet = historyOriginalEntry.resources();
                historyOriginalEntry = null;
            }
            setChanged();
            syncToClient();
        } else {
            applyHistoryEntry();
        }
    }

    private void applyHistoryEntry() {
        if (historyBrowseIndex > 0 && historyBrowseIndex <= searchHistory.size()) {
            SearchHistoryEntry entry = searchHistory.get(historyBrowseIndex - 1);
            celestialBodyData = entry.body();
            planetaryResourceSet = entry.resources();
        }
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
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
        // TODO Phase 8: replace with actual CelestialForgingAnvilMenu when ported
        return null;
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
        if (isAcceleratorActive()) {
            // Cannot unlock during stellar evolution
            return;
        }
        this.locked = !this.locked;
        if (!this.locked) {
            // Unlocking: clear megastructure and accelerator to revert to restriction ring
            clearMegastructure();
            clearAcceleratorState();
        }
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private void clearAcceleratorState() {
        megastructureManager.getAcceleratorHandler().onClear(this);
    }

    /**
     * Clear the active megastructure and all related state, reverting to the restriction ring.
     */
    private void clearMegastructure() {
        megastructureManager.clearMegastructure(this);
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
            celestialBodyData,
            isAmplify,
            this.planetaryResourceSet
        );
        if (megastructureManager.getActiveIndex() >= 0) {
            options = options.stream().filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())).toList();
        }
        return options;
    }

    /**
     * Get the currently active megastructure option, or null if none is built.
     */
    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() {
        return megastructureManager.getActiveOption(this);
    }

    /**
     * Get the portals placed on this CFA's sides (unmodifiable).
     */
    public Map<Cube323PartHalf, BlockPos> getPortals() {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        return wh.getPortals();
    }

    /**
     * Attempt to build a megastructure. Called from the server when the player clicks "Start Refactoring".
     *
     * @param optionIndex the selected refactor option index
     */
    public void buildMegastructure(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (celestialBodyData == null) return;
        List<CelestialRefactorOption> options = getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        CelestialRefactorOption option = options.get(optionIndex);

        // Check materials first
        if (option.needsMaterial()) {
            ItemStack contained = materialContainer.getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(contained, required) || contained.getCount() < required.getCount()) {
                return;
            }
            contained.shrink(required.getCount());
        }

        // Delegate to megastructure manager
        megastructureManager.buildMegastructure(optionIndex, this);

        // Re-register with power grid so the component type change takes effect
        PowerGrid.addComponent(this);
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
     * Delegates to WormholeStabilizerHandler.
     * TODO Phase 7: Implement wormhole content syncing across connected CFAs
     */
    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot) {
        // Phase 7: WormholeNetwork content sync
    }

    /**
     * Register a portal on a specific side of the CFA.
     *
     * @return true if successful, false if side already has a portal or invalid side
     */
    public boolean addPortal(Cube323PartHalf side, BlockPos portalPos) {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        return wh.addPortal(side, portalPos, this);
    }

    /**
     * Unregister a portal from a specific side.
     */
    public void removePortal(Cube323PartHalf side) {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        wh.removePortal(side, this);
    }

}
