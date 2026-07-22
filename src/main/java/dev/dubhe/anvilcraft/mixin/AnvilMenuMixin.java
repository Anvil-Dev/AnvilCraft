package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.anvil.AnvilMenuResult;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu {
    @Shadow
    public int repairItemCountCost;

    @Shadow
    public String itemName;

    protected AnvilMenuMixin(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(null, containerId, playerInventory, access, null);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void handleMultiphaseMerge(CallbackInfo ci) {
        ItemStack inputLeft = this.inputSlots.getItem(0);
        ItemStack inputRight = this.inputSlots.getItem(1);
        if (inputRight.isEmpty()
            || !inputLeft.is(inputRight.getItem())
            || !inputLeft.has(ModComponents.MULTIPHASE)
            || !inputRight.has(ModComponents.MULTIPHASE)
        ) {
            return;
        }

        final AnvilMenu menu = (AnvilMenu) (Object) this;
        AnvilMenuResult calculation = AnvilMenuResult.builder().create();
        calculation.createResult(this.player, inputLeft, inputRight, this.itemName);
        ItemStack result = calculation.result;
        if (calculation.xpCost >= 40 && !this.player.hasInfiniteMaterials()) result = ItemStack.EMPTY;
        this.resultSlots.setItem(0, result);
        menu.setCost(calculation.xpCost);
        this.repairItemCountCost = calculation.repairItemCountCost;
        CommonHooks.onAnvilUpdate(
            menu,
            inputLeft,
            inputRight,
            this.resultSlots,
            this.itemName,
            this.player
        );
        ci.cancel();
    }

    @Inject(method = "isValidBlock", at = @At("HEAD"), cancellable = true)
    private void voj(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.is(ModBlocks.GIANT_ANVIL.get()) || state.is(BlockTags.ANVIL));
    }
}
