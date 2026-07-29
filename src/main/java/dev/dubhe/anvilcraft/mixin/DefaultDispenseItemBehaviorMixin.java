package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerPlayer;
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

import static net.minecraft.core.dispenser.DefaultDispenseItemBehavior.spawnItem;

@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DefaultDispenseItemBehaviorMixin {
    @Inject(
        method = "execute",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"),
        cancellable = true
    )
    public void betterDispense(BlockSource blockSource, ItemStack item, CallbackInfoReturnable<ItemStack> cir) {
        if (!(item.getItem() instanceof BucketItem)
            && !item.is(Items.POWDER_SNOW_BUCKET)
            && !item.is(Items.GLASS_BOTTLE)
            && !item.is(Items.HONEY_BOTTLE)
            && !item.is(Items.POTION)
            && !item.is(Items.EXPERIENCE_BOTTLE)) {
            return;
        }
        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
        BlockPos targetBlockPos = blockSource.pos().relative(direction);
        BlockState targetState = blockSource.level().getBlockState(targetBlockPos);
        if (!(targetState.getBlock() instanceof AbstractCauldronBlock cauldronBlock)) return;
        ServerPlayer player = AnvilCraftFakePlayers.getBlockPlacer().offerPlayer(blockSource.level());
        ItemStack result;
        try {
            ItemStack itemStack = item.copy();
            itemStack.setCount(1);
            player.setItemInHand(player.getUsedItemHand(), itemStack);
            // noinspection DataFlowIssue
            cauldronBlock.useItemOn(
                itemStack,
                targetState,
                blockSource.level(),
                targetBlockPos,
                player,
                player.getUsedItemHand(),
                null
            );
            result = player.getItemInHand(player.getUsedItemHand());
        } finally {
            AnvilCraftFakePlayers.getBlockPlacer().disable(player);
        }
        if (result.is(item.getItem())) return;
        ItemStack out;
        if (item.getCount() == 1) {
            out = result;
        } else {
            out = item;
            out.split(1);
            ItemStack insertResult = blockSource.blockEntity().insertItem(result);
            if (!insertResult.isEmpty()) {
                Position position = DispenserBlock.getDispensePosition(blockSource);
                spawnItem(blockSource.level(), insertResult, 6, direction, position);
            }
        }
        cir.setReturnValue(out);
    }
}
