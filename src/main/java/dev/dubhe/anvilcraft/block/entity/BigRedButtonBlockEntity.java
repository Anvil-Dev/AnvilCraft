package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.ButtonMount;
import dev.dubhe.anvilcraft.client.selection.ButtonOverhangPicking;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.ChromaticButtonMode;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BigRedButtonBlockEntity extends BlockEntity {
    private static final int HOLD_TIMEOUT = 20;
    private static final float SQUASH_TICKS = 4.5f;
    private static final float REBOUND_TICKS = 6.5f;
    private final Map<UUID, Long> holders = new HashMap<>();
    private float previousProgress;
    private @Nullable DataComponentMap indexedComponents;
    private @Nullable Direction indexedFacing;
    private @Nullable AABB indexedBounds;
    private boolean displayPowered;
    private boolean firstPowerCheck = true;
    private @Nullable SmartBlockPlacerDance placerDance;
    private float progress;
    private float squashStart;
    private float squashTarget;
    private float squashTangent;
    private float squashAge = REBOUND_TICKS;

    public BigRedButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_RED_BUTTON.get(), pos, state);
        this.progress = state.getValue(BigRedButtonBlock.PRESSED) ? 1 : 0;
        this.previousProgress = this.progress;
        this.squashStart = this.progress;
        this.squashTarget = this.progress;
    }

    public DyeColor getColor() {
        return this.components().getOrDefault(DataComponents.BASE_COLOR, DyeColor.RED);
    }

    public @Nullable ButtonCap getCap() {
        return this.components().get(ModComponents.BUTTON_CAP);
    }

    public @Nullable MountedButtonBlock getMountedBlock() {
        return MountedButtonBlock.get(this.components());
    }

    public boolean isDisplayPowered() {
        return this.displayPowered;
    }

    public double getPlacerDanceTime(float partialTick) {
        return this.placerDance == null ? 0 : this.placerDance.getRenderTime(partialTick, this.displayPowered);
    }

    public boolean isBareBase() {
        return this.getCap() == ButtonCap.BASE && this.getMountedBlock() == null;
    }

    public boolean mountBlock(MountedButtonBlock mounted) {
        if (!this.isBareBase()) {
            return false;
        }
        this.setCap(ButtonCap.BASE, mounted);
        return true;
    }

    public boolean stampCap(ButtonCap cap) {
        if (!cap.isHammerCap() || !this.isBareBase()) {
            return false;
        }
        this.setCap(cap, null);
        return true;
    }

    public boolean setMountedState(BlockState state) {
        MountedButtonBlock mounted = this.getMountedBlock();
        if (mounted == null || !state.is(mounted.state().getBlock())) {
            return false;
        }
        this.setCap(ButtonCap.BASE, mounted.withState(state));
        return true;
    }

    public boolean setChromaticMode(ChromaticButtonMode mode) {
        MountedButtonBlock mounted = this.getMountedBlock();
        if (mounted == null || !mounted.state().is(ModBlocks.CHROMATIC_STONE) || mounted.mode() == mode) {
            return false;
        }
        this.setCap(ButtonCap.BASE, mounted.withMode(mode));
        return true;
    }

    private void setCap(ButtonCap cap, @Nullable MountedButtonBlock mounted) {
        this.setComponents(DataComponentMap.builder().addAll(this.components())
            .set(ModComponents.BUTTON_CAP, cap).set(ModComponents.MOUNTED_BLOCK, mounted).build());
        this.setChanged();
        if (this.level != null) {
            if (!this.level.isClientSide()) {
                this.displayPowered = this.hasGridPower(this.level);
            }
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = this.saveWithoutMetadata(registries);
        if (this.displayPowered) {
            tag.putBoolean("GridPowered", true);
        }
        return tag;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.displayPowered = input.getBooleanOr("GridPowered", false);
        this.firstPowerCheck = true;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide() && this.getBlockState().getValue(BigRedButtonBlock.PRESSED)) {
            this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
        }
    }

    public boolean isCapHeldBy(Player player) {
        return !this.isBareBase() && this.getBlockState().getValue(BigRedButtonBlock.PRESSED)
            && this.holders.containsKey(player.getUUID());
    }

    public boolean press(Player player) {
        if (this.level == null || this.level.isClientSide() || this.isBareBase() || !player.isAlive() || player.isSpectator()
            || player.isShiftKeyDown() || player.getMainHandItem().is(ModItems.BUILDING_ROD)) {
            return false;
        }
        this.holders.put(player.getUUID(), this.level.getGameTime());
        this.setPressed(true);
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
        return true;
    }

    public void release(Player player) {
        if (this.level == null || this.level.isClientSide()) return;
        this.holders.remove(player.getUUID());
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
    }

    public void checkPressed() {
        if (this.level == null || this.level.isClientSide()) return;
        this.holders.entrySet().removeIf(entry -> {
            Player player = this.level.getPlayerByUUID(entry.getKey());
            return player == null || !player.isAlive() || player.isSpectator() || player.isShiftKeyDown()
                || player.getMainHandItem().is(ModItems.BUILDING_ROD)
                || this.getBlockState().getShape(this.level, this.worldPosition).bounds().move(this.worldPosition)
                    .distanceToSqr(player.getEyePosition()) > Mth.square(player.blockInteractionRange() + 1)
                || this.level.getGameTime() - entry.getValue() > HOLD_TIMEOUT;
        });
        this.setPressed(!this.holders.isEmpty());
        if (!this.holders.isEmpty()) {
            this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
        }
    }

    private void setPressed(boolean pressed) {
        if (this.level == null) return;
        BlockState state = this.getBlockState();
        if (state.getValue(BigRedButtonBlock.PRESSED) == pressed) return;
        this.level.setBlock(this.worldPosition, state.setValue(BigRedButtonBlock.PRESSED, pressed), 3);
        ((BigRedButtonBlock) state.getBlock()).updateNeighbours(state, this.level, this.worldPosition);
        this.level.playSound(
            null, this.worldPosition, pressed ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
            SoundSource.BLOCKS, 0.3f, pressed ? 0.6f : 0.5f
        );
        this.level.gameEvent(null, pressed ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, this.worldPosition);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BigRedButtonBlockEntity button) {
        if (level.isClientSide()) {
            button.updatePickBounds(level, state.getValue(BigRedButtonBlock.FACING));
            MountedButtonBlock mounted = button.getMountedBlock();
            if (mounted != null && mounted.state().is(ModBlocks.SMART_BLOCK_PLACER)) {
                if (button.placerDance == null) {
                    button.placerDance = new SmartBlockPlacerDance();
                }
                button.placerDance.tick(button.displayPowered);
            } else {
                button.placerDance = null;
            }
        }
        float target = state.getValue(BigRedButtonBlock.PRESSED) ? 1 : 0;
        button.previousProgress = button.progress;
        button.progress = Mth.approach(button.progress, target, 0.5f);
        float duration = button.squashDuration();
        button.squashAge = Math.min(button.squashAge + 1, duration);
        if (button.squashTarget != target) {
            float t = button.squashAge / duration;
            float position = button.getSquashProgress(0);
            float slope = Mth.HALF_PI * Mth.sin(Mth.PI * t);
            if (button.squashTarget == 0) {
                slope += 16 * t * (1 - t) * (1 - 2 * t);
            }
            // 两段行程耗时不同，先还原每刻速度，使改向时砧帽仍延续原来的动量。
            float velocity = ((button.squashTarget - button.squashStart) * slope
                + button.squashTangent * (1 - t) * (1 - 3 * t)) / duration;
            button.squashStart = position;
            button.squashTarget = target;
            button.squashTangent = velocity * button.squashDuration();
            button.squashAge = 0;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BigRedButtonBlockEntity button) {
        // 电网在 ServerTickEvent.Pre 结算，轮到方块实体时世界时间已增加一刻。
        if (!button.firstPowerCheck && (level.getGameTime() - 1) % PowerGrid.gridInterval() != 0) {
            return;
        }
        button.firstPowerCheck = false;
        boolean powered = button.hasGridPower(level);
        if (button.displayPowered != powered) {
            button.displayPowered = powered;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private boolean hasGridPower(Level level) {
        MountedButtonBlock mounted = this.getMountedBlock();
        return mounted != null && mounted.state().hasProperty(IPowerComponent.OVERLOAD)
            && PowerGrid.findPowerGridContains(level, this.worldPosition.getCenter()).filter(PowerGrid::isWorking).isPresent();
    }

    private void updatePickBounds(Level level, Direction facing) {
        if (this.indexedComponents == this.components() && this.indexedFacing == facing) {
            return;
        }
        this.indexedComponents = this.components();
        this.indexedFacing = facing;
        MountedButtonBlock mounted = this.getMountedBlock();
        AABB bounds = null;
        if (mounted != null) {
            ButtonMount mount = ButtonMount.forMounted(mounted, level.registryAccess());
            AABB envelope = mount.outline(facing, false).bounds().minmax(mount.outline(facing, true).bounds());
            if (envelope.minX < 0 || envelope.minY < 0 || envelope.minZ < 0
                || envelope.maxX > 1 || envelope.maxY > 1 || envelope.maxZ > 1) {
                bounds = envelope.move(this.worldPosition);
            }
        }
        ButtonOverhangPicking.update(level, this.worldPosition, this.indexedBounds, bounds);
        this.indexedBounds = bounds;
    }

    @Override
    public void setRemoved() {
        if (this.level != null && this.level.isClientSide() && this.indexedBounds != null) {
            ButtonOverhangPicking.update(this.level, this.worldPosition, this.indexedBounds, null);
        }
        this.indexedBounds = null;
        this.indexedComponents = null;
        super.setRemoved();
    }

    public float getPressProgress(float partialTick) {
        float value = Mth.lerp(partialTick, this.previousProgress, this.progress);
        return value * value * (3 - 2 * value);
    }

    public float getSquashProgress(float partialTick) {
        float t = (this.squashAge + partialTick) / this.squashDuration();
        if (t >= 1) {
            return this.squashTarget;
        }
        float weight = (1 - Mth.cos(Mth.PI * t)) * 0.5f;
        if (this.squashTarget == 0) {
            // 释放行程越过原高一次，首尾切线均为零，长按和松开后的静止轮廓仍然准确。
            float arc = t * (1 - t);
            weight += 8 * arc * arc;
        }
        return Mth.lerp(weight, this.squashStart, this.squashTarget) + this.squashTangent * t * (1 - t) * (1 - t);
    }

    private float squashDuration() {
        return this.squashTarget == 0 ? REBOUND_TICKS : SQUASH_TICKS;
    }
}
