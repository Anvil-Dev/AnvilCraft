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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

@SuppressWarnings("deprecation")
public class BurningHeaterBlockEntity extends BlockEntity {
    public static final int MAX_BURN_TIME = 1200 * 20;
    public static final int LIT_THRESHOLD = 240 * 20;

    @Getter
    private int burnTime = 0;

    @SuppressWarnings("deprecation")
    @Getter
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getItemBurnTime(stack) > 0 || stack.is(Items.BUCKET);
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
            tryConsumeFuel();
            if (this.burnTime != burnTimeBeforeFuel) {
                needsUpdate = true;
            }

            if (needsUpdate) {
                setChanged();
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }

            updateBurningState(level, pos, state);
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
            ItemStack fuel = this.itemHandler.getStackInSlot(0);
            int burnTimePerItem = getItemBurnTime(fuel);
            if (burnTimePerItem <= 0) break;

            int itemsToConsume = (remaining + burnTimePerItem - 1) / burnTimePerItem;
            itemsToConsume = Math.min(itemsToConsume, fuel.getCount());
            if (itemsToConsume <= 0) break;

            remaining -= itemsToConsume * burnTimePerItem;
            this.itemHandler.extractItem(0, itemsToConsume, false);
            if (fuel.getCraftingRemainder() != null && this.itemHandler.getStackInSlot(0).isEmpty()) {
                this.itemHandler.setStackInSlot(0, fuel.getCraftingRemainder().create());
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

        ItemStack fuel = this.itemHandler.getStackInSlot(0);
        int burnTimePerItem = getItemBurnTime(fuel);
        if (burnTimePerItem <= 0) return;

        int itemsToConsume = Math.min(fuel.getCount(), (MAX_BURN_TIME - this.burnTime) / burnTimePerItem);
        if (itemsToConsume <= 0) return;

        this.burnTime += itemsToConsume * burnTimePerItem;
        this.itemHandler.extractItem(0, itemsToConsume, false);
        if (fuel.getCraftingRemainder() != null && this.itemHandler.getStackInSlot(0).isEmpty()) {
            this.itemHandler.setStackInSlot(0, fuel.getCraftingRemainder().create());
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
