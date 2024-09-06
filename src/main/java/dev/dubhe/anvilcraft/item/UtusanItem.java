package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.util.PlayerUtil;
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
    @SuppressWarnings("UnreachableCode")
    public @NotNull ItemStack finishUsingItem(
        @NotNull ItemStack itemStack, @NotNull Level level, @NotNull LivingEntity livingEntity
    ) {
        if (!(livingEntity instanceof Player player)) return itemStack;
        if (PlayerUtil.isFakePlayer(player)) return itemStack;
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            UtusanItem.removeHarmfulEffects(livingEntity);
        }
        return itemStack;
    }

    /**
     * 移除负面效果
     *
     * @param livingEntity 生物
     */
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
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 4));
            return;
        }
        for (MobEffect effect : effects) livingEntity.removeEffect(effect);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack) {
        return 10;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack itemStack) {
        return UseAnim.EAT;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level, @NotNull Player player, @NotNull InteractionHand interactionHand
    ) {
        return ItemUtils.startUsingInstantly(level, player, interactionHand);
    }
}
