package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetResourceGenerator;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyType;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.TempleDemandRecipe;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.util.GravityManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CelestialForgingAnvilBlockEntity extends BlockEntity implements MenuProvider, IPowerConsumer, IPowerProducer, IDiskCloneable {
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
    @Nullable
    private PlanetaryResourceSet planetaryResourceSet = null;

    /**
     * Index of the currently built megastructure (refactor option), or -1 if none.
     */
    @Getter
    private int activeMegastructureIndex = -1;

    /**
     * Whether the excavator has valid laser input (for model switching).
     */
    @Getter
    private boolean excavatorLaserActive = false;

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
     * Whether the current day's demand has been satisfied.
     */
    @Getter
    private boolean templeDemandSatisfied = false;

    // === Stellar Ring Collider state ===
    /**
     * Remaining cooldown ticks after a cycle, 0 = ready.
     */
    private int colliderCooldown = 0;
    /**
     * Remaining work ticks for the current cycle, 0 = idle.
     */
    private int colliderCycleRemaining = 0;
    /**
     * Anvil item reserved for the current cycle.
     */
    private ItemStack colliderReservedAnvil = ItemStack.EMPTY;
    /**
     * Position of the logistics interface where the anvil was extracted from.
     */
    @Nullable
    private BlockPos colliderReservedAnvilSource = null;
    /**
     * Hit block item reserved for the current cycle.
     */
    private ItemStack colliderReservedHitBlock = ItemStack.EMPTY;
    /**
     * Position of the logistics interface where the hit block was extracted from.
     */
    @Nullable
    private BlockPos colliderReservedHitBlockSource = null;
    /**
     * Speed of the active recipe, for T calculation.
     */
    private int colliderActiveSpeed = 0;
    /**
     * Valid hit-block target items pushed to logistics for display.
     */
    private final List<ItemStack> colliderTargetItems = new ArrayList<>();

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === IPowerConsumer ===

    @Override
    public int getInputPower() {
        if (searching && searchTicksRemaining > 0) {
            return isAmplify ? 4000 : 1000;
        }
        // Eco station: CFA本体 consumes 1MW constantly
        if (activeMegastructureIndex >= 0) {
            CelestialRefactorOption option = getActiveMegastructureOption();
            if (option != null) {
                if ("eco_station".equals(option.megastructure())) {
                    return 1000;
                }
                // Stellar Ring Collider: 4MW
                if ("stellar_ring_collider".equals(option.megastructure())) {
                    return 4000;
                }
                // Dyson Sphere: passive, no power consumption
                if ("dyson_sphere_small".equals(option.megastructure())
                    || "dyson_sphere_large".equals(option.megastructure())) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * IPowerProducer: generate power when Dyson Sphere is active.
     * Power formula: P = (E × R²) / 800 (MW, rounded down), returned in kW (×1000)
     * E = energy anvil count from celestial body parameters (StarData.energy)
     * R = celestial body size from celestial body parameters (StarData.size)
     */
    @Override
    public int getOutputPower() {
        if (activeMegastructureIndex >= 0 && celestialBodyData instanceof StarData star) {
            CelestialRefactorOption option = getActiveMegastructureOption();
            if (option != null) {
                if ("dyson_sphere_small".equals(option.megastructure())
                    || "dyson_sphere_large".equals(option.megastructure())) {
                    int e = star.energy();
                    int r = star.size();
                    if (e > 0 && r > 0) {
                        // P = (E × R²) / 800 MW, return in kW (×1000)
                        int powerMW = (e * r * r) / 800;
                        return powerMW * 1000;
                    }
                }
            }
        }
        return 0;
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
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        if (activeMegastructureIndex >= 0) {
            CelestialRefactorOption option = getActiveMegastructureOption();
            if (option != null) {
                if ("dyson_sphere_small".equals(option.megastructure())
                    || "dyson_sphere_large".equals(option.megastructure())) {
                    return PowerComponentType.PRODUCER;
                }
            }
        }
        return IPowerConsumer.super.getComponentType();
    }

    private boolean hasEnoughPower() {
        if (grid == null) return false;
        int required = getInputPower();
        return required <= 0 || grid.getRemaining() >= required;
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
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            celestialBodyData,
            isAmplify,
            this.planetaryResourceSet
        );
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
     */
    private static final int GRAVITY_RADIUS = 3;
    /**
     * Gravity strength formula scale factor. strength = stellarMass / size * SCALE.
     */
    private static final double GRAVITY_STRENGTH_SCALE = 20.0;

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

        // Megastructure logic
        if (activeMegastructureIndex >= 0) {
            serverTickExcavator();
            serverTickExtractor();
            serverTickGiantExtractor();
            serverTickEcoStation();
            serverTickTemple();
            serverTickStellarRingCollider();
            serverTickDysonSphere();
        }
    }

    private void updateGravitySource() {
        if (level == null || level.isClientSide()) return;

        boolean shouldHaveGravity =
            amplifierPresent && celestialBodyData instanceof StarData && stellarMass > 0 && celestialBodyData.size() > 0;

        double newStrength = shouldHaveGravity ? (stellarMass / (double) celestialBodyData.size()) * GRAVITY_STRENGTH_SCALE : 0;
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

    private void destroyEntitiesAtCenter() {
        BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
        AABB centerBox = new AABB(centerPos);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, centerBox);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                // Insta-kill living entities (mobs, players, etc.) via fire damage
                living.hurt(level.damageSources().inFire(), 1.0E12f);
            } else {
                // Destroy non-living entities (items, falling blocks, projectiles, etc.)
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
    }

    public void setAmplify(boolean amplify) {
        if (this.isAmplify != amplify) {
            this.isAmplify = amplify;
            if (!amplify && celestialBodyData instanceof StarData) {
                this.locked = true; // Lock when amplifier removed with stellar body
            }
            if (level != null && !level.isClientSide()) {
                randomizeBody();
            }
            this.setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void randomizeBody() {
        tryMatchCelestialBody();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (gravitySourceActive && level != null && !level.isClientSide()) {
            BlockPos centerPos = worldPosition.offset(0, GRAVITY_CENTER_Y_OFFSET, 0);
            GravityManager.GravitySourceManager.removeSource(level, centerPos);
            gravitySourceActive = false;
        }
    }

    /**
     * Clear all data that is tied to the world position, player actions, or multiblock
     * state. Called in {@code onRemove} so the dropped block item starts fresh, without
     * carrying stale celestial bodies, megastructures, inventory contents, or runtime
     * flags from the previous placement.
     *
     * <p>
     * Chunk save/load is unaffected — this is only called when the block is actually
     * broken.
     */
    public void clearPositionDependentData() {
        // Inventories
        for (int i = 0; i < anvilInventory.getContainerSize(); i++) {
            anvilInventory.setItem(i, ItemStack.EMPTY);
        }
        materialContainer.setItem(0, ItemStack.EMPTY);

        // Celestial matching results
        this.celestialBodyData = null;
        this.planetaryResourceSet = null;
        this.searchHistory.clear();
        this.bodySeed = 0;
        this.stellarMass = 0;
        this.ageAnvilCount = 0;

        // Megastructure state
        this.activeMegastructureIndex = -1;
        this.excavatorLaserActive = false;

        // Temple state
        this.templeCycleDay = 0;
        this.templeLastDay = -1;
        this.templeDemandItem = ItemStack.EMPTY;
        this.templeDemandCount = 0;
        this.templeDemandSatisfied = false;

        // Collider state
        this.colliderCooldown = 0;
        this.colliderCycleRemaining = 0;
        this.colliderReservedAnvil = ItemStack.EMPTY;
        this.colliderReservedAnvilSource = null;
        this.colliderReservedHitBlock = ItemStack.EMPTY;
        this.colliderReservedHitBlockSource = null;
        this.colliderActiveSpeed = 0;
        this.colliderTargetItems.clear();

        // History browsing state
        this.historyBrowseIndex = 0;
        this.historyOriginalEntry = null;

        // Multiblock state
        this.isAmplify = false;
        this.amplifierPresent = false;

        // Runtime / transient state
        this.searching = false;
        this.searchTicksRemaining = 0;
        this.searchFailed = false;
        this.powerInsufficient = false;
        this.lastConsumedSeedItem = null;
        this.lastConsumedSeedNbt = null;

        // User selections
        this.locked = false;
        this.materialFilter = new ItemStack(Items.BARRIER);
        this.materialLimit = 0;

        // Rotation / animation (cosmetic)
        this.rotation = 0;
        this.preRotation = 0;
        this.bodyRotation = 0;
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

        // First: check for seed item snapshot (disk / singularity crystal)
        if (lastConsumedSeedNbt != null && lastConsumedSeedNbt.contains("celestialBody")) {
            applySnapshot(lastConsumedSeedNbt);
            consumeSeedItem();
            return;
        }

        // Second: check for special celestial body discovery via seed item
        if (lastConsumedSeedItem != null) {
            SpecialCelestialBodyData specialBody = tryMatchSpecialCelestialBody(
                time, space, mass, energy, lastConsumedSeedItem, ((ServerLevel) level).getSeed());
            if (specialBody != null) {
                this.celestialBodyData = specialBody;
                if (!level.isClientSide()) {
                    this.planetaryResourceSet = specialBody.specialType().generateResources();
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
        int time, int space, int mass, int energy,
        Item consumedSeedItem, long worldSeed
    ) {
        for (SpecialCelestialBodyType type : SpecialCelestialBodyType.values()) {
            if (type.getTime() == time
                && type.getSpace() == space
                && type.getMass() == mass
                && type.getEnergy() == energy
                && type.isEffectiveSeedItem(consumedSeedItem, worldSeed)) {
                return new SpecialCelestialBodyData(type);
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
            tag.putIntArray("anvilCounts", new int[]{
                getAnvilCount(0), getAnvilCount(1), getAnvilCount(2), getAnvilCount(3)
            });
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
    public InteractionResult useDisk(
        Level level, Player player, InteractionHand hand,
        ItemStack itemStack, BlockHitResult hitResult
    ) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (itemStack.is(ModItems.DISK.get())) {
            // Only allow storing, not applying
            if (!DiskItem.hasDataStored(itemStack)) {
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
     * Load a celestial snapshot from a disk item's stored data.
     */
    @javax.annotation.Nullable
    public static CompoundTag loadSnapshotFromStack(ItemStack stack) {
        if (stack.getItem() instanceof DiskItem && DiskItem.hasDataStored(stack)) {
            CompoundTag data = DiskItem.getData(stack);
            if (data.contains("celestialBody")) return data.copy();
        }
        return null;
    }

    /**
     * Save a snapshot into a disk item.
     */
    public static void saveSnapshotToStack(ItemStack stack, CompoundTag snapshot) {
        if (stack.getItem() instanceof DiskItem) {
            CompoundTag diskTag = DiskItem.createData(stack);
            diskTag.merge(snapshot);
        }
    }

    // === CFA block interaction ===

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            // Re-register with power grid to ensure CFA is in both producer and consumer sets
            PowerGrid.addComponent(this);
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
        tag.putInt("activeMegastructure", activeMegastructureIndex);
        tag.putBoolean("excavatorLaserActive", excavatorLaserActive);
        // Temple state
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        // Collider state (runtime only — not persisted)
        tag.putInt("historyBrowseIndex", historyBrowseIndex);
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
        if (level != null && level.isClientSide()) {
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
        this.activeMegastructureIndex = tag.contains("activeMegastructure") ? tag.getInt("activeMegastructure") : -1;
        this.excavatorLaserActive = tag.getBoolean("excavatorLaserActive");
        // Temple state
        this.templeCycleDay = tag.getInt("templeCycleDay");
        this.templeLastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.templeDemandItem = ItemStack.parse(registries, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        // Collider runtime state is not persisted — always start clean on load
        this.historyBrowseIndex = tag.getInt("historyBrowseIndex");
        // Sync to client — important for when loadAdditional is called after onLoad
        // (e.g., BlockItem.updateCustomBlockEntityTag during placement restores saved NBT)
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
        tag.putInt("activeMegastructure", activeMegastructureIndex);
        tag.putBoolean("excavatorLaserActive", excavatorLaserActive);
        // Temple state (client sync)
        tag.putInt("templeCycleDay", templeCycleDay);
        tag.putLong("templeLastDay", templeLastDay);
        if (!templeDemandItem.isEmpty()) {
            tag.put("templeDemand", templeDemandItem.save(registries));
        }
        tag.putInt("templeDemandCount", templeDemandCount);
        tag.putBoolean("templeDemandSatisfied", templeDemandSatisfied);
        // Collider runtime state not synced to client
        tag.putInt("historyBrowseIndex", historyBrowseIndex);
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
        if (level != null && level.isClientSide()) {
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
        this.activeMegastructureIndex = tag.contains("activeMegastructure") ? tag.getInt("activeMegastructure") : -1;
        this.excavatorLaserActive = tag.getBoolean("excavatorLaserActive");
        // Temple state (client side)
        this.templeCycleDay = tag.getInt("templeCycleDay");
        this.templeLastDay = tag.contains("templeLastDay") ? tag.getLong("templeLastDay") : -1;
        if (tag.contains("templeDemand")) {
            this.templeDemandItem = ItemStack.parse(lookupProvider, tag.getCompound("templeDemand")).orElse(ItemStack.EMPTY);
        } else {
            this.templeDemandItem = ItemStack.EMPTY;
        }
        this.templeDemandCount = tag.getInt("templeDemandCount");
        this.templeDemandSatisfied = tag.getBoolean("templeDemandSatisfied");
        // Collider runtime state not synced to client
        this.historyBrowseIndex = tag.getInt("historyBrowseIndex");
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
        this.locked = !this.locked;
        if (!this.locked) {
            // Unlocking: clear megastructure to revert to restriction ring
            clearMegastructure();
        }
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /**
     * Clear the active megastructure and all related state, reverting to the restriction ring.
     */
    private void clearMegastructure() {
        this.activeMegastructureIndex = -1;
        this.excavatorLaserActive = false;
        // Temple state
        this.templeCycleDay = 0;
        this.templeLastDay = -1;
        this.templeDemandItem = ItemStack.EMPTY;
        this.templeDemandCount = 0;
        this.templeDemandSatisfied = false;
        pushTempleDemandToLogistics(); // clear demand display
        // Clear collider state
        outputColliderReservedItems();
        resetColliderState();
        // Clear material filter
        this.materialFilter = new ItemStack(Items.BARRIER);
        this.materialLimit = 0;
        // Re-register with power grid to restore CONSUMER type
        PowerGrid.addComponent(this);
    }

    /**
     * Get the currently active megastructure option, or null if none is built.
     */
    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() {
        if (activeMegastructureIndex < 0 || celestialBodyData == null) return null;
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            celestialBodyData,
            isAmplify,
            this.planetaryResourceSet
        );
        if (activeMegastructureIndex >= options.size()) return null;
        return options.get(activeMegastructureIndex);
    }

    /**
     * Attempt to build a megastructure. Called from the server when the player clicks "Start Refactoring".
     *
     * @param optionIndex the selected refactor option index
     */
    public void buildMegastructure(int optionIndex) {
        if (level == null || level.isClientSide()) return;
        if (celestialBodyData == null) return;
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            celestialBodyData,
            isAmplify,
            this.planetaryResourceSet
        );
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        CelestialRefactorOption option = options.get(optionIndex);

        // If any megastructure is already built, block
        if (activeMegastructureIndex >= 0) {
            return;
        }

        if (!option.needsMaterial()) {
            // No materials needed — build immediately
            activeMegastructureIndex = optionIndex;
        } else {
            // Check material container
            ItemStack contained = materialContainer.getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(contained, required) || contained.getCount() < required.getCount()) {
                return;
            }
            // Consume materials
            contained.shrink(required.getCount());
            activeMegastructureIndex = optionIndex;
        }

        // Re-register with power grid so the component type change takes effect
        // (e.g., Dyson Sphere switches CFA from CONSUMER to PRODUCER)
        PowerGrid.addComponent(this);
        this.setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // === Excavator ===

    private static final int EXCAVATOR_LASER_THRESHOLD = 16;
    private static final int EXCAVATOR_MAX_LASERS = 4;

    private int excavatorLogisticsRoundRobin = 0;
    private int ecoStationLogisticsRoundRobin = 0;

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void serverTickExcavator() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;

        // Check if this is the excavator
        if (!"planet_excavator".equals(option.megastructure())) {
            excavatorLaserActive = false;
            return;
        }
        if (planetaryResourceSet == null) return;

        // Count valid laser inputs (level >= 16, capped at 16)
        int laserCount = countValidLasers();
        boolean hasValidLaser = laserCount > 0;
        if (excavatorLaserActive != hasValidLaser) {
            excavatorLaserActive = hasValidLaser;
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        if (!hasValidLaser) return;

        // Efficiency = number of valid lasers (each 16-level laser = 1x)
        int efficiency = Math.min(laserCount, EXCAVATOR_MAX_LASERS);

        // Combine minerals and wasteland items into a single mining pool
        List<PlanetaryResourceSet.WeightedItemStack> miningPool = new ArrayList<>();
        miningPool.addAll(planetaryResourceSet.getMinerals());
        miningPool.addAll(planetaryResourceSet.getWastelandItems());
        if (miningPool.isEmpty()) return;

        // Weighted random selection
        int totalWeight = miningPool.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        if (totalWeight <= 0) return;

        int roll = level.getRandom().nextInt(totalWeight);
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

        // Create output items
        ItemLike item = BuiltInRegistries.ITEM.get(chosenItem);
        if (item.asItem() == Items.AIR) return;
        ItemStack output = new ItemStack(item, efficiency);

        // Output to logistics interfaces
        List<IItemHandler> logisticsInterfaces = findLogisticsInterfaces();
        if (logisticsInterfaces.isEmpty()) return;

        // Round-robin distribution
        int startIdx = excavatorLogisticsRoundRobin % logisticsInterfaces.size();
        for (int attempt = 0; attempt < logisticsInterfaces.size(); attempt++) {
            int idx = (startIdx + attempt) % logisticsInterfaces.size();
            IItemHandler handler = logisticsInterfaces.get(idx);
            ItemStack remainder = insertIntoHandler(handler, output);
            if (remainder.getCount() < output.getCount()) {
                excavatorLogisticsRoundRobin = (idx + 1) % logisticsInterfaces.size();
                return; // Successfully inserted at least some items
            }
        }
    }

    private int countValidLasers() {
        List<CelestialForgingAnvilLaserInterfaceBlockEntity> lasers = findLaserInterfaces();
        int count = 0;
        for (CelestialForgingAnvilLaserInterfaceBlockEntity laser : lasers) {
            int level = laser.getReceivedLaserLevel();
            if (level >= EXCAVATOR_LASER_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    private List<CelestialForgingAnvilLaserInterfaceBlockEntity> findLaserInterfaces() {
        List<CelestialForgingAnvilLaserInterfaceBlockEntity> result = new ArrayList<>();
        if (level == null) return result;
        // Scan a 5×5×5 cube around the controller (the CFA is a 3×3×2 multiblock,
        // so laser interfaces may be adjacent to any face, not just the controller)
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserBe) {
                result.add(laserBe);
            }
        });
        return result;
    }

    private List<IItemHandler> findLogisticsInterfaces() {
        List<IItemHandler> result = new ArrayList<>();
        if (level == null) return result;
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logisticsBe) {
                result.add(logisticsBe.getItemHandler());
            }
        });
        return result;
    }

    /**
     * Scan the 12 face-adjacent positions at the CFA bottom layer (Y = controller Y).
     * The CFA occupies X=-1..1, Z=-1..1 at Y=0. The 12 positions are the blocks
     * directly touching each face of this 3×3 base, excluding the 4 corners
     * (which are edge-adjacent, not face-adjacent).
     */
    private void scanAdjacentBlocks(java.util.function.Consumer<BlockPos> consumer) {
        if (level == null) return;
        int y = worldPosition.getY();
        // North face (Z = -2): 3 positions
        for (int dx = -1; dx <= 1; dx++) {
            consumer.accept(new BlockPos(worldPosition.getX() + dx, y, worldPosition.getZ() - 2));
        }
        // South face (Z = 2): 3 positions
        for (int dx = -1; dx <= 1; dx++) {
            consumer.accept(new BlockPos(worldPosition.getX() + dx, y, worldPosition.getZ() + 2));
        }
        // West face (X = -2): 3 positions
        for (int dz = -1; dz <= 1; dz++) {
            consumer.accept(new BlockPos(worldPosition.getX() - 2, y, worldPosition.getZ() + dz));
        }
        // East face (X = 2): 3 positions
        for (int dz = -1; dz <= 1; dz++) {
            consumer.accept(new BlockPos(worldPosition.getX() + 2, y, worldPosition.getZ() + dz));
        }
    }

    private static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    // === Extractor ===

    private static final int EXTRACTOR_FLUID_PER_TICK = 250; // mB per fluid interface per gt

    private void serverTickExtractor() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;
        if (!"planet_exctractor".equals(option.megastructure())) return;
        if (planetaryResourceSet == null) return;

        // Get available fluids from the planet
        List<PlanetaryResourceSet.WeightedFluidStack> fluids = planetaryResourceSet.getFluids();
        if (fluids.isEmpty()) return;

        // Find fluid interfaces
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> fluidInterfaces = findFluidInterfaces();
        if (fluidInterfaces.isEmpty()) return;

        // Weighted total for random selection
        int totalWeight = fluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        if (totalWeight <= 0) return;

        // Each fluid interface works independently, producing fluid per gt
        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidInterface : fluidInterfaces) {
            // Weighted random selection based on planet resource proportions
            int roll = level.getRandom().nextInt(totalWeight);
            int cumulative = 0;
            ResourceLocation chosenFluid = null;
            for (PlanetaryResourceSet.WeightedFluidStack fluid : fluids) {
                cumulative += fluid.weight();
                if (roll < cumulative) {
                    chosenFluid = fluid.fluidId();
                    break;
                }
            }
            if (chosenFluid == null) chosenFluid = fluids.getFirst().fluidId();

            // Resolve fluid and produce 250mB
            var fluid = BuiltInRegistries.FLUID.get(chosenFluid);
            if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) continue;
            FluidStack output = new FluidStack(fluid, EXTRACTOR_FLUID_PER_TICK);
            if (output.isEmpty()) continue;

            // Try to insert into the fluid interface's own capacity
            fluidInterface.getFluidHandler().fill(output, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private List<CelestialForgingAnvilFluidInterfaceBlockEntity> findFluidInterfaces() {
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> result = new ArrayList<>();
        if (level == null) return result;
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilFluidInterfaceBlockEntity fluidBe) {
                result.add(fluidBe);
            }
        });
        return result;
    }

    // === Giant Extractor ===

    private static final String GIANT_EXTRACTOR_MEGASTRUCTURE = "giant_planet_exctractor";

    private void serverTickGiantExtractor() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;
        if (!GIANT_EXTRACTOR_MEGASTRUCTURE.equals(option.megastructure())) return;
        if (planetaryResourceSet == null) return;

        // Get giant fluid and item resources
        List<PlanetaryResourceSet.WeightedFluidStack> giantFluids = planetaryResourceSet.getGiantFluids();
        List<PlanetaryResourceSet.WeightedItemStack> giantItems = planetaryResourceSet.getGiantItems();

        // Find fluid interfaces
        List<CelestialForgingAnvilFluidInterfaceBlockEntity> fluidInterfaces = findFluidInterfaces();
        if (fluidInterfaces.isEmpty()) return;

        // Each fluid interface works independently, producing fluid per gt
        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidInterface : fluidInterfaces) {
            // Produce fluid
            if (!giantFluids.isEmpty()) {
                int totalFluidWeight = giantFluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
                if (totalFluidWeight > 0) {
                    int roll = level.getRandom().nextInt(totalFluidWeight);
                    int cumulative = 0;
                    ResourceLocation chosenFluid = null;
                    for (PlanetaryResourceSet.WeightedFluidStack fluid : giantFluids) {
                        cumulative += fluid.weight();
                        if (roll < cumulative) {
                            chosenFluid = fluid.fluidId();
                            break;
                        }
                    }
                    if (chosenFluid == null) chosenFluid = giantFluids.getFirst().fluidId();

                    var fluid = BuiltInRegistries.FLUID.get(chosenFluid);
                    if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                        FluidStack output = new FluidStack(fluid, EXTRACTOR_FLUID_PER_TICK);
                        if (!output.isEmpty()) {
                            fluidInterface.getFluidHandler().fill(output, IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
            }
        }

        // Produce 1 item to logistics interface
        if (!giantItems.isEmpty()) {
            int totalItemWeight = giantItems.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
            if (totalItemWeight > 0) {
                int roll = level.getRandom().nextInt(totalItemWeight);
                int cumulative = 0;
                ResourceLocation chosenItem = null;
                for (PlanetaryResourceSet.WeightedItemStack item : giantItems) {
                    cumulative += item.weight();
                    if (roll < cumulative) {
                        chosenItem = item.itemId();
                        break;
                    }
                }
                if (chosenItem == null) chosenItem = giantItems.getFirst().itemId();

                ItemLike item = BuiltInRegistries.ITEM.get(chosenItem);
                if (item.asItem() != Items.AIR) {
                    ItemStack output = new ItemStack(item, 1);
                    List<IItemHandler> logistics = findLogisticsInterfaces();
                    if (!logistics.isEmpty()) {
                        int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                        for (int attempt = 0; attempt < logistics.size(); attempt++) {
                            int idx = (startIdx + attempt) % logistics.size();
                            IItemHandler handler = logistics.get(idx);
                            ItemStack remainder = insertIntoHandler(handler, output);
                            if (remainder.getCount() < output.getCount()) {
                                excavatorLogisticsRoundRobin = (idx + 1) % logistics.size();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // === Stellar Ring Collider ===

    private static final String COLLIDER_MEGASTRUCTURE = "stellar_ring_collider";
    private static final int COLLIDER_COOLDOWN_TICKS = 10;
    private static final int COLLIDER_MAX_COLLISIONS = 16;

    private record LogisticsRef(IItemHandler handler, BlockPos pos) {}

    private record LocatedStack(int li, int slot, ItemStack stack, Block block) {}

    private void serverTickStellarRingCollider() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;
        if (!COLLIDER_MEGASTRUCTURE.equals(option.megastructure())) return;
        if (planetaryResourceSet == null) return;
        if (!(celestialBodyData instanceof StarData star)) return;
        if (star.size() >= 26) return;

        boolean starMissing = !amplifierPresent;
        boolean isProcessing = colliderCycleRemaining > 0 && !powerInsufficient;

        // Refresh targets on 20-tick boundary
        if (level.getGameTime() % 20 == 0) {
            refreshColliderTargetItems();
        }

        // When amplifier is missing, star is not rendered — collider is disabled
        if (starMissing) {
            // Clear stale cycle state
            if (colliderCycleRemaining > 0 || !colliderReservedAnvil.isEmpty() || !colliderReservedHitBlock.isEmpty()) {
                outputColliderReservedItems();
                resetColliderState();
            }
            broadcastColliderState(false, true);
            return;
        }

        // Normal path: broadcast current state every tick so the client stays in sync.
        // This is the same pattern as the starMissing branch above — without this,
        // state transitions (e.g. processing→idle, starMissing→idle) never reach the client.
        broadcastColliderState(isProcessing, false);

        // Power check — if insufficient, output reserved items
        if (powerInsufficient) {
            outputColliderReservedItems();
            resetColliderState();
            return;
        }

        // Cooldown phase
        if (colliderCooldown > 0) {
            colliderCooldown--;
            return;
        }

        // Working phase
        if (colliderCycleRemaining > 0) {
            colliderCycleRemaining--;
            if (colliderCycleRemaining == 0) {
                completeColliderCycle();
                colliderCooldown = COLLIDER_COOLDOWN_TICKS;
            }
            return;
        }

        // Idle: try to start a new cycle
        tryStartColliderCycle();
    }

    /**
     * Broadcast collider processing/star-missing state to all nearby logistics interfaces.
     */
    private void broadcastColliderState(boolean processing, boolean starMissing) {
        if (level == null || level.isClientSide()) return;
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                logiBe.setColliderProcessing(processing);
                logiBe.setColliderStarMissing(starMissing);
                logiBe.setChanged();
            }
        });
    }

    /**
     * Broadcast current target items to all nearby logistics interfaces.
     */
    private void broadcastColliderTargets() {
        if (level == null || level.isClientSide()) return;
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                logiBe.setColliderTargetItems(new ArrayList<>(colliderTargetItems));
                logiBe.setChanged();
            }
        });
    }

    private void refreshColliderTargetItems() {
        colliderTargetItems.clear();
        if (level == null) return;
        List<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());
        for (var holder : recipes) {
            AnvilCollisionCraftRecipe recipe = holder.value();
            if (recipe.outputItems().isEmpty()) continue;
            BlockStatePredicate hitPred = recipe.hitBlock();
            if (hitPred.getStatesCache().isEmpty()) continue;
            for (BlockState state : hitPred.getStatesCache()) {
                ItemStack item = new ItemStack(state.getBlock().asItem(), 1);
                boolean has = false;
                for (ItemStack existing : colliderTargetItems) {
                    if (ItemStack.isSameItemSameComponents(existing, item)) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    colliderTargetItems.add(item);
                }
            }
        }
        broadcastColliderTargets();
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void tryStartColliderCycle() {
        if (level == null || !(celestialBodyData instanceof StarData star)) return;
        int mass = this.stellarMass;
        int mag = star.magneticFieldStrength();
        int denominator = mass * mag + 10;
        if (denominator <= 0) return;

        // Get all collision craft recipes with output items
        List<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());

        // Scan the 12 adjacent positions for logistics interfaces
        List<LogisticsRef> logistics = new ArrayList<>();
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity blockEntity = level.getBlockEntity(checkPos);
            if (blockEntity instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                logistics.add(new LogisticsRef(logiBe.getItemHandler(), checkPos.immutable()));
            }
        });
        if (logistics.isEmpty()) return;

        // Build candidate lists: (logisticsIndex, slotIndex, stack, block)
        List<LocatedStack> anvilStacks = new ArrayList<>();
        List<LocatedStack> hitStacks = new ArrayList<>();

        for (int li = 0; li < logistics.size(); li++) {
            IItemHandler handler = logistics.get(li).handler;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                Block block = Block.byItem(stack.getItem());
                if (block == Blocks.AIR) continue;
                for (var holder : recipes) {
                    AnvilCollisionCraftRecipe recipe = holder.value();
                    if (recipe.outputItems().isEmpty()) continue;
                    if (recipe.anvil().test(level, block.defaultBlockState(), null)) {
                        anvilStacks.add(new LocatedStack(li, slot, stack, block));
                        break;
                    }
                }
                for (var holder : recipes) {
                    AnvilCollisionCraftRecipe recipe = holder.value();
                    if (recipe.outputItems().isEmpty()) continue;
                    if (recipe.hitBlock().test(level, block.defaultBlockState(), null)) {
                        hitStacks.add(new LocatedStack(li, slot, stack, block));
                        break;
                    }
                }
            }
        }

        // Find best cross-interface recipe match (highest speed)
        AnvilCollisionCraftRecipe bestRecipe = null;
        int bestSpeed = Integer.MIN_VALUE;
        LocatedStack bestAnvil = null;
        LocatedStack bestHit = null;

        for (LocatedStack anvil : anvilStacks) {
            for (LocatedStack hit : hitStacks) {
                if (anvil.li == hit.li && anvil.slot == hit.slot) continue;
                for (var holder : recipes) {
                    AnvilCollisionCraftRecipe recipe = holder.value();
                    if (recipe.outputItems().isEmpty()) continue;
                    if (recipe.speed() <= bestSpeed) continue;
                    if (recipe.anvil().test(level, anvil.block.defaultBlockState(), null) && recipe.hitBlock()
                        .test(level, hit.block.defaultBlockState(), null)) {
                        bestSpeed = recipe.speed();
                        bestRecipe = recipe;
                        bestAnvil = anvil;
                        bestHit = hit;
                    }
                }
            }
        }

        if (bestRecipe == null || bestAnvil == null || bestHit == null) return;

        // Calculate T = 1000 * V / (M * B + 10), rounded up
        int t = (1000 * bestRecipe.speed() + denominator - 1) / denominator;
        if (t <= 0) t = 1;

        // Reserve items from the logistics interfaces
        int anvilToTake = Math.min(bestAnvil.stack.getCount(), COLLIDER_MAX_COLLISIONS);
        int hitToTake = Math.min(bestHit.stack.getCount(), COLLIDER_MAX_COLLISIONS);

        LogisticsRef anvilSrc = logistics.get(bestAnvil.li);
        LogisticsRef hitSrc = logistics.get(bestHit.li);

        colliderReservedAnvil = anvilSrc.handler.extractItem(bestAnvil.slot, anvilToTake, false);
        colliderReservedHitBlock = hitSrc.handler.extractItem(bestHit.slot, hitToTake, false);

        // Track which interfaces the items came from for cleanup later
        colliderReservedAnvilSource = anvilSrc.pos;
        colliderReservedHitBlockSource = hitSrc.pos;

        colliderActiveSpeed = bestRecipe.speed();
        colliderCycleRemaining = t;
        colliderCooldown = 0;

        // Mark the source interfaces as processing + push targets
        markLogisticsProcessing(colliderReservedAnvilSource, true);
        if (!colliderReservedHitBlockSource.equals(colliderReservedAnvilSource)) {
            markLogisticsProcessing(colliderReservedHitBlockSource, true);
        }
        // Broadcast to all nearby interfaces
        broadcastColliderTargets();
        broadcastColliderState(true, false);

        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Set processing + targets on a specific logistics interface by position.
     */
    private void markLogisticsProcessing(BlockPos pos, boolean processing) {
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
            logiBe.setColliderTargetItems(new ArrayList<>(colliderTargetItems));
            logiBe.setColliderProcessing(processing);
            logiBe.setChanged();
        }
    }

    private void completeColliderCycle() {
        if (level == null) return;
        List<IItemHandler> logistics = findLogisticsInterfaces();

        // Find the active recipe to produce outputs
        List<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());

        AnvilCollisionCraftRecipe activeRecipe = null;
        for (var holder : recipes) {
            AnvilCollisionCraftRecipe recipe = holder.value();
            if (recipe.outputItems().isEmpty()) continue;
            if (recipe.speed() != colliderActiveSpeed) continue;
            Block anvilBlock = Block.byItem(colliderReservedAnvil.getItem());
            Block hitBlock = Block.byItem(colliderReservedHitBlock.getItem());
            if (anvilBlock != Blocks.AIR && hitBlock != Blocks.AIR && recipe.anvil()
                .test(level, anvilBlock.defaultBlockState(), null) && recipe.hitBlock().test(level, hitBlock.defaultBlockState(), null)) {
                activeRecipe = recipe;
                break;
            }
        }

        // Calculate how many collisions we actually perform
        int anvilReserved = colliderReservedAnvil.getCount();
        int hitReserved = colliderReservedHitBlock.getCount();
        boolean consumeAnvil = activeRecipe != null && activeRecipe.consume();
        int collisionCount = consumeAnvil ? Math.min(anvilReserved, hitReserved) : hitReserved;

        // Consume/return hit blocks
        int hitRemaining = hitReserved - collisionCount;
        if (hitRemaining > 0) {
            ItemStack hitReturn = colliderReservedHitBlock.copyWithCount(hitRemaining);
            if (!logistics.isEmpty()) {
                int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    ItemStack remainder = insertIntoHandler(logistics.get(idx), hitReturn);
                    if (remainder.getCount() < hitReturn.getCount()) {
                        excavatorLogisticsRoundRobin = (idx + 1) % logistics.size();
                        if (remainder.isEmpty()) {
                            hitReturn = ItemStack.EMPTY;
                            break;
                        }
                        hitReturn = remainder;
                    }
                }
            }
            if (!hitReturn.isEmpty() && logistics.isEmpty()) {
                dropItemOnGround(hitReturn);
            }
        }

        // Consume/return anvils
        if (consumeAnvil) {
            int anvilRemaining = anvilReserved - collisionCount;
            if (anvilRemaining > 0) {
                ItemStack anvilReturn = colliderReservedAnvil.copyWithCount(anvilRemaining);
                if (!logistics.isEmpty()) {
                    int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                    for (int attempt = 0; attempt < logistics.size(); attempt++) {
                        int idx = (startIdx + attempt) % logistics.size();
                        ItemStack remainder = insertIntoHandler(logistics.get(idx), anvilReturn);
                        if (remainder.getCount() < anvilReturn.getCount()) {
                            excavatorLogisticsRoundRobin = (idx + 1) % logistics.size();
                            if (remainder.isEmpty()) {
                                anvilReturn = ItemStack.EMPTY;
                                break;
                            }
                            anvilReturn = remainder;
                        }
                    }
                }
                if (!anvilReturn.isEmpty() && logistics.isEmpty()) {
                    dropItemOnGround(anvilReturn);
                }
            }
        } else {
            if (!colliderReservedAnvil.isEmpty() && !logistics.isEmpty()) {
                ItemStack anvilReturn = colliderReservedAnvil.copy();
                int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    ItemStack remainder = insertIntoHandler(logistics.get(idx), anvilReturn);
                    if (remainder.getCount() < anvilReturn.getCount()) {
                        excavatorLogisticsRoundRobin = (idx + 1) % logistics.size();
                        if (remainder.isEmpty()) {
                            anvilReturn = ItemStack.EMPTY;
                            break;
                        }
                        anvilReturn = remainder;
                    }
                }
                if (!anvilReturn.isEmpty()) {
                    dropItemOnGround(anvilReturn);
                }
            } else if (!colliderReservedAnvil.isEmpty()) {
                dropItemOnGround(colliderReservedAnvil.copy());
            }
        }

        // Output products
        if (activeRecipe != null && level instanceof ServerLevel serverLevel && collisionCount > 0) {
            for (ChanceItemStack chanceStack : activeRecipe.outputItems()) {
                for (int c = 0; c < collisionCount; c++) {
                    ItemStack output = chanceStack.getResult(serverLevel);
                    if (output.isEmpty()) continue;
                    int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                    for (int attempt = 0; attempt < logistics.size(); attempt++) {
                        int idx = (startIdx + attempt) % logistics.size();
                        ItemStack remainder = insertIntoHandler(logistics.get(idx), output);
                        if (remainder.getCount() < output.getCount()) {
                            excavatorLogisticsRoundRobin = (idx + 1) % logistics.size();
                            if (!remainder.isEmpty()) {
                                output = remainder;
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Clear processing on source interfaces + broadcast idle to all nearby
        markLogisticsProcessing(colliderReservedAnvilSource, false);
        if (colliderReservedHitBlockSource != null && !colliderReservedHitBlockSource.equals(colliderReservedAnvilSource)) {
            markLogisticsProcessing(colliderReservedHitBlockSource, false);
        }
        broadcastColliderState(false, false);

        // Reset cycle tracking
        colliderCooldown = 0;
        colliderCycleRemaining = 0;
        colliderReservedAnvil = ItemStack.EMPTY;
        colliderReservedAnvilSource = null;
        colliderReservedHitBlock = ItemStack.EMPTY;
        colliderReservedHitBlockSource = null;
        colliderActiveSpeed = 0;
    }

    private void dropItemOnGround(ItemStack stack) {
        if (level == null || stack.isEmpty()) return;
        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
            level,
            worldPosition.getX() + 0.5,
            worldPosition.getY() + 1,
            worldPosition.getZ() + 0.5,
            stack
        );
        level.addFreshEntity(entity);
    }

    private void outputColliderReservedItems() {
        if (level == null) return;
        List<IItemHandler> logistics = findLogisticsInterfaces();

        if (!colliderReservedAnvil.isEmpty()) {
            ItemStack remaining = colliderReservedAnvil.copy();
            if (!logistics.isEmpty()) {
                int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size() && !remaining.isEmpty(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    remaining = insertIntoHandler(logistics.get(idx), remaining);
                }
            }
            if (!remaining.isEmpty()) {
                dropItemOnGround(remaining);
            }
        }

        if (!colliderReservedHitBlock.isEmpty()) {
            ItemStack remaining = colliderReservedHitBlock.copy();
            if (!logistics.isEmpty()) {
                int startIdx = excavatorLogisticsRoundRobin % logistics.size();
                for (int attempt = 0; attempt < logistics.size() && !remaining.isEmpty(); attempt++) {
                    int idx = (startIdx + attempt) % logistics.size();
                    remaining = insertIntoHandler(logistics.get(idx), remaining);
                }
            }
            if (!remaining.isEmpty()) {
                dropItemOnGround(remaining);
            }
        }

        // Clear processing on source interfaces before resetting
        markLogisticsProcessing(colliderReservedAnvilSource, false);
        if (colliderReservedHitBlockSource != null && !colliderReservedHitBlockSource.equals(colliderReservedAnvilSource)) {
            markLogisticsProcessing(colliderReservedHitBlockSource, false);
        }
        resetColliderState();
    }

    private void resetColliderState() {
        colliderCooldown = 0;
        colliderCycleRemaining = 0;
        colliderReservedAnvil = ItemStack.EMPTY;
        colliderReservedAnvilSource = null;
        colliderReservedHitBlock = ItemStack.EMPTY;
        colliderReservedHitBlockSource = null;
        colliderActiveSpeed = 0;
        broadcastColliderState(false, false);
    }

    // === Dyson Sphere ===

    /**
     * Dyson Sphere passive power generation tick.
     * Power is generated through {@link #getOutputPower()} called by the power grid.
     * This tick handles state transitions and grid sync.
     */
    private void serverTickDysonSphere() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;
        String name = option.megastructure();
        if (!"dyson_sphere_small".equals(name) && !"dyson_sphere_large".equals(name)) return;
        // Dyson Sphere is passive — no per-tick work needed.
        // Power is generated via getOutputPower() called by the power grid during flush().
    }

    // === Eco Station ===

    private void serverTickEcoStation() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;
        if (!"eco_station".equals(option.megastructure())) return;
        if (planetaryResourceSet == null) return;

        // Must have biological resources and NOT have a low-level civilization
        if (planetaryResourceSet.hasCivilization()) return;
        List<PlanetaryResourceSet.WeightedItemStack> bioItems = planetaryResourceSet.getBiologicalItems();
        List<PlanetaryResourceSet.WeightedFluidStack> bioFluids = planetaryResourceSet.getBiologicalFluids();
        if (bioItems.isEmpty() && bioFluids.isEmpty()) return;

        // Power check — skip if insufficient
        if (powerInsufficient) return;

        // Combined weighted random across biological items and fluids
        int itemWeight = bioItems.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        int fluidWeight = bioFluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        int totalWeight = itemWeight + fluidWeight;
        if (totalWeight <= 0) return;

        int roll = level.getRandom().nextInt(totalWeight);
        int cumulative = 0;

        // Check items first
        for (PlanetaryResourceSet.WeightedItemStack item : bioItems) {
            cumulative += item.weight();
            if (roll < cumulative) {
                // Output 1 item to logistics interface
                ItemLike itemLike = BuiltInRegistries.ITEM.get(item.itemId());
                if (itemLike.asItem() != Items.AIR) {
                    ItemStack output = new ItemStack(itemLike, 1);
                    List<IItemHandler> logistics = findLogisticsInterfaces();
                    if (!logistics.isEmpty()) {
                        int startIdx = ecoStationLogisticsRoundRobin % logistics.size();
                        for (int attempt = 0; attempt < logistics.size(); attempt++) {
                            int idx = (startIdx + attempt) % logistics.size();
                            IItemHandler handler = logistics.get(idx);
                            ItemStack remainder = insertIntoHandler(handler, output);
                            if (remainder.getCount() < output.getCount()) {
                                ecoStationLogisticsRoundRobin = (idx + 1) % logistics.size();
                                return;
                            }
                        }
                    }
                }
                return;
            }
        }

        // Check fluids
        for (PlanetaryResourceSet.WeightedFluidStack fluid : bioFluids) {
            cumulative += fluid.weight();
            if (roll < cumulative) {
                // Output 250mB to fluid interface
                var f = BuiltInRegistries.FLUID.get(fluid.fluidId());
                if (f != net.minecraft.world.level.material.Fluids.EMPTY) {
                    FluidStack output = new FluidStack(f, EXTRACTOR_FLUID_PER_TICK);
                    if (!output.isEmpty()) {
                        List<CelestialForgingAnvilFluidInterfaceBlockEntity> fluidIfs = findFluidInterfaces();
                        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidIf : fluidIfs) {
                            int filled = fluidIf.getFluidHandler().fill(output, IFluidHandler.FluidAction.EXECUTE);
                            if (filled > 0) return;
                        }
                    }
                }
                return;
            }
        }
    }

    // === Temple ===

    private static final String TEMPLE_MEGASTRUCTURE = "temple";
    private static final int TEMPLE_CYCLE_PUNISHMENT = 2;

    private void serverTickTemple() {
        if (level == null || level.isClientSide()) return;
        CelestialRefactorOption option = getActiveMegastructureOption();
        if (option == null) return;
        if (!TEMPLE_MEGASTRUCTURE.equals(option.megastructure())) return;
        if (planetaryResourceSet == null || !planetaryResourceSet.hasCivilization()) return;

        // Daily demand refresh (also triggers on first tick when demand is empty)
        long currentDay = level.getDayTime() / 24000;
        if (templeLastDay != currentDay || templeDemandItem.isEmpty()) {
            templeLastDay = currentDay;
            templeCycleDay = (templeCycleDay + 1) % 3;
            templeDemandSatisfied = false;
            // Pick new demand from recipes
            TempleDemandRecipe.Category cat = templeCycleDay == TEMPLE_CYCLE_PUNISHMENT
                                              ? TempleDemandRecipe.Category.PUNISHMENT
                                              : TempleDemandRecipe.Category.BLESSING;
            var demand = pickTempleDemand(cat);
            templeDemandItem = demand.item();
            templeDemandCount = demand.count();
            pushTempleDemandToLogistics();
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        // Check demand satisfaction
        if (!templeDemandSatisfied && !templeDemandItem.isEmpty()) {
            if (trySatisfyDemand()) {
                templeDemandSatisfied = true;
                pushTempleDemandToLogistics();
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        // Produce offerings if demand is satisfied
        if (templeDemandSatisfied) {
            produceTempleOfferings();
        }

        // Push demand state once per second as a fallback for newly-placed logistics interfaces
        if (level.getGameTime() % 20 == 0) {
            pushTempleDemandToLogistics();
        }
    }

    /**
     * Picked demand: item identity (count=1) and the total required count.
     */
    private record TempleDemandResult(ItemStack item, int count) {
        static final TempleDemandResult EMPTY = new TempleDemandResult(ItemStack.EMPTY, 0);
    }

    /**
     * Push current temple demand state to all connected logistics interfaces
     * so their tooltips show demand info. Called every tick when temple is active.
     */
    private void pushTempleDemandToLogistics() {
        if (level == null || level.isClientSide()) return;
        scanAdjacentBlocks((checkPos) -> {
            BlockEntity be = level.getBlockEntity(checkPos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logiBe) {
                logiBe.setTempleDemandItem(templeDemandSatisfied ? ItemStack.EMPTY : templeDemandItem);
                logiBe.setTempleDemandCount(templeDemandSatisfied ? 0 : templeDemandCount);
                logiBe.setTempleDemandSatisfied(templeDemandSatisfied);
                logiBe.setChanged();
            }
        });
    }

    private TempleDemandResult pickTempleDemand(TempleDemandRecipe.Category category) {
        if (level == null) return TempleDemandResult.EMPTY;
        var recipes = level.getRecipeManager()
            .getAllRecipesFor(dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes.TEMPLE_DEMAND_TYPE.get())
            .stream()
            .map(RecipeHolder::value)
            .toList();

        List<TempleDemandRecipe.Entry> candidates = new ArrayList<>();
        for (var recipe : recipes) {
            if (recipe.category() == category) {
                candidates.addAll(recipe.entries());
            }
        }
        if (candidates.isEmpty()) return TempleDemandResult.EMPTY;

        TempleDemandRecipe.Entry entry = candidates.get(level.getRandom().nextInt(candidates.size()));
        var item = BuiltInRegistries.ITEM.get(entry.itemResource());
        if (item == Items.AIR) return TempleDemandResult.EMPTY;
        return new TempleDemandResult(new ItemStack(item, 1), entry.count());
    }

    /**
     * Try to consume the demanded items from any connected logistics interface.
     */
    private boolean trySatisfyDemand() {
        if (templeDemandItem.isEmpty() || templeDemandCount <= 0) return false;
        List<IItemHandler> logistics = findLogisticsInterfaces();
        if (logistics.isEmpty()) return false;

        int remaining = templeDemandCount;
        for (IItemHandler handler : logistics) {
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack contained = handler.getStackInSlot(slot);
                if (ItemStack.isSameItemSameComponents(contained, templeDemandItem)) {
                    ItemStack extracted = handler.extractItem(slot, remaining, false);
                    remaining -= extracted.getCount();
                }
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    /**
     * Produce offerings: per gt, randomly picks one offering item (weighted)
     * and outputs 1 item to a logistics interface.
     */
    private void produceTempleOfferings() {
        List<PlanetaryResourceSet.WeightedItemStack> offerings = null;
        if (planetaryResourceSet != null) {
            offerings = planetaryResourceSet.getOfferings();
        }
        if (offerings != null && offerings.isEmpty()) return;

        int totalWeight = 0;
        if (offerings != null) {
            totalWeight = offerings.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        }
        if (totalWeight <= 0) return;

        int roll = 0;
        if (level != null) {
            roll = level.getRandom().nextInt(totalWeight);
        }
        int cumulative = 0;
        ResourceLocation chosenItem = null;
        for (PlanetaryResourceSet.WeightedItemStack offering : offerings) {
            cumulative += offering.weight();
            if (roll < cumulative) {
                chosenItem = offering.itemId();
                break;
            }
        }
        if (chosenItem == null) chosenItem = offerings.getFirst().itemId();

        ItemLike item = BuiltInRegistries.ITEM.get(chosenItem);
        if (item.asItem() == Items.AIR) return;
        ItemStack output = new ItemStack(item, 1);

        List<IItemHandler> logistics = findLogisticsInterfaces();
        if (logistics.isEmpty()) return;

        int startIdx = ecoStationLogisticsRoundRobin % logistics.size();
        for (int attempt = 0; attempt < logistics.size(); attempt++) {
            int idx = (startIdx + attempt) % logistics.size();
            IItemHandler handler = logistics.get(idx);
            ItemStack remainder = insertIntoHandler(handler, output);
            if (remainder.getCount() < output.getCount()) {
                ecoStationLogisticsRoundRobin = (idx + 1) % logistics.size();
                return;
            }
        }
    }
}
