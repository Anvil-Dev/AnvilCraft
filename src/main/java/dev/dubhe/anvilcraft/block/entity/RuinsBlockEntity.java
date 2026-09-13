package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsParticles;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;
import javax.annotation.Nullable;

public class RuinsBlockEntity extends BlockEntity {
    @Getter
    private BlockState displayState = Blocks.AIR.defaultBlockState();
    private LootTable drops = LootTable.EMPTY;
    @Nullable
    private ResourceKey<LootTable> dropsId = BuiltInLootTables.EMPTY;
    @Getter
    private boolean fragile;
    private CompoundTag displayData = new CompoundTag();
    @Nullable
    private BlockEntity displayEntity;
    private boolean displayEntityCreated;

    public RuinsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setDisplay(BlockState state, CompoundTag data) {
        this.displayState = state.getBlock() instanceof RuinsBlock ? Blocks.AIR.defaultBlockState() : state;
        this.displayData = data.copy();
        this.displayEntity = null;
        this.displayEntityCreated = false;
        this.sync();
    }

    public boolean canMove() {
        return !this.displayState.hasBlockEntity() || this.displayState.getBlock() instanceof IMoveableEntityBlock;
    }

    public void invalidateDisplayEntity() {
        this.displayEntity = null;
        this.displayEntityCreated = false;
    }

    public void setFragile(boolean fragile) {
        this.fragile = fragile;
        this.sync();
    }

    public void setDrops(ResourceLocation id) {
        this.dropsId = ResourceKey.create(Registries.LOOT_TABLE, id);
        this.drops = LootTable.EMPTY;
        this.sync();
    }

    public String getDropsId() {
        return this.dropsId == null ? "" : this.dropsId.location().toString();
    }

    public List<ItemStack> getDrops(LootParams.Builder builder) {
        LootParams params = builder.withParameter(LootContextParams.BLOCK_STATE, this.displayState)
            .create(LootContextParamSets.BLOCK);
        LootTable table = this.dropsId == null ? this.drops
            : params.getLevel().getServer().reloadableRegistries().getLootTable(this.dropsId);
        return table.getRandomItems(params);
    }

    @Nullable
    public BlockEntity getDisplayEntity() {
        if (this.level == null || !this.level.isClientSide || this.displayEntityCreated) return this.displayEntity;
        this.displayEntityCreated = true;
        if (!(this.displayState.getBlock() instanceof EntityBlock block)) return null;
        this.displayEntity = block.newBlockEntity(this.worldPosition, this.displayState);
        if (this.displayEntity != null) {
            // 先读取快照再关联世界，避免被伪装实体的加载逻辑注册机器功能或触发世界更新。
            this.displayEntity.loadWithComponents(this.displayData.copy(), this.level.registryAccess());
            this.displayEntity.setLevel(this.level);
        }
        return this.displayEntity;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("displayState", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.displayState).getOrThrow());
        tag.putBoolean("fragile", this.fragile);
        tag.put("displayData", this.displayData.copy());
        if (this.dropsId != null) {
            tag.putString("drops", this.dropsId.location().toString());
        } else {
            tag.put("drops", LootTable.DIRECT_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this.drops)
                .getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.displayState = BlockState.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("displayState"))
            .result().filter(state -> !(state.getBlock() instanceof RuinsBlock)).orElse(Blocks.AIR.defaultBlockState());
        this.fragile = tag.getBoolean("fragile");
        this.displayData = tag.getCompound("displayData").copy();
        this.displayEntity = null;
        this.displayEntityCreated = false;
        this.drops = LootTable.EMPTY;
        this.dropsId = BuiltInLootTables.EMPTY;
        if (tag.contains("drops", Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("drops"));
            if (id != null) this.dropsId = ResourceKey.create(Registries.LOOT_TABLE, id);
        } else if (tag.contains("drops", Tag.TAG_COMPOUND)) {
            this.drops = LootTable.DIRECT_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("drops"))
                .result().orElse(LootTable.EMPTY);
            this.dropsId = null;
        }
        this.updateMovement();
        this.refreshLight();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.updateMovement();
        this.refreshLight();
    }

    private void updateMovement() {
        if (this.level == null || this.level.isClientSide) return;
        BlockState state = this.getBlockState();
        if (state.getValue(RuinsBlock.MOVABLE) != this.canMove()) {
            // 可推动标记不改变形状；转换尚未完成时也不能触发原结构的邻居拆除。
            this.level.setBlock(this.worldPosition, state.setValue(RuinsBlock.MOVABLE, this.canMove()),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private void sync() {
        this.setChanged();
        this.updateMovement();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
        this.refreshLight();
    }

    private void refreshLight() {
        if (this.level != null) this.level.getChunkSource().getLightEngine().checkBlock(this.worldPosition);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("displayState", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.displayState).getOrThrow());
        tag.putBoolean("fragile", this.fragile);
        tag.put("displayData", this.displayData.copy());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        if (this.level != null && this.level.isClientSide) RuinsParticles.remember(this);
        super.setRemoved();
    }
}
