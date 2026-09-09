package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.MonolithBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MonolithCoreBlockEntity extends BlockEntity {
    public static final int OFFERING_TICKS = 100;
    public static final int DISSOLVE_TICKS = 60;

    private BlockState offering = Blocks.AIR.defaultBlockState();
    private long offeringStart;
    private int lineHeight;

    public MonolithCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static boolean acceptsOffering(ItemStack stack, boolean giant) {
        if (giant) return stack.is(ModBlocks.GIANT_ANVIL.asItem());
        return stack.is(Items.ANVIL) || stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL)
            || stack.is(ModBlocks.SPECTRAL_ANVIL.asItem()) || stack.is(ModBlocks.NEOFORGE.asItem())
            || stack.is(ModBlocks.ROYAL_ANVIL.asItem()) || stack.is(ModBlocks.FROST_ANVIL.asItem())
            || stack.is(ModBlocks.EMBER_ANVIL.asItem()) || stack.is(ModBlocks.TRANSCENDENCE_ANVIL.asItem());
    }

    public boolean beginOffering(ItemStack stack) {
        if (!(this.level instanceof ServerLevel serverLevel) || this.isCoolingDown()) return false;
        if (!acceptsOffering(stack, this.isGiant())) return false;
        this.offering = Block.byItem(stack.getItem()).defaultBlockState();
        if (this.isGiant()) {
            this.offering = this.offering.setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER);
        }
        this.offeringStart = serverLevel.getGameTime();
        this.lineHeight = 0;
        BlockPos.MutableBlockPos line = this.worldPosition.above(this.isGiant() ? 2 : 1).mutable();
        Block lineBlock = this.isGiant() ? ModBlocks.GIANT_MONOLITH_LINE.get() : ModBlocks.MONOLITH_LINE.get();
        while (serverLevel.hasChunkAt(line) && line.getY() < serverLevel.getMaxBuildHeight()) {
            BlockState state = serverLevel.getBlockState(line);
            if (!state.is(lineBlock) || state.getValue(MonolithBlock.AXIS) != this.getAxis()) break;
            this.lineHeight++;
            line.move(Direction.UP);
        }
        this.setChanged();
        serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }

    public boolean isGiant() {
        return this.getBlockState().is(ModBlocks.GIANT_MONOLITH_CORE.get());
    }

    public Direction.Axis getAxis() {
        return this.getBlockState().getValue(this.isGiant() ? GiantMonolithCoreBlock.AXIS : MonolithBlock.AXIS);
    }

    public BlockState getOffering() {
        return this.offering;
    }

    public int getLineHeight() {
        return this.lineHeight;
    }

    public float getAnimationAge(float partialTick) {
        if (this.level == null || this.offering.isAir()) return OFFERING_TICKS;
        return Math.max(0, this.level.getGameTime() - this.offeringStart + partialTick);
    }

    public boolean isCoolingDown() {
        return this.getAnimationAge(0) < OFFERING_TICKS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.offering.isAir()) return;
        tag.put("Offering", NbtUtils.writeBlockState(this.offering));
        tag.putLong("OfferingStart", this.offeringStart);
        tag.putInt("LineHeight", this.lineHeight);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.offering = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Offering"));
        this.offeringStart = tag.getLong("OfferingStart");
        this.lineHeight = Math.max(0, tag.getInt("LineHeight"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        if (this.level == null || !this.level.isClientSide) return;
        // Legacy chunks may gain their first core block entity only when this packet arrives.
        this.requestModelDataUpdate();
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
