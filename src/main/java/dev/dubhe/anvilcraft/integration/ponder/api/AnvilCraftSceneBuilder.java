package dev.dubhe.anvilcraft.integration.ponder.api;

import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.ISimpleMultiPartBlockState;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.integration.ponder.api.instruction.Interpolation;
import dev.dubhe.anvilcraft.integration.ponder.api.instruction.InterpolationAnimateWorldSectionInstruction;
import dev.dubhe.anvilcraft.integration.ponder.api.instruction.LineInstruction;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderSceneBuilder;
import net.createmod.ponder.foundation.SelectionImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AnvilCraftSceneBuilder extends PonderSceneBuilder {
    private final EffectInstructions effects;
    private final WorldInstructions world;
    private final SpecialInstructions special;
    private final OverlayInstructions overlay;

    private AnvilCraftSceneBuilder(PonderScene ponderScene) {
        super(ponderScene);
        this.effects = new EffectInstructions();
        this.world = new WorldInstructions();
        this.special = new SpecialInstructions();
        this.overlay = new OverlayInstructions();
    }

    public AnvilCraftSceneBuilder(SceneBuilder baseSceneBuilder) {
        this(baseSceneBuilder.getScene());
    }

    @Override
    public EffectInstructions effects() {
        return this.effects;
    }

    @Override
    public SpecialInstructions special() {
        return this.special;
    }

    @Override
    public WorldInstructions world() {
        return this.world;
    }

    @Override
    public OverlayInstructions overlay() {
        return this.overlay;
    }

    public class WorldInstructions extends PonderWorldInstructions {
        public <T extends Comparable<T>> Selection setMultiPartBlock(BlockPos pos, BlockState state, boolean spawnParticles) {
            int maxX = 0;
            int maxY = 0;
            int maxZ = 0;
            int minX = 0;
            int minY = 0;
            int minZ = 0;
            if (!(state.getBlock() instanceof AbstractMultiPartBlock<?> block)) {
                this.setBlock(pos, state, spawnParticles);
                return SelectionImpl.of(BoundingBox.fromCorners(pos, pos));
            }
            for (Enum<?> part : block.getParts()) {
                if (!(part instanceof ISimpleMultiPartBlockState<?> state1)) continue;
                Vec3i offset = state1.getOffset();
                maxX = Math.max(maxX, offset.getX());
                maxY = Math.max(maxY, offset.getY());
                maxZ = Math.max(maxZ, offset.getZ());
                minX = Math.min(minX, offset.getX());
                minY = Math.min(minY, offset.getY());
                minZ = Math.min(minZ, offset.getZ());
                // noinspection unchecked
                BlockState blockState = state.setValue((Property<T>) block.getPart(), (T) part);
                this.setBlock(pos.offset(offset), blockState, spawnParticles);
            }
            return SelectionImpl.of(BoundingBox.fromCorners(
                new Vec3i(minX, minY, minZ).offset(pos),
                new Vec3i(maxX, maxY, maxZ).offset(pos)
            ));
        }

        public void moveSectionInterpolation(ElementLink<WorldSectionElement> link, Vec3 offset, Interpolation interpolation) {
            AnvilCraftSceneBuilder.this.addInstruction(InterpolationAnimateWorldSectionInstruction.move(link, offset, interpolation));
            AnvilCraftSceneBuilder.this.idle((int) Math.ceil(interpolation.duration(offset.length())));
        }

        public void falldownSection(ElementLink<WorldSectionElement> link, float height) {
            this.moveSectionInterpolation(link, new Vec3(0, -height, 0), Interpolation.acceleration(0.08));
        }

        public void falldownSection(ElementLink<WorldSectionElement> link) {
            this.falldownSection(link, 1);
        }

        public void riseSection(ElementLink<WorldSectionElement> link, float height) {
            this.moveSectionInterpolation(link, new Vec3(0, height, 0), Interpolation.acceleration(0.05));
        }

        public void riseSection(ElementLink<WorldSectionElement> link) {
            this.riseSection(link, 1);
        }

        public ElementLink<EntityElement> createItemEntity(Vec3 pos, ItemStack item) {
            ElementLink<EntityElement> link = AnvilCraftSceneBuilder.this.world().createItemEntity(pos, Vec3.ZERO, item);
            AnvilCraftSceneBuilder.this.idle(10);
            return link;
        }

        public ElementLink<EntityElement> createItemEntity(BlockPos pos, ItemStack item) {
            return this.createItemEntity(pos.getCenter(), item);
        }

        public ElementLink<EntityElement> replaceItemEntity(Vec3 pos, ItemStack item, ElementLink<EntityElement> link) {
            this.removeEntity(link);
            return this.createItemEntity(pos, Vec3.ZERO, item);
        }

        public ElementLink<EntityElement> replaceItemEntity(BlockPos pos, ItemStack item, ElementLink<EntityElement> link) {
            return this.replaceItemEntity(pos.getCenter(), item, link);
        }

        public void removeEntity(ElementLink<EntityElement> link) {
            this.modifyEntity(link, Entity::discard);
        }

        public void modifyEntity(ElementLink<EntityElement> link, int duration, BiConsumer<Integer, Entity> entityCallBack) {
            for (int tick = 0; tick < duration; tick++) {
                int finalTick = tick;
                this.modifyEntity(link, entity -> entityCallBack.accept(finalTick, entity));
                AnvilCraftSceneBuilder.this.idle(1);
            }
        }

        public void letLivingEntityDie(
            ElementLink<EntityElement> link,
            BlockPos particlesPos,
            boolean discard,
            Consumer<Entity> entityCallBack
        ) {
            AnvilCraftSceneBuilder.this.effects().emitParticles(
                new Vec3(particlesPos.getX(), particlesPos.getY(), particlesPos.getZ()),
                AnvilCraftSceneBuilder.this.effects().simpleParticleEmitter(ParticleTypes.POOF, new Vec3(0, 0.1, 0)),
                10,
                1
            );
            this.modifyEntity(
                link, 20, (tick, entity) -> {
                    entityCallBack.accept(entity);
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.deathTime = tick;
                    }
                }
            );
            if (discard) this.removeEntity(link);
        }

        public void letLivingEntityDie(ElementLink<EntityElement> link, BlockPos particlesPos, boolean discard) {
            this.letLivingEntityDie(
                link,
                particlesPos,
                discard,
                entity -> {
                }
            );
        }
    }

    public class EffectInstructions extends PonderEffectInstructions {
    }

    public class SpecialInstructions extends PonderSpecialInstructions {
    }

    public class OverlayInstructions extends PonderOverlayInstructions {
        public void showLine(int color, Vec3 start, Vec3 end, int duration, float thickness) {
            AnvilCraftSceneBuilder.this.addInstruction(new LineInstruction(color, start, end, duration, thickness));
        }

        public void showLine(int color, Vec3 start, Vec3 end, int duration) {
            this.showLine(color, start, end, duration, 1 / 16f);
        }

        public void showTransmitterLine(BlockPos start, BlockPos end, int duration) {
            this.showLine(Constant.TRANSMITTER_LINE_COLOR, start.getCenter(), end.getCenter(), duration, 1 / 48f);
        }

        public void showBigLine(int color, Vec3 start, Vec3 end, int duration) {
            this.showLine(color, start, end, duration, 1 / 8f);
        }
    }
}
