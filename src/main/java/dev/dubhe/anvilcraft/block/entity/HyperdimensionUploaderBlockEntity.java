package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.config.AnvilCraftServerConfig;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * 超维上传站方块实体。
 *
 * <p>内部有 16 格缓存，只能容纳 16 组物品，可通过溜槽 / 漏斗等输入输出。
 * 缓存内物品会在服务端按「性能墙」限速尝试存入绑定的超维存储站。</p>
 *
 * <p>绑定目标为全局存储（超维存储站），访问它不会强制加载任何区块；
 * 本站只在自身所在区块被加载时才会被 tick。</p>
 */
public class HyperdimensionUploaderBlockEntity extends BlockEntity implements IItemResourceHandlerHolder {
    /** 缓存格数：最多容纳 16 组相同物品 */
    public static final int BUFFER_SLOTS = 16;

    @Getter
    private final ItemStacksResourceHandler buffer = new ItemStacksResourceHandler(HyperdimensionUploaderBlockEntity.BUFFER_SLOTS) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            // 每格只允许同一种物品：空格可放入任意物品，已占用格只接受与自身相同的物品
            ItemResource existing = this.getResource(slot);
            return existing.isEmpty() || existing.equals(resource);
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            HyperdimensionUploaderBlockEntity.this.setChanged();
            if (HyperdimensionUploaderBlockEntity.this.level != null) {
                HyperdimensionUploaderBlockEntity.this.level.sendBlockUpdated(
                    getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL
                );
            }
        }
    };

    /** 绑定的超维存储站存储 id；null 表示未绑定 */
    @Getter
    @Nullable
    private UUID storageId;

    private int workCountdown = 0;

    public HyperdimensionUploaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setStorageId(@Nullable UUID storageId) {
        this.storageId = storageId;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * 服务端主循环：按性能墙限速把缓存内物品存入绑定的超维存储站。
     *
     * <p>只在自身区块被加载时被调用；绑定目标是全局存储，不加载任何区块。</p>
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide() || this.storageId == null) {
            return;
        }
        if (this.workCountdown-- > 0) {
            return;
        }
        AnvilCraftServerConfig.HyperdimensionUploader config = AnvilCraft.CONFIG.hyperdimensionUploader;
        this.workCountdown = config.workInterval;
        this.pushToStorage(config.maxItemsPerScan);
    }

    /**
     * 执行一次物品上传：把缓存内物品尽力存入绑定的超维存储站，最多移动 {@code maxItemsPerScan} 个。
     */
    private void pushToStorage(int maxItemsPerScan) {
        ResourceHandler<ItemResource> target = this.getStorageHandler();
        if (target == null) {
            return;
        }
        ItemHandlerUtil.exportToTarget(this.buffer, maxItemsPerScan, (resource, amount) -> true, target);
    }

    @Nullable
    private ResourceHandler<ItemResource> getStorageHandler() {
        if (this.level == null || this.storageId == null) {
            return null;
        }
        // 超维存储站的存储内容保存在全局 Storages 中，与区块位置无关，不会加载任何区块
        return Storages.get().getOrCreate(this.storageId, HyperdimensionStorage.class).getItems();
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.buffer;
    }

    /**
     * 缓存是否为空。
     */
    public boolean isBufferEmpty() {
        for (int slot = 0; slot < this.buffer.size(); slot++) {
            if (!this.buffer.getResource(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把缓存与绑定写入掉落的方块物品（拆除保留内容）。
     */
    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        BlockItem.setBlockEntityData(stack, this.getType(), output);
        stack.applyComponents(this.collectComponents());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.storageId != null) output.store("storage_id", UUIDUtil.CODEC, this.storageId);
        this.buffer.serialize(output.child("buffer"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storageId = input.read("storage_id", UUIDUtil.CODEC).orElse(null);
        ItemHandlerUtil.deserializeCompatibleStacks(this.buffer, input.childOrEmpty("buffer"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        return output.buildResult();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
