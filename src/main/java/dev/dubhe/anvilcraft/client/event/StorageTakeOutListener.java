package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.StoragePortTakeOutPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/** 仓储端口与创造板条箱共用长按计时，避免取少量物品时立即连续取出。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StorageTakeOutListener {
    private static @Nullable BlockEntity target;
    private static long startedAt;
    private static long lastTakeAt;

    private StorageTakeOutListener() {
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onAttack(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
            && event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            return;
        }
        BlockEntity next = findTarget();
        if (next == null || !next.getBlockPos().equals(event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (target != next) {
            target = next;
            startedAt = event.getLevel().getGameTime();
            take(startedAt);
        }
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Post event) {
        var key = Minecraft.getInstance().options.keyAttack.getKey();
        if (event.getAction() == InputConstants.RELEASE && key.getType() == InputConstants.Type.MOUSE
            && key.getValue() == event.getButton()) {
            target = null;
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        var key = Minecraft.getInstance().options.keyAttack.getKey();
        if (event.getAction() == InputConstants.RELEASE
            && key.getValue() == (key.getType() == InputConstants.Type.SCANCODE ? event.getScanCode() : event.getKey())
            && key.getType() != InputConstants.Type.MOUSE) {
            target = null;
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (!client.options.keyAttack.isDown() || target == null || target != findTarget()) {
            target = null;
            return;
        }
        if (client.level == null) {
            return;
        }
        long now = client.level.getGameTime();
        long held = now - startedAt;
        int interval = held < 20 ? 10 : held < 40 ? 5 : 2;
        if (now - lastTakeAt >= interval) {
            take(now);
        }
    }

    private static void take(long now) {
        if (target != null) {
            ClientPacketDistributor.sendToServer(new StoragePortTakeOutPacket(target.getBlockPos()));
            lastTakeAt = now;
        }
    }

    private static @Nullable BlockEntity findTarget() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.screen != null || client.player.isSpectator()
            || client.player.getMainHandItem().getItem() instanceof AnvilHammerItem
            || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockEntity entity = client.level.getBlockEntity(hit.getBlockPos());
        if (entity instanceof CreativeCrateBlockEntity crate && !crate.getDisplayStack().isEmpty()) {
            return entity;
        }
        if (client.level.getBlockState(hit.getBlockPos()).getBlock() instanceof AbstractStoragePortBlock port
            && port.interceptsLeftClick(client.level, hit.getBlockPos(), client.player, hit)) {
            return entity;
        }
        return null;
    }
}
