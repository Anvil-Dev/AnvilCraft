package dev.dubhe.anvilcraft.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UtusanItem extends Item {
    public UtusanItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack itemStack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }
        if (livingEntity instanceof Player && !((Player) livingEntity).getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        if (!level.isClientSide) {
            UtusanItem.removeHarmfulEffects(livingEntity);
        }
        return itemStack;
    }

    public static void removeHarmfulEffects(@NotNull LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide) return;
        boolean bl = false;
        List<MobEffect> effects = new ArrayList<>();
        for (MobEffectInstance effect : livingEntity.getActiveEffects()) {
            if (!effect.getEffect().getCategory().equals(MobEffectCategory.HARMFUL)) continue;
            effects.add(effect.getEffect());
            bl = true;
        }
        if (!bl) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            return;
        }
        for (MobEffect effect : effects) livingEntity.removeEffect(effect);
    }

    @Override
    public int getUseDuration(ItemStack itemStack) {
        return 1;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.EAT;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        return ItemUtils.startUsingInstantly(level, player, interactionHand);
    }
}
