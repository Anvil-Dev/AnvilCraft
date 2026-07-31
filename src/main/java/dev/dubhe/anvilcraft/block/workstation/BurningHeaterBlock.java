package dev.dubhe.anvilcraft.block.workstation;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.block.IDamagingHeater;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.HeaterUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BurningHeaterBlock extends BaseEntityBlock implements IHammerRemovable, IDamagingHeater {
    /**
     * 燃烧等级：0=熄灭，1=阴燃(0-300s)，2=点燃(≥300s)
     */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 2);

    public BurningHeaterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BurningHeaterBlock.LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(BurningHeaterBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurningHeaterBlockEntity(ModBlockEntities.BURNING_HEATER.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BurningHeaterBlock.LEVEL);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return BaseEntityBlock.createTickerHelper(type, ModBlockEntities.BURNING_HEATER.get(),
                                                  (lvl, pos, st, entity) -> entity.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BurningHeaterBlockEntity be)) return InteractionResult.PASS;
        ItemStacksResourceHandler handler = be.getItemHandler();

        ItemStack held = player.getMainHandItem();
        ItemResource currentResource = handler.getResource(0);
        boolean hasItem = !currentResource.isEmpty();

        if (!held.isEmpty() && BurningHeaterBlockEntity.getItemBurnTime(held) > 0) {
            ItemResource heldResource = ItemResource.of(held.getItem(), held.getComponentsPatch());
            try (Transaction tx = Transaction.openRoot()) {
                int inserted = handler.insert(0, heldResource, held.getCount(), tx);
                tx.commit();
                held.setCount(held.getCount() - inserted);
            }
            return InteractionResult.CONSUME;
        } else if (held.isEmpty() && hasItem) {
            int amount = handler.getAmountAsInt(0);
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = handler.extract(0, currentResource, amount, tx);
                tx.commit();
                if (extracted > 0) {
                    player.setItemInHand(player.getUsedItemHand(), currentResource.toStack(extracted));
                }
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        HeaterUtil.hurtEntity(level, state, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public boolean isActive(BlockState state) {
        return state.getValue(BurningHeaterBlock.LEVEL) > 0;
    }
}
