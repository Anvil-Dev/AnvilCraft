package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import dev.dubhe.anvilcraft.util.PistonMoveGuard;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
abstract class TntBlockMixin extends Block {
    public TntBlockMixin(Properties properties) {
        super(properties);
    }

    /// 滑轨重新落位时会额外自触发一次 neighborChanged，被搬运的 TNT 在此跳过点燃。
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$skipPrimingWhileMoving(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston,
        CallbackInfo ci
    ) {
        if (PistonMoveGuard.isReserved(level, pos)) ci.cancel();
    }

    /// onPlace 在旧方块不是 TNT 时同样点燃，落位路径也要跳过。
    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$skipPrimingOnPlaceWhileMoving(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston,
        CallbackInfo ci
    ) {
        if (PistonMoveGuard.isReserved(level, pos)) ci.cancel();
    }

    @Inject(
        method = "useItemOn",
        at = @At("HEAD"),
        cancellable = true
    )
    private void useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (MultitoolItem.isActingAs(itemStack, MultitoolMode.FLINT_AND_STEEL)) {
            this.onCaughtFire(state, level, pos, hitResult.getDirection(), player);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item item = itemStack.getItem();
            itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());

            player.awardStat(Stats.ITEM_USED.get(item));
            cir.setReturnValue(Util.sidedSuccess(level));
        }
    }
}
