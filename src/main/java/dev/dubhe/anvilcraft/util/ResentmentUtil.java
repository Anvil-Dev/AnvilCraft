package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class ResentmentUtil {
    private static final String BASE_RESENTMENT_TAG = "anvilcraft_resin_base_resentment";
    private static final String FORCED_RESENTMENT_TAG = "anvilcraft_resin_forced_resentment";

    private ResentmentUtil() {
    }

    public static void initializeBaseResentment(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(BASE_RESENTMENT_TAG)) return;
        long seed = mob.getUUID().getMostSignificantBits() ^ mob.getUUID().getLeastSignificantBits();
        data.putInt(BASE_RESENTMENT_TAG, RandomSource.create(seed).nextInt(11));
    }

    public static void setForcedResentment(Mob mob, int resentment) {
        initializeBaseResentment(mob);
        mob.getPersistentData().putInt(FORCED_RESENTMENT_TAG, Mth.clamp(resentment, 0, 100));
    }

    public static int getResentment(SavedEntity savedEntity, Level level) {
        if (!savedEntity.isMonster()) return 0;
        Entity entity = savedEntity.toEntity(level);
        if (!(entity instanceof LivingEntity livingEntity)) return 0;
        return getResentment(livingEntity);
    }

    public static int getResentment(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(FORCED_RESENTMENT_TAG)) {
            return Mth.clamp(data.getIntOr(FORCED_RESENTMENT_TAG, 0), 0, 100);
        }

        int base = Mth.clamp(data.getIntOr(BASE_RESENTMENT_TAG, 0), 0, 10);
        long harmfulEffectCount = entity.getActiveEffects().stream()
            .filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
            .count();
        float healthRatio = entity.getMaxHealth() <= 0.0F
            ? 1.0F
            : Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
        int healthBonus = (int) Math.floor((1.0F - healthRatio) * 20.0F + 0.0001F);
        return Mth.clamp(base + (int) harmfulEffectCount * 5 + healthBonus, 0, 100);
    }
}
