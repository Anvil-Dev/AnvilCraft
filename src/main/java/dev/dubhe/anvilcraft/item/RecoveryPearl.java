package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

import java.util.Optional;

public class RecoveryPearl extends Item {
    public RecoveryPearl(Properties properties) {
        super(properties);
    }

    @SuppressWarnings({"DataFlowIssue"})
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            MinecraftServer server = level.getServer();
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
            DimensionTransition dimensionTransition = serverPlayer.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);

            Optional<GlobalPos> lastDeathLocation = player.getLastDeathLocation();
            ResourceKey<Level> currentDimension = level.dimension();
            BlockPos currentPos = player.getOnPos();

            if (lastDeathLocation.isPresent()) {
                GlobalPos globalPos = lastDeathLocation.get();
                ResourceKey<Level> lastDeathDimension = globalPos.dimension();
                BlockPos lastDeathPos = globalPos.pos();
                if (currentDimension == lastDeathDimension) {
                    if (getDistance(currentPos, lastDeathPos) < 12) {
                        player.changeDimension(dimensionTransition);
                    } else {
                        player.teleportTo(lastDeathPos.getX(), lastDeathPos.getY(), lastDeathPos.getZ());
                    }
                } else {
                    crossDimensionTeleportTo(lastDeathDimension, player, lastDeathPos);
                }
            } else {
                player.changeDimension(dimensionTransition);
            }
            player.hurt(level.damageSources().fall(), 4);
        }

        player.getCooldowns().addCooldown(this, 20);
        itemStack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private double getDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(pos1.distToLowCornerSqr(pos2.getX(), pos2.getY(), pos2.getZ()));
    }

    public static void crossDimensionTeleportTo(ResourceKey<Level> dimension, Player player, BlockPos pos) {
        Level level = player.level();
        MinecraftServer server = level.getServer();
        if (server != null) {
            ServerLevel serverLevel = server.getLevel(dimension);
            if (serverLevel != null) {
                player.changeDimension(new DimensionTransition(serverLevel, player, DimensionTransition.DO_NOTHING));
                player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }
}
