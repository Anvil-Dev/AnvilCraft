package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RubyItem extends Item {
    public RubyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemInHand = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState state = level.getBlockState(clickedPos);
        if (state.is(Blocks.STONE)) {
            Player player = context.getPlayer();
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ModCriterionTriggers.USE_ITEM.get().trigger(serverPlayer, this);
            }
            processStoneArea(level, clickedPos);
            if (player != null && player.getAbilities().instabuild) return InteractionResult.SUCCESS;
            if (player != null) this.breakItem(player, itemInHand);
            itemInHand.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private void processStoneArea(Level level, BlockPos centerPos) {
        if (level.isClientSide) return;
        level.setBlock(centerPos, Blocks.LAVA.defaultBlockState(), 3);
        playExtinguishSound(level, centerPos);
        spawnMagmaParticles(level, centerPos);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos targetPos = centerPos.offset(dx, 0, dz);
                BlockState targetState = level.getBlockState(targetPos);

                if (targetState.is(Blocks.STONE)) {
                    double roll = level.random.nextDouble();

                    if (roll < 0.10) {
                        level.setBlock(targetPos, Blocks.LAVA.defaultBlockState(), 3);
                        spawnMagmaParticles(level, targetPos);
                    } else if (roll < 0.75) {
                        level.setBlock(targetPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                        spawnMagmaParticles(level, targetPos);
                    }
                }
            }
        }
    }

    private void playExtinguishSound(Level level, BlockPos pos) {
        level.playSound(
            null,
            pos,
            SoundEvents.FIRE_EXTINGUISH,
            SoundSource.BLOCKS,
            1.0f,
            (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1.0f
        );
    }

    private void spawnMagmaParticles(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        for (int i = 0; i < 8; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
            double offsetY = level.random.nextDouble() * 0.5 + 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;

            serverLevel.sendParticles(
                ParticleTypes.LAVA,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                1,
                0.1, 0.1, 0.1,
                0.01
            );
        }

        for (int i = 0; i < 4; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;

            serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.8,
                pos.getZ() + 0.5 + offsetZ,
                1,
                0.05, 0.1, 0.05,
                0.005
            );
        }
    }

    private void breakItem(Player player, ItemStack stack) {
        if (!stack.isEmpty()) {
            if (!player.isSilent()) {
                player.level()
                    .playLocalSound(
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ITEM_BREAK,
                        player.getSoundSource(),
                        0.8f,
                        0.8f + player.level().random.nextFloat() * 0.4f,
                        false);
            }
            this.spawnItemParticles(player, stack);
        }
    }

    private void spawnItemParticles(Player player, ItemStack stack) {
        for (int i = 0; i < 5; ++i) {
            Vec3 vec3 = new Vec3(
                ((double) player.getRandom().nextFloat() - 0.5) * 0.1,
                Math.random() * 0.1 + 0.1,
                0.0);
            vec3 = vec3.xRot(-player.getXRot() * ((float) Math.PI / 180));
            vec3 = vec3.yRot(-player.getYRot() * ((float) Math.PI / 180));
            double d = (double) (-player.getRandom().nextFloat()) * 0.6 - 0.3;
            Vec3 vec32 = new Vec3(
                ((double) player.getRandom().nextFloat() - 0.5) * 0.3,
                d,
                0.6);
            vec32 = vec32.xRot(-player.getXRot() * ((float) Math.PI / 180));
            vec32 = vec32.yRot(-player.getYRot() * ((float) Math.PI / 180));
            vec32 = vec32.add(player.getX(), player.getEyeY(), player.getZ());
            player.level()
                .addParticle(
                    new ItemParticleOption(ParticleTypes.ITEM, stack),
                    vec32.x,
                    vec32.y,
                    vec32.z,
                    vec3.x,
                    vec3.y + 0.05,
                    vec3.z);
        }
    }
}