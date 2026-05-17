package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DefaultDispenseItemBehaviorMixin {
    @Inject(
        method = "execute",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"),
        cancellable = true
    )
    public void betterDispense(BlockSource source, ItemStack dispensed, CallbackInfoReturnable<ItemStack> cir) {
        if (
            !(dispensed.getItem() instanceof BucketItem)
            && !dispensed.is(Items.POWDER_SNOW_BUCKET)
            && !dispensed.is(Items.GLASS_BOTTLE)
            && !dispensed.is(Items.HONEY_BOTTLE)
            && !dispensed.is(Items.POTION)
        ) {
            return;
        }
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        BlockPos targetBlockPos = source.pos().relative(direction);
        BlockState targetState = source.level().getBlockState(targetBlockPos);
        if (!(targetState.getBlock() instanceof AbstractCauldronBlock cauldronBlock)) return;
        Player player = AnvilCraftFakePlayers.anvilcraftBlockPlacer.getPlayer();
        ItemStack itemStack = dispensed.copy();
        itemStack.setCount(1);
        player.setItemInHand(player.getUsedItemHand(), itemStack);
        cauldronBlock.useItemOn(itemStack, targetState, source.level(), targetBlockPos, player, player.getUsedItemHand(), null);
        ItemStack result = player.getItemInHand(player.getUsedItemHand());
        if (result.is(dispensed.getItem())) return;
        ItemStack out;
        if (dispensed.getCount() == 1) {
            out = result;
        } else {
            out = dispensed;
            out.split(1);
            ItemStack insertResult = source.blockEntity().insertItem(result);
            if (!insertResult.isEmpty()) {
                Position position = DispenserBlock.getDispensePosition(source);
                DefaultDispenseItemBehavior.spawnItem(source.level(), insertResult, 6, direction, position);
            }
        }
        cir.setReturnValue(out);
    }
}
