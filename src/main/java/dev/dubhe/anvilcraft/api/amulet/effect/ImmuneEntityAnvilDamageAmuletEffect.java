package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.entity.ModEntityTypeTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/// 免疫铁砧实体或铁砧锤相关伤害的护符效果
public record ImmuneEntityAnvilDamageAmuletEffect() implements IImmuneDamageAmuletEffect {
    public static final ImmuneEntityAnvilDamageAmuletEffect INSTANCE = new ImmuneEntityAnvilDamageAmuletEffect();

    @Override
    public boolean shouldImmune(LivingEntity entity, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        Entity direct = source.getDirectEntity();
        HolderSet.Named<EntityType<?>> valid = BuiltInRegistries.ENTITY_TYPE.getOrCreateTag(ModEntityTypeTags.ANVIL_AMULET_VALID);
        if (direct == null || !direct.getType().is(valid)) {
            ItemStack weapon = source.getWeaponItem();
            return direct instanceof Player && weapon != null && weapon.is(ModItemTags.ANVIL_HAMMER);
        }
        return true;
    }
}
