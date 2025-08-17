package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.ai.goal.GenericZombieAttackGoal;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Objects;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.SCARE_ENTITIES;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LivingEntityEventListener {
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        Mob entity = Util.castSafely(event.getEntity(), Mob.class).orElse(null);
        if (entity == null) return;
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) return;
        EntityType<?> type = entity.getType();
        if ((type.is(EntityTypeTags.SKELETONS) && player.getData(SCARE_ENTITIES).getBoolean("skeletons"))
            || (entity instanceof Creeper && player.getData(SCARE_ENTITIES).getBoolean("creepers"))
            || (entity instanceof Phantom && player.getData(SCARE_ENTITIES).getBoolean("phantoms"))
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Mob entity = Util.castSafely(event.getEntity(), Mob.class).orElse(null);
        if (entity == null) return;
        if (!(entity.getTarget() instanceof Player player)) return;
        EntityType<?> type = entity.getType();
        if ((type.is(EntityTypeTags.SKELETONS) && player.getData(SCARE_ENTITIES).getBoolean("skeletons"))
            || (entity instanceof Creeper && player.getData(SCARE_ENTITIES).getBoolean("creepers"))
            || (entity instanceof Phantom && player.getData(SCARE_ENTITIES).getBoolean("phantoms"))
        ) {
            entity.setTarget(null);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Warden
            && event.getSource().typeHolder().is(DamageTypes.SONIC_BOOM)
        ) {
            LivingEntity entity = event.getEntity();
            Level level = entity.level();
            level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), Items.ECHO_SHARD.getDefaultInstance()));
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Giant giant) {
            if (!giant.goalSelector.getAvailableGoals().isEmpty()) return;
            giant.goalSelector.addGoal(2, new GenericZombieAttackGoal<>(giant, 1.0, false));
            giant.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(giant, 1.0));
            giant.targetSelector.addGoal(1, new HurtByTargetGoal(giant).setAlertOthers(ZombifiedPiglin.class));
            giant.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(giant, Player.class, true));
            giant.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(giant, AbstractVillager.class, false));
            giant.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(giant, IronGolem.class, true));
            giant.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(giant, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));
            giant.goalSelector.addGoal(8, new LookAtPlayerGoal(giant, Player.class, 8.0F));
            giant.goalSelector.addGoal(8, new RandomLookAroundGoal(giant));
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Entity entity = event.getTarget();
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();
        if (level.isClientSide) return;
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack item1 = livingEntity.getMainHandItem();
            if (!item1.isEmpty() && !item1.is(Items.AIR) && item1.getItem() != heldItem.getItem()) return;
            RecipeManager manager = Objects.requireNonNull(level.getServer()).getRecipeManager();
            // 注意：matches并不能匹配到生物符合而物品不符合达的目标生物
            List<RecipeHolder<MobTransformWithItemRecipe>> listRecipeHolder =
                manager.getAllRecipesFor(ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get());
            if (listRecipeHolder.isEmpty()) return;
            for (RecipeHolder<MobTransformWithItemRecipe> holder : listRecipeHolder) {
                MobTransformWithItemRecipe recipe = holder.value();
                if (recipe.testEntity(livingEntity) && recipe.testItem(heldItem)) {
                    if (item1.getItem() == heldItem.getItem()) {
                        if (item1.getCount() >= item1.getItem().getMaxStackSize(item1)) return;
                        item1.setCount(item1.getCount() + 1);
                    } else {
                        ItemStack handItem = new ItemStack(heldItem.getItem(), 1);
                        livingEntity.setItemInHand(InteractionHand.MAIN_HAND, handItem);
                        if (livingEntity instanceof Mob mob) {
                            mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                            mob.setPersistenceRequired();
                        }
                    }
                    heldItem.shrink(1);
                    break;
                }
            }
        }
    }
}
