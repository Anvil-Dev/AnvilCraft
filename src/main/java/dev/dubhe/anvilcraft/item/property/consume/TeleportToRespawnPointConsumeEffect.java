package dev.dubhe.anvilcraft.item.property.consume;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModConsumeEffects;
import dev.dubhe.anvilcraft.init.item.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.LevelData;

import java.util.Optional;
import java.util.Set;

public class TeleportToRespawnPointConsumeEffect implements ConsumeEffect {
    public static final TeleportToRespawnPointConsumeEffect INSTANCE = new TeleportToRespawnPointConsumeEffect();
    public static final MapCodec<TeleportToRespawnPointConsumeEffect> CODEC = MapCodec.unit(TeleportToRespawnPointConsumeEffect.INSTANCE);
    public static final StreamCodec<ByteBuf, TeleportToRespawnPointConsumeEffect> STREAM_CODEC = StreamCodec.unit(
        TeleportToRespawnPointConsumeEffect.INSTANCE);

    @Override
    public Type<TeleportToRespawnPointConsumeEffect> getType() {
        return ModConsumeEffects.TP_TO_RESPAWN.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        if (!(user instanceof ServerPlayer player)) return true;
        player.fallDistance = 0;
        player.awardStat(Stats.ITEM_USED.get(ModItems.TOTEM_OF_RECOVERY.get()), 1);
        CriteriaTriggers.USED_TOTEM.trigger(player, ModItems.TOTEM_OF_RECOVERY.asStack());
        user.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        player.getInventory().add(ModItems.RECOVERY_PEARL.asStack());
        ResourceKey<Level> deathDimension = player.level().dimension();
        BlockPos deathPos = player.getOnPos();
        if (deathDimension == Level.OVERWORLD) {
            if (deathPos.getY() < -64) {
                deathPos = deathPos.atY(-63);
            }
        } else {
            if (deathPos.getY() < 0) {
                deathPos = deathPos.atY(1);
            }
        }
        deathPos = deathPos.atY(deathPos.getY() + 1);
        player.setLastDeathLocation(Optional.of(GlobalPos.of(deathDimension, deathPos)));
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        LevelData.RespawnData respawnData;
        if (respawnConfig != null) {
            respawnData = respawnConfig.respawnData();
        } else {
            respawnData = player.level().getRespawnData();
        }
        ResourceKey<Level> respawnDimension = respawnData.dimension();
        BlockPos respawnPos = respawnData.pos();
        TeleportToRespawnPointConsumeEffect.crossDimensionTeleportTo(respawnDimension, player, respawnPos);
        return true;
    }

    public static void crossDimensionTeleportTo(ResourceKey<Level> dimension, Player player, BlockPos pos) {
        Level level = player.level();
        MinecraftServer server = level.getServer();
        if (server != null) {
            ServerLevel serverLevel = server.getLevel(dimension);
            if (serverLevel != null) {
                player.teleportTo(
                    serverLevel,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    Set.of(),
                    player.getYRot(),
                    player.getXRot(),
                    true
                );
            }
        }
    }
}
