package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.util.WeaponRaycastUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.Tags;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class EnergyWeaponUseHUD {
    private static final int DAMAGE_STAGE_TICKS = 100;
    private static final int MAX_DAMAGE_STAGE = 4;
    private static final LaserProgressState LASER_PROGRESS = new LaserProgressState();

    private EnergyWeaponUseHUD() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isUsingItem()) {
            LASER_PROGRESS.reset();
            return;
        }

        ItemStack stack = player.getUseItem();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());
        float progress;
        if (stack.getItem() instanceof AnvilRailgunItem) {
            LASER_PROGRESS.reset();
            if (AnvilRailgunItem.isLoading(player, stack, player.getUsedItemHand())) return;
            int elapsed = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
            progress = AnvilRailgunItem.chargeProgress(player.level(), stack, elapsed, partialTick);
        } else if (stack.getItem() instanceof LaserGunItem) {
            progress = LASER_PROGRESS.get(player, stack, partialTick);
        } else {
            LASER_PROGRESS.reset();
            return;
        }

        if (progress <= 0.0F) return;
        AnvilHammerUseHUD.renderRing(
            graphics,
            24,
            minecraft.getWindow().getGuiScaledHeight() - 24,
            Math.clamp(progress, 0.0F, 1.0F)
        );
    }

    private static final class LaserProgressState {
        private int useStartTick = Integer.MIN_VALUE;
        private int lastElapsed;
        private int miningTicks;
        private int targetTicks;
        private UUID target;
        private Block veinBlock;
        private BlockPos veinAnchor;
        private final Set<BlockPos> vein = new HashSet<>();

        private float get(LocalPlayer player, ItemStack stack, float partialTick) {
            int elapsed = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
            int currentUseStartTick = player.tickCount - elapsed;
            if (currentUseStartTick != this.useStartTick || elapsed < this.lastElapsed) {
                this.reset();
                this.useStartTick = currentUseStartTick;
            }
            int elapsedDelta = Math.max(0, elapsed - this.lastElapsed);
            this.lastElapsed = elapsed;

            WeaponRaycastUtil.Ray ray = WeaponRaycastUtil.ray(player, 48.0);
            List<LivingEntity> targets = WeaponRaycastUtil.livingEntities(player.level(), player, ray, 1);
            if (!targets.isEmpty()) {
                this.resetMining();
                UUID currentTarget = targets.getFirst().getUUID();
                if (!currentTarget.equals(this.target)) {
                    this.target = currentTarget;
                    this.targetTicks = 0;
                }
                this.targetTicks += elapsedDelta;
                if (this.targetTicks >= DAMAGE_STAGE_TICKS * MAX_DAMAGE_STAGE) return 1.0F;
                return ((this.targetTicks % DAMAGE_STAGE_TICKS) + partialTick) / DAMAGE_STAGE_TICKS;
            }

            this.target = null;
            this.targetTicks = 0;
            BlockHitResult hit = player.level().clip(new ClipContext(
                ray.start(), ray.end(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (!this.vein.isEmpty()
                && (hit.getType() == HitResult.Type.MISS || !hit.getBlockPos().equals(this.veinAnchor))) {
                this.resetMining();
            }
            this.removeMinedPositions(player);
            boolean acquired = this.vein.isEmpty() && this.findAimedVein(player, hit);
            if (this.vein.isEmpty()) {
                this.miningTicks = 0;
                return -1.0F;
            }
            this.miningTicks = acquired ? elapsedDelta : this.miningTicks + elapsedDelta;

            int period = LaserGunItem.miningPeriod(player.level(), stack);
            return ((this.miningTicks % period) + partialTick) / period;
        }

        private boolean findAimedVein(LocalPlayer player, BlockHitResult hit) {
            if (hit.getType() == HitResult.Type.MISS) return false;
            BlockPos origin = hit.getBlockPos();
            BlockState ore = player.level().getBlockState(origin);
            if (!ore.is(Tags.Blocks.ORES)) return false;
            this.veinBlock = ore.getBlock();
            this.veinAnchor = origin.immutable();
            this.vein.addAll(LaserGunItem.findVein(
                player.level(),
                origin,
                ore,
                AnvilCraft.CONFIG.laserOreClusterMaxSize,
                player.position()
            ));
            return !this.vein.isEmpty();
        }

        private void removeMinedPositions(LocalPlayer player) {
            this.vein.removeIf(pos -> !player.level().getBlockState(pos).is(this.veinBlock));
            if (this.vein.isEmpty()) {
                this.veinBlock = null;
                this.veinAnchor = null;
            }
        }

        private void resetMining() {
            this.miningTicks = 0;
            this.veinBlock = null;
            this.veinAnchor = null;
            this.vein.clear();
        }

        private void reset() {
            this.useStartTick = Integer.MIN_VALUE;
            this.lastElapsed = 0;
            this.resetMining();
            this.targetTicks = 0;
            this.target = null;
        }
    }
}
