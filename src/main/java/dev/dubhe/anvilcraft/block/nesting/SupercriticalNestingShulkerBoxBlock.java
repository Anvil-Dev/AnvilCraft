package dev.dubhe.anvilcraft.block.nesting;

import dev.dubhe.anvilcraft.api.block.ITooltipBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBlock;
import dev.dubhe.anvilcraft.block.entity.nesting.SupercriticalNestingShulkerBoxBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class SupercriticalNestingShulkerBoxBlock extends BetterBlock implements EntityBlock, IHammerRemovable, ITooltipBlock {
    private static final int SOUND_DELAY = 8;
    public static final BooleanProperty COOLDOWN = BooleanProperty.create("cooldown");
    public static final IntegerProperty SOUNDSETID = IntegerProperty.create("soundsetid", 0, 4);

    public SupercriticalNestingShulkerBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (state.getValue(COOLDOWN)) return InteractionResult.SUCCESS;
        level.playSound(null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 0));
        level.scheduleTick(pos, this, SOUND_DELAY);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        switch (state.getValue(SOUNDSETID)) {
            case 0:
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 1));
                level.scheduleTick(pos, this, SOUND_DELAY);
                break;
            case 1:
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 0.9F);
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 2));
                level.scheduleTick(pos, this, SOUND_DELAY);
                break;
            case 2:
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 3));
                level.scheduleTick(pos, this, SOUND_DELAY);
                break;
            case 3:
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 4));
                level.scheduleTick(pos, this, 2 * SOUND_DELAY);
                break;
            case 4:
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, false).setValue(SOUNDSETID, 0));
                break;
            default:
                break;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COOLDOWN, SOUNDSETID);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(COOLDOWN, false).setValue(SOUNDSETID, 0);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SupercriticalNestingShulkerBoxBlockEntity nesting) {
            if (!level.isClientSide() && player.isCreative() && !ResourceHandlerUtil.isEmpty(nesting.getItems())) {
                ItemStack stack = this.asItem().getDefaultInstance();
                stack.applyComponents(be.collectComponents());
                ItemEntity itemEntity = new ItemEntity(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    stack
                );
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockentity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof SupercriticalNestingShulkerBoxBlockEntity box) {
            params = params.withDynamicDrop(
                ShulkerBoxBlock.CONTENTS,
                consumer -> {
                    for (int i = 0; i < box.getItemHandler().size(); i++) {
                        consumer.accept(box.getItemHandler().getResource(i).toStack(box.getItemHandler().getAmountAsInt(i)));
                    }
                }
            );
        }

        return super.getDrops(state, params);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, Consumer<Component> builder, TooltipFlag flag) {
        int validLine = 0;
        int nonEmpty = 0;

        for (var stack1 : stack.getOrDefault(ModComponents.OVER_LIMIT_CONTAINER, OverLimitItemContainerContents.EMPTY).nonEmptyItems()) {
            nonEmpty++;
            if (validLine > 4) continue;
            validLine++;
            builder.accept(Component.translatable(
                "container.shulkerBox.itemCount",
                stack1.getStack().getHoverName(),
                stack1.getCount()
            ));
        }

        if (nonEmpty - validLine <= 0) return;
        builder.accept(Component.translatable("container.shulkerBox.more", nonEmpty - validLine).withStyle(ChatFormatting.ITALIC));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SUPERCRITICAL_NESTING_SHULKER_BOX.create(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * Returns the analog signal this block emits. This is the signal a comparator can read from it.
     */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof SupercriticalNestingShulkerBoxBlockEntity be)) return 0;
        ResourceHandler<ItemResource> handler = be.getItemHandler();
        float f = 0.0F;

        for (int i = 0; i < handler.size(); i++) {
            ItemStack stack = ItemResourceHelper.getStackInSlot(handler, i);
            if (stack.isEmpty()) continue;
            f += (float) stack.getCount() / (float) ItemResourceHelper.getSlotLimit(handler, i);
        }

        f /= (float) handler.size();
        return Mth.lerpDiscrete(f, 0, 15);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData, player);
        if (includeData) {
            level.getBlockEntity(pos, ModBlockEntities.SUPERCRITICAL_NESTING_SHULKER_BOX.get())
                .ifPresent(be -> stack.applyComponents(be.collectComponents()));
        }
        return stack;
    }
}
