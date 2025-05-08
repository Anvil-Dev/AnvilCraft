package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConfinementChamberBlock extends BaseEntityBlock {
    public ConfinementChamberBlock(Properties properties) {
        super(properties);
    }

    public static final ResourceLocation CONTENTS = ResourceLocation.withDefaultNamespace("contents");

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ConfinementChamberBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new ConfinementChamberBlockEntity(blockPos, blockState);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    //Unimplemented for current version
    /*@Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ConfinementChamberBlockEntity confinementChamberBlockEntity))
            return ItemInteractionResult.FAIL;
        ItemStack itemStack = confinementChamberBlockEntity.getItemHandler().getStackInSlot(0);
        ItemStack handItemStack = player.getItemInHand(hand);
        if (itemStack.is(handItemStack.getItem())) return ItemInteractionResult.FAIL;
        player.setItemInHand(hand, itemStack.copy());
        confinementChamberBlockEntity.getItemHandler().setStackInSlot(0, handItemStack.copy());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }*/

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof ConfinementChamberBlockEntity confinementChamberBlockEntity) {
            if (!level.isClientSide && player.isCreative() && !confinementChamberBlockEntity.getItemHandler().getStackInSlot(0).isEmpty()) {
                ItemStack itemstack = new ItemStack(ModBlocks.CONFINEMENT_CHAMBER.asItem());
                itemstack.applyComponents(blockentity.collectComponents());
                ItemEntity itementity = new ItemEntity(
                    level, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemstack
                );
                itementity.setDefaultPickUpDelay();
                level.addFreshEntity(itementity);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.Builder params) {
        BlockEntity blockentity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity) {
            params = params.withDynamicDrop(CONTENTS, it -> {
                for (int i = 0; i < shulkerboxblockentity.getContainerSize(); i++) {
                    it.accept(shulkerboxblockentity.getItem(i));
                }
            });
        }

        return super.getDrops(state, params);
    }
}
