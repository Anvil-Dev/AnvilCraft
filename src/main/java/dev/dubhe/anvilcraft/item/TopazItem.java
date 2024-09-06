package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class TopazItem extends Item {
    public TopazItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        ItemStack itemInHand = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState state = level.getBlockState(clickedPos);
        if (state.is(Blocks.LIGHTNING_ROD)) {
            LightningBolt lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            Player player = context.getPlayer();
            lightningBolt.setPos(clickedPos.getCenter());
            level.addFreshEntity(lightningBolt);
            if (player != null && player.getAbilities().instabuild) return InteractionResult.SUCCESS;
            if (player != null) this.breakItem(player, itemInHand);
            itemInHand.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private void breakItem(Player player, @NotNull ItemStack stack) {
        if (!stack.isEmpty()) {
            if (!player.isSilent()) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, player.getSoundSource(),
                    0.8f, 0.8f + player.level().random.nextFloat() * 0.4f, false);
            }
            this.spawnItemParticles(player, stack);
        }
    }

    private void spawnItemParticles(Player player, ItemStack stack) {
        for (int i = 0; i < 5; ++i) {
            Vec3 vec3 = new Vec3(((double) player.getRandom().nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
            vec3 = vec3.xRot(-player.getXRot() * ((float) Math.PI / 180));
            vec3 = vec3.yRot(-player.getYRot() * ((float) Math.PI / 180));
            double d = (double) (-player.getRandom().nextFloat()) * 0.6 - 0.3;
            Vec3 vec32 = new Vec3(((double) player.getRandom().nextFloat() - 0.5) * 0.3, d, 0.6);
            vec32 = vec32.xRot(-player.getXRot() * ((float) Math.PI / 180));
            vec32 = vec32.yRot(-player.getYRot() * ((float) Math.PI / 180));
            vec32 = vec32.add(player.getX(), player.getEyeY(), player.getZ());
            player.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack),
                vec32.x, vec32.y, vec32.z, vec3.x, vec3.y + 0.05, vec3.z);
        }
    }
}
