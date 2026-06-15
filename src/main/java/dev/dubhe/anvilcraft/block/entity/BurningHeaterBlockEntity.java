package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("deprecation")
public class BurningHeaterBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    public static final int MAX_BURN_TIME = 1200 * 20;
    public static final int LIT_THRESHOLD = 240 * 20;

    @Getter
    private int burnTime = 0;

    /** 客户端上次同步到 burnTime 时的游戏时间 */
    private long lastSyncGameTime = 0;

    @Getter
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(1) {
        @Override
        public void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return getItemBurnTime(resource.toStack()) > 0 || resource.toStack().is(Items.BUCKET);
        }
    };

    public BurningHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static BurningHeaterBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new BurningHeaterBlockEntity(type, pos, blockState);
    }

    /**
     * 获取用于显示的燃烧时间。
     * 客户端上根据上次同步时间进行本地倒计时估算，避免频繁网络同步。
     */
    public int getDisplayBurnTime() {
        if (level == null || !level.isClientSide()) return this.burnTime;
        if (this.lastSyncGameTime <= 0) return this.burnTime;
        long elapsed = level.getGameTime() - this.lastSyncGameTime;
        return Math.max(0, this.burnTime - (int) elapsed);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            int oldBurnTime = this.burnTime;
            int oldLevel = state.getValue(BurningHeaterBlock.LEVEL);

            if (this.burnTime > 0) {
                this.burnTime--;
            }

            this.tryConsumeFuel();
            int newLevel = state.getValue(BurningHeaterBlock.LEVEL);

            // 仅在燃烧时间大幅变化或燃烧等级改变时同步到客户端
            boolean bigChange = Math.abs(this.burnTime - oldBurnTime) > 20
                || oldLevel != newLevel;

            if (bigChange) {
                setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }

            this.updateBurningState(level, pos, state);
            HeaterManager.addProducer(pos, level, ModHeaterInfos.BURNING_HEATER);
            return;
        }

        // 客户端逻辑：音效和粒子
        int burningLevel = state.getValue(BurningHeaterBlock.LEVEL);
        if (burningLevel >= 1) {
            RandomSource random = level.getRandom();
            if (burningLevel == 2 && random.nextInt(40) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    ModSoundEvents.BURNING_HEATER.get(), SoundSource.BLOCKS, 1.0f, 1.0f, false);
            }
            if (burningLevel == 2 && random.nextInt(3) == 0) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.05, 0.0);
            }
            if (burningLevel == 2) {
                if (random.nextInt(5) == 0) {
                    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                    double y = pos.getY() + 1.0;
                    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.1, 0.0);
                }
            } else {
                if (random.nextInt(10) == 0) {
                    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
                    double y = pos.getY() + 0.8 + random.nextDouble() * 0.4;
                    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0, 0.05, 0.0);
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            this.lastSyncGameTime = level.getGameTime();
        }
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        this.saveAdditional(output);
        return output.buildResult();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnTime = input.getIntOr("BurnTime", 0);
        // 从磁盘加载燃料物品
        input.read("FuelItem", ItemStack.CODEC).ifPresent(stack -> {
            if (!stack.isEmpty()) {
                ItemResource resource = ItemResource.of(stack.getItem(), stack.getComponentsPatch());
                this.itemHandler.set(0, resource, stack.getCount());
            }
        });
        if (level != null) {
            this.lastSyncGameTime = level.getGameTime();
        }
    }

    public void consumeBurnTime(int ticks) {
        int remaining = ticks;

        while (remaining > 0) {
            ItemResource fuelResource = this.itemHandler.getResource(0);
            if (fuelResource.isEmpty()) break;
            int fuelCount = this.itemHandler.getAmountAsInt(0);
            int burnTimePerItem = getItemBurnTime(fuelResource.toStack());
            if (burnTimePerItem <= 0) break;

            int itemsToConsume = (remaining + burnTimePerItem - 1) / burnTimePerItem;
            itemsToConsume = Math.min(itemsToConsume, fuelCount);
            if (itemsToConsume <= 0) break;

            remaining -= itemsToConsume * burnTimePerItem;
            try (Transaction tx = Transaction.openRoot()) {
                this.itemHandler.extract(0, fuelResource, itemsToConsume, tx);
                tx.commit();
            }
            ItemStack fuelStack = fuelResource.toStack(itemsToConsume);
            if (fuelStack.getCraftingRemainder() != null && this.itemHandler.getResource(0).isEmpty()) {
                ItemStack remainderStack = fuelStack.getCraftingRemainder().create();
                this.itemHandler.set(0,
                    ItemResource.of(remainderStack.getItem(), remainderStack.getComponentsPatch()),
                    remainderStack.getCount()
                );
            }
        }

        if (remaining > 0) {
            this.burnTime = Math.max(0, this.burnTime - remaining);
        }

        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private void updateBurningState(Level level, BlockPos pos, BlockState state) {
        int targetLevel;
        if (this.burnTime >= LIT_THRESHOLD) {
            targetLevel = 2;
        } else if (this.burnTime > 0) {
            targetLevel = 1;
        } else {
            targetLevel = 0;
        }
        if (state.getValue(BurningHeaterBlock.LEVEL) != targetLevel) {
            level.setBlock(pos, state.setValue(BurningHeaterBlock.LEVEL, targetLevel), 3);
        }
    }

    private void tryConsumeFuel() {
        if (this.burnTime >= MAX_BURN_TIME) return;

        ItemResource fuelResource = this.itemHandler.getResource(0);
        if (fuelResource.isEmpty()) return;
        int fuelCount = this.itemHandler.getAmountAsInt(0);
        int burnTimePerItem = getItemBurnTime(fuelResource.toStack());
        if (burnTimePerItem <= 0) return;

        int itemsToConsume = Math.min(fuelCount, (MAX_BURN_TIME - this.burnTime) / burnTimePerItem);
        if (itemsToConsume <= 0) return;

        this.burnTime += itemsToConsume * burnTimePerItem;
        try (Transaction tx = Transaction.openRoot()) {
            this.itemHandler.extract(0, fuelResource, itemsToConsume, tx);
            tx.commit();
        }
        if (fuelResource.toStack().getCraftingRemainder() != null && this.itemHandler.getResource(0).isEmpty()) {
            ItemStack remainderStack = fuelResource.toStack().getCraftingRemainder().create();
            this.itemHandler.set(0,
                ItemResource.of(remainderStack.getItem(), remainderStack.getComponentsPatch()),
                remainderStack.getCount()
            );
        }
    }

    public static int getItemBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        FurnaceFuel fuel = stack.getItem().builtInRegistryHolder()
            .getData(NeoForgeDataMaps.FURNACE_FUELS);
        return fuel != null ? fuel.burnTime() : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BurnTime", this.burnTime);
        // 持久化燃料物品
        int count = this.itemHandler.getAmountAsInt(0);
        ItemStack fuelStack = this.itemHandler.getResource(0).toStack();
        if (!fuelStack.isEmpty()) {
            fuelStack = fuelStack.copyWithCount(count);
            output.store("FuelItem", ItemStack.CODEC, fuelStack);
        }
    }
}

