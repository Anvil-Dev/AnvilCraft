package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.client.event.BigRedButtonInputListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public final class ButtonInteractionClientScene {
    private static final BlockPos POS = new BlockPos(0, 81, 4);
    private static int stage;
    private static long deadline;
    private static volatile boolean ready;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 90000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Button scene stage " + stage);
        if (client.screen != null || client.getOverlay() != null) return;
        if (!client.isWindowActive()) {
            GLFW.glfwFocusWindow(client.getWindow().handle());
            return;
        }
        client.player.setNoGravity(true);
        client.player.setPos(0.5, 82, 7);
        var delta = POS.getCenter().subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        client.hitResult = new BlockHitResult(POS.getCenter(), Direction.UP, POS, false);
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.overworld().setBlockAndUpdate(POS, ModBlocks.BIG_RED_BUTTON.getDefaultState());
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setPos(0.5, 82, 7);
                    player.setNoGravity(true);
                    player.getInventory().clearContent();
                    player.inventoryMenu.broadcastChanges();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    ready = true;
                });
                stage = 1;
            }
            case 1 -> {
                if (!ready || !client.level.getBlockState(POS).is(ModBlocks.BIG_RED_BUTTON)) return;
                client.options.keyUse.setDown(true);
                use(client, true);
                stage = 2;
            }
            case 2 -> {
                if (!pressed(client)) return;
                client.options.keyShift.setDown(true);
                stage = 3;
            }
            case 3 -> {
                if (!client.player.isShiftKeyDown() || pressed(client)) return;
                use(client, false);
                client.options.keyUse.setDown(false);
                client.options.keyShift.setDown(false);
                equip(client, true);
                stage = 4;
            }
            case 4 -> {
                if (client.player.isShiftKeyDown() || !client.player.getMainHandItem().is(ModItems.BUILDING_ROD)) return;
                use(client, false);
                equip(client, false);
                stage = 5;
            }
            case 5 -> {
                if (!client.player.getOffhandItem().is(ModItems.BUILDING_ROD)) return;
                use(client, false);
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
                    player.inventoryMenu.broadcastChanges();
                });
                stage = 6;
            }
            case 6 -> {
                if (!client.player.getMainHandItem().is(Items.STONE) || !client.player.getOffhandItem().isEmpty()) return;
                client.options.keyUse.setDown(true);
                use(client, true);
                stage = 7;
            }
            case 7 -> {
                if (!pressed(client)) return;
                client.options.keyUse.setDown(false);
                stage = 8;
            }
            case 8 -> {
                if (pressed(client)) return;
                AnvilCraft.LOGGER.info("PORT_BUTTON_INTERACTION_PASSED: normal hold, crouch release, "
                    + "both rod hands and ordinary-item hold/release");
                client.stop();
                stage = 9;
            }
            default -> {
            }
        }
    }

    private static void equip(Minecraft client, boolean mainHand) {
        client.getSingleplayerServer().execute(() -> {
            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHand ? ModItems.BUILDING_ROD.asStack() : ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, mainHand ? ItemStack.EMPTY : ModItems.BUILDING_ROD.asStack());
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static boolean pressed(Minecraft client) {
        return client.level.getBlockState(POS).getValue(BigRedButtonBlock.PRESSED);
    }

    private static void use(Minecraft client, boolean intercept) {
        var event = new InputEvent.InteractionKeyMappingTriggered(1, client.options.keyUse, InteractionHand.MAIN_HAND);
        BigRedButtonInputListener.onUse(event);
        if (event.isCanceled() != intercept) throw new IllegalStateException("Button input interception at stage " + stage);
    }
}
