package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BundleActionScene {
    private static boolean started;
    private static int insertSounds;
    private static int removeSounds;
    private static boolean originalInverted;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static AbstractContainerScreen<?> screen;
    private static int stage;
    private static long nextAction;
    private static boolean capturing;

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (!started) return;
        var sound = event.getOriginalSound().getIdentifier();
        if (sound.equals(SoundEvents.BUNDLE_INSERT.location())) insertSounds++;
        if (sound.equals(SoundEvents.BUNDLE_REMOVE_ONE.location())) removeSounds++;
    }

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            originalInverted = AnvilCraftClient.CONFIG.invertOverrideAction;
            AnvilCraftClient.CONFIG.invertOverrideAction = false;
            prepare(client, false);
        }
        if (failure != null) throw new IllegalStateException("收纳盒客户端验证失败", failure);
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                if (client.player.hasInfiniteMaterials() || !client.player.getInventory().getItem(10).is(ModFoodItems.PILL)) return;
                client.options.guiScale().set(2);
                client.resizeGui();
                screen = new InventoryScreen(client.player);
                client.setScreen(screen);
                advance(1);
            }
            case 1 -> {
                click(client, 10, 0);
                advance(2);
            }
            case 2 -> {
                require(carried().is(ModFoodItems.PILL) && carried().getCount() == 5, "生存背包未拿起药片");
                click(client, 9, 1);
                advance(3);
            }
            case 3 -> {
                require(carried().isEmpty() && pills(client.player.getInventory().getItem(9)) == 5, "默认右键收纳预测或服务端同步失败");
                require(insertSounds == 1, "生存收纳音效必须恰好播放一次");
                capture(client, "survival", 4);
            }
            case 4 -> {
                AnvilCraftClient.CONFIG.invertOverrideAction = true;
                advance(5);
            }
            case 5 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    if (!BundleLikeServerStub.isInvertedAction(player)) failure = new IllegalStateException("反转配置未通过 RPC 同步");
                });
                click(client, 9, 1);
                advance(6);
            }
            case 6 -> {
                require(carried().is(ModFoodItems.PILL) && carried().getCount() == 5, "反转后右键取出失败");
                click(client, 9, 0);
                advance(7);
            }
            case 7 -> {
                require(carried().isEmpty() && pills(client.player.getInventory().getItem(9)) == 5, "反转后左键收纳未生效");
                screen.onClose();
                prepare(client, true);
                advance(8);
            }
            case 8 -> {
                if (!client.player.hasInfiniteMaterials() || pills(client.player.getInventory().getItem(9)) != 0) return;
                var creative = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(creative);
                try {
                    var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                    method.setAccessible(true);
                    method.invoke(creative, BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
                screen = creative;
                advance(9);
            }
            case 9 -> {
                click(client, 9, 0);
                advance(10);
            }
            case 10 -> {
                require(carried().is(ModItems.PILL_BOX), "创造背包未拿起药盒");
                click(client, 10, 0);
                advance(11);
            }
            case 11 -> {
                require(pills(carried()) == 5 && client.player.getInventory().getItem(10).isEmpty(), "创造背包本地收纳未执行");
                click(client, 13, 1);
                advance(12);
            }
            case 12 -> {
                require(pills(carried()) == 0 && client.player.getInventory().getItem(13).getCount() == 5,
                    "创造背包本地取出未执行或数量错误");
                click(client, 9, 0);
                advance(13);
            }
            case 13 -> {
                click(client, 12, 0);
                advance(14);
            }
            case 14 -> {
                require(carried().is(Items.TOTEM_OF_UNDYING), "未拿起图腾");
                click(client, 11, 0);
                advance(15);
            }
            case 15 -> {
                require(carried().isEmpty() && client.player.getInventory().getItem(11)
                    .getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).usage() == 1, "创造背包护符盒收纳失败");
                capture(client, "creative", 16);
            }
            case 16 -> {
                prepared = false;
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        require(player.getInventory().getItem(9).is(ModItems.PILL_BOX) && pills(player.getInventory().getItem(9)) == 0
                            && player.getInventory().getItem(13).getCount() == 5, "创造模式药片与盒子没有正确同步回服务端");
                        require(player.getInventory().getItem(12).isEmpty() && player.getInventory().getItem(11)
                            .getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).usage() == 1, "创造模式护符盒服务端内容错误");
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(17);
            }
            case 17 -> {
                require(insertSounds == 4 && removeSounds == 2, "收纳音效缺失或重复：" + insertSounds + "/" + removeSounds);
                AnvilCraftClient.CONFIG.invertOverrideAction = originalInverted;
                AnvilCraft.LOGGER.info("PORT_BUNDLE_ACTION_SCENE_PASSED: survival packets, inverted RPC, creative inventory, conservation");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown bundle stage " + stage);
        }
    }

    private static void prepare(Minecraft client, boolean creative) {
        prepared = false;
        client.getSingleplayerServer().execute(() -> {
            try {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
                player.setNoGravity(true);
                player.getAbilities().invulnerable = true;
                player.onUpdateAbilities();
                for (int x = 9; x <= 11; x++) {
                    for (int z = 14; z <= 16; z++) {
                        player.level().setBlock(new BlockPos(x, 85, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 10.5 86 15.5");

                for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                player.getInventory().setItem(9, new ItemStack(ModItems.PILL_BOX.get()));
                player.getInventory().setItem(10, new ItemStack(ModFoodItems.PILL.get(), 5));
                player.getInventory().setItem(11, new ItemStack(ModItems.AMULET_BOX.get()));
                player.getInventory().setItem(12, new ItemStack(Items.TOTEM_OF_UNDYING));
                player.inventoryMenu.setCarried(ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                prepared = true;
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static ItemStack carried() {
        return screen.getMenu().getCarried();
    }

    private static int pills(ItemStack box) {
        return box.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY).pills().stream()
            .mapToInt(ItemStack::getCount).sum();
    }

    private static void click(Minecraft client, int index, int button) {
        var slot = screen.getMenu().getSlot(index);
        double x = screen.getLeftPos() + slot.x + 8;
        double y = screen.getTopPos() + slot.y + 8;
        try {
            var window = client.getWindow();
            var horizontal = MouseHandler.class.getDeclaredField("xpos");
            var vertical = MouseHandler.class.getDeclaredField("ypos");
            horizontal.setAccessible(true);
            vertical.setAccessible(true);
            horizontal.setDouble(client.mouseHandler, x * window.getScreenWidth() / window.getGuiScaledWidth());
            vertical.setDouble(client.mouseHandler, y * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_BUNDLE_ACTION_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "bundle-actions-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
