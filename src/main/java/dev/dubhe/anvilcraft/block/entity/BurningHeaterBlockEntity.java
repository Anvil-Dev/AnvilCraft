package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BurningHeaterBlockEntity extends BlockEntity implements IItemHandlerHolder {
    /**
     * 低于该阈值时消耗一个燃料补充，500秒 = 10000tick
     */
    public static final int REFUEL_THRESHOLD = 500 * 20;

    /**
     * 点燃 (level=2) 的燃烧时间阈值：4分钟 = 240秒 = 4800tick
     */
    public static final int LIT_THRESHOLD = 240 * 20;

    @Getter
    private int burnTime = 0;

    /**
     * 客户端上次同步到 burnTime 时的游戏时间（用于本地倒计时估算）
     */
    private long lastSyncGameTime = 0;

    /**
     * 双击判定的最大间隔（tick），5 tick = 250ms
     */
    private static final long DOUBLE_CLICK_INTERVAL = 5;

    /**
     * 玩家最后一次右键该方块时的游戏 tick，用于双击检测
     */
    private final Object2LongMap<UUID> lastRightClickTicks = new Object2LongOpenHashMap<>();

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

    @Override
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    /**
     * 记录一次右键并判断是否为双击（距上次右键不超过 {@link #DOUBLE_CLICK_INTERVAL} tick）。
     */
    public boolean isDoubleClick(Player player) {
        if (level == null) return false;
        long now = level.getGameTime();
        UUID uuid = player.getUUID();
        long last = this.lastRightClickTicks.getLong(uuid);
        this.lastRightClickTicks.put(uuid, now);
        return last != 0 && now - last <= DOUBLE_CLICK_INTERVAL;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        super.onDataPacket(net, pkt, provider);
        if (level != null) {
            this.lastSyncGameTime = level.getGameTime();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            this.lastSyncGameTime = level.getGameTime();
        }
    }

    /**
     * 获取用于显示的燃烧时间。
     * 在客户端上，根据上次同步时间进行本地倒计时估算，实现平滑刷新。
     */
    public int getDisplayBurnTime() {
        if (level == null || !level.isClientSide()) return burnTime;
        if (lastSyncGameTime <= 0) return burnTime;
        int elapsed = (int) (level.getGameTime() - this.lastSyncGameTime);
        return Math.max(0, this.burnTime - elapsed);
    }

    /**
     * 服务端tick：倒计时燃烧时间并自动补充燃料，更新方块状态
     * 客户端tick：播放燃烧音效和粒子
     */
    public void tick(Level level, BlockPos pos, BlockState state) {
        // 服务器逻辑
        if (!level.isClientSide()) {
            boolean needsUpdate = false;

            if (this.burnTime > 0) {
                this.burnTime--;
                if (this.burnTime % 20 == 0) {
                    needsUpdate = true;
                }
            }

            // 记录燃料消耗前的燃烧时间，检测是否有大幅度变化
            int burnTimeBeforeFuel = this.burnTime;
            tryConsumeFuel();
            boolean fuelConsumed = this.burnTime != burnTimeBeforeFuel;

            // 燃料消耗导致燃烧时间大幅度变化时同步到客户端
            if (fuelConsumed) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                needsUpdate = true;
            }

            if (needsUpdate) {
                setChanged();
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }

            updateBurningState(level, pos, state);
            HeaterManager.addProducer(pos, level, ModHeaterInfos.BURNING_HEATER);
            return;
        }

        // 客户端逻辑：播放音效和粒子
        int burningLevel = state.getValue(BurningHeaterBlock.LEVEL);
        if (burningLevel >= 1) {
            RandomSource random = level.random;
            // 音效：使用自定义声音事件
            if (burningLevel == 2 && random.nextInt(40) == 0) {
                level.playLocalSound(pos, ModSoundEvents.BURNING_HEATER.get(), SoundSource.BLOCKS, 1.0f, 1.0f, true);
            }
            // 火焰粒子：仅点燃时
            if (burningLevel == 2 && random.nextInt(3) == 0) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.05, 0.0);
            }
            // 烟雾粒子：点燃时浓烟，阴燃时营火烟
            if (burningLevel == 2) {
                if (random.nextInt(5) == 0) {
                    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                    double y = pos.getY() + 1.0;
                    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.1, 0.0);
                }
            } else {
                // 阴燃：营火风格的袅袅烟
                if (random.nextInt(10) == 0) {
                    double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
                    double y = pos.getY() + 0.8 + random.nextDouble() * 0.4;
                    double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0, 0.05, 0.0);
                }
            }
        }
    }

    /**
     * 消耗燃烧时间（用于铁砧合成）
     *
     * <p>从当前缓存中精确扣除燃烧时间，并立即更新燃烧状态和客户端显示。</p>
     *
     * @param ticks 消耗的tick数
     */
    public void consumeBurnTime(int ticks) {
        if (ticks <= 0) return;
        this.burnTime = Math.max(0, this.burnTime - ticks);
        setChanged();
        if (level != null && !level.isClientSide()) {
            updateBurningState(level, worldPosition, getBlockState());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /**
     * 根据燃烧时间更新方块状态
     */
    private void updateBurningState(Level level, BlockPos pos, BlockState state) {
        int targetLevel;
        if (this.burnTime >= LIT_THRESHOLD) {
            targetLevel = 2; // 点燃
        } else if (this.burnTime > 0) {
            targetLevel = 1; // 阴燃
        } else {
            targetLevel = 0; // 熄灭
        }
        if (state.getValue(BurningHeaterBlock.LEVEL) != targetLevel) {
            level.setBlock(pos, state.setValue(BurningHeaterBlock.LEVEL, targetLevel), 3);
        }
    }

    /**
     * 燃烧时间低于阈值时消耗一个燃料来补充燃烧时间。
     * 燃料能提供的燃烧时间全部累加，没有上限。
     */
    private void tryConsumeFuel() {
        if (this.burnTime >= REFUEL_THRESHOLD) return;

        ItemStack fuel = this.itemHandler.getStackInSlot(0);
        int burnTimePerItem = getItemBurnTime(fuel);
        if (burnTimePerItem <= 0) return;

        this.burnTime += burnTimePerItem;
        this.itemHandler.extractItem(0, 1, false);
        // 熔岩桶等容器物品：消耗后槽位空时留下空桶
        if (fuel.hasCraftingRemainingItem() && this.itemHandler.getStackInSlot(0).isEmpty()) {
            this.itemHandler.setStackInSlot(0, fuel.getCraftingRemainingItem());
        }
    }

    /**
     * 获取单个物品的燃烧时间（tick）
     */
    public static int getItemBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        FurnaceFuel fuel = stack.getItem().builtInRegistryHolder()
            .getData(NeoForgeDataMaps.FURNACE_FUELS);
        return fuel != null ? fuel.burnTime() : 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("BurnTime", this.burnTime);
        tag.put("Inventory", this.itemHandler.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.burnTime = tag.getInt("BurnTime");
        this.itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }
}
