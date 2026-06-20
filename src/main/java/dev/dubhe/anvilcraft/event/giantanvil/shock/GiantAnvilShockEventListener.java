package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.behavior.BehaviorTree;
import dev.dubhe.anvilcraft.api.behavior.TreeNode;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.entity.FallingSpectralBlockEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.GiantAnvilShockEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GiantAnvilShockEventListener {
    public static final String DESTROY_MODE = "destroy_mode";
    public static final String DESTROY_TYPE = "destroy_type";
    public static final String HURT_TYPE = "hurt_type";
    public static final String NO_HURT = "no_hurt";
    private static final BehaviorTree<ShockContext> behaviorTree;

    static {
        TreeNode<ShockContext> root = TreeNode.<ShockContext>predicatedExecutable(
            it -> {
                @SuppressWarnings("resource") Level level = it.unwrap().level();
                return level.getBlockState(it.unwrap().centerPos()).is(ModBlocks.HEAVY_IRON_BLOCK);
            }
        ).then(
            // break mode
            TreeNode.<ShockContext>executes(it -> {
                if (it.has(DESTROY_MODE) && it.has(DESTROY_TYPE)) {
                    DestroyMode mode = it.getAttachment(DESTROY_MODE, DestroyMode.class);
                    DestroyType type = it.getAttachment(DESTROY_TYPE, DestroyType.class);
                    type.accept(it.unwrap(), it.unwrap().rangePosList(), mode);
                }
            }).then(
                // test anvil type
                TreeNode.multiple(
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        (it.unwrap().testBorder(AnvilBlock.class) || it.unwrap().testBorder(ModBlocks.SPECTRAL_ANVIL))
                        && !it.unwrap().testBorder(ModBlocks.ROYAL_ANVIL)
                        && !it.unwrap().testBorder(ModBlocks.FROST_ANVIL)
                        && !it.unwrap().testBorder(ModBlocks.EMBER_ANVIL)
                        && !it.unwrap().testBorder(ModBlocks.TRANSCENDENCE_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.NORMAL)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.ROYAL_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.SILK_TOUCH)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.FROST_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.DISINTEGRATION)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.EMBER_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.AUTO_SMELTING)),
                    TreeNode.<ShockContext>predicatedExecutable(
                        it -> it.unwrap().testBorder(ModBlocks.TRANSCENDENCE_ANVIL)
                    ).executes(it -> it.putAttachment(DESTROY_MODE, DestroyMode.FORTUNE))
                )
            ).then(
                // test block type
                TreeNode.multiple(
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(BlockTags.LOGS)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.FELLING)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.HAY_BLOCK)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.HARVESTING)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.GRASS_BLOCK)
                        || it.unwrap().testCorner(Blocks.MYCELIUM)
                        || it.unwrap().testCorner(Blocks.PODZOL)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.CLEANING)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.OBSIDIAN)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.GENERAL)),
                    TreeNode.<ShockContext>predicatedExecutable(it ->
                        it.unwrap().testCorner(Blocks.AMETHYST_BLOCK)
                    ).executes(it -> it.putAttachment(DESTROY_TYPE, DestroyType.BROKEN_CRYSTALS))
                )
            )
        ).then(
            // hurt mode
            TreeNode.<ShockContext>predicate(
                it -> it.unwrap().testBorder(ModBlocks.CURSED_GOLD_BLOCK)
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.RUBY_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.FIRE))
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.SAPPHIRE_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.FROZEN))
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.TOPAZ_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.SHOCK))
            ).then(
                TreeNode.<ShockContext>predicatedExecutable(it -> it.unwrap().testCorner(ModBlocks.VOID_MATTER_BLOCK))
                    .executes(it -> it.putAttachment(HURT_TYPE, HurtType.VOID))
            )
        ).then(
            TreeNode.<ShockContext>predicatedExecutable(it ->
                it.unwrap().testCorner(ModBlocks.RESIN_BLOCK) && it.unwrap().testBorder(ModBlocks.RESIN_BLOCK)
            ).executes(it -> {
                Level level = it.unwrap().level();
                for (BlockPos pos : it.unwrap().rangePosList()) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.SPECTRAL_ANVIL.get())) {
                        FallingSpectralBlockEntity entity = FallingSpectralBlockEntity.fall(level, pos, state, false, true);
                        entity.setDeltaMovement(0, 0.31, 0);
                    } else if (state.getBlock() instanceof AnvilBlock) {
                        FallingBlockEntity entity = new FallingBlockEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            state.hasProperty(BlockStateProperties.WATERLOGGED)
                                ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                                : state
                        );
                        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
                        entity.setDeltaMovement(0, 0.31, 0);
                        level.addFreshEntity(entity);
                    }
                }
                // 让范围内的生物原地弹跳
                int radius = (int) Math.min(Math.ceil(it.unwrap().fallDistance()), AnvilCraft.CONFIG.giantAnvilMaxShockRadius);
                AABB aabb = AABB.ofSize(
                    Vec3.atCenterOf(it.unwrap().centerPos().above()),
                    radius * 2 + 1,
                    2,
                    radius * 2 + 1
                );
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, aabb);
                for (LivingEntity living : entities) {
                    // 坐在载具上的生物会被弹下车
                    boolean ejected = false;
                    if (living.isPassenger()) {
                        living.stopRiding();
                        ejected = true;
                    }
                    if (!ejected && !living.onGround()) continue;
                    if (living.isCrouching()) continue;
                    double dist = Math.max(
                        Math.abs(living.getX() - it.unwrap().centerPos().getX()),
                        Math.abs(living.getZ() - it.unwrap().centerPos().getZ())
                    );
                    double ratio = 1.0 - Math.min(dist / radius, 1.0);
                    double upwardSpeed = 0.4 + ratio * 0.35;
                    if (ejected) {
                        // 下车后延迟弹起，等待客户端同步位置
                        if (level instanceof ServerLevel sl) {
                            final double finalSpeed = upwardSpeed;
                            sl.getServer().tell(new net.minecraft.server.TickTask(
                                sl.getServer().getTickCount() + 4,
                                () -> {
                                    if (living.isAlive()) {
                                        living.setDeltaMovement(living.getDeltaMovement().x, finalSpeed, living.getDeltaMovement().z);
                                        living.hurtMarked = true;
                                    }
                                }
                            ));
                        }
                    } else {
                        living.setDeltaMovement(living.getDeltaMovement().x, upwardSpeed, living.getDeltaMovement().z);
                        living.hurtMarked = true;
                    }
                }
                it.putAttachment(NO_HURT, true);
            })
        ).executes(it -> {
            if (it.has(NO_HURT)) return;
            int radius = (int) Math.min(Math.ceil(it.unwrap().fallDistance()), AnvilCraft.CONFIG.giantAnvilMaxShockRadius);
            AABB aabb = AABB.ofSize(
                Vec3.atCenterOf(it.unwrap().centerPos().above()),
                radius * 2 + 1,
                1,
                radius * 2 + 1
            );
            @SuppressWarnings("resource") Level level = it.unwrap().level();
            List<LivingEntity> e = level.getEntitiesOfClass(LivingEntity.class, aabb);
            for (LivingEntity l : e) {
                if (it.has(HURT_TYPE)) {
                    HurtType hurtType = it.getAttachment(HURT_TYPE, HurtType.class);
                    l.hurt(hurtType.damageSource(l.level()), it.unwrap().fallDistance() * 2 * 2);
                    hurtType.postApply(l.level(), l, it.unwrap().fallDistance());
                } else {
                    if (l.getItemBySlot(EquipmentSlot.FEET).is(Items.AIR)) {
                        l.hurt(
                            level.damageSources().source(DamageTypes.FALL, it.unwrap().fallingGiantAnvil()),
                            it.unwrap().fallDistance() * 2
                        );
                    }
                }
            }
        });
        behaviorTree = new BehaviorTree<>(root);
    }

    @SubscribeEvent
    public static void onLand(AnvilEvent.GiantOnLand event) {
        ShockContext context = ShockContext.inflate(event);
        behaviorTree.run(context);
        // 仅当冲击机制实际触发（中心为重型铁块）时才生成撼地效果
        if (event.getLevel()
                .getBlockState(event.getPos().below(2))
                .is(ModBlocks.HEAVY_IRON_BLOCK)) {
            float fallDistance = event.getFallDistance();
            int radius = (int) Math.min(Math.ceil(fallDistance), AnvilCraft.CONFIG.giantAnvilMaxShockRadius);
            BlockPos shockCenter = event.getPos().below(2);

            // 发送震波效果包到附近所有玩家
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    new ChunkPos(event.getPos()),
                    new GiantAnvilShockEffectPacket(shockCenter, radius)
                );
            }

            // 音效与粒子
            if (AnvilCraft.CLIENT_CONFIG.groundHeaveParticlesEnabled) {
                boolean isResin = context.testCorner(ModBlocks.RESIN_BLOCK.get())
                    && context.testBorder(ModBlocks.RESIN_BLOCK.get());
                if (isResin) {
                    event.getLevel().playSound(null, event.getPos(), ModSoundEvents.GIANT_ANVIL_RESIN_SHOCK.get(),
                        SoundSource.BLOCKS, 2.0f, 0.8f + event.getLevel().random.nextFloat() * 0.4f);
                } else {
                    event.getLevel().playSound(null, event.getPos(), ModSoundEvents.GIANT_ANVIL_SHOCK.get(),
                        SoundSource.BLOCKS, 1.8f, 1.2f + event.getLevel().random.nextFloat() * 0.2f);
                }
                spawnGroundHeave(event);
            }
        }
    }

    private static void spawnGroundHeave(AnvilEvent.GiantOnLand event) {
        if (!AnvilCraft.CLIENT_CONFIG.groundHeaveParticlesEnabled) return;

        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        float fallDistance = event.getFallDistance();
        int radius = (int) Math.min(Math.ceil(fallDistance), AnvilCraft.CONFIG.giantAnvilMaxShockRadius);
        BlockPos centerPos = event.getPos();
        var server = serverLevel.getServer();
        RandomSource random = level.getRandom();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;

                BlockPos pos = centerPos.below(2).offset(dx, 0, dz);
                if (level.getBlockState(pos).isAir()) continue;

                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                double ratio = (double) ring / radius;
                // 粒子弹跳高度：整体降低约 50%
                // 旧公式：0.3 + (1.0 - ratio)  → 范围 [1.3, 0.3]
                // 新公式：0.15 + (1.0 - ratio) * 0.5 → 范围 [0.65, 0.15]
                // 减缓粒子过高飞散，更贴近地面效果
                double jumpHeight = 0.15 + (1.0 - ratio) * 0.5;
                int particleCount = AnvilCraft.CLIENT_CONFIG.groundHeaveParticleCount;
                double speed = 0.15 + jumpHeight * 0.2;

                // 概率触发粒子
                if (random.nextFloat() >= AnvilCraft.CLIENT_CONFIG.groundHeaveParticleChance) continue;

                // 方形圈延迟，同一圈同时发射
                long delayMs = ring * 30L;
                Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ignored) {
                        return;
                    }
                    server.execute(() -> serverLevel.sendParticles(
                        ParticleTypes.POOF,
                        pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5,
                        particleCount,
                        0.15, jumpHeight * 0.2, 0.15,
                        speed
                    ));
                });
            }
        }
    }
}
