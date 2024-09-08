package dev.dubhe.anvilcraft.data.generator.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
    private static final EnchantmentPredicate BEHEADING_ANY =
        new EnchantmentPredicate(ModEnchantments.BEHEADING, MinMaxBounds.Ints.ANY);
    private static final EnchantmentPredicate BEHEADING_1 =
        new EnchantmentPredicate(ModEnchantments.BEHEADING, MinMaxBounds.Ints.between(1, 1));
    private static final EnchantmentPredicate BEHEADING_2 =
        new EnchantmentPredicate(ModEnchantments.BEHEADING, MinMaxBounds.Ints.between(2, 2));
    private static final EnchantmentPredicate BEHEADING_3 =
        new EnchantmentPredicate(ModEnchantments.BEHEADING, MinMaxBounds.Ints.between(3, 3));


    public static final ResourceKey<LootTable> SKELETON_LOOT = createKey("entities/beheading/skeleton");
    public static final ResourceKey<LootTable> WITHER_SKELETON_LOOT = createKey("entities/beheading/wither_skeleton");
    public static final ResourceKey<LootTable> ZOMBIE_LOOT = createKey("entities/beheading/zombie");
    public static final ResourceKey<LootTable> PLAYER_LOOT = createKey("entities/beheading/player");
    public static final ResourceKey<LootTable> CREEPER_LOOT = createKey("entities/beheading/creeper");
    public static final ResourceKey<LootTable> ENDER_DRAGON_LOOT = createKey("entities/beheading/ender_dragon");
    public static final ResourceKey<LootTable> PIGLIN_LOOT = createKey("entities/beheading/piglin");

    private static final LootTable.Builder SKELETON = genBeheading(Items.SKELETON_SKULL);
    private static final LootTable.Builder WITHER_SKELETON;

    private static ResourceKey<LootTable> createKey(String path){
        return ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of(path));
    }

    static {
        ItemEnchantments.Mutable beheading1 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        beheading1.set(ModEnchantments.BEHEADING, 1);
        ItemEnchantments.Mutable beheading2 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        beheading2.set(ModEnchantments.BEHEADING, 1);
        ItemEnchantments.Mutable beheading3 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        beheading3.set(ModEnchantments.BEHEADING, 1);
        WITHER_SKELETON = LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.WITHER_SKELETON_SKULL))
                    .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                    ItemPredicate.Builder.item().hasComponents(
                                        DataComponentPredicate.builder()
                                            .expect(
                                                DataComponents.ENCHANTMENTS,
                                                beheading1.toImmutable()
                                            ).build()
                                    )
                                ).build()
                            )
                    ))
                    .when(LootItemRandomChanceCondition.randomChance(0.07f))
                    .when(LootItemKilledByPlayerCondition.killedByPlayer())
            )
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.WITHER_SKELETON_SKULL))
                    .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasComponents(
                                    DataComponentPredicate.builder()
                                        .expect(
                                            DataComponents.ENCHANTMENTS,
                                            beheading2.toImmutable()
                                        ).build()
                                )
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
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasComponents(
                                    DataComponentPredicate.builder()
                                        .expect(
                                            DataComponents.ENCHANTMENTS,
                                            beheading3.toImmutable()
                                        ).build()
                                )
                            ).build())
                    ))
                    .when(LootItemRandomChanceCondition.randomChance(0.11f))
                    .when(LootItemKilledByPlayerCondition.killedByPlayer())
            );
    }

    private static final LootTable.Builder PLAYER = genBeheading(Items.PLAYER_HEAD);

    private static final LootTable.Builder ZOMBIE = genBeheading(Items.ZOMBIE_HEAD);
    private static final LootTable.Builder CREEPER = genBeheading(Items.CREEPER_HEAD);
    private static final LootTable.Builder ENDER_DRAGON = genBeheading(Items.DRAGON_HEAD);
    private static final LootTable.Builder PIGLIN = genBeheading(Items.PIGLIN_HEAD);

    public static final Map<EntityType<?>, ResourceLocation> BEHEADING = new HashMap<>() {
        {
            this.put(EntityType.SKELETON, SKELETON_LOOT.location());
            this.put(EntityType.WITHER_SKELETON, WITHER_SKELETON_LOOT.location());
            this.put(EntityType.PLAYER, PLAYER_LOOT.location());
            this.put(EntityType.ZOMBIE, ZOMBIE_LOOT.location());
            this.put(EntityType.CREEPER, CREEPER_LOOT.location());
            this.put(EntityType.ENDER_DRAGON, ENDER_DRAGON_LOOT.location());
            this.put(EntityType.PIGLIN, PIGLIN_LOOT.location());
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
        ItemEnchantments.Mutable beheading1 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        beheading1.set(ModEnchantments.BEHEADING, 1);
        ItemEnchantments.Mutable beheading2 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        beheading2.set(ModEnchantments.BEHEADING, 2);
        ItemEnchantments.Mutable beheading3 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        beheading3.set(ModEnchantments.BEHEADING, 3);
        return LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(item))
                    .when(LootItemEntityPropertyCondition.hasProperties(
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasComponents(
                                    DataComponentPredicate.builder()
                                        .expect(
                                            DataComponents.ENCHANTMENTS,
                                            beheading1.toImmutable()
                                        ).build()
                                )
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
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasComponents(
                                    DataComponentPredicate.builder()
                                        .expect(
                                            DataComponents.ENCHANTMENTS,
                                            beheading2.toImmutable()
                                        ).build()
                                )
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
                        LootContext.EntityTarget.DIRECT_ATTACKER,
                        EntityPredicate.Builder.entity()
                            .equipment(EntityEquipmentPredicate.Builder.equipment().mainhand(
                                ItemPredicate.Builder.item().hasComponents(
                                    DataComponentPredicate.builder()
                                        .expect(
                                            DataComponents.ENCHANTMENTS,
                                            beheading3.toImmutable()
                                        ).build()
                                )
                            ).build())
                    ))
                    .when(LootItemRandomChanceCondition.randomChance(0.03f))
                    .when(LootItemKilledByPlayerCondition.killedByPlayer())
            );
    }
}
