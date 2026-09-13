package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageCraftingInputClientChecks {
    private static boolean requested;
    private static boolean done;
    private static Throwable failure;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (failure != null) throw new IllegalStateException("合成输入客户端检查失败", failure);
        if (!requested) {
            requested = true;
            StorageClientStub.craftingPutCraftingSlot(corePos, 0, 0, new ItemStack(Items.STICK, 5)).thenComposeAsync(result -> {
                if (!result.changed() || !result.carried().isEmpty()) throw new IllegalStateException("空格存入失败");
                return StorageClientStub.craftingPutCraftingSlot(corePos, 0, 1, ItemStack.EMPTY);
            }, client).thenComposeAsync(result -> {
                if (!result.carried().is(Items.STICK) || result.carried().getCount() != 3) {
                    throw new IllegalStateException("右键半堆返回错误");
                }
                return StorageClientStub.craftingPutCraftingSlot(corePos, 0, 1, result.carried());
            }, client).thenComposeAsync(result -> {
                if (result.carried().getCount() != 2) throw new IllegalStateException("右键放入数量错误");
                return StorageClientStub.craftingPutStonecutterInput(corePos, 0, new ItemStack(Items.DIAMOND));
            }, client).thenComposeAsync(result -> {
                if (result.changed()) throw new IllegalStateException("切石输入错误接受钻石");
                return StorageClientStub.craftingPutStonecutterInput(corePos, 0, new ItemStack(Items.ANDESITE, 5));
            }, client).thenComposeAsync(result -> {
                if (!result.carried().is(Items.STONE) || result.carried().getCount() != 12) {
                    throw new IllegalStateException("切石输入交换未返还原材料");
                }
                return StorageClientStub.craftingStonecutterRecipes(corePos);
            }, client).thenComposeAsync(recipes -> {
                if (recipes.isEmpty()) throw new IllegalStateException("切石候选结果未通过 RPC 返回");
                return StorageClientStub.craftingClearToStorage(corePos);
            }, client).thenComposeAsync(changed -> {
                if (!changed) throw new IllegalStateException("清空输入失败");
                return StorageClientStub.craftingGet(corePos);
            }, client).whenCompleteAsync((state, error) -> {
                failure = error;
                if (error == null) {
                    if (!state.stonecutterInput().isEmpty() || !state.craftingInput().stream().allMatch(ItemStack::isEmpty)
                        || !state.autoFill() || !state.toStorage() || !state.lastOpened()) {
                        failure = new IllegalStateException("清空后状态错误");
                    }
                    done = true;
                }
            }, client);
        }
        if (done && failure == null) {
            AnvilCraft.LOGGER.info("PORT_CRAFTING_INPUT_CLIENT_PASSED: slot exchange, recipe list and clear-to-storage");
            client.stop();
        }
    }
}
