package dev.dubhe.anvilcraft.event;


import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.FallingBlockCollisionEvent;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.elements.OutputItem;
import dev.dubhe.anvilcraft.util.BlockTransformExplosion;
import dev.dubhe.anvilcraft.util.MergeCooldownItemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class FallingBlockCollisionEventListener {

    @SubscribeEvent
    public static void anvilCollisionCraft(FallingBlockCollisionEvent event) {
        Vec3 entityPos = event.getFallingBlockEntity().position();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (AnvilCraft.config.anvilCollisionCraftSpeed > event.getSpeed()) return;
        //if (level.isClientSide()) return;
        BlockState blockState = level.getBlockState(pos);
        for (RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get())) {
            AnvilCollisionCraftRecipe recipe = recipeHolder.value();
            if (!recipe.anvil().is(event.getFallingBlockEntity().blockState)) continue;
            if (!recipe.hitBlock().is(blockState)) continue;
            removeBlock(level, pos);
            if (recipe.consume())
                event.getFallingBlockEntity().kill();

            //Entity source = event.getFallingBlockEntity();
            DamageSource damageSource = Explosion.getDefaultDamageSource(level, null);
            ExplosionDamageCalculator damageCalculator = new ItemImmuneExplosionDamage();
            double x = pos.getCenter().x;
            double y = pos.getCenter().y;
            double z = pos.getCenter().z;
            float radius = 4f;
            boolean fire = true;
            boolean spawnParticles = true;
            ParticleOptions smallExplosionParticles = ParticleTypes.EXPLOSION;
            ParticleOptions largeExplosionParticles = ParticleTypes.EXPLOSION_EMITTER;
            Holder<SoundEvent> explosionSound = SoundEvents.GENERIC_EXPLODE;
            Explosion.BlockInteraction blockInteraction =
                level.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY)
                    ? Explosion.BlockInteraction.DESTROY_WITH_DECAY
                    : Explosion.BlockInteraction.DESTROY;
            Explosion explosion = new Explosion(
                level,
                null,
                damageSource,
                damageCalculator,
                x,
                y,
                z,
                radius,
                fire,
                blockInteraction,
                smallExplosionParticles,
                largeExplosionParticles,
                explosionSound
            );
            ((BlockTransformExplosion) explosion).setBlockTransformExplosion(recipe.transformBlocks());
            explosion.explode();
            explosion.finalizeExplosion(spawnParticles);
            if (level instanceof ServerLevel serverLevel) {
                for (ServerPlayer serverplayer : serverLevel.players()) {
                    if (serverplayer.distanceToSqr(x, y, z) < 4096.0) {
                        serverplayer.connection
                            .send(
                                new ClientboundExplodePacket(
                                    x,
                                    y,
                                    z,
                                    radius,
                                    explosion.getToBlow(),
                                    explosion.getHitPlayers().get(serverplayer),
                                    explosion.getBlockInteraction(),
                                    explosion.getSmallExplosionParticles(),
                                    explosion.getLargeExplosionParticles(),
                                    explosion.getExplosionSound()
                                )
                            );
                    }
                }
            }

            ArrayList<ItemStack> itemEntities = new ArrayList<>();
            for (OutputItem outputItem : recipe.outputItems()) {
                ItemStack itemStack;
                if ((itemStack = outputItem.getResult(level.random)) == null) continue;
                itemEntities.add(itemStack);
            }
            for (ItemStack itemStack : itemEntities) {
                int number = Math.min(itemStack.getCount(), 16);
                Vec3 originItemPos = entityPos.add(pos.getCenter().subtract(entityPos).scale(0.5)).subtract(0, 0.4, 0);
                Vec3 normal = pos.getCenter().subtract(entityPos).scale(0.6).multiply(1, 0, 1);
                float dRoute;
                if (number == 2) dRoute = 0f;
                else dRoute = (float) (2 * Math.PI / (number - 2));
                Vector3f deltaMovement = normal.toVector3f().rotateY((float) (Math.PI / 2));
                int remainder = itemStack.getCount() % number;
                int quotient = itemStack.getCount() / number;
                for (int i = 0; i < number; i++) {
                    Vec3 deltaMovementVec3 = new Vec3(deltaMovement);
                    Vec3 itemPos = originItemPos.add(deltaMovementVec3.scale(0.2));
                    ItemEntity itemEntity = new ItemEntity(
                        level,
                        itemPos.x,
                        itemPos.y,
                        itemPos.z,
                        new ItemStack(
                            itemStack.getItem(),
                            quotient + Math.clamp(remainder, 0, 1)
                        )
                    );
                    deltaMovement.rotateAxis(dRoute, (float) normal.x, (float) normal.y, (float) normal.z);
                    itemEntity.setDeltaMovement(new Vec3(deltaMovement));
                    MergeCooldownItemEntity.castFromItemEntity(itemEntity).setMergeCooldown(5);
                    level.addFreshEntity(itemEntity);
                    remainder--;
                }
            }
            return;
        }
        if (event.getFallingBlockEntity().getBlockState().is(BlockTags.ANVIL)) {
            if (level.getBlockState(pos).getDestroySpeed(level, pos) > 0
                && !level.getBlockState(pos).is(ModBlockTags.COLLISION_IMMUNE)) {
                removeBlock(level, pos);
            }
            level.explode(
                null,
                Explosion.getDefaultDamageSource(level, null),
                null,
                pos.getCenter().x,
                pos.getCenter().y,
                pos.getCenter().z,
                4F,
                true,
                Level.ExplosionInteraction.TNT
            );
        }
    }

    public static class ItemImmuneExplosionDamage extends ExplosionDamageCalculator {
        @Override
        public boolean shouldDamageEntity(@NotNull Explosion explosion, @NotNull Entity entity) {
            if (entity instanceof ItemEntity) {
                return false;
            }
            return super.shouldDamageEntity(explosion, entity);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void removeBlock(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof AbstractMultiPartBlock multiPartBlock) {
            multiPartBlock.removePartsAndUpdate(level, pos);
        } else level.removeBlock(pos, false);

    }
}
