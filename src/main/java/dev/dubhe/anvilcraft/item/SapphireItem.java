package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class SapphireItem extends Item {
    public SapphireItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);

        BlockPos playerPos = new BlockPos(
            (int) Math.floor(player.getX()),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ())
        );
        BlockState playerState = level.getBlockState(playerPos);
        FluidState playerFluidState = playerState.getFluidState();

        if (playerState.is(Blocks.WATER) && playerFluidState.isSource() && playerFluidState.is(Fluids.WATER)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ModCriterionTriggers.USE_ITEM.get().trigger(serverPlayer, this);
            }
            processWaterArea(level, playerPos);
            if (player.getAbilities().instabuild) return InteractionResultHolder.success(itemInHand);
            this.breakItem(player, itemInHand);
            itemInHand.shrink(1);
            return InteractionResultHolder.success(itemInHand);
        }

        if (playerState.isAir()) {
            if (!canSupportSnowLayer(level, playerPos)) {
                return InteractionResultHolder.fail(itemInHand);
            }
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                ModCriterionTriggers.USE_ITEM.get().trigger(serverPlayer, this);
            }
            processSnowArea(level, playerPos);
            if (player.getAbilities().instabuild) return InteractionResultHolder.success(itemInHand);
            this.breakItem(player, itemInHand);
            itemInHand.shrink(1);
            return InteractionResultHolder.success(itemInHand);
        }
        return InteractionResultHolder.fail(itemInHand);
    }

    private boolean canSupportSnowLayer(Level level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.below());
        if (blockstate.is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
            return false;
        } else {
            return blockstate.is(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)
                   || Block.isFaceFull(blockstate.getCollisionShape(level, pos.below()), Direction.UP)
                   || blockstate.is(Blocks.SNOW) && blockstate.getValue(SnowLayerBlock.LAYERS) == 8;
        }
    }

    private void processWaterArea(Level level, BlockPos centerPos) {
        if (level.isClientSide) return;

        level.setBlock(centerPos, Blocks.ICE.defaultBlockState(), 3);
        playFreezeSound(level, centerPos);
        spawnFreezeParticles(level, centerPos);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos targetPos = centerPos.offset(dx, 0, dz);
                BlockState targetState = level.getBlockState(targetPos);
                FluidState targetFluidState = targetState.getFluidState();

                if (targetState.is(Blocks.WATER) && targetFluidState.isSource() && targetFluidState.is(Fluids.WATER)) {
                    if (level.random.nextDouble() < 0.80) {
                        level.setBlock(targetPos, Blocks.ICE.defaultBlockState(), 3);
                        spawnFreezeParticles(level, targetPos);
                    }
                }
            }
        }
    }

    private void processSnowArea(Level level, BlockPos centerPos) {
        if (level.isClientSide) return;

        playSnowSound(level, centerPos);
        spawnSnowParticles(level, centerPos);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos targetPos = centerPos.offset(dx, 0, dz);
                BlockState targetState = level.getBlockState(targetPos);

                if (!canSupportSnowLayer(level, targetPos)) {
                    continue;
                }

                if (targetState.isAir() || targetState.is(Blocks.SNOW)) {
                    int layers = level.random.nextInt(3) + 1; // 1~3层
                    BlockState snowState = Blocks.SNOW.defaultBlockState()
                        .setValue(SnowLayerBlock.LAYERS, layers);
                    level.setBlock(targetPos, snowState, 3);
                }
            }
        }
    }

    private void playFreezeSound(Level level, BlockPos pos) {
        level.playSound(
            null,
            pos,
            SoundEvents.GLASS_BREAK,
            SoundSource.BLOCKS,
            1.0f,
            (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1.0f
        );
    }

    private void playSnowSound(Level level, BlockPos pos) {
        level.playSound(
            null,
            pos,
            SoundEvents.SNOW_PLACE,
            SoundSource.BLOCKS,
            1.0f,
            (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1.0f
        );
    }

    private void spawnFreezeParticles(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        for (int i = 0; i < 8; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
            double offsetY = level.random.nextDouble() * 0.5 + 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;

            serverLevel.sendParticles(
                ParticleTypes.SNOWFLAKE,
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
                ParticleTypes.CLOUD,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + 0.8,
                pos.getZ() + 0.5 + offsetZ,
                1,
                0.05, 0.1, 0.05,
                0.005
            );
        }
    }

    private void spawnSnowParticles(Level level, BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) level;
        for (int i = 0; i < 12; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 1.5 + 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            serverLevel.sendParticles(
                ParticleTypes.SNOWFLAKE,
                pos.getX() + 0.5 + offsetX,
                pos.getY() + offsetY,
                pos.getZ() + 0.5 + offsetZ,
                1,
                0.2, 0.2, 0.2,
                0.01
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