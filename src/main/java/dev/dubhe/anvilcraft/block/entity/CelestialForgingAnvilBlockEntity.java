package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
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
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.util.GravityManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CelestialForgingAnvilBlockEntity extends BlockEntity implements MenuProvider, IPowerConsumer, IPowerProducer, IDiskCloneable {

    // === Megastructure delegation ===
    @Getter
    private final CfaMegastructureManager megastructureManager = new CfaMegastructureManager();

    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
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
        ExcavatorHandler h = megastructureManager.findHandler(ExcavatorHandler.class);
        return h != null && h.isLaserActive();
    }

    /**
     * Whether the Penrose Sphere has valid laser input/output pairs (for model switching).
     * Delegates to PenroseSphereHandler.
     */
    public boolean isPenroseSphereLaserActive() {
        PenroseSphereHandler h = megastructureManager.findHandler(PenroseSphereHandler.class);
        return h != null && h.isLaserActive();
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
     * Whether this CFA is currently registered in the wormhole network.
     * Map from cube part (side center) to the BlockPos of the portal placed there.
     */

    private final Map<Cube323PartHalf, BlockPos> portals = new EnumMap<>(Cube323PartHalf.class);
    /**
     * Tracked chunk-loaded connected CFAs, keyed by dimension + position.
     */
    private final Map<WormholeChunkLoadKey, LoadChuckData> wormholeLoadedChunks = new HashMap<>();

    private record WormholeChunkLoadKey(ResourceLocation dimension, BlockPos pos) {}
    // Wormhole canonical interface state is now stored globally in
    // WormholeInterfaceStates (BetterSavedData), shared across the entire network group.

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
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        return megastructureManager.getComponentType(this);
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
    private ItemStack materialFilter = new ItemStack(net.minecraft.world.item.Items.BARRIER);
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
            setMaterialFilter(new ItemStack(net.minecraft.world.item.Items.BARRIER));
            setMaterialLimit(0);
        } else {
            CelestialRefactorOption opt = options.get(optionIndex);
            if (opt.needsMaterial()) {
                setMaterialFilter(opt.material().copy());
                setMaterialLimit(opt.materialCount());
            } else {
                setMaterialFilter(new ItemStack(net.minecraft.world.item.Items.BARRIER));
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
    @javax.annotation.Nullable
    private Item lastConsumedSeedItem = null;
    @javax.annotation.Nullable
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
            newStrength = massRatio * GRAVITY_STRENGTH_MULTIPLIER / (GRAVITY_REFERENCE_RADIUS_RATIO * GRAVITY_REFERENCE_RADIUS_RATIO);
        } // 引力乘上这个常数得到感官合适的值 ↑
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
            CelestialBodyData body = CelestialBodyData.fromTag(tag.getCompound("body"));
            PlanetaryResourceSet resources = null;
            if (tag.contains("resources")) {
                resources = PlanetaryResourceSet.fromTag(tag.getCompound("resources"));
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
        // Update star color locally during collapse so the renderer picks up the
        // red→blue transition even when no server sync has arrived yet this frame.
        // (delegated to AcceleratorHandler; client visuals update via server sync)
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
                    ResourceLocation recipeId = ResourceLocation.parse(specialBody.recipeId());
                    level.getRecipeManager().byKey(recipeId).ifPresent(holder -> {
                        if (holder.value() instanceof SpecialCelestialBodyRecipe recipe) {
                            this.planetaryResourceSet = recipe.generateResources();
                        }
                    });
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
     *
     */
    private void consumeSeedItem() {
        if (level == null || level.isClientSide()) return;
        ItemStack seed = this.anvilInventory.getItem(4);
        if (!seed.isEmpty()) {
            this.anvilInventory.setItem(4, ItemStack.EMPTY);
        }
    }

    @javax.annotation.Nullable
    private SpecialCelestialBodyData tryMatchSpecialCelestialBody(
        int time,
        int space,
        int mass,
        int energy,
        Item consumedSeedItem,
        long worldSeed
    ) {
        if (level == null) return null;
        List<SpecialCelestialBodyRecipe> recipes = level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get())
            .stream().map(RecipeHolder::value).toList();
        for (SpecialCelestialBodyRecipe recipe : recipes) {
            if (recipe.time() == time && recipe.space() == space
                && recipe.mass() == mass && recipe.energy() == energy
                && recipe.isEffectiveSeedItem(consumedSeedItem, worldSeed)
            ) {
                // Find the recipe holder to get the full ID
                return level.getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.SPECIAL_CELESTIAL_BODY_TYPE.get())
                    .stream()
                    .filter(h -> h.value() == recipe)
                    .findFirst()
                    .map(h -> SpecialCelestialBodyData.fromRecipe(recipe, h.id().toString()))
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
        this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        this.bodySeed = tag.getLong("bodySeed");
        this.ageAnvilCount = tag.getInt("ageAnvilCount");
        this.stellarMass = tag.getInt("stellarMass");
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"));
        }
        addToSearchHistory(this.celestialBodyData, this.planetaryResourceSet);
        if (!level.isClientSide()) {
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // === IDiskCloneable ===

    @Override
    public void storeDiskData(CompoundTag tag) {
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
            tag.putLong("bodySeed", this.bodySeed);
            tag.putInt("ageAnvilCount", this.ageAnvilCount);
            tag.putInt("stellarMass", this.stellarMass);
            tag.putIntArray(
                "anvilCounts", new int[]{
                    getAnvilCount(0),
                    getAnvilCount(1),
                    getAnvilCount(2),
                    getAnvilCount(3)
                }
            );
            tag.putBoolean("isAmplify", this.isAmplify);
            if (planetaryResourceSet != null) {
                tag.put("planetaryResources", planetaryResourceSet.toTag());
            }
        }
    }

    @Override
    public void applyDiskData(CompoundTag tag) {
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
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.disk.extreme_body_requires_crystal")
                            .withStyle(ChatFormatting.RED),
                        true
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
    @javax.annotation.Nullable
    public static CompoundTag extractSnapshot(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            return DiskItem.getData(stack).copy();
        }
        return loadSnapshotFromStack(stack);
    }

    /**
     * Load a celestial snapshot from a disk or singularity crystal.
     */
    @javax.annotation.Nullable
    public static CompoundTag loadSnapshotFromStack(ItemStack stack) {
        // Disk
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            CompoundTag data = DiskItem.getData(stack);
            if (data.contains("celestialBody")) return data.copy();
        }
        // Singularity crystal
        if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            var customData = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
            );
            CompoundTag tag = customData.copyTag();
            if (!tag.isEmpty() && tag.contains("celestialSnapshot")) {
                CompoundTag snapshot = tag.getCompound("celestialSnapshot");
                if (snapshot.contains("celestialBody")) return snapshot.copy();
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
                CompoundTag bodyTag = snapshot.getCompound("celestialBody");
                String bodyClass = bodyTag.getString("bodyClass");
                if ("BLACK_HOLE".equals(bodyClass) || "NEUTRON_STAR".equals(bodyClass)) {
                    return; // silently reject — extreme bodies require singularity crystal
                }
            }
            CompoundTag diskTag = DiskItem.createData(stack);
            diskTag.merge(snapshot);
        } else if (stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem())) {
            var oldCustom = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY
            );
            CompoundTag updated = oldCustom.copyTag();
            updated.put("celestialSnapshot", snapshot.copy());
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(updated));
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
            if (megastructureManager.getActiveIndex() >= 0 && getActiveMegastructureOption() != null && "wormhole_stabilizer".equals(
                getActiveMegastructureOption().megastructure())) {
                wh.onBuild(this);
            }
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
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
        // Anvil inventory
        CompoundTag invTag = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            ItemStack stack = this.anvilInventory.getItem(i);
            if (!stack.isEmpty()) {
                invTag.put("s" + i, stack.save(registries));
            }
        }
        tag.put("anvils", invTag);
        // Material slot
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        tag.putInt("materialLimit", materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            tag.put("planetaryResources", planetaryResourceSet.toTag());
        }
        // Wormhole stabilizer state (handled by WormholeStabilizerHandler NBT)
        if (!portals.isEmpty()) {
            CompoundTag portalTag = new CompoundTag();
            for (Map.Entry<Cube323PartHalf, BlockPos> entry : portals.entrySet()) {
                BlockPos p = entry.getValue();
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", p.getX());
                posTag.putInt("y", p.getY());
                posTag.putInt("z", p.getZ());
                portalTag.put(entry.getKey().getSerializedName(), posTag);
            }
            tag.put("portals", portalTag);
        }
        // Temple state
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putInt("templeDemandProgress", templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        tag.putInt("historyBrowseIndex", historyBrowseIndex);
        // Delegate megastructure NBT to manager
        megastructureManager.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isAmplify = tag.getBoolean("amplified");
        this.stellarMass = tag.getInt("stellarMass");
        this.locked = tag.getBoolean("locked");
        this.amplifierPresent = tag.getBoolean("amplifierPresent");
        this.searching = tag.getBoolean("searching");
        this.searchTicksRemaining = tag.getInt("searchTicks");
        this.searchFailed = tag.getBoolean("searchFailed");
        this.powerInsufficient = tag.getBoolean("powerInsufficient");
        // If searching was true but no timer was saved (old data or newly placed),
        // reset the flag to prevent stuck searching state
        if (this.searching && this.searchTicksRemaining <= 0) {
            this.searching = false;
        }
        this.bodySeed = tag.getLong("bodySeed");
        // Capture old body data for animation transition detection
        CelestialBodyData oldBodyData = this.celestialBodyData;
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
        // Detect transitions for animation (client-side only, e.g. singleplayer chunk load)
        // Skip animation during accelerator evolution or supernova flash
        boolean skipAnimLoad = getAcceleratorStage() >= 1 || getSupernovaFlashTicks() > 0;
        if (level != null && level.isClientSide() && !skipAnimLoad) {
            boolean hadBody = oldBodyData != null;
            boolean hasBody = this.celestialBodyData != null;
            if (!hadBody && hasBody) {
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = null;
            } else if (hadBody && !hasBody) {
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = false;
                this.animationPreviousBodyData = oldBodyData;
            } else if (hadBody && !oldBodyData.toTag().equals(this.celestialBodyData.toTag())) {
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = oldBodyData;
            }
        }
        loadSearchHistory(tag);
        loadInventory(tag, registries);
        // Material filter
        if (tag.contains("materialFilter")) {
            this.materialFilter = ItemStack.parse(registries, tag.getCompound("materialFilter"))
                .orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
        } else {
            this.materialFilter = new ItemStack(net.minecraft.world.item.Items.BARRIER);
        }
        this.materialLimit = tag.getInt("materialLimit");
        this.ageAnvilCount = tag.getInt("ageAnvilCount");
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"));
        } else {
            this.planetaryResourceSet = null;
        }
        // Wormhole stabilizer state (handled by WormholeStabilizerHandler NBT)
        this.portals.clear();
        if (tag.contains("portals")) {
            CompoundTag portalTag = tag.getCompound("portals");
            for (String key : portalTag.getAllKeys()) {
                CompoundTag posTag = portalTag.getCompound(key);
                Cube323PartHalf side = Cube323PartHalf.valueOf(key.toUpperCase());
                BlockPos pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
                portals.put(side, pos);
            }
        }
        // Temple state
        this.templeCycleDay = tag.getInt("templeCycleDay");
        this.templeLastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandProgress = tag.getInt("templeDemandProgress");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        // Collider runtime state is not persisted — always start clean on load
        this.historyBrowseIndex = tag.getInt("historyBrowseIndex");
        // Delegate megastructure NBT to manager (must be last so managers overwrite BE fields)
        megastructureManager.loadAdditional(tag, registries);
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

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
        // Material filter sync
        if (!materialFilter.isEmpty()) {
            tag.put("materialFilter", materialFilter.save(registries));
        }
        tag.putInt("materialLimit", materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (planetaryResourceSet != null) {
            tag.put("planetaryResources", planetaryResourceSet.toTag());
        }
        // Temple state (client sync)
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", templeDemandItem.save(registries));
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

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.isAmplify = tag.getBoolean("amplified");
        this.stellarMass = tag.getInt("stellarMass");
        this.locked = tag.getBoolean("locked");
        this.amplifierPresent = tag.getBoolean("amplifierPresent");
        this.searching = tag.getBoolean("searching");
        this.searchTicksRemaining = tag.getInt("searchTicks");
        this.searchFailed = tag.getBoolean("searchFailed");
        this.powerInsufficient = tag.getBoolean("powerInsufficient");
        this.bodySeed = tag.getLong("bodySeed");

        // Capture old body data for animation transition detection
        CelestialBodyData oldBodyData = this.celestialBodyData;
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
        // Detect transitions for animation (client-side only)
        // Skip animation during accelerator evolution or supernova flash
        boolean skipAnim = getAcceleratorStage() >= 1 || getSupernovaFlashTicks() > 0;
        if (level != null && level.isClientSide() && !skipAnim) {
            boolean hadBody = oldBodyData != null;
            boolean hasBody = this.celestialBodyData != null;
            if (!hadBody && hasBody) {
                // Body appeared — start forward (grow-in) animation
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = null;
            } else if (hadBody && !hasBody) {
                // Body disappeared — start reverse (shrink-out) animation
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = false;
                this.animationPreviousBodyData = oldBodyData;
            } else if (hadBody && !oldBodyData.toTag().equals(this.celestialBodyData.toTag())) {
                // Body changed to a different type — animate transition
                this.animationTicks = ANIMATION_DURATION_TICKS;
                this.animationForward = true;
                this.animationPreviousBodyData = oldBodyData;
            }
        }
        loadSearchHistory(tag);
        loadInventory(tag, lookupProvider);
        // Material filter (client side — read from sync)
        if (tag.contains("materialFilter")) {
            this.materialFilter = ItemStack.parse(lookupProvider, tag.getCompound("materialFilter"))
                .orElse(new ItemStack(net.minecraft.world.item.Items.BARRIER));
        } else {
            this.materialFilter = new ItemStack(net.minecraft.world.item.Items.BARRIER);
        }
        this.materialLimit = tag.getInt("materialLimit");
        this.ageAnvilCount = tag.getInt("ageAnvilCount");
        if (tag.contains("planetaryResources")) {
            this.planetaryResourceSet = PlanetaryResourceSet.fromTag(tag.getCompound("planetaryResources"));
        } else {
            this.planetaryResourceSet = null;
        }
        // Temple state (client side)
        this.templeCycleDay = tag.getInt("templeCycleDay");
        this.templeLastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.templeDemandItem = ItemStack.parse(lookupProvider, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandProgress = tag.getInt("templeDemandProgress");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        // Collider runtime state not synced to client
        this.historyBrowseIndex = tag.getInt("historyBrowseIndex");
        // Delegate megastructure NBT to manager
        megastructureManager.readUpdateTag(tag, lookupProvider);
    }

    private void loadInventory(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("anvils")) {
            CompoundTag invTag = tag.getCompound("anvils");
            for (int i = 0; i < 5; i++) {
                String key = "s" + i;
                this.anvilInventory.setItem(
                    i,
                    invTag.contains(key)
                    ? ItemStack.parse(registries, Objects.requireNonNull(invTag.get(key))).orElse(ItemStack.EMPTY)
                    : ItemStack.EMPTY
                );
            }
        }
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
        if (tag.contains("searchHistory")) {
            CompoundTag histTag = tag.getCompound("searchHistory");
            int size = Math.min(histTag.getInt("size"), MAX_HISTORY);
            for (int i = 0; i < size; i++) {
                if (histTag.contains("h" + i)) {
                    CompoundTag entryTag = histTag.getCompound("h" + i);
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
        return new CelestialForgingAnvilMenu(ModMenuTypes.CFA.get(), containerId, inventory, this);
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

    private List<CelestialForgingAnvilLaserInterfaceBlockEntity> findLaserInterfaces() {
        return CfaInterfaceScanner.findLaserInterfaces(level, worldPosition);
    }

    /**
     * Sync laser requirements to all connected laser interfaces based on the active megastructure.
     * This enables the laser interface tooltip to show "Required: Lv.X" / "Required: Gamma Lv.X"
     * and the ✓/✗ valid status.
     */

    private List<IItemHandler> findLogisticsInterfaces() {
        return CfaInterfaceScanner.findLogisticsInterfaces(level, worldPosition);
    }

    private static void setHandlerSlot(IItemHandler handler, int slot, ItemStack stack) {
        ItemStack existing = handler.getStackInSlot(slot);
        if (!existing.isEmpty()) {
            handler.extractItem(slot, existing.getCount(), false);
        }
        if (!stack.isEmpty()) {
            handler.insertItem(slot, stack, false);
        }
    }

    /**
     * Scan the 12 face-adjacent positions at the CFA bottom layer (Y = controller Y).
     * The CFA occupies X=-1..1, Z=-1..1 at Y=0. The 12 positions are the blocks
     * directly touching each face of this 3×3 base, excluding the 4 corners
     * (which are edge-adjacent, not face-adjacent).
     */
    private void scanAdjacentBlocks(java.util.function.Consumer<BlockPos> consumer) {
        CfaInterfaceScanner.scanAdjacentBlocks(worldPosition, level, consumer);
    }

    // === Wormhole interface scanning (public for cross-CFA access) ===

    /**
     * Generic helper: scan adjacent blocks and map any that are instances of the given type
     * keyed by relative offset from this CFA's controller.
     */
    private <T extends BlockEntity> Map<BlockPos, T> getInterfacesMap(Class<T> type) {
        return CfaInterfaceScanner.getInterfacesMap(type, level, worldPosition);
    }

    /**
     * Get all laser interfaces mapped by relative offset from this CFA's controller.
     */
    public Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> getLaserInterfacesMap() {
        return getInterfacesMap(CelestialForgingAnvilLaserInterfaceBlockEntity.class);
    }

    /**
     * Get all logistics interfaces mapped by relative offset from this CFA's controller.
     */
    public Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> getLogisticsInterfacesMap() {
        return getInterfacesMap(CelestialForgingAnvilLogisticsInterfaceBlockEntity.class);
    }

    /**
     * Get all fluid interfaces mapped by relative offset from this CFA's controller.
     */
    public Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> getFluidInterfacesMap() {
        return getInterfacesMap(CelestialForgingAnvilFluidInterfaceBlockEntity.class);
    }

    // === Wormhole content syncing ===

    /**
     * Called immediately when a player inserts/removes items in a logistics interface.
     * Delegates to WormholeStabilizerHandler.
     */
    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot) {
        WormholeStabilizerHandler wh = megastructureManager.getWormholeHandler();
        wh.syncLogisticsOnChange(interfacePos, changedSlot, this);
    }

    private List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces() {
        return CfaInterfaceScanner.findFluidInterfaces(level, worldPosition);
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
