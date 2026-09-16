package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageCraftingPickupScene {
    private static StorageScreen screen;
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static int stage;
    private static int frames;
    private static boolean capturing;

    public static void frame(Minecraft client, BlockPos corePos, StorageScreen current) {
        screen = current;
        if (!started) {
            started = true;
            prepare(client, corePos, false);
        }
        if (failure != null) throw new IllegalStateException("双击收集界面验证失败", failure);
        if (!prepared || capturing) return;
        if (++frames > 1000) throw new IllegalStateException("双击收集超时：" + stage);
        if (frames < 40 || (boolean) field("interactionPending") || (int) field("queuedCraftingPickup") >= -1) return;
        switch (stage) {
            case 0, 4, 7 -> {
                set("carried", ItemStack.EMPTY);
                client.player.inventoryMenu.setCarried(ItemStack.EMPTY);
                invoke("refreshCrafting");
                advance(stage + 1);
            }
            case 1 -> {
                click(51, 206);
                click(51, 206);
                if ((int) field("queuedCraftingPickup") != 9) throw new IllegalStateException("快速双击未在首次请求完成前排队");
                advance(2);
            }
            case 2 -> {
                if (!carried().is(Items.STONE) || carried().getCount() != 22) throw new IllegalStateException("合成区双击数量错误");
                var state = (CraftingStorage) field("crafting");
                if (!state.stonecutterInput().isEmpty() || !state.craftingInput().stream().allMatch(ItemStack::isEmpty)) {
                    throw new IllegalStateException("输入槽收集不完整");
                }
                if (client.player.getInventory().getItem(10).getCount() != 11) throw new IllegalStateException("不应收集异组件物品");
                capture(client, "crafting", 3);
            }
            case 3 -> {
                prepare(client, corePos, true);
                advance(4);
            }
            case 5 -> {
                click(122, 148);
                click(122, 148);
                advance(6);
            }
            case 6 -> {
                if (!carried().is(Items.STONE) || carried().getCount() != 23) throw new IllegalStateException("背包双击未补入合成区物品");
                prepare(client, corePos, false);
                advance(7);
            }
            case 8 -> {
                click(51, 206);
                click(51, 206);
                screen.onClose();
                if (client.screen != screen) throw new IllegalStateException("收集未完成时不能丢弃指针状态");
                advance(9);
            }
            case 9 -> {
                if (client.screen != null) return;
                int count = 0;
                for (int slot = 0; slot < 36; slot++) {
                    var stack = client.player.getInventory().getItem(slot);
                    if (stack.is(Items.STONE) && !stack.has(DataComponents.CUSTOM_NAME)) count += stack.getCount();
                }
                if (count != 22 || !client.player.inventoryMenu.getCarried().isEmpty()) {
                    throw new IllegalStateException("关闭后物品未完整返还背包：" + count);
                }
                AnvilCraft.LOGGER.info("PORT_CRAFTING_PICKUP_SCENE_PASSED: queued double-click, inventory supplement, close conservation");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown pickup stage " + stage);
        }
    }

    private static void prepare(Minecraft client, BlockPos corePos, boolean inventory) {
        prepared = false;
        client.getSingleplayerServer().execute(() -> {
            try {
                var server = client.getSingleplayerServer();
                var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                var storage = Storages.get().get(core.getId()).orElseThrow();
                var state = CraftingStorage.EMPTY.withLastOpened(true).withStonecutterInput(new ItemStack(Items.STONE, 5));
                state = inventory ? state.withCraftingSlot(2, new ItemStack(Items.STONE, 10))
                    : state.withCraftingSlot(0, new ItemStack(Items.STONE, 6)).withCraftingSlot(8, new ItemStack(Items.STONE, 4));
                storage.setCrafting(state);
                var player = server.getPlayerList().getPlayers().getFirst();
                for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                player.containerMenu.setCarried(ItemStack.EMPTY);
                player.getInventory().setItem(9, new ItemStack(Items.STONE, inventory ? 2 : 7));
                if (inventory) player.getInventory().setItem(10, new ItemStack(Items.STONE, 6));
                var named = new ItemStack(Items.STONE, 11);
                named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep components"));
                player.getInventory().setItem(inventory ? 11 : 10, named);
                player.inventoryMenu.broadcastChanges();
                prepared = true;
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static ItemStack carried() {
        return (ItemStack) field("carried");
    }

    private static Object field(String name) {
        try {
            var field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void set(String name, Object value) {
        try {
            var field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(screen, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void invoke(String name) {
        try {
            var method = StorageScreen.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void click(int x, int y) {
        var event = new MouseButtonEvent((int) field("left") + x, (int) field("top") + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_CRAFTING_PICKUP_STAGE: {} -> {}", stage, next);
        stage = next;
        frames = 0;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "crafting-pickup-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
