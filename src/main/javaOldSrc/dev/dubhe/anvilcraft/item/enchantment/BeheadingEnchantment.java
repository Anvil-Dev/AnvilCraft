package dev.dubhe.anvilcraft.item.enchantment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BeheadingEnchantment extends ModEnchantment {
    public BeheadingEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
        super(rarity, category, applicableSlots);
    }

    @Override
    public boolean canEnchant(@NotNull ItemStack stack) {
        return stack.getItem() instanceof SwordItem;
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    /**
     * 使该附魔无法从村民交易附魔书获得
     */
    @Override
    public boolean isTradeable() {
        return false;
    }

    /**
     * 斩首
     */
    public static void beheading(
        EntityType<?> type, @NotNull ServerLevel level, LivingEntity entity,
        DamageSource damageSource, boolean hitByPlayer,
        long seed, Consumer<ItemStack> output
    ) {
        ResourceLocation resourceLocation = BeheadingLootLoader.getBeheading(type);
        MinecraftServer server = level.getServer();
        LootTable lootTable = server.getLootData().getLootTable(resourceLocation);
        LootParams.Builder builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, entity)
            .withParameter(LootContextParams.ORIGIN, entity.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
            .withOptionalParameter(LootContextParams.KILLER_ENTITY, damageSource.getEntity())
            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, damageSource.getDirectEntity());
        LivingEntity killCredit = entity.getKillCredit();
        if (!(killCredit instanceof Player player)) return;
        if (hitByPlayer) {
            builder = builder
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .withLuck(player.getLuck());
        }
        LootParams lootParams = builder.create(LootContextParamSets.ENTITY);
        lootTable.getRandomItems(lootParams, seed, output);
    }
}
