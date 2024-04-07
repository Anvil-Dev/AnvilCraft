//package dev.dubhe.anvilcraft.block.entity;
//
//import dev.architectury.injectables.annotations.ExpectPlatform;
//import dev.dubhe.anvilcraft.block.ChuteBlock;
//import dev.dubhe.anvilcraft.init.ModBlockEntities;
//import dev.dubhe.anvilcraft.init.ModBlocks;
//import dev.dubhe.anvilcraft.inventory.ChuteMenu;
//import lombok.Getter;
//import lombok.Setter;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.core.NonNullList;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.chat.Component;
//import net.minecraft.world.entity.player.Inventory;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.inventory.AbstractContainerMenu;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.entity.BlockEntityType;
//import net.minecraft.world.level.block.state.BlockState;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import javax.annotation.Nonnull;
//
//public class ChuteBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity {
//    private int cooldown = 0;
//    @Getter
//    @Setter
//    private boolean record = false;
//    @Getter
//    private final NonNullList<Boolean> disabled = this.getNewDisabled();
//    @Getter
//    private final NonNullList<ItemStack> filter = this.getNewFilter();
//
//    public ChuteBlockEntity(BlockPos pos, BlockState blockState) {
//        this(ModBlockEntities.CHUTE.get(), pos, blockState);
//    }
//
//    public ChuteBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
//        super(type, pos, blockState, 9);
//    }
//
//    @ExpectPlatform
//    public static ChuteBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
//        throw new AssertionError();
//    }
//
//    @ExpectPlatform
//    public static void onBlockEntityRegister(BlockEntityType<ChuteBlockEntity> type) {
//        throw new AssertionError();
//    }
//
//    @Override
//    public @NotNull Component getDisplayName() {
//        return Component.translatable("block.anvilcraft.chute");
//    }
//
//    public static void tick(Level level, BlockPos pos, BlockEntity entity) {
//    }
//
//    @Override
//    public void load(@Nonnull CompoundTag tag) {
//        super.load(tag);
//        this.cooldown = tag.getInt("cooldown");
//        this.loadTag(tag);
//    }
//
//    @Override
//    protected void saveAdditional(@Nonnull CompoundTag tag) {
//        super.saveAdditional(tag);
//        tag.putInt("cooldown", this.cooldown);
//        this.saveTag(tag);
//    }
//
//
//    @Override
//    public Direction getDirection() {
//        if (this.level == null) return Direction.UP;
//        BlockState state = this.level.getBlockState(this.getBlockPos());
//        if (state.is(ModBlocks.CHUTE.get())) return state.getValue(ChuteBlock.FACING);
//        return Direction.UP;
//    }
//
//    @Override
//    public void setDirection(Direction direction) {
//        if (direction == Direction.UP) return;
//        BlockPos pos = this.getBlockPos();
//        Level level = this.getLevel();
//        if (null == level) return;
//        BlockState state = level.getBlockState(pos);
//        if (!state.is(ModBlocks.CHUTE.get())) return;
//        level.setBlockAndUpdate(pos, state.setValue(ChuteBlock.FACING, direction));
//    }
//
//
//    @Nullable
//    @Override
//    public AbstractContainerMenu createMenu(int i, @Nonnull Inventory inventory, @Nonnull Player player) {
//        return ChuteMenu.serverOf(i, inventory, this);
//    }
//
//    @Override
//    public int getDepositorySize() {
//        return this.getDepository().getSlots();
//    }
//
//    @Override
//    public NonNullList<ItemStack> getItems() {
//        return this.getDepository().getStacks();
//    }
//}
