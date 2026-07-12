package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
    public void dispenseToCauldron(BlockSource source, ItemStack dispensed, CallbackInfoReturnable<ItemStack> cir) {
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
        ServerPlayer player = AnvilCraftFakePlayers.getBlockPlacer().offerPlayer(source.level());
        ItemStack itemStack = dispensed.copy();
        itemStack.setCount(1);
        player.setItemInHand(player.getUsedItemHand(), itemStack);
        // noinspection DataFlowIssue
        cauldronBlock.useItemOn(itemStack, targetState, source.level(), targetBlockPos, player, player.getUsedItemHand(), null);
        ItemStack result = player.getItemInHand(player.getUsedItemHand());
        if (result.is(dispensed.getItem())) {
            AnvilCraftFakePlayers.getBlockPlacer().disable(player);
            return;
        }
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
        AnvilCraftFakePlayers.getBlockPlacer().disable(player);
    }

    @Inject(
        method = "execute",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"),
        cancellable = true
    )
    public void dispenseToFishTank(BlockSource source, ItemStack dispensed, CallbackInfoReturnable<ItemStack> cir) {
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        BlockPos targetBlockPos = source.pos().relative(direction);
        FishTankBlockEntity targetBE = source.level().getBlockEntity(targetBlockPos, ModBlockEntities.FISH_TANK.get()).orElse(null);
        if (targetBE == null) return;
        ServerPlayer player = AnvilCraftFakePlayers.getBlockPlacer().offerPlayer(source.level());
        ItemStack stack = dispensed.copy();
        stack.setCount(1);
        InteractionHand hand = player.getUsedItemHand();
        player.setItemInHand(hand, stack);
        targetBE.interactWithFluid(source.level(), player, hand, player.getItemInHand(hand));
        ItemStack result = player.getItemInHand(player.getUsedItemHand());
        if (result.is(dispensed.getItem())) {
            AnvilCraftFakePlayers.getBlockPlacer().disable(player);
            return;
        }
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
        AnvilCraftFakePlayers.getBlockPlacer().disable(player);
    }
}
