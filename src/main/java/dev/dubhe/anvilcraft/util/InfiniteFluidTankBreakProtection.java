package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 含无限液体储罐的玩家破坏保护。 */
public final class InfiniteFluidTankBreakProtection {
    private static final int CONFIRMATION_DURATION = 20 * 10;
    private static final int MODIFIER_AUTHORIZATION_DURATION = 5;
    private static final double INTERACTION_DISTANCE_TOLERANCE = 2.0;
    private static final Map<UUID, TimedTarget> CONFIRMATIONS = new HashMap<>();
    private static final Map<UUID, TimedTarget> MODIFIER_AUTHORIZATIONS = new HashMap<>();

    private InfiniteFluidTankBreakProtection() {
    }

    public static boolean isProtected(Level level, BlockPos pos) {
        return findProtectedTarget(level, pos).isPresent();
    }

    /**
     * 接收客户端挖掘时的组合键状态。授权只短暂保留，避免玩家松开按键后仍可破坏。
     */
    public static void updateModifierAuthorization(ServerPlayer player, BlockPos pos, boolean modifiersHeld) {
        UUID playerId = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();
        if (!modifiersHeld || !level.isLoaded(pos) || !isProtected(level, pos)) {
            MODIFIER_AUTHORIZATIONS.remove(playerId);
            return;
        }

        double maxDistance = player.blockInteractionRange() + INTERACTION_DISTANCE_TOLERANCE;
        if (player.distanceToSqr(pos.getCenter()) > maxDistance * maxDistance) {
            MODIFIER_AUTHORIZATIONS.remove(playerId);
            return;
        }

        MODIFIER_AUTHORIZATIONS.put(
            playerId,
            new TimedTarget(level.dimension(), pos.immutable(), level.getGameTime() + MODIFIER_AUTHORIZATION_DURATION)
        );
    }

    /**
     * 检查并处理一次玩家破坏。
     *
     * @return 是否应取消本次破坏
     */
    public static boolean shouldCancelBreak(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> protectedTarget = findProtectedTarget(level, pos);
        if (protectedTarget.isEmpty()) return false;

        UUID playerId = player.getUUID();
        long gameTime = level.getGameTime();
        TimedTarget confirmation = CONFIRMATIONS.get(playerId);
        TimedTarget authorization = MODIFIER_AUTHORIZATIONS.remove(playerId);
        boolean previouslyConfirmed = confirmation != null
            && confirmation.matches(level, protectedTarget.get(), gameTime);
        boolean modifiersAuthorized = authorization != null && authorization.matches(level, pos, gameTime);
        if (previouslyConfirmed && modifiersAuthorized) {
            CONFIRMATIONS.remove(playerId);
            return false;
        }

        CONFIRMATIONS.put(
            playerId,
            new TimedTarget(
                level.dimension(),
                protectedTarget.get(),
                gameTime + CONFIRMATION_DURATION
            )
        );
        String message = previouslyConfirmed
                         ? "screen.anvilcraft.tooltip.fluid_tank.break_modifiers"
                         : "screen.anvilcraft.tooltip.fluid_tank.break_confirm";
        player.sendOverlayMessage(Component.translatable(message).withStyle(ChatFormatting.RED));
        return true;
    }

    public static void showToolBreakDenied(Player player) {
        player.sendOverlayMessage(
            Component.translatable("screen.anvilcraft.tooltip.fluid_tank.tool_break_failed")
                .withStyle(ChatFormatting.RED)
        );
    }

    public static void clear(Player player) {
        UUID playerId = player.getUUID();
        CONFIRMATIONS.remove(playerId);
        MODIFIER_AUTHORIZATIONS.remove(playerId);
    }

    private static Optional<BlockPos> findProtectedTarget(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FluidTankBlockEntity tank && tank.containsInfiniteFluid()) {
            return Optional.of(pos.immutable());
        }
        if (blockEntity instanceof LargeFluidTankBlockEntity tank && tank.containsInfiniteFluid()) {
            return Optional.of(MultiPartBlockUtil.getMainPartPos(level, pos).immutable());
        }
        return Optional.empty();
    }

    private record TimedTarget(ResourceKey<Level> dimension, BlockPos pos, long validUntil) {
        private boolean matches(ServerLevel level, BlockPos targetPos, long gameTime) {
            return this.dimension.equals(level.dimension())
                && this.pos.equals(targetPos)
                && gameTime <= this.validUntil;
        }
    }
}
