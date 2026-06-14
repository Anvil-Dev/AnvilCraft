package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@SuppressWarnings("deprecation")
public class BurningHeaterBlockEntity extends BlockEntity {
    public static final int MAX_BURN_TIME = 1200 * 20;
    public static final int LIT_THRESHOLD = 240 * 20;

    @Getter
    private int burnTime = 0;

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

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            boolean needsUpdate = false;

            if (this.burnTime > 0) {
                this.burnTime--;
                if (this.burnTime % 20 == 0) {
                    needsUpdate = true;
                }
            }

            int burnTimeBeforeFuel = this.burnTime;
            this.tryConsumeFuel();
            if (this.burnTime != burnTimeBeforeFuel) {
                needsUpdate = true;
            }

            if (needsUpdate) {
                setChanged();
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }

            this.updateBurningState(level, pos, state);
            HeaterManager.addProducer(pos, level, ModHeaterInfos.BURNING_HEATER);
            return;
        }

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
        ItemStack fuelStack = fuelResource.toStack(itemsToConsume);
        if (fuelStack.getCraftingRemainder() != null && this.itemHandler.getResource(0).isEmpty()) {
            ItemStack remainderStack = fuelStack.getCraftingRemainder().create();
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnTime = input.getIntOr("BurnTime", 0);
    }
}
