package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.power.transmitting.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.renderer.item.EquipmentPoweredProperty;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ChestFlightScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static volatile List<Integer> stands = List.of();
    private static int jumpTick;
    private static int afterJump;

    @SubscribeEvent
    public static void pulse(ClientTickEvent.Post event) {
        if (jumpTick == 0) return;
        var client = Minecraft.getInstance();
        jumpTick++;
        AnvilCraft.LOGGER.info("PORT_CHEST_PULSE: {}", jumpTick);
        if (jumpTick == 3 || jumpTick == 7) client.options.keyJump.setDown(false);
        if (jumpTick == 5) client.options.keyJump.setDown(true);
        if (jumpTick == 7) {
            jumpTick = 0;
            advance(afterJump);
        }
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 180000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            client.options.keyJump.setDown(false);
            throw new IllegalStateException("胸甲客户端验证失败，阶段 " + stage, failure);
        }
        if (capturing || jumpTick > 0 || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                AnvilCraftClient.CONFIG.weatherproofChestplateHud.hudScale = 1;
                AnvilCraftClient.CONFIG.weatherproofChestplateHud.hudX = 8;
                AnvilCraftClient.CONFIG.weatherproofChestplateHud.hudY = 8;
                server(client, () -> prepare(client));
                advance(1);
                next += 3000;
            }
            case 1 -> {
                if (!prepared || stands.size() != 4 || stands.stream().anyMatch(id -> client.level.getEntity(id) == null)) return;
                require(client.level.getEntity(stands.get(1)).getData(ModDataAttachments.IN_POWER_GRID), "盔甲架电网状态必须同步");
                for (int i = 0; i < stands.size(); i++) {
                    var stand = (ArmorStand) client.level.getEntity(stands.get(i));
                    var state = client.getEntityRenderDispatcher().getRenderer(stand).createRenderState(stand, 0);
                    require(Boolean.TRUE.equals(state.getRenderData(EquipmentPoweredProperty.IN_GRID)) == (i == 1),
                        "电网标志必须正确进入延后绘制使用的渲染快照");
                }
                capture(client, "models-hud", 2);
            }
            case 2 -> {
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    final var level = server.overworld();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    level.setBlock(new BlockPos(200, 120, 40), ModBlocks.CREATIVE_GENERATOR.getDefaultState(), Block.UPDATE_ALL);
                    BlockPos pole = new BlockPos(202, 120, 40);
                    for (Vertical3PartHalf part : Vertical3PartHalf.values()) {
                        level.setBlock(pole.offset(part.getOffset()), ModBlocks.TRANSMISSION_POLE.getDefaultState()
                            .setValue(TransmissionPoleBlock.HALF, part), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                    player.setNoGravity(false);
                    player.setItemSlot(EquipmentSlot.CHEST, ModItems.IONOCRAFT_BACKPACK.asStack());
                    player.getInventory().setItem(0, ModItems.IONOCRAFT_BACKPACK.asStack());
                    player.inventoryMenu.broadcastChanges();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 200.5 121 40.5 180 0");
                });
                advance(3);
            }
            case 3 -> {
                if (!client.player.getData(ModDataAttachments.IN_POWER_GRID) || !client.player.getAbilities().mayfly) return;
                client.player.getInventory().setSelectedSlot(0);
                capture(client, "grid-powered", 4);
            }
            case 4 -> doubleJump(client, 5);
            case 5 -> {
                if (!client.player.getAbilities().flying) return;
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                advance(6);
            }
            case 6 -> capture(client, "flight-exhaust", 7);
            case 7 -> {
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 260.5 140 40.5 180 0");
                });
                advance(8);
            }
            case 8 -> {
                if (!client.player.getData(ModDataAttachments.IONOCRAFT_DESCENT_AVAILABLE)
                    || !IonoCraftBackpackItem.isSlowFalling(client.player)) return;
                require(!client.player.getAbilities().flying, "离网应退出飞行");
                capture(client, "descent", 9);
            }
            case 9 -> doubleJump(client, 10);
            case 10 -> {
                if (IonoCraftBackpackItem.isSlowFalling(client.player)) return;
                require(client.player.getData(ModDataAttachments.IONOCRAFT_DESCENT_AVAILABLE), "主动关闭缓降不应丢失本次恢复资格");
                doubleJump(client, 11);
            }
            case 11 -> {
                if (!IonoCraftBackpackItem.isSlowFalling(client.player)) return;
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 200.5 123 40.5 180 0");
                });
                advance(12);
            }
            case 12 -> {
                if (!client.player.getAbilities().flying || !client.player.getData(ModDataAttachments.IN_POWER_GRID)) return;
                require(!IonoCraftBackpackItem.isSlowFalling(client.player), "重新入网必须移除缓降");
                AnvilCraft.LOGGER.info(
                    "PORT_CHEST_FLIGHT_PASSED: power textures, HUD, actual grid flight, exhaust, descent toggle and reentry");
                client.options.keyJump.setDown(false);
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown chest scene stage " + stage);
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        final var level = server.overworld();
        var player = server.getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.removeAllEffects();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
        ItemStack chest = ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack();
        chest.set(ModComponents.STORED_ENERGY, new StoredEnergy(80000000));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.getInventory().setItem(0, ModItems.CAPACITOR.asStack(3));
        player.getInventory().setItem(1, ModItems.SUPER_CAPACITOR.asStack(2));
        for (int x = 192; x <= 208; x++) {
            for (int z = 8; z <= 21; z++) level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            var stand = new ArmorStand(level, 195.5 + i * 3, 81, 10.5);
            stand.setYRot(180);
            stand.yRotO = 180;
            stand.yBodyRot = 180;
            stand.yBodyRotO = 180;
            stand.setNoBasePlate(true);
            stand.setShowArms(true);
            stand.setNoGravity(true);
            ItemStack worn = i < 2 ? ModItems.IONOCRAFT_BACKPACK.asStack() : ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE.asStack();
            if (i == 3) worn.set(ModComponents.STORED_ENERGY, new StoredEnergy(80000000));
            stand.setItemSlot(EquipmentSlot.CHEST, worn);
            level.addFreshEntity(stand);
            stand.setData(ModDataAttachments.IN_POWER_GRID, i == 1);
            ids.add(stand.getId());
        }
        stands = List.copyOf(ids);
        player.inventoryMenu.broadcastChanges();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 200.5 81 19.5 180 0");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
        prepared = true;
    }

    private static void doubleJump(Minecraft client, int nextStage) {
        afterJump = nextStage;
        jumpTick = 1;
        client.options.keyJump.setDown(true);
    }

    private static void server(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_CHEST_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 800;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "chest-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
