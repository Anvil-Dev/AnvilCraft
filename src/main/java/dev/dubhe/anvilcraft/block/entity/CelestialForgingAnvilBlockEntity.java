package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.util.GravityManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CelestialForgingAnvilBlockEntity extends BlockEntity implements MenuProvider, IPowerConsumer {
    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
    private boolean isAmplify = false;

    @Getter @Setter
    @Nullable
    private CelestialBodyData celestialBodyData = null;

    @Getter @Setter
    private long bodySeed = 0;

    /** Mass anvil count at time of body matching, for gravity calculation. */
    @Getter @Setter
    private int stellarMass = 0;

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === IPowerConsumer ===

    @Override
    public int getInputPower() {
        if (!searching || searchTicksRemaining <= 0) return 0;
        return isAmplify ? 4000 : 1000;
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
        return IPowerConsumer.super.getComponentType();
    }

    private boolean hasEnoughPower() {
        if (grid == null) return false;
        int required = isAmplify ? 4000 : 1000;
        return grid.getRemaining() >= required;
    }

    @Getter
    private int bodyRotation = 0;

    @Getter @Setter
    private boolean locked = false;

    /** Whether the amplifier multiblock is physically formed. */
    @Getter @Setter
    private boolean amplifierPresent = false;

    // Search timer
    @Getter private int searchTicksRemaining = 0;
    @Getter private boolean searching = false;
    @Getter @Setter private boolean searchFailed = false;
    @Getter private boolean powerInsufficient = false;
    private static final int SEARCH_TICKS = 200;

    // Power grid
    @Setter
    @Nullable
    private PowerGrid grid;

    // Gravity source state
    private boolean gravitySourceActive = false;
    private double currentGravityStrength = 0;
    private int currentGravitySize = 0;
    /** Y-offset from controller block to the rendered star center. */
    private static final int GRAVITY_CENTER_Y_OFFSET = 6;
    /** Gravity influence radius (blocks), covers the Ring6 7×7×7 area. */
    private static final int GRAVITY_RADIUS = 3;
    /** Gravity strength formula scale factor. strength = stellarMass / size * SCALE. */
    private static final double GRAVITY_STRENGTH_SCALE = 20.0;

    public void startSearch() {
        this.searchFailed = false;
        this.powerInsufficient = false;

        // Server-side parameter pre-check
        if (level != null && !level.isClientSide()) {
            var preCheck = CelestialBodyMatcher.match(
                getAnvilCount(0), getAnvilCount(1), getAnvilCount(2), getAnvilCount(3),
                this.isAmplify, level.getRandom());
            if (preCheck == null) {
                this.searchFailed = true;
                this.searching = false;
                this.searchTicksRemaining = 0;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                return;
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
    }

    private void updateGravitySource() {
        if (level == null || level.isClientSide()) return;

        boolean shouldHaveGravity = amplifierPresent
            && celestialBodyData instanceof StarData
            && stellarMass > 0
            && celestialBodyData.size() > 0;

        double newStrength = shouldHaveGravity
            ? (stellarMass / (double) celestialBodyData.size()) * GRAVITY_STRENGTH_SCALE
            : 0;
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

    /** Search history, max 10 entries. Index 0 = newest. */
    @Getter
    private final List<CelestialBodyData> searchHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

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

    public void tryMatchCelestialBody() {
        if (level == null) return;
        int time = getAnvilCount(0);
        int space = getAnvilCount(1);
        int mass = getAnvilCount(2);
        int energy = getAnvilCount(3);
        this.bodySeed = level.getRandom().nextLong();
        this.stellarMass = mass;
        this.celestialBodyData = CelestialBodyMatcher.match(
            time, space, mass, energy, this.isAmplify, level.getRandom());
        if (this.celestialBodyData != null) {
            addToSearchHistory(this.celestialBodyData);
        } else {
            this.searchTicksRemaining = 0; // Stop timer on failure
        }
        if (!level.isClientSide()) {
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
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
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
        loadSearchHistory(tag);
        loadInventory(tag, registries);
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
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
        loadSearchHistory(tag);
        loadInventory(tag, lookupProvider);
    }

    private void loadInventory(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("anvils")) {
            CompoundTag invTag = tag.getCompound("anvils");
            for (int i = 0; i < 5; i++) {
                String key = "s" + i;
                this.anvilInventory.setItem(i,
                    invTag.contains(key)
                        ? ItemStack.parse(registries, invTag.get(key)).orElse(ItemStack.EMPTY)
                        : ItemStack.EMPTY);
            }
        }
    }

    public int getAnvilCount(int slot) {
        return this.anvilInventory.getItem(slot).getCount();
    }

    public void addToSearchHistory(CelestialBodyData data) {
        // Dedup: don't add if it's already the most recent entry
        if (!searchHistory.isEmpty()) {
            CelestialBodyData latest = searchHistory.get(0);
            if (latest.toTag().toString().equals(data.toTag().toString())) return;
        }
        searchHistory.add(0, data);
        while (searchHistory.size() > MAX_HISTORY) {
            searchHistory.remove(searchHistory.size() - 1);
        }
    }

    private void loadSearchHistory(CompoundTag tag) {
        searchHistory.clear();
        if (tag.contains("searchHistory")) {
            CompoundTag histTag = tag.getCompound("searchHistory");
            int size = Math.min(histTag.getInt("size"), MAX_HISTORY);
            for (int i = 0; i < size; i++) {
                if (histTag.contains("h" + i)) {
                    searchHistory.add(CelestialBodyData.fromTag(histTag.getCompound("h" + i)));
                }
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
}
