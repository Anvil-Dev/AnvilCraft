package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(Minecraft.class)
abstract class MinecraftClientMixin extends ReentrantBlockableEventLoop<Runnable> {
    /** 中键取物从终端补库的进行中标志，防止连续点击重复取物。 */
    @Unique
    private static boolean anvilcraft$pickingFromTerminal;

    @Shadow
    public MultiPlayerGameMode gameMode;

    public MinecraftClientMixin(String name) {
        super(name);
    }

    @Inject(
        method = "updateLevelInEngines",
        at = @At("HEAD")
    )
    void updateLevel(ClientLevel level, CallbackInfo ci) {
        CacheableBERenderingPipeline.updateLevel(level);
    }

    @WrapOperation(
        method = "pickBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;"
                     + "getPickedResult(Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack preserveFluidTankMinecartDataWhenControlDown(
        Entity entity,
        HitResult hitResult,
        Operation<ItemStack> original
    ) {
        if (Screen.hasControlDown() && entity instanceof FluidTankMinecartEntity tankMinecart) {
            return tankMinecart.createDropStack();
        }
        return original.call(entity, hitResult);
    }

    /**
     * 中键取方块：背包没有对应物品时，从绑定终端存储中取一个补入背包后选中。
     * 仅生存模式（创造模式直接取无限物品，无需补库）；补库为异步，完成后在
     * 主线程重试选中逻辑（与 {@code pickBlock} 原逻辑一致）。
     */
    @WrapOperation(
        method = "pickBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;"
                     + "findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"
        )
    )
    private int anvilcraft$pickFromTerminal(
        Inventory inventory,
        ItemStack stack,
        Operation<Integer> original
    ) {
        int slot = original.call(inventory, stack);
        if (slot != -1 || inventory.player.getAbilities().instabuild) {
            return slot;
        }
        if (anvilcraft$pickingFromTerminal) {
            return slot; // 已有一次补库进行中，避免重复
        }
        List<UUID> storageIds = TerminalJeiStorageCache.boundStorages(inventory.player);
        if (storageIds.isEmpty()) {
            return slot;
        }
        anvilcraft$pickingFromTerminal = true;
        ItemStack need = stack.copyWithCount(stack.getMaxStackSize());
        StorageTerminalClientStub.withdrawToInventory(storageIds, List.of(need)).whenComplete((withdrawn, error) ->
            this.execute(() -> {
                try {
                    if (error != null) {
                        return; // 补库失败：保持原状
                    }
                    int found = inventory.findSlotMatchingItem(stack);
                    if (found == -1) {
                        return; // 存储没有该物品：未补入
                    }
                    if (Inventory.isHotbarSlot(found)) {
                        inventory.selected = found;
                    } else {
                        this.gameMode.handlePickItem(found);
                    }
                } finally {
                    anvilcraft$pickingFromTerminal = false;
                }
            })
        );
        return slot;
    }
}
