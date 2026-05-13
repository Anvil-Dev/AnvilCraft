package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.util.TriggerUtil;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class FallingBlockCollisionEventListener {

    @SubscribeEvent
    public static void anvilCollisionCraft(AnvilEvent.CollisionBlock event) {
        Vec3 entityPos = event.getEntity().position();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (AnvilCraft.CONFIG.anvilCollisionCraftSpeed > event.getSpeed()) return;
        RecipeHolder<AnvilCollisionCraftRecipe> resultRecipe = null;
        Collection<RecipeHolder<AnvilCollisionCraftRecipe>> recipes = level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.ANVIL_COLLISION_CRAFT.get());
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        for (RecipeHolder<AnvilCollisionCraftRecipe> recipe : recipes) {
            if (!recipe.value().anvil().test(level, event.getEntity().getBlockState(), null)) continue;
            if (!recipe.value().hitBlock().test(level, state, entity)) continue;
            if (event.getSpeed() < recipe.value().speed()) continue;
            if (resultRecipe != null && resultRecipe.value().speed() > recipe.value().speed()) continue;
            resultRecipe = recipe;
        }
        if (resultRecipe != null) {
            executeRecipe(event, resultRecipe, level, pos, entityPos);
            return;
        }
        if (event.getEntity().getBlockState().is(BlockTags.ANVIL)) {
            if (state.getDestroySpeed(level, pos) > 0) {
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

    private static void executeRecipe(
        AnvilEvent.CollisionBlock event,
        RecipeHolder<AnvilCollisionCraftRecipe> recipeHolder,
        Level level,
        BlockPos pos,
        Vec3 entityPos
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        removeBlock(level, pos);
        AnvilCollisionCraftRecipe recipe = recipeHolder.value();
        if (recipe.consume()) {
            event.getEntity().discard();
        }

        DamageSource damageSource = Explosion.getDefaultDamageSource(level, null);
        ExplosionDamageCalculator damageCalculator = new ItemImmuneExplosionDamage();
        double x = pos.getCenter().x;
        double y = pos.getCenter().y;
        double z = pos.getCenter().z;
        float radius = 4F;
        boolean fire = true;
        boolean spawnParticles = true;
        ParticleOptions smallExplosionParticles = ParticleTypes.EXPLOSION;
        ParticleOptions largeExplosionParticles = ParticleTypes.EXPLOSION_EMITTER;
        Holder<SoundEvent> explosionSound = SoundEvents.GENERIC_EXPLODE;
        Explosion.BlockInteraction blockInteraction =
            level.getServer().getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)
                ? Explosion.BlockInteraction.DESTROY_WITH_DECAY
                : Explosion.BlockInteraction.DESTROY;
        ServerExplosion explosion = new ServerExplosion(
            serverLevel,
            null,
            damageSource,
            damageCalculator,
            pos.getCenter(),
            radius,
            fire,
            blockInteraction
        );
        explosion.anvilcraft$setBlockTransformExplosion(recipe.transformBlocks());
        int blockCount = explosion.explode();
        ParticleOptions explosionParticle = explosion.isSmall() ? smallExplosionParticles : largeExplosionParticles;
        for (ServerPlayer serverplayer : serverLevel.players()) {
            if (serverplayer.distanceToSqr(x, y, z) < 4096.0) {
                Optional<Vec3> playerKnockback = Optional.ofNullable(explosion.getHitPlayers().get(serverplayer));
                serverplayer.connection.send(
                    new ClientboundExplodePacket(
                        pos.getCenter(),
                        radius,
                        blockCount,
                        playerKnockback,
                        explosionParticle,
                        explosionSound,
                        Level.DEFAULT_EXPLOSION_BLOCK_PARTICLES
                    )
                );
            }
        }

        ArrayList<ItemStack> itemEntities = new ArrayList<>();
        for (ChanceItemStack outputItem : recipe.outputItems()) {
            ItemStack itemStack = outputItem.getResult(serverLevel).create();
            if (itemStack.isEmpty()) continue;
            itemEntities.add(itemStack);
        }
        for (ItemStack itemStack : itemEntities) {
            int number = Math.min(itemStack.getCount(), 16);
            Vec3 originItemPos = entityPos.add(pos.getCenter().subtract(entityPos).scale(0.5)).subtract(0, 0.4, 0);
            Vec3 normal = pos.getCenter().subtract(entityPos).scale(0.6).multiply(1, 0, 1);
            final float dRoute;
            if (number == 2) {
                dRoute = 0F;
            } else {
                dRoute = (float) (2 * Math.PI / (number - 2));
            }
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
                itemEntity.anvilcraft$setMergeCooldown(5);
                level.addFreshEntity(itemEntity);
                remainder--;
            }
        }
        TriggerUtil.recipe(level, pos, recipeHolder.id().identifier(), itemEntities);
    }

    public static class ItemImmuneExplosionDamage extends ExplosionDamageCalculator {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            if (entity instanceof ItemEntity) {
                return false;
            }
            return super.shouldDamageEntity(explosion, entity);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void removeBlock(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.is(ModBlockTags.COLLISION_IMMUNE)) return;
        if (blockState.getBlock() instanceof AbstractMultiPartBlock multiPartBlock) {
            multiPartBlock.removePartsAndUpdate(level, pos);
        } else {
            level.removeBlock(pos, false);
        }

    }
}
