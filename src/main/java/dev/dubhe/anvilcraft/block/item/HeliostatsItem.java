package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.heatable.HeatableBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.HeliostatsData;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class HeliostatsItem extends BlockItem {
    public HeliostatsItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * 磁盘中是否存储有数据
     */
    public static boolean hasDataStored(ItemStack stack) {
        return stack.has(ModComponents.HELIOSTATS_DATA);
    }

    /**
     * 获取存储的数据
     */
    @SuppressWarnings("DataFlowIssue")
    public static BlockPos getData(ItemStack stack) {
        HeliostatsData heliostatsData = stack.get(ModComponents.HELIOSTATS_DATA);
        return heliostatsData.pos();
    }

    public static void deleteData(ItemStack stack) {
        stack.remove(ModComponents.HELIOSTATS_DATA);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
        BlockPos pos,
        Level level,
        @Nullable Player player,
        ItemStack stack,
        BlockState state) {
        if (level.isClientSide) return false;
        if (!hasDataStored(stack)) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("block.anvilcraft.heliostats.placement_no_pos")
                        .withStyle(ChatFormatting.RED),
                    true);
            }
            return false;
        }

        BlockPos irritatePos = getData(stack);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof HeliostatsBlockEntity e) {
            if (!e.setIrritatePos(irritatePos) && player != null) {
                player.displayClientMessage(
                    Component.translatable("block.anvilcraft.heliostats.invalid_placement")
                        .withStyle(ChatFormatting.RED),
                    true);
            }
            return true;
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    @Override
    public void appendHoverText(
        ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        if (hasDataStored(stack)) {
            BlockPos pos = getData(stack);
            tooltipComponents.add(Component.translatable("item.anvilcraft.heliostats.pos_set", pos.toShortString())
                .withStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }
    }

    @Override
    protected SoundEvent getPlaceSound(BlockState pState, Level world, BlockPos pos, Player entity) {
        return ModBlocks.HELIOSTATS.getDefaultState().getSoundType(world, pos, entity).getPlaceSound();
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
            && hasDataStored(context.getItemInHand())) {
            deleteData(context.getItemInHand());
            return InteractionResult.SUCCESS;
        }
        BlockState blockState = level.getBlockState(context.getClickedPos());
        if (blockState.is(Blocks.NETHERITE_BLOCK)
            || blockState.getBlock() instanceof HeatableBlock
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
                stack.set(ModComponents.HELIOSTATS_DATA, new HeliostatsData(clickPos));
            }
            return InteractionResult.SUCCESS;
        } else {
            return super.useOn(context);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
        Level level, Player player, InteractionHand usedHand) {
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
