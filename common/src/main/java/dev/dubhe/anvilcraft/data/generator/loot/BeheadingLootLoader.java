package dev.dubhe.anvilcraft.data.generator.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.FillPlayerHead;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BeheadingLootLoader {
    private static final EnchantmentPredicate BEHEADING_1 =
        new EnchantmentPredicate(ModEnchantments.BEHEADING.get(), MinMaxBounds.Ints.between(1, 1));
    private static final EnchantmentPredicate BEHEADING_2 =
        new EnchantmentPredicate(ModEnchantments.BEHEADING.get(), MinMaxBounds.Ints.between(2, 2));
    private static final EnchantmentPredicate BEHEADING_3 =
        new EnchantmentPredicate(ModEnchantments.BEHEADING.get(), MinMaxBounds.Ints.between(3, 3));


    public static final ResourceLocation SKELETON_LOOT = AnvilCraft.of("entities/beheading/skeleton");
    public static final ResourceLocation WITHER_SKELETON_LOOT = AnvilCraft.of("entities/beheading/wither_skeleton");
    public static final ResourceLocation ZOMBIE_LOOT = AnvilCraft.of("entities/beheading/zombie");
    public static final ResourceLocation PLAYER_LOOT = AnvilCraft.of("entities/beheading/player");
    public static final ResourceLocation CREEPER_LOOT = AnvilCraft.of("entities/beheading/creeper");
    public static final ResourceLocation ENDER_DRAGON_LOOT = AnvilCraft.of("entities/beheading/ender_dragon");
    public static final ResourceLocation PIGLIN_LOOT = AnvilCraft.of("entities/beheading/piglin");

    private static final LootTable.Builder SKELETON = genBeheading(Items.SKELETON_SKULL);
    private static final LootTable.Builder WITHER_SKELETON = LootTable.lootTable()
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Items.WITHER_SKELETON_SKULL))
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_1).build()
                        ).build())
                ))
                .when(LootItemRandomChanceCondition.randomChance(0.07f))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Items.WITHER_SKELETON_SKULL))
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_2).build()
                        ).build())
                ))
                .when(LootItemRandomChanceCondition.randomChance(0.09f))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Items.WITHER_SKELETON_SKULL))
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_3).build()
                        ).build())
                ))
                .when(LootItemRandomChanceCondition.randomChance(0.11f))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
        );
    private static final LootTable.Builder PLAYER = LootTable.lootTable()
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(
                    LootItem.lootTableItem(Items.PLAYER_HEAD)
                        .apply(FillPlayerHead.fillPlayerHead(LootContext.EntityTarget.THIS))
                )
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_1).build()
                        ).build())
                ))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(0.5f))
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(
                    LootItem.lootTableItem(Items.PLAYER_HEAD)
                        .apply(FillPlayerHead.fillPlayerHead(LootContext.EntityTarget.THIS))
                )
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_2).build()
                        ).build())
                ))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(0.8f))
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(
                    LootItem.lootTableItem(Items.PLAYER_HEAD)
                        .apply(FillPlayerHead.fillPlayerHead(LootContext.EntityTarget.THIS))
                )
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_3).build()
                        ).build())
                ))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
        );
    private static final LootTable.Builder ZOMBIE = genBeheading(Items.ZOMBIE_HEAD);
    private static final LootTable.Builder CREEPER = genBeheading(Items.CREEPER_HEAD);
    private static final LootTable.Builder ENDER_DRAGON =  LootTable.lootTable()
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Items.DRAGON_HEAD))
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_1).build()
                        ).build())
                ))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(0.5f))
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Items.DRAGON_HEAD))
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_2).build()
                        ).build())
                ))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(LootItemRandomChanceCondition.randomChance(0.8f))
        )
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(Items.DRAGON_HEAD))
                .when(LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.KILLER,
                    EntityPredicate.Builder.entity()
                        .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                            ItemPredicate.Builder.item().hasEnchantment(BEHEADING_3).build()
                        ).build())
                ))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
        );
    private static final LootTable.Builder PIGLIN = genBeheading(Items.PIGLIN_HEAD);

    public static final Map<EntityType<?>, ResourceLocation> BEHEADING = new HashMap<>() {
        {
            this.put(EntityType.SKELETON, SKELETON_LOOT);
            this.put(EntityType.WITHER_SKELETON, WITHER_SKELETON_LOOT);
            this.put(EntityType.PLAYER, PLAYER_LOOT);
            this.put(EntityType.ZOMBIE, ZOMBIE_LOOT);
            this.put(EntityType.CREEPER, CREEPER_LOOT);
            this.put(EntityType.ENDER_DRAGON, ENDER_DRAGON_LOOT);
            this.put(EntityType.PIGLIN, PIGLIN_LOOT);
        }
    };

    /**
     * 初始化斩首相关的战利品表
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLootTableProvider provider) {
        provider.addLootAction(LootContextParamSets.ENTITY, (bi) -> {
            bi.accept(SKELETON_LOOT, SKELETON);
            bi.accept(WITHER_SKELETON_LOOT, WITHER_SKELETON);
            bi.accept(PLAYER_LOOT, PLAYER);
            bi.accept(ZOMBIE_LOOT, ZOMBIE);
            bi.accept(CREEPER_LOOT, CREEPER);
            bi.accept(ENDER_DRAGON_LOOT, ENDER_DRAGON);
            bi.accept(PIGLIN_LOOT, PIGLIN);
        });
    }

    /**
     * @param type 实体类型
     * @return 斩首战利品表 ResourceLocation
     */
    public static ResourceLocation getBeheading(EntityType<?> type) {
        return BEHEADING.getOrDefault(
            type,
            AnvilCraft.of("entities/beheading/" + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath())
        );
    }

    private static LootTable.@NotNull Builder genBeheading(ItemLike item) {
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(item))
                    .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.KILLER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasEnchantment(BEHEADING_1).build()
                            ).build())
                    ))
                    .when(LootItemRandomChanceCondition.randomChance(0.01f))
                    .when(LootItemKilledByPlayerCondition.killedByPlayer())
            )
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(item))
                    .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.KILLER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasEnchantment(BEHEADING_2).build()
                            ).build())
                    ))
                    .when(LootItemRandomChanceCondition.randomChance(0.02f))
                    .when(LootItemKilledByPlayerCondition.killedByPlayer())
            )
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(item))
                    .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.KILLER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasEnchantment(BEHEADING_3).build()
                            ).build())
                    ))
                    .when(LootItemRandomChanceCondition.randomChance(0.03f))
                    .when(LootItemKilledByPlayerCondition.killedByPlayer())
            );
    }
}
