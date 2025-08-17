package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SpaceOvercompressorBlockEntity extends BlockEntity {

    public static long NEUTRONIUM_INGOT_MASS = 10_000_000;
    public static long DISPLAYED_MASS = NEUTRONIUM_INGOT_MASS / 100;
    public static int MAX_OUTPUT_PER_TIME = 640;
    private long storedMass = 0;

    public SpaceOvercompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public SpaceOvercompressorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.SPACE_OVERCOMPRESSOR.get(), pos, blockState);
    }

    public static SpaceOvercompressorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new SpaceOvercompressorBlockEntity(type, pos, state);
    }

    private final ItemStackHandler itemHandler = new ItemStackHandler(9);

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLong("storedMass", this.storedMass);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.storedMass = tag.getLong("storedMass");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compound = new CompoundTag();
        compound.putLong("storedMass", this.storedMass);
        return compound;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        Level level = this.level;
        if (level != null) {
            BlockState state = this.getBlockState();
            level.sendBlockUpdated(this.getBlockPos(), state, state, 2);
        }
        super.setChanged();
    }

    public void injectMass(long mass) {
        this.storedMass += mass;
        this.produceNeutronium();
        this.setChanged();
    }

    public void produceNeutronium() {
        Level level = this.level;
        if (level == null) return;
        BlockPos pos = this.getBlockPos();
        int produceCount = (int) Math.min(MAX_OUTPUT_PER_TIME, this.storedMass / NEUTRONIUM_INGOT_MASS);
        if (produceCount <= 0) return;
        this.storedMass -= produceCount * NEUTRONIUM_INGOT_MASS;
        AnvilUtil.dropItems(List.of(ModItems.NEUTRONIUM_INGOT.asStack(produceCount)),
            level,
            pos.below().getCenter());
    }

    public Component displayStoredMass() {
        return MassInjectRecipe.displayMassValue(this.storedMass);
    }

}
