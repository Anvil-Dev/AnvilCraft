package dev.dubhe.anvilcraft.item.template;

import dev.dubhe.anvilcraft.init.ModItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
public class EightToOneTemplateItem extends BaseMultipleToOneTemplateItem {
    private final Map<ResourceKey<Enchantment>, Item> enchantmentMappings = new Object2ObjectOpenHashMap<>() {
        {
            put(Enchantments.SOUL_SPEED, Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.FIRE_PROTECTION, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.FIRE_ASPECT, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.FLAME, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.BLAST_PROTECTION, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.SWIFT_SNEAK, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.PROTECTION, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.MENDING, Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.INFINITY, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.DENSITY, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.BREACH, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.WIND_BURST, Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.PROJECTILE_PROTECTION, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.FORTUNE, Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.LOOTING, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.LUCK_OF_THE_SEA, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.LURE, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.DEPTH_STRIDER, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.RESPIRATION, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.AQUA_AFFINITY, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.IMPALING, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
            put(Enchantments.RIPTIDE, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
        }
    };

    private final List<Item> otherTemplate = new ArrayList<>() {
        {
            add(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
            add(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
            add(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
            add(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
        }
    };

    public static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.eight.missing");
    public static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
    );

    public EightToOneTemplateItem(Properties properties) {
        super(properties, 8);
    }

    @Override
    public Component getMaterialTooltip() {
        return MISSING_TOOLTIP;
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures() {
        return EMPTY_SLOT_TEXTURES;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
        Level level = itemEntity.level();
        List<Item> result = new ArrayList<>();
        ItemStack itemStack = itemEntity.getItem();
        if (itemStack.is(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE)) {
            ItemEnchantments itemEnchantments = itemStack.get(DataComponents.ENCHANTMENTS);
            if (itemEnchantments != null && !itemEnchantments.isEmpty()) {
                List<Holder<Enchantment>> enchantments = itemEnchantments.keySet().stream().toList();
                int count = Math.min(4, enchantments.size());
                for (int i = 0; i < count; i++) {
                    int randomIndex = level.random.nextIntBetweenInclusive(0, enchantments.size() - 1);
                    Holder<Enchantment> randomEnchantment = enchantments.get(randomIndex);
                    boolean selected = false;
                    for (ResourceKey<Enchantment> enchantmentResourceKey : enchantmentMappings.keySet()) {
                        if (randomEnchantment.is(enchantmentResourceKey)) {
                            result.add(enchantmentMappings.get(enchantmentResourceKey));
                            selected = true;
                            break;
                        }
                    }
                    if (!selected) {
                        randomIndex = level.random.nextIntBetweenInclusive(0, otherTemplate.size() - 1);
                        result.add(otherTemplate.get(randomIndex));
                    }
                }

                if (Set.copyOf(result).size() == 4) {
                    result.add(ModItems.TRANSCENDIUM_UPGRADE_SMITHING_TEMPLATE.get());
                }

                for (Item item : result) {
                    level.addFreshEntity(new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), new ItemStack(item)));
                }
            }
        }
    }
}
