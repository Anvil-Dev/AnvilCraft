package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsParticles;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RuinsBlockEntity extends BlockEntity {
    public static final ResourceKey<LootTable> EMPTY_DROPS =
        ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace("empty"));
    @Getter
    private BlockState displayState = Blocks.AIR.defaultBlockState();
    private LootTable drops = LootTable.EMPTY;
    @Nullable
    private ResourceKey<LootTable> dropsId = EMPTY_DROPS;
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

    public void setDrops(Identifier id) {
        this.dropsId = ResourceKey.create(Registries.LOOT_TABLE, id);
        this.drops = LootTable.EMPTY;
        this.sync();
    }

    public String getDropsId() {
        return this.dropsId == null ? "" : this.dropsId.identifier().toString();
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
        if (this.level == null || !this.level.isClientSide() || this.displayEntityCreated) return this.displayEntity;
        this.displayEntityCreated = true;
        if (!(this.displayState.getBlock() instanceof EntityBlock block)) return null;
        this.displayEntity = block.newBlockEntity(this.worldPosition, this.displayState);
        if (this.displayEntity != null) {
            // 先读取快照再关联世界，避免被伪装实体的加载逻辑注册机器功能或触发世界更新。
            this.displayEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,
                this.level.registryAccess(), this.displayData.copy()));
            this.displayEntity.setLevel(this.level);
        }
        return this.displayEntity;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.saveDisplay(output);
        if (this.dropsId != null) output.putString("drops", this.dropsId.identifier().toString());
        else output.store("drops", LootTable.DIRECT_CODEC, this.drops);
    }

    private void saveDisplay(ValueOutput output) {
        output.store("displayState", BlockState.CODEC, this.displayState);
        output.putBoolean("fragile", this.fragile);
        output.store("displayData", CompoundTag.CODEC, this.displayData.copy());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.displayState = input.read("displayState", BlockState.CODEC)
            .filter(state -> !(state.getBlock() instanceof RuinsBlock)).orElse(Blocks.AIR.defaultBlockState());
        this.fragile = input.getBooleanOr("fragile", false);
        this.displayData = input.read("displayData", CompoundTag.CODEC).orElseGet(CompoundTag::new).copy();
        this.invalidateDisplayEntity();
        this.drops = LootTable.EMPTY;
        this.dropsId = EMPTY_DROPS;
        var name = input.getString("drops");
        if (name.isPresent()) {
            Identifier id = Identifier.tryParse(name.get());
            if (id != null) this.dropsId = ResourceKey.create(Registries.LOOT_TABLE, id);
        } else {
            var inline = input.read("drops", LootTable.DIRECT_CODEC);
            if (inline.isPresent()) {
                this.drops = inline.get();
                this.dropsId = null;
            }
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
        if (this.level == null || this.level.isClientSide()) return;
        BlockState state = this.getBlockState();
        BlockState updated = state.setValue(RuinsBlock.MOVABLE, this.canMove())
            .setValue(RuinsBlock.LIGHT_BLOCK, this.displayState.getLightDampening())
            .setValue(RuinsBlock.SKYLIGHT, this.displayState.propagatesSkylightDown());
        if (state != updated) {
            this.level.setBlock(this.worldPosition, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
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
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveDisplay(output);
        return output.buildResult();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        if (this.level != null && this.level.isClientSide()) RuinsParticles.remember(this);
        super.setRemoved();
    }
}
