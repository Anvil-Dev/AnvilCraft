package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.RedhotMetalBlock;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HeliostatsItem extends BlockItem {
    public HeliostatsItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * 磁盘中是否存储有数据
     */
    public static boolean hasDataStored(ItemStack stack) {
        return stack.getOrCreateTag().contains("HeliostatsData")
                && stack.getOrCreateTag().get("HeliostatsData") instanceof CompoundTag
                && !stack.getOrCreateTag().getCompound("HeliostatsData").isEmpty();
    }

    /**
     * 获取存储的数据
     */
    public static BlockPos getData(ItemStack stack) {
        CompoundTag ct = stack.getOrCreateTag().getCompound("HeliostatsData");
        int x = ct.getInt("X");
        int y = ct.getInt("Y");
        int z = ct.getInt("Z");
        return new BlockPos(x, y, z);
    }

    /**
     *
     */
    public static CompoundTag createData(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        stack.getOrCreateTag().put("HeliostatsData", tag);
        return tag;
    }

    public static void deleteData(ItemStack stack) {
        stack.getOrCreateTag().remove("HeliostatsData");
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            @NotNull BlockPos pos,
            @NotNull Level level,
            @Nullable Player player,
            @NotNull ItemStack stack,
            @NotNull BlockState state
    ) {
        if (level.isClientSide) return false;
        if (!hasDataStored(stack)) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("block.anvilcraft.heliostats.placement_no_pos")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
            return false;
        }
        CompoundTag tag = stack.getOrCreateTag().getCompound("HeliostatsData");
        int x = tag.getInt("X");
        int y = tag.getInt("Y");
        int z = tag.getInt("Z");
        BlockPos irritatePos = new BlockPos(x, y, z);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof HeliostatsBlockEntity e) {
            if (!e.trySetIrritatePos(irritatePos) && player != null) {
                player.displayClientMessage(
                        Component.translatable("block.anvilcraft.heliostats.invalid_placement")
                                .withStyle(ChatFormatting.RED),
                        true
                );
            }
            deleteData(stack);
            return true;
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @Nullable Level level,
            @NotNull List<Component> tooltipComponents,
            @NotNull TooltipFlag isAdvanced
    ) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        if (hasDataStored(stack)) {
            CompoundTag tag = stack.getOrCreateTag().getCompound("HeliostatsData");
            int x = tag.getInt("X");
            int y = tag.getInt("Y");
            int z = tag.getInt("Z");
            BlockPos pos = new BlockPos(x, y, z);
            tooltipComponents.add(
                    Component.translatable("item.anvilcraft.heliostats.pos_set", pos.toShortString())
                            .withStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY))
            );
        }
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        if (context.getPlayer().isShiftKeyDown()) {
            if (hasDataStored(context.getItemInHand())) {
                deleteData(context.getItemInHand());
            }
            return InteractionResult.SUCCESS;
        }
        BlockState blockState = level.getBlockState(context.getClickedPos());
        if (blockState.is(ModBlocks.TUNGSTEN_BLOCK.get())
                || blockState.is(Blocks.NETHERITE_BLOCK)
                || blockState.is(ModBlocks.HEATED_TUNGSTEN.get())
                || blockState.is(ModBlocks.HEATED_NETHERITE.get())
                || blockState.getBlock() instanceof RedhotMetalBlock
        ) {
            ItemStack stack = context.getItemInHand();
            if (hasDataStored(stack)) {
                InteractionResult result = super.useOn(context);
                if (result != InteractionResult.FAIL) {
                    level.playSound(
                            context.getPlayer(),
                            context.getClickedPos(),
                            blockState.getSoundType().getPlaceSound(),
                            SoundSource.BLOCKS
                    );
                }
                return result;
            } else {
                BlockPos clickPos = context.getClickedPos();
                CompoundTag tag = createData(context.getItemInHand());
                tag.putInt("X", clickPos.getX());
                tag.putInt("Y", clickPos.getY());
                tag.putInt("Z", clickPos.getZ());
            }
            return InteractionResult.SUCCESS;
        } else {
            InteractionResult result = super.useOn(context);
            if (result != InteractionResult.FAIL) {
                level.playSound(
                        context.getPlayer(),
                        context.getClickedPos(),
                        blockState.getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS
                );
            }
            return result;
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player,
                                                           @NotNull InteractionHand usedHand) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            if (hasDataStored(itemStack)) {
                deleteData(itemStack);
            }
            return InteractionResultHolder.success(itemStack);
        }
        return super.use(level, player, usedHand);
    }
}
