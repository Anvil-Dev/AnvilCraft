package dev.dubhe.anvilcraft.item.property.consume;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModConsumeEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.util.Optional;

public class TeleportToDeathPointConsumeEffect implements ConsumeEffect {
    public static final TeleportToDeathPointConsumeEffect INSTANCE = new TeleportToDeathPointConsumeEffect();
    public static final MapCodec<TeleportToDeathPointConsumeEffect> CODEC = MapCodec.unit(
        TeleportToDeathPointConsumeEffect.INSTANCE);
    public static final StreamCodec<ByteBuf, TeleportToDeathPointConsumeEffect> STREAM_CODEC = StreamCodec.unit(
        TeleportToDeathPointConsumeEffect.INSTANCE);

    private static final double NEAR_DEATH_RANGE_SQR = 144.0;

    @Override
    public Type<TeleportToDeathPointConsumeEffect> getType() {
        return ModConsumeEffects.TP_TO_DEATH_POINT.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        if (!(user instanceof ServerPlayer player)) return true;
        player.fallDistance = 0;
        LevelData.RespawnData respawnData = TeleportToDeathPointConsumeEffect.respawnData(player);
        Optional<GlobalPos> lastDeath = player.getLastDeathLocation();
        if (lastDeath.isEmpty()) {
            TeleportToRespawnPointConsumeEffect.crossDimensionTeleportTo(
                respawnData.dimension(), player, respawnData.pos());
        } else {
            GlobalPos death = lastDeath.get();
            BlockPos deathPos = death.pos();
            boolean near = death.dimension().equals(player.level().dimension())
                && deathPos.distToLowCornerSqr(player.getX(), player.getY(), player.getZ())
                   < TeleportToDeathPointConsumeEffect.NEAR_DEATH_RANGE_SQR;
            if (near) {
                TeleportToRespawnPointConsumeEffect.crossDimensionTeleportTo(
                    respawnData.dimension(), player, respawnData.pos());
            } else {
                TeleportToRespawnPointConsumeEffect.crossDimensionTeleportTo(death.dimension(), player, deathPos);
            }
        }
        // 与 1.21.1 一致：落地扣 4 点摔落伤害
        player.hurtServer(player.level(), player.damageSources().fall(), 4.0F);
        return true;
    }

    private static LevelData.RespawnData respawnData(ServerPlayer player) {
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        return respawnConfig != null ? respawnConfig.respawnData() : player.level().getRespawnData();
    }
}