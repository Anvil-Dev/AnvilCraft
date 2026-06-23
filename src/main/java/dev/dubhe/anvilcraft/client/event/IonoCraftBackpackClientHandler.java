package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 飘升机背包客户端处理器 — 负责飞行时的排气粒子效果。
 * 粒子位置跟随玩家身体模型（yBodyRot）旋转，与背包引擎模型实际位置一致。
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class IonoCraftBackpackClientHandler {
    private static final double SIDE_OFFSET = 0.3;
    private static final double BACK_OFFSET = 0.45;
    private static final double Y_OFFSET = 1.1;

    /** 服务器同步的正在用背包飞行的玩家 entityId 集合 */
    private static final Set<Integer> SYNCED_FLYING_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 由 {@code IonoCraftBackpackFlyingPacket} 在客户端调用，记录服务器同步的飞行状态。
     */
    public static void onFlyingSync(int playerId, boolean flying) {
        if (flying) {
            SYNCED_FLYING_PLAYERS.add(playerId);
        } else {
            SYNCED_FLYING_PLAYERS.remove(playerId);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (minecraft.isPaused()) return;
        if (!AnvilCraftClient.CONFIG.ionocraftBackpackExhaustParticlesEnabled) return;

        ClientLevel level = minecraft.level;
        LocalPlayer localPlayer = minecraft.player;
        boolean firstPerson = minecraft.options.getCameraType() == CameraType.FIRST_PERSON;

        for (Player player : level.players()) {
            // 自己的背包：第一人称时不显示粒子
            if (player == localPlayer && firstPerson) continue;

            if (player.isCreative() || player.isSpectator()) continue;

            ItemStack backpack = IonoCraftBackpackItem.getByPlayer(player);
            if (backpack.isEmpty()) continue;

            // 本地玩家用精确 abilities；远程玩家用服务器同步的精确状态
            boolean flying = player == localPlayer
                ? player.getAbilities().flying
                : SYNCED_FLYING_PLAYERS.contains(player.getId());
            if (!flying) continue;

            spawnExhaustParticles(level, player, level.random);
        }
    }

    private static void spawnExhaustParticles(ClientLevel level, Player player, RandomSource random) {
        float yawRad = (float) Math.toRadians(player.yBodyRot);
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        double backZ = -cosYaw;

        double[][] exhausts = {{SIDE_OFFSET, BACK_OFFSET}, {-SIDE_OFFSET, BACK_OFFSET}};

        for (double[] exhaust : exhausts) {
            double sideComp = exhaust[0];
            double backComp = exhaust[1];

            double worldX = player.getX() + sideComp * (-cosYaw) + backComp * sinYaw;
            double worldZ = player.getZ() + sideComp * (-sinYaw) + backComp * backZ;
            double worldY = player.getY() + Y_OFFSET;

            double velX = random.nextGaussian() * 0.02;
            double velY = -0.3 - random.nextFloat() * 0.3;
            double velZ = random.nextGaussian() * 0.02;

            for (int i = 0; i < 2; i++) {
                level.addParticle(
                    ModParticles.IONOCRAFT_BACKPACK_EXHAUST.get(),
                    true,
                    worldX + random.nextGaussian() * 0.08,
                    worldY + random.nextGaussian() * 0.05,
                    worldZ + random.nextGaussian() * 0.08,
                    velX, velY, velZ
                );
            }
        }
    }
}
