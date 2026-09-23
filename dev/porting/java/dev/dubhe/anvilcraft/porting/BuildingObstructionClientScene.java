package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.building.BuildingRodObstructionHighlight;
import dev.dubhe.anvilcraft.network.BuildingRodObstructionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class BuildingObstructionClientScene {
    private static int stage;
    private static volatile int entityId = -1;
    private static volatile Throwable failure;
    private static long deadline;
    private static long highlightedAt;
    private static boolean capturing;
    private static boolean captured;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 45000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Obstruction stage " + stage, failure);
        }
        client.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        client.player.setPos(8.5, 81.6, 11.5);
        client.player.setYRot(180);
        client.player.setXRot(25);
        client.options.hideGui = true;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var level = client.getSingleplayerServer().overworld();
                        var cow = EntityType.COW.create(level, EntitySpawnReason.LOAD);
                        cow.setPos(8.5, 81, 8.5);
                        cow.setNoAi(true);
                        cow.setNoGravity(true);
                        level.addFreshEntity(cow);
                        entityId = cow.getId();
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                stage = 1;
            }
            case 1 -> {
                if (client.level.getEntity(entityId) == null) return;
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    PacketDistributor.sendToPlayer(player, new BuildingRodObstructionPacket(List.of(entityId)));
                });
                stage = 2;
            }
            case 2 -> {
                var entity = client.level.getEntity(entityId);
                if (!BuildingRodObstructionHighlight.isHighlighted(entity)) return;
                if (!client.shouldEntityAppearGlowing(entity) || entity.isCurrentlyGlowing()
                    || client.shouldEntityAppearGlowing(client.player)) throw new IllegalStateException("Glow scope mismatch");
                highlightedAt = System.currentTimeMillis();
                stage = 3;
            }
            case 3 -> {
                if (!capturing && System.currentTimeMillis() - highlightedAt > 300) {
                    capturing = true;
                    Screenshot.grab(client.gameDirectory, "building-obstruction-26.1-glow.png", client.getMainRenderTarget(), 1,
                        message -> client.execute(() -> captured = true));
                }
                if (!captured || System.currentTimeMillis() - highlightedAt < 3500) return;
                var entity = client.level.getEntity(entityId);
                if (BuildingRodObstructionHighlight.isHighlighted(entity) || client.shouldEntityAppearGlowing(entity)) {
                    throw new IllegalStateException("Obstruction glow did not expire");
                }
                AnvilCraft.LOGGER.info("PORT_BUILDING_OBSTRUCTION_PASSED: real packet, native glow, scope, expiry");
                client.stop();
                stage = 4;
            }
            default -> {
            }
        }
    }
}
