package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.api.fluid.CapacityModifiableFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.power.consumer.ExpCollectorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpCollectorBlockEntity extends BlockEntity
    implements MenuProvider, IFluidResourceHandlerHolder, IPowerConsumer, IDiskCloneable, IHasAffectRange {
    private static final int CAPACITY = 4000;
    private static final int[][] POWER_CONSUMPTION = {
        {8, 12, 20, 32},
        {5, 8, 12, 20},
        {3, 5, 8, 12},
        {2, 3, 5, 8}
    };
    private static final Table<Level, ChunkPos, Set<ExpCollectorBlockEntity>> POACHING_COLLECTORS =
        HashBasedTable.create();

    @Getter
    private final WatchableCyclingValue<Integer> rangeRadius = new WatchableCyclingValue<>(
        "rangeRadius",
        ignored -> this.setChanged(),
        1,
        2,
        4,
        8
    );
    @Getter
    private final WatchableCyclingValue<Integer> cooldown = new WatchableCyclingValue<>(
        "cooldown",
        value -> {
            this.cd = value.get();
            this.setChanged();
        },
        0,
        2,
        10,
        60
    );
    private final CapacityModifiableFluidHandler tank = new CapacityModifiableFluidHandler(1, ExpCollectorBlockEntity.CAPACITY) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.getFluid() == ModFluids.EXP_FLUID.get();
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            ExpCollectorBlockEntity.this.setChanged();
            ExpCollectorBlockEntity.this.sendUpdate();
        }
    };
    private final ResourceHandler<FluidResource> externalTank = new ResourceHandler<>() {
        @Override
        public int size() {
            return ExpCollectorBlockEntity.this.tank.size();
        }

        @Override
        public FluidResource getResource(int index) {
            return ExpCollectorBlockEntity.this.tank.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return ExpCollectorBlockEntity.this.tank.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return ExpCollectorBlockEntity.this.tank.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return false;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return ExpCollectorBlockEntity.this.tank.extract(index, resource, amount, transaction);
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            return ExpCollectorBlockEntity.this.tank.extract(resource, amount, transaction);
        }
    };

    @Getter
    @Setter
    private @Nullable PowerGrid grid;
    private int cd;
    private int oldCooldown = -1;
    private int oldRange = -1;
    private @Nullable AABB boundingBox;

    public ExpCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.cooldown.next();
        this.resetCooldown();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.getBlockPos());
        }
        ExpCollectorBlockEntity.removePoachingCollector(this);
        super.setRemoved();
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.externalTank;
    }

    public ResourceHandler<FluidResource> getInternalFluidHandler() {
        return this.tank;
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(
                player,
                hand,
                this.getBlockPos(),
                this.externalTank,
                transaction
            );
            if (success) transaction.commit();
            return success;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("FluidTank"));
        output.putInt("Cooldown", this.cooldown.index());
        output.putInt("RangeRadius", this.rangeRadius.index());
        output.putInt("cd", this.cd);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("FluidTank"));
        this.cooldown.fromIndex(input.getIntOr("Cooldown", 0));
        this.rangeRadius.fromIndex(input.getIntOr("RangeRadius", 0));
        this.cd = input.getIntOr("cd", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()),
            registries
        );
        this.saveAdditional(output);
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    @Override
    public void gridTick() {
        if (this.level == null || this.level.isClientSide()) return;
        BlockState state = this.getBlockState();
        if (!this.isGridWorking() || state.getValue(ExpCollectorBlock.POWERED)) {
            this.resetCooldown();
            return;
        }
        if (this.cd > 1) {
            this.cd--;
            return;
        }
        List<ExperienceOrb> orbs = this.level.getEntitiesOfClass(ExperienceOrb.class, this.shape())
            .stream()
            .sorted(Comparator.comparingInt(ExperienceOrb::getValue))
            .toList();
        for (ExperienceOrb orb : orbs) {
            this.acceptExperienceOrb(orb);
        }
        this.resetCooldown();
    }

    private void resetCooldown() {
        this.cd = this.cooldown.get() > 0 ? this.cooldown.get() : 5;
    }

    private TriState acceptExperienceOrb(ExperienceOrb orb) {
        if (!this.isGridWorking()
            || this.getBlockState().getValue(ExpCollectorBlock.POWERED)
            || this.isRemoved()) {
            return TriState.FALSE;
        }
        int amountPerOrb = orb.getValue() * 20;
        if (amountPerOrb <= 0) return TriState.FALSE;
        FluidResource experience = FluidResource.of(ModFluids.EXP_FLUID);
        int remaining = this.tank.getCapacityAsInt(0, experience) - this.tank.getAmountAsInt(0);
        int absorbedCount = Math.min(orb.count, remaining / amountPerOrb);
        if (absorbedCount <= 0) return TriState.FALSE;
        int requested = absorbedCount * amountPerOrb;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = this.tank.insert(experience, requested, transaction);
            absorbedCount = inserted / amountPerOrb;
            if (absorbedCount <= 0) return TriState.FALSE;
            transaction.commit();
        }
        orb.count -= absorbedCount;
        if (orb.count <= 0) {
            orb.discard();
            return TriState.TRUE;
        }
        return TriState.DEFAULT;
    }

    public void tick(Level level, BlockPos blockPos) {
        this.flushState(level, blockPos);
        if (this.cooldown.get() != this.oldCooldown) {
            this.oldCooldown = this.cooldown.get();
            if (this.oldCooldown == 0) {
                ExpCollectorBlockEntity.addPoachingCollector(this);
            } else {
                ExpCollectorBlockEntity.removePoachingCollector(this);
            }
        }
        if (this.rangeRadius.get() != this.oldRange || this.boundingBox == null) {
            this.boundingBox = AABB.ofSize(
                Vec3.atCenterOf(this.getBlockPos()),
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1
            );
            this.oldRange = this.rangeRadius.get();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.exp_collector");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ExpCollectorMenu(ModMenuTypes.EXP_COLLECTOR.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
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
        int power = ExpCollectorBlockEntity.POWER_CONSUMPTION[this.cooldown.index()][this.rangeRadius.index()];
        if (this.level == null) return power;
        return this.getBlockState().getValue(ExpCollectorBlock.POWERED) ? 0 : power;
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        if (this.level == null) return;
        output.putInt("Cooldown", this.cooldown.index());
        output.putInt("RangeRadius", this.rangeRadius.index());
        output.putInt("cd", this.cd);
    }

    @Override
    public void applyDiskData(ValueInput input) {
        if (this.level == null) return;
        input.getInt("Cooldown").ifPresent(this.cooldown::fromIndex);
        input.getInt("RangeRadius").ifPresent(this.rangeRadius::fromIndex);
        input.getInt("cd").ifPresent(value -> this.cd = value);
        this.oldCooldown = -1;
        this.oldRange = -1;
        this.setChanged();
        Vec3 center = this.getPos().getCenter();
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        Packet<ClientGamePacketListener> packet = this.getUpdatePacket();
        if (packet == null) return;
        server.getPlayerList().broadcast(
            null,
            center.x(),
            center.y(),
            center.z(),
            256,
            this.level.dimension(),
            packet
        );
    }

    @Override
    public List<String> getDiskCompatibleGroups() {
        return List.of("anvilcraft:has_collector_config");
    }

    public int getRedstoneSignal() {
        int amount = this.tank.getAmountAsInt(0);
        int strength = amount == 0 ? 0 : amount * 14 / ExpCollectorBlockEntity.CAPACITY + 1;
        return Mth.clamp(strength, 0, 15);
    }

    @Override
    public AABB shape() {
        if (this.boundingBox == null) {
            this.boundingBox = AABB.ofSize(
                Vec3.atCenterOf(this.getBlockPos()),
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1,
                this.rangeRadius.get() * 2.0 + 1
            );
        }
        return this.boundingBox;
    }

    public static void clearPoachingCollectors() {
        ExpCollectorBlockEntity.POACHING_COLLECTORS.clear();
    }

    private static Set<ExpCollectorBlockEntity> getOrCreateCollectorList(Level level, ChunkPos chunkPos) {
        Set<ExpCollectorBlockEntity> collectors = ExpCollectorBlockEntity.POACHING_COLLECTORS.get(level, chunkPos);
        if (collectors == null) {
            collectors = new HashSet<>();
            ExpCollectorBlockEntity.POACHING_COLLECTORS.put(level, chunkPos, collectors);
        }
        return collectors;
    }

    private static void addPoachingCollector(ExpCollectorBlockEntity collector) {
        Level level = collector.level;
        if (level != null) {
            ExpCollectorBlockEntity.getOrCreateCollectorList(
                level,
                ChunkPos.containing(collector.worldPosition)
            ).add(collector);
        }
    }

    private static void removePoachingCollector(ExpCollectorBlockEntity collector) {
        Level level = collector.level;
        if (level != null) {
            ExpCollectorBlockEntity.getOrCreateCollectorList(
                level,
                ChunkPos.containing(collector.worldPosition)
            ).remove(collector);
        }
    }

    public static boolean poachExperienceOrb(ExperienceOrb orb) {
        ChunkPos currentPos = ChunkPos.containing(orb.blockPosition());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Set<ExpCollectorBlockEntity> collectors = ExpCollectorBlockEntity.POACHING_COLLECTORS.get(
                    orb.level(),
                    new ChunkPos(currentPos.x() + x, currentPos.z() + z)
                );
                if (collectors == null) continue;
                for (ExpCollectorBlockEntity collector : collectors) {
                    if (!collector.shape().contains(orb.position())) continue;
                    if (collector.acceptExperienceOrb(orb) == TriState.TRUE) return true;
                }
            }
        }
        return orb.isRemoved();
    }
}
