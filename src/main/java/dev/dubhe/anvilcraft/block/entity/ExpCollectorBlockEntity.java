package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.OnlyDrainFluidTank;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.ExpCollectorBlock;
import dev.dubhe.anvilcraft.block.ItemCollectorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpCollectorBlockEntity extends BlockEntity
    implements MenuProvider,
    IFluidHandlerHolder,
    IPowerConsumer,
    IHasAffectRange {
    private static final Map<Integer, Map<Integer, Integer>> POWER_CONSUMPTION = Map.of(
        0,
        Map.of(1, 8, 2, 12, 4, 20, 8, 32),
        2,
        Map.of(1, 5, 2, 8, 4, 12, 8, 20),
        10,
        Map.of(1, 3, 2, 5, 4, 8, 8, 12),
        60,
        Map.of(1, 2, 2, 3, 4, 5, 8, 8)
    );

    public static final Map<Level, Map<ChunkPos, List<ExpCollectorBlockEntity>>> POACHING_COLLECTOR_MAP = new HashMap<>();

    @Getter
    @Setter
    private PowerGrid grid;

    @Getter
    private final OnlyDrainFluidTank fluidTank = new OnlyDrainFluidTank(4000, (fluid) -> fluid.is(ModFluids.EXP_FLUID)) {
        @Override
        protected void onContentsChanged() {
            ExpCollectorBlockEntity.this.setChanged();
            if (ExpCollectorBlockEntity.this.level != null) {
                ExpCollectorBlockEntity.this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    @Getter
    private final WatchableCyclingValue<Integer> rangeRadius = new WatchableCyclingValue<>(
        "rangeRadius", (ignore) -> this.setChanged(),
        1, 2, 4, 8
    );

    @Getter
    private final WatchableCyclingValue<Integer> cooldown = new WatchableCyclingValue<>(
        "cooldown",
        (self) -> {
            this.cd = self.get();
            this.setChanged();
        },
        0, 2, 10, 60
    );

    private int cd = cooldown.next();

    public ExpCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.fluidTank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.fluidTank.isEmpty()) {
            tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        }
        tag.putInt("Cooldown", this.cooldown.index());
        tag.putInt("RangeRadius", this.rangeRadius.index());
        tag.putInt("cd", this.cd);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("FluidTank", CompoundTag.TAG_COMPOUND)) {
            this.fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        }
        if (tag.contains("Cooldown", CompoundTag.TAG_INT)) {
            this.cooldown.fromIndex(tag.getInt("Cooldown"));
        }
        if (tag.contains("RangeRadius", CompoundTag.TAG_INT)) {
            this.rangeRadius.fromIndex(tag.getInt("RangeRadius"));
        }
        if (tag.contains("cd", CompoundTag.TAG_INT)) {
            this.cd = tag.getInt("cd");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        CompoundTag fluidTag = new CompoundTag();
        this.fluidTank.writeToNBT(registries, fluidTag);
        tag.put("FluidTank", fluidTag);
        tag.putInt("Cooldown", this.cooldown.index());
        tag.putInt("RangeRadius", this.rangeRadius.index());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide) return;
        this.updatePoachingMapForThis();
        // 如果保持“截胡模式就不再主动吸取物品”的设定就把下面一行取消注释回来
        // if (cooldown.get() == 0) return;

        if (cd > 1) {
            cd--;
            return;
        }
        if (!this.isGridWorking()) return;
        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(ItemCollectorBlock.POWERED) && state.getValue(ItemCollectorBlock.POWERED)) return;
        AABB box = AABB.ofSize(
            Vec3.atCenterOf(getBlockPos()),
            this.rangeRadius.get() * 2.0 + 1,
            this.rangeRadius.get() * 2.0 + 1,
            this.rangeRadius.get() * 2.0 + 1
        );
        List<ExperienceOrb> experienceOrbs = level.getEntitiesOfClass(ExperienceOrb.class, box)
            .stream().sorted(Comparator.comparing(ExperienceOrb::getValue))
            .toList();
        for (ExperienceOrb experienceOrb : experienceOrbs) {
            int count = experienceOrb.count;
            int value = experienceOrb.value;
            int expFluid = value * 20;
            int totalExpFluid = value * count * 20;
            if (this.fluidTank.getCapacity() - this.fluidTank.getFluidAmount() >= totalExpFluid) {
                this.fluidTank.internalFill(new FluidStack(ModFluids.EXP_FLUID, totalExpFluid), IFluidHandler.FluidAction.EXECUTE);
                level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_ALL);
                experienceOrb.discard();
            } else {
                while (this.fluidTank.getCapacity() - this.fluidTank.getFluidAmount() >= expFluid) {
                    this.fluidTank.internalFill(new FluidStack(ModFluids.EXP_FLUID, expFluid), IFluidHandler.FluidAction.EXECUTE);
                    level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_ALL);
                    experienceOrb.count--;
                    if (experienceOrb.count < 1) {
                        experienceOrb.discard();
                        break;
                    }
                }
            }
        }
        if (this.cooldown.get() > 0) {
            this.cd = this.cooldown.get();
        } else {
            this.cd = 5; // 这个地方是给“即便是截胡模式也主动吸取物品”的设定准备的，暂时随便写了个数值
        }
    }

    public List<ChunkPos> getPoachingMapPositions(int range) {
        List<ChunkPos> chunkPosList = new ArrayList<>();
        BlockPos center = getBlockPos();
        int d = range * 2 + 1;
        int minX = center.getX() - d;
        int maxX = center.getX() + d;
        int minZ = center.getZ() - d;
        int maxZ = center.getZ() + d;
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunkPosList.add(new ChunkPos(cx, cz));
            }
        }
        return chunkPosList;
    }

    public void updatePoachingMapForThis() {
        List<ChunkPos> chunkPosListMax = getPoachingMapPositions(8);
        List<ChunkPos> chunkPosListReal = getPoachingMapPositions(rangeRadius.get());
        for (ChunkPos chunkPos : chunkPosListMax) {
            if (cooldown.get() == 0 && chunkPosListReal.contains(chunkPos)) {
                if (!POACHING_COLLECTOR_MAP.containsKey(level)) POACHING_COLLECTOR_MAP.put(level, new HashMap<>());
                if (!POACHING_COLLECTOR_MAP.get(level).containsKey(chunkPos)) {
                    POACHING_COLLECTOR_MAP.get(level).put(chunkPos, new ArrayList<>());
                }
                List<ExpCollectorBlockEntity> list = POACHING_COLLECTOR_MAP.get(level).get(chunkPos);
                if (!list.contains(this)) list.add(this);
            } else {
                if (POACHING_COLLECTOR_MAP.containsKey(level) && POACHING_COLLECTOR_MAP.get(level).containsKey(chunkPos)) {
                    List<ExpCollectorBlockEntity> list = POACHING_COLLECTOR_MAP.get(level).get(chunkPos);
                    list.remove(this);
                }
            }
        }

    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.exp_collector");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (player.isSpectator()) {
            return null;
        }
        return new ExpCollectorMenu(ModMenuTypes.EXP_COLLECTOR.get(), containerId, playerInventory, this);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public int getInputPower() {
        int power = POWER_CONSUMPTION.get(this.cooldown.get()).get(this.rangeRadius.get());
        if (this.level == null) {
            return power;
        }
        return this.getBlockState().getValue(ExpCollectorBlock.POWERED) ? 0 : power;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(
            Vec3.atCenterOf(getBlockPos()),
            this.rangeRadius.get() * 2.0 + 1,
            this.rangeRadius.get() * 2.0 + 1,
            this.rangeRadius.get() * 2.0 + 1
        );
    }
}
