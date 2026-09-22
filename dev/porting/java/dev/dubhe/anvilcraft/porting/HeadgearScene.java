package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class HeadgearScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static float fogStart;
    private static boolean waterCanceled;
    private static boolean capturing;
    private static int stage;
    private static long next;
    private static long deadline;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fog(ViewportEvent.RenderFog event) {
        if (started) fogStart = event.getNearPlaneDistance();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void water(RenderBlockScreenEffectEvent event) {
        if (started && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER) waterCanceled = event.isCanceled();
    }

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            next = System.currentTimeMillis() + 1000;
            deadline = next + 180000;
        }
        if (failure != null) throw new IllegalStateException("头盔客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("头盔客户端验证超时：" + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> prepare(client));
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return;
                client.player.getInventory().setSelectedSlot(0);
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(2);
            }
            case 2 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 1);
                advance(3);
            }
            case 3 -> capture(client, "wheel", 4);
            case 4 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(5);
            }
            case 5 -> {
                if (client.screen != null || client.player.getMainHandItem().getOrDefault(ModComponents.NIGHT_VISION_ENABLED, true)) return;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(6);
            }
            case 6 -> {
                if (!client.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return;
                require(!client.player.hasEffect(MobEffects.NIGHT_VISION), "轮盘关闭状态必须保留到实际穿戴");
                checkModels(client);
                client.options.hideGui = true;
                advance(7);
            }
            case 7 -> capture(client, "models", 8);
            case 8 -> {
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    for (int x = 17; x <= 23; x++) {
                        for (int z = 8; z <= 15; z++) {
                            for (int y = 81; y <= 84; y++) {
                                level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                });
                advance(9);
                next += 1500;
            }
            case 9 -> {
                if (!client.player.isEyeInFluid(FluidTags.WATER)) return;
                require(fogStart >= 48 && waterCanceled, "全天候头盔必须清除水下近雾和水遮罩");
                capture(client, "clear-water", 10);
            }
            case 10 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack());
                });
                advance(11);
            }
            case 11 -> {
                if (!client.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BREATHING_HELMET)) return;
                require(fogStart < 48 && !waterCanceled, "普通呼吸头盔不能获得全天候的清晰视野");
                capture(client, "ordinary-water", 12);
            }
            case 12 -> {
                client.options.hideGui = false;
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    var helmet = ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack();
                    helmet.set(ModComponents.NIGHT_VISION_ENABLED, false);
                    player.getInventory().setItem(0, helmet);
                    player.inventoryMenu.broadcastChanges();
                });
                advance(13);
            }
            case 13 -> {
                if (!client.player.getMainHandItem().is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return;
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(14);
            }
            case 14 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(15);
            }
            case 15 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(16);
            }
            case 16 -> {
                if (!client.player.getMainHandItem().getOrDefault(ModComponents.NIGHT_VISION_ENABLED, false)) return;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(17);
            }
            case 17 -> {
                if (!client.player.hasEffect(MobEffects.NIGHT_VISION)) return;
                client.options.hideGui = true;
                advance(18);
            }
            case 18 -> capture(client, "night-water", 19);
            case 19 -> {
                AnvilCraft.LOGGER.info(
                    "PORT_HEADGEAR_PASSED: worn models, ability wheel, actual equip, night vision, fluid fog and overlay");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown headgear stage " + stage);
        }
    }

    private static void checkModels(Minecraft client) {
        var adult = new ArmorStandArmorModel(client.getEntityModels().bakeLayer(ModelLayers.ARMOR_STAND_ARMOR.get(EquipmentSlot.HEAD)));
        var small = new ArmorStandArmorModel(
            client.getEntityModels().bakeLayer(ModelLayers.ARMOR_STAND_SMALL_ARMOR.get(EquipmentSlot.HEAD)));
        var adultHelmet = ModModelLayers.getEquipmentHelmetModel(adult);
        final var smallHelmet = ModModelLayers.getEquipmentHelmetModel(small);
        var state = new ArmorStandRenderState();
        adult.setupAnim(state);
        adultHelmet.setupAnim(state);
        small.setupAnim(state);
        smallHelmet.setupAnim(state);
        require(adultHelmet != smallHelmet && adultHelmet == ModModelLayers.getEquipmentHelmetModel(adult),
            "不同原版模型必须隔离延后绘制状态，同一原版模型应复用缓存");
        require(adultHelmet.head.y == adult.head.y && smallHelmet.head.y == small.head.y
            && adultHelmet.root().yScale == adult.root().yScale && smallHelmet.root().yScale == small.root().yScale,
            "头盔必须保留原版成年和小型盔甲架的骨骼原点及根节点缩放");
    }

    private static void prepare(Minecraft client) {
        try {
            var server = client.getSingleplayerServer();
            final var level = server.overworld();
            var player = server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.getAbilities().invulnerable = true;
            player.getAbilities().flying = false;
            player.setNoGravity(true);
            player.removeAllEffects();
            player.onUpdateAbilities();
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
            player.getInventory().setItem(0, ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack());
            for (int x = 17; x <= 23; x++) {
                for (int z = 8; z <= 15; z++) {
                    level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            for (int index = 0; index < 2; index++) {
                var stand = new ArmorStand(level, 19.5 + index * 2, 81, 10.5);
                stand.setYRot(0);
                stand.yRotO = 0;
                stand.yBodyRot = 0;
                stand.yBodyRotO = 0;
                stand.setNoBasePlate(true);
                stand.setShowArms(true);
                stand.setNoGravity(true);
                stand.setItemSlot(EquipmentSlot.HEAD, index == 0 ? ModItems.BREATHING_HELMET.asStack()
                    : ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack());
                level.addFreshEntity(stand);
            }
            player.inventoryMenu.broadcastChanges();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 20.5 81 12.5 180 0");
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
        AnvilCraft.LOGGER.info("PORT_HEADGEAR_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "headgear-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
