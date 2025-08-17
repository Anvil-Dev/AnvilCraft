package dev.dubhe.anvilcraft.block.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.FireCauldronBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.TriState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class PlasmaJetsBlockEntity extends BlockEntity {
    private static final int MAX_DURATION = 10 * 60 * 20;
    private final Set<TubeWallLayer> tubeWalls = new HashSet<>();
    private BlockPos cauldronPos = null;
    private int duration = 0;

    public PlasmaJetsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public PlasmaJetsBlockEntity(BlockPos pos, BlockState blockState, int duration, Set<TubeWallLayer> tubeWalls) {
        super(ModBlockEntities.PLASMA_JETS.get(), pos, blockState);
        this.duration = duration;
        this.tubeWalls.addAll(tubeWalls);
        this.cauldronPos = this.getBlockPos().below(this.tubeWalls.size() + 1);
    }

    public static PlasmaJetsBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new PlasmaJetsBlockEntity(type, pos, blockState);
    }

    private boolean tryRaise() {
        if (this.tubeWalls.size() >= 4) return false;
        HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        BlockPos pos = this.getBlockPos();
        if (this.level != null
            && (this.level.getBlockState(pos.north()).isAir()
            || this.level.getBlockState(pos.south()).isAir()
            || this.level.getBlockState(pos.east()).isAir()
            || this.level.getBlockState(pos.west()).isAir())
        ) return false;
        this.tubeWalls.add(TubeWallLayer.of(pos));
        this.level.removeBlock(pos, false);
        this.level.setBlock(pos.above(), ModBlocks.PLASMA_JETS.getDefaultState(), 3);
        this.level.setBlockEntity(new PlasmaJetsBlockEntity(pos.above(), this.getBlockState(), this.duration, this.tubeWalls));
        HeaterManager.addProducer(this.getBlockPos().above(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.addProducer(this.getBlockPos().above(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        return true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
    }

    public Pair<Set<BlockPos>, Set<BlockPos>> getHeatingPoses() {
        if (this.getLevel() != null) {
            return this.getHeatingPoses(this.getLevel());
        }
        return new Pair<>(Set.of(), Set.of());
    }

    public Pair<Set<BlockPos>, Set<BlockPos>> getHeatingPoses(Level level) {
        Set<BlockPos> noMagnet = new HashSet<>();
        Set<BlockPos> magnet = new HashSet<>();
        for (TubeWallLayer layer : this.tubeWalls) {
            if (layer.isMagnet(level) == TriState.DEFAULT) {
                noMagnet.addAll(layer.getHeatablePoses(level));
            } else {
                magnet.addAll(layer.getHeatablePoses(level));
            }
        }
        return new Pair<>(noMagnet, magnet);
    }

    private static final BiConsumer<PlasmaJetsBlockEntity, Level> CLIENT_TICK = (entity, level) -> entity.clientTick((ClientLevel) level);

    public static void tick(Level level, BlockPos ignored, BlockState ignored1, PlasmaJetsBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            entity.serverTick(serverLevel);
        } else if (level.isClientSide) {
            CLIENT_TICK.accept(entity, level);
        }
    }

    private void serverTick(ServerLevel level) {
        if (this.tryRaise()) return;

        this.refreshCauldronPos(level);
        this.checkTubeWallIntegrity(level);
        this.refreshDuration(level);

        HeaterManager.addProducer(this.getBlockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.addProducer(this.getBlockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        this.hurtEntities(level);
        this.provideCharge(level);
    }

    @OnlyIn(Dist.CLIENT)
    private void clientTick(ClientLevel level) {
        this.refreshCauldronPos(level);
        this.summonParticles(level);
    }

    protected void checkTubeWallIntegrity(Level level) {
        boolean wallBroken = this.tubeWalls.isEmpty();
        for (TubeWallLayer layer : this.tubeWalls) {
            if (layer.isBroken(level)) {
                wallBroken = true;
                break;
            }
        }
        boolean blocked = false;
        for (int i = 1; i <= this.tubeWalls.size(); i++) {
            if (!level.getBlockState(this.getBlockPos().below(i)).isAir()) {
                blocked = true;
                break;
            }
        }
        boolean cauldronExisting = level.getBlockState(cauldronPos).is(ModBlocks.FIRE_CAULDRON)
            || level.getBlockState(cauldronPos).is(Blocks.CAULDRON);
        boolean belowCauldronIsNotHeater = !level.getBlockState(cauldronPos.below(1))
            .is(ModBlocks.HEATER);
        boolean heaterOverload = level.getBlockState(cauldronPos.below(1))
            .getOptionalValue(HeaterBlock.OVERLOAD).orElse(true);
        if (wallBroken || blocked || !cauldronExisting || belowCauldronIsNotHeater || heaterOverload) {
            level.removeBlockEntity(this.getBlockPos());
            level.removeBlock(this.getBlockPos(), false);
            HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
            HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        }
    }

    protected void refreshDuration(Level level) {
        this.duration--;
        if (level.getBlockState(cauldronPos).getOptionalValue(FireCauldronBlock.LEVEL).orElse(0) > 0
            && this.duration + MAX_DURATION / 2 < MAX_DURATION
        ) {
            this.duration += MAX_DURATION / 2;
            FireCauldronBlock.lowerFillLevel(level.getBlockState(cauldronPos), level, cauldronPos);
        }
        if (this.duration < 0) {
            level.removeBlock(this.getBlockPos(), false);
        }
    }

    protected void hurtEntities(Level level) {
        if (level.getGameTime() % 10 != 0) return;
        Collection<Entity> entities = level.getEntitiesOfClass(
            Entity.class,
            AABB.of(BoundingBox.fromCorners(this.getBlockPos().below(this.tubeWalls.size()), this.getBlockPos())),
            entity -> !entity.fireImmune()
        );
        for (Entity entity : entities) {
            entity.igniteForSeconds(15.0f);
            if (entity.hurt(entity.damageSources().inFire(), 16.0f)) {
                entity.playSound(SoundEvents.GENERIC_BURN, 0.4f, 2.0f + RandomSource.create().nextFloat() * 0.4f);
            }
        }
    }

    protected void provideCharge(Level level) {
        if (level.getGameTime() % (ChargeCollectorBlockEntity.COOLDOWN * 20) != 0) return;
        for (TubeWallLayer layer : this.tubeWalls) {
            Pair<BlockPos, BlockPos> posPair = switch (layer.isMagnet(level)) {
                case TRUE -> layer.first;
                case FALSE -> layer.second;
                case DEFAULT -> null;
            };
            if (posPair == null) continue;
            BlockPos pos = posPair.getFirst();
            double uncharged = 256;
            for (ChargeCollectorManager.Entry entry : ChargeCollectorManager.getInstance(level).getNearestChargeCollect(pos)) {
                ChargeCollectorBlockEntity entity = entry.getBlockEntity();
                if (ChargeCollectorManager.getInstance(level).canCollect(entity, pos)) {
                    uncharged = entity.incomingCharge(uncharged, pos);
                    if (uncharged == 0) {
                        break;
                    }
                }
            }
            pos = posPair.getSecond();
            uncharged = 256;
            for (ChargeCollectorManager.Entry entry : ChargeCollectorManager.getInstance(level).getNearestChargeCollect(pos)) {
                ChargeCollectorBlockEntity entity = entry.getBlockEntity();
                if (ChargeCollectorManager.getInstance(level).canCollect(entity, pos)) {
                    uncharged = entity.incomingCharge(uncharged, pos);
                    if (uncharged == 0) {
                        break;
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void summonParticles(ClientLevel level) {
        Vec3 start = this.getParticleStartPos(level);
        Vec3 vector = start.vectorTo(this.getParticleEndPos());
        RandomSource random = level.getRandom();
        for (int i = 0; i < 5; i++) {
            level.addParticle(
                ModParticles.PLASMA_JETS.get(),
                true,
                start.x, start.y, start.z,
                (random.nextIntBetweenInclusive(0, 20) - 10) / 100.0,
                vector.y * 0.13,
                (random.nextIntBetweenInclusive(0, 20) - 10) / 100.0
            );
        }
    }

    protected void refreshCauldronPos(Level level) {
        if (this.cauldronPos != null
            && (level.getBlockState(this.cauldronPos).is(ModBlocks.FIRE_CAULDRON)
            || level.getBlockState(this.cauldronPos).is(Blocks.CAULDRON))
        ) return;
        for (int i = 1; i < 6; i++) {
            if (level.getBlockState(this.getBlockPos().below(i)).is(ModBlocks.FIRE_CAULDRON)
                || level.getBlockState(this.getBlockPos().below(i)).is(Blocks.CAULDRON)) {
                this.cauldronPos = this.getBlockPos().below(i);
                break;
            }
        }
    }

    public Vec3 getParticleStartPos(Level level) {
        if (this.cauldronPos == null) {
            this.refreshCauldronPos(level);
        }
        if (this.cauldronPos == null) return this.getBlockPos().getBottomCenter();
        return this.cauldronPos.getCenter();
    }

    public Vec3 getParticleEndPos() {
        return this.getBlockPos().above(1).getBottomCenter();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("duration", this.duration);
        ListTag tubeWalls = new ListTag();
        for (TubeWallLayer layer : this.tubeWalls) {
            tubeWalls.add(TubeWallLayer.CODEC.encode(layer, NbtOps.INSTANCE, new CompoundTag()).getOrThrow());
        }
        tag.put("tube_walls", tubeWalls);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.duration = tag.getInt("duration");
        ListTag tubeWalls = tag.getList("tube_walls", Tag.TAG_COMPOUND);
        for (Tag tubeWallTag1 : tubeWalls) {
            if (!(tubeWallTag1 instanceof CompoundTag tubeWallTag)) continue;
            this.tubeWalls.add(TubeWallLayer.CODEC.decode(NbtOps.INSTANCE, tubeWallTag).getOrThrow().getFirst());
        }
        this.cauldronPos = this.getBlockPos().below(this.tubeWalls.size() + 1);
    }

    public record TubeWallLayer(Pair<BlockPos, BlockPos> first, Pair<BlockPos, BlockPos> second) {
        public static final Codec<TubeWallLayer> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BlockPos.CODEC.fieldOf("center").forGetter(TubeWallLayer::getCenter)
        ).apply(ins, TubeWallLayer::of));

        public static TubeWallLayer of(BlockPos center) {
            return new TubeWallLayer(new Pair<>(center.north(), center.south()), new Pair<>(center.east(), center.west()));
        }

        private BlockPos getCenter() {
            return this.first.getFirst().south();
        }

        public boolean isBroken(Level level) {
            return level.getBlockState(this.second.getFirst()).isAir()
                || level.getBlockState(this.second.getSecond()).isAir()
                || level.getBlockState(this.first.getFirst()).isAir()
                || level.getBlockState(this.first.getSecond()).isAir();
        }

        /**
         * @return {@link TriState#DEFAULT default} 说明该层不是磁铁层
         * {@link TriState#TRUE true} 说明 {@link TubeWallLayer#first() 第一对} 是可加热方块
         * {@link TriState#FALSE false} 说明 {@link TubeWallLayer#second() 第二对} 是可加热方块
         */
        public TriState isMagnet(Level level) {
            if (level.getBlockState(this.second.getFirst()).is(ModBlockTags.MAGNET)
                && level.getBlockState(this.second.getSecond()).is(ModBlockTags.MAGNET)
                && level.getBlockState(this.first.getFirst()).is(ModBlockTags.HEATABLE_BLOCKS)
                && level.getBlockState(this.first.getSecond()).is(ModBlockTags.HEATABLE_BLOCKS)
            ) {
                return TriState.TRUE;
            } else if (
                level.getBlockState(this.first.getFirst()).is(ModBlockTags.MAGNET)
                    && level.getBlockState(this.first.getSecond()).is(ModBlockTags.MAGNET)
                    && level.getBlockState(this.second.getFirst()).is(ModBlockTags.HEATABLE_BLOCKS)
                    && level.getBlockState(this.second.getSecond()).is(ModBlockTags.HEATABLE_BLOCKS)
            ) {
                return TriState.FALSE;
            }
            return TriState.DEFAULT;
        }

        public Set<BlockPos> getHeatablePoses(Level level) {
            Set<BlockPos> poses = new HashSet<>();
            if (level.getBlockState(this.first.getFirst()).is(ModBlockTags.HEATABLE_BLOCKS)) {
                poses.add(this.first.getFirst());
            }
            if (level.getBlockState(this.first.getSecond()).is(ModBlockTags.HEATABLE_BLOCKS)) {
                poses.add(this.first.getSecond());
            }
            if (level.getBlockState(this.second.getFirst()).is(ModBlockTags.HEATABLE_BLOCKS)) {
                poses.add(this.second.getFirst());
            }
            if (level.getBlockState(this.second.getSecond()).is(ModBlockTags.HEATABLE_BLOCKS)) {
                poses.add(this.second.getSecond());
            }
            return poses;
        }
    }
}
