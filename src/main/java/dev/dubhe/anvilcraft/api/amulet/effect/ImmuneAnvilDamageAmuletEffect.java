package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModEntityTypeTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/// 免疫铁砧相关伤害的护符效果
public record ImmuneAnvilDamageAmuletEffect() implements IImmuneDamageAmuletEffect {
    public static final ImmuneAnvilDamageAmuletEffect INSTANCE = new ImmuneAnvilDamageAmuletEffect();

    @Override
    public boolean shouldImmune(LivingEntity entity, ItemStack amulet, DamageSource source, AmuletEffectContext ctx) {
        if (!source.is(ModDamageTypeTags.ANVIL_AMULET_VALID)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        HolderSet.Named<EntityType<?>> valid = BuiltInRegistries.ENTITY_TYPE.getOrCreateTag(ModEntityTypeTags.ANVIL_AMULET_VALID);
        if (direct == null || !direct.getType().is(valid)) {
            ItemStack weapon = source.getWeaponItem();
            return direct instanceof Player && weapon != null && weapon.is(ModItemTags.ANVIL_HAMMER);
        }
        if (!(source.getEntity() instanceof FallingBlockEntity falling)) {
            return true;
        }
        BlockState state = falling.getBlockState();
        return state.is(BlockTags.ANVIL) || state.is(ModBlockTags.GIANT_ANVIL);
    }
}
