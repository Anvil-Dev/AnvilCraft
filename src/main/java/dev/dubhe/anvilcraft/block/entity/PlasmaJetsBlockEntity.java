package dev.dubhe.anvilcraft.block.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.special.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PlasmaJetsBlockEntity extends BlockEntity {
    private static final int MAX_DURATION = 10 * 60 * 20;
    private final Set<TubeWallLayer> tubeWalls = new HashSet<>();
    @Nullable
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
        if (this.level != null) {
            HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
            HeaterManager.removeProducer(this.getBlockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        }
        BlockPos pos = this.getBlockPos();
        if (
            this.level != null
            && (
                !this.level.getBlockState(pos.north()).isFaceSturdy(level, pos.north(), Direction.SOUTH)
                || !this.level.getBlockState(pos.south()).isFaceSturdy(level, pos.south(), Direction.NORTH)
                || !this.level.getBlockState(pos.east()).isFaceSturdy(level, pos.east(), Direction.WEST)
                || !this.level.getBlockState(pos.west()).isFaceSturdy(level, pos.west(), Direction.EAST)
            )
        ) {
            return false;
        }
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
        if (this.level == null) {
            return;
        }
        HeaterManager.removeProducer(this.getBlockPos(), this.level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.removeProducer(this.getBlockPos(), this.level, ModHeaterInfos.MAGNET_PLASMA_JETS);
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

    public static void tick(Level level, BlockPos ignored, BlockState ignored1, PlasmaJetsBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            entity.serverTick(serverLevel);
        } else if (level.isClientSide()) {
            entity.clientTick(level);
        }
    }

    private void serverTick(ServerLevel level) {
        if (this.tryRaise()) return;

        this.refreshCauldronPos(level);
        this.tryIgniteValidCauldron(level);
        this.checkTubeWallIntegrity(level);
        this.refreshDuration(level);

        HeaterManager.addProducer(this.getBlockPos(), level, ModHeaterInfos.NO_MAGNET_PLASMA_JETS);
        HeaterManager.addProducer(this.getBlockPos(), level, ModHeaterInfos.MAGNET_PLASMA_JETS);
        this.hurtEntities(level);
        this.provideCharge(level);
        this.playJetSound(level);
    }

    // @OnlyIn(Dist.CLIENT)
    private void clientTick(Level level) {
        this.refreshCauldronPos(level);
        this.summonParticles(level);
    }

    protected void tryIgniteValidCauldron(Level level) {
        BlockState state = level.getBlockState(Objects.requireNonNull(this.cauldronPos));
        if (!(state.getBlock() instanceof IIgnitableCauldron cauldron)) return;

        BlockCache cache = new BlockCache(level);
        // noinspection deprecation
        if (!cauldron.getFluid(cache, this.cauldronPos).is(ModFluidTags.OIL)) return;
        cauldron.setIgnited(cache, this.cauldronPos, true);
        cache.accept();
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
        boolean cauldronExisting = PlasmaJetsBlock.isValidBaseCauldron(level, Objects.requireNonNull(this.cauldronPos));
        boolean belowCauldronIsNotHeater = !level.getBlockState(this.cauldronPos.below(1))
            .is(ModBlocks.HEATER);
        boolean heaterOverload = level.getBlockState(this.cauldronPos.below(1))
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
        if (
            this.duration + MAX_DURATION / 2 < MAX_DURATION
            && PlasmaJetsBlock.tryConsumeOnce(level, Objects.requireNonNull(this.cauldronPos))
        ) {
            this.duration += MAX_DURATION / 2;
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
            entity.igniteForSeconds(15.0F);
            if (level.isClientSide()) {
                if (entity.hurtClient(ModDamageTypes.plasmaJet(level))) {
                    entity.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + RandomSource.create().nextFloat() * 0.4F);
                }
            } else {
                if (entity.hurtServer(Util.cast(level), ModDamageTypes.plasmaJet(level), 16.0F)) {
                    entity.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + RandomSource.create().nextFloat() * 0.4F);
                }
            }
        }
    }

    protected void provideCharge(Level level) {
        if (level.getGameTime() % (ChargeCollectorBlockEntity.INPUT_COOLDOWN * 20) != 0) return;
        for (TubeWallLayer layer : this.tubeWalls) {
            Pair<BlockPos, BlockPos> posPair = switch (layer.isMagnet(level)) {
                case TRUE -> layer.first;
                case FALSE -> layer.second;
                case DEFAULT -> null;
            };
            if (posPair == null) continue;
            ChargeCollectorManager instance = ChargeCollectorManager.getInstance(level);
            instance.charge(256, posPair.getFirst());
            instance.charge(256, posPair.getSecond());
        }
    }

    protected void playJetSound(ServerLevel level) {
        // 主音效：自定义声音事件，烈焰人持续咆哮
        if (level.getGameTime() % 5 == 0) {
            level.playSound(
                null,
                this.getBlockPos(),
                ModSoundEvents.PLASMA_JET.get(),
                SoundSource.BLOCKS,
                3.0f,
                0.8f + level.getRandom().nextFloat() * 0.2f
            );
        }
        // 岩浆咝咝声：较低频率，模拟高压喷射气流
        if (level.getGameTime() % 60 == 0) {
            level.playSound(
                null,
                this.getBlockPos(),
                ModSoundEvents.PLASMA_JET_LAVA.get(),
                SoundSource.BLOCKS,
                1.0f,
                1.0f + level.getRandom().nextFloat() * 0.3f
            );
        }
    }

    // @OnlyIn(Dist.CLIENT)
    protected void summonParticles(Level level) {
        Vec3 start = this.getParticleStartPos(level);
        Vec3 vector = start.vectorTo(this.getParticleEndPos());
        RandomSource random = level.getRandom();
        for (int i = 0; i < 5; i++) {
            level.addParticle(
                ModParticles.PLASMA_JETS.get(),
                false,
                true,
                start.x,
                start.y,
                start.z,
                (random.nextIntBetweenInclusive(0, 20) - 10) / 100.0,
                vector.y * 0.13,
                (random.nextIntBetweenInclusive(0, 20) - 10) / 100.0
            );
        }
    }

    protected void refreshCauldronPos(Level level) {
        if (this.cauldronPos != null && PlasmaJetsBlock.isValidBaseCauldron(level, this.cauldronPos)) {
            return;
        }
        for (int i = 1; i < 6; i++) {
            BlockPos pos = this.getBlockPos().below(i);
            if (PlasmaJetsBlock.isValidBaseCauldron(level, pos)) {
                this.cauldronPos = pos;
                break;
            }
        }
    }

    public Vec3 getParticleStartPos(Level level) {
        if (this.cauldronPos == null) {
            this.refreshCauldronPos(level);
        }
        if (this.cauldronPos == null) {
            return this.getBlockPos().getBottomCenter();
        }
        return this.cauldronPos.getCenter();
    }

    public Vec3 getParticleEndPos() {
        return this.getBlockPos().above(1).getBottomCenter();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("duration", this.duration);
        ValueOutput.ValueOutputList tubeWalls = output.childrenList("tube_walls");
        for (TubeWallLayer layer : this.tubeWalls) {
            tubeWalls.addChild().store(TubeWallLayer.CODEC, layer);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.duration = input.getIntOr("duration", 0);
        for (ValueInput wall : input.childrenListOrEmpty("tube_walls")) {
            wall.read(TubeWallLayer.CODEC).ifPresent(this.tubeWalls::add);
        }
        this.cauldronPos = this.getBlockPos().below(this.tubeWalls.size() + 1);
    }

    public record TubeWallLayer(Pair<BlockPos, BlockPos> first, Pair<BlockPos, BlockPos> second) {
        public static final MapCodec<TubeWallLayer> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            BlockPos.CODEC
                .fieldOf("center")
                .forGetter(TubeWallLayer::getCenter)
        ).apply(ins, TubeWallLayer::of));

        public static TubeWallLayer of(BlockPos center) {
            return new TubeWallLayer(new Pair<>(center.north(), center.south()), new Pair<>(center.east(), center.west()));
        }

        private BlockPos getCenter() {
            return this.first.getFirst().south();
        }

        public boolean isBroken(Level level) {
            return !level.getBlockState(this.second.getFirst()).isFaceSturdy(level, this.second.getFirst(), Direction.WEST)
                   || !level.getBlockState(this.second.getSecond()).isFaceSturdy(level, this.second.getSecond(), Direction.EAST)
                   || !level.getBlockState(this.first.getFirst()).isFaceSturdy(level, this.first.getFirst(), Direction.SOUTH)
                   || !level.getBlockState(this.first.getSecond()).isFaceSturdy(level, this.first.getSecond(), Direction.NORTH);
        }

        /// 判断该层是否是磁铁层
        ///
        /// @return {@link TriState#DEFAULT default} 说明该层不是磁铁层
        ///      {@link TriState#TRUE true} 说明 {@link TubeWallLayer#first() 第一对} 是可加热方块
        ///      {@link TriState#FALSE false} 说明 {@link TubeWallLayer#second() 第二对} 是可加热方块
        public TriState isMagnet(Level level) {
            if (
                level.getBlockState(this.second.getFirst()).is(ModBlockTags.MAGNET)
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
