package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.hud.BufferBootsChargeHUD;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BufferBootsScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static double baseY;
    private static double peakY;

    @SubscribeEvent
    public static void swatches(RenderGuiLayerEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portBufferBootsScene") || stage != 15
            || !event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        for (int index = 0; index < 4; index++) {
            var graphics = event.getGuiGraphics();
            graphics.pose().pushMatrix();
            graphics.pose().translate(0, -(index + 4) * 12);
            BufferBootsChargeHUD.render(graphics, index == 3 ? 0.5F : (index + 1) / 4F, index == 3);
            graphics.pose().popMatrix();
        }
    }

    @SubscribeEvent
    public static void trackJump(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (stage == 13 && player != null) peakY = Math.max(peakY, player.getY());
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 120000;
        }
        if (failure != null) throw new IllegalStateException("缓冲靴客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("缓冲靴客户端验证超时：" + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> prepare(client));
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) return;
                checkModels(client);
                client.player.getInventory().setSelectedSlot(0);
                advance(15);
            }
            case 15 -> capture(client, "models-bars", 2);
            case 2 -> {
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(3);
            }
            case 3 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 1);
                advance(30);
            }
            case 30 -> capture(client, "wheel", 4);
            case 4 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(5);
            }
            case 5 -> {
                if (client.screen != null || client.player.getMainHandItem().getOrDefault(ModComponents.CHARGED_JUMP_ENABLED, true)) return;
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(6);
            }
            case 6 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(60);
            }
            case 60 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(7);
            }
            case 7 -> {
                if (client.screen != null
                    || !client.player.getMainHandItem().getOrDefault(ModComponents.CHARGED_JUMP_ENABLED, false)) return;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(8);
            }
            case 8 -> {
                if (!client.player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) return;
                baseY = client.player.getY();
                peakY = baseY;
                client.options.keyShift.setDown(true);
                advance(9);
            }
            case 9 -> {
                if (EquipmentAbilities.chargeTicks(client.player) != 20) return;
                capture(client, "charged", 10);
            }
            case 10 -> {
                client.options.keyShift.setDown(false);
                advance(11);
            }
            case 11 -> {
                if (!EquipmentAbilities.isChargeHeld(client.player)) return;
                require(EquipmentAbilities.chargeTicks(client.player) == 20, "松开后应保持满蓄力");
                capture(client, "held", 12);
            }
            case 12 -> {
                client.options.keyShift.setDown(true);
                advance(120);
            }
            case 120 -> {
                if (EquipmentAbilities.chargeTicks(client.player) != 20) return;
                client.options.keyShift.setDown(false);
                advance(121);
                next = 0;
            }
            case 121 -> {
                if (!EquipmentAbilities.isChargeHeld(client.player)) return;
                require(EquipmentAbilities.chargeTicks(client.player) == 20, "保持状态起跳前必须满蓄力");
                AnvilCraft.LOGGER.info("PORT_BOOTS_JUMP_CHARGE: {}", EquipmentAbilities.chargeTicks(client.player));
                client.options.keyJump.setDown(true);
                advance(13);
                next = 0;
            }
            case 13 -> {
                if (client.player.getY() > baseY + 0.1) client.options.keyJump.setDown(false);
                peakY = Math.max(peakY, client.player.getY());
                if (!client.player.onGround() || peakY <= baseY + 1) return;
                require(peakY - baseY > 4.2 && peakY - baseY < 4.6, "真实键盘跳跃高度异常：" + (peakY - baseY));
                require(EquipmentAbilities.chargeTicks(client.player) == 0, "实际起跳应消费蓄力");
                AnvilCraft.LOGGER.info("PORT_BUFFER_BOOTS_PASSED: models, HUD, wheel toggles, equip, held jump; height={}", peakY - baseY);
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown boots stage " + stage);
        }
    }

    private static void checkModels(Minecraft client) {
        for (var layers : java.util.List.of(ModelLayers.ARMOR_STAND_ARMOR, ModelLayers.ARMOR_STAND_SMALL_ARMOR)) {
            var original = new ArmorStandArmorModel(client.getEntityModels().bakeLayer(layers.get(EquipmentSlot.FEET)));
            var replacement = ModModelLayers.getEquipmentBootsModel(original);
            original.setupAnim(new ArmorStandRenderState());
            replacement.setupAnim(new ArmorStandRenderState());
            require(replacement == ModModelLayers.getEquipmentBootsModel(original), "靴子模型缓存必须复用");
            require(replacement.root().yScale == original.root().yScale
                && replacement.rightLeg.x == original.rightLeg.x && replacement.rightLeg.y == original.rightLeg.y
                && replacement.leftLeg.x == original.leftLeg.x && replacement.leftLeg.y == original.leftLeg.y,
                "靴子模型必须保留成年和小型盔甲架的根缩放及双腿原点");
        }
    }

    private static void prepare(Minecraft client) {
        try {
            var server = client.getSingleplayerServer();
            final var level = server.overworld();
            var player = server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(false);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeAllEffects();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            player.getInventory().setItem(0, ModItems.WEATHERPROOF_SPACESUIT_BOOTS.asStack());
            player.setItemSlot(EquipmentSlot.FEET, ModItems.BUFFER_BOOTS.asStack());
            for (int x = 17; x <= 23; x++) {
                for (int z = 8; z <= 15; z++) {
                    level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            for (int i = 0; i < 2; i++) {
                var stand = new ArmorStand(level, 19.5 + i * 2, 81, 10.5);
                stand.setYRot(0);
                stand.yRotO = 0;
                stand.yBodyRot = 0;
                stand.yBodyRotO = 0;
                stand.setNoBasePlate(true);
                stand.setShowArms(true);
                stand.setNoGravity(true);
                stand.setItemSlot(EquipmentSlot.FEET,
                    i == 0 ? ModItems.BUFFER_BOOTS.asStack() : ModItems.WEATHERPROOF_SPACESUIT_BOOTS.asStack());
                level.addFreshEntity(stand);
            }
            player.inventoryMenu.broadcastChanges();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 20.5 81 14.5 180 18");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
            prepared = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_BOOTS_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 200;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "boots-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
