package dev.dubhe.anvilcraft.api.advancement;

import com.google.common.collect.Lists;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerClickBlockTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHammerHurtEntityTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilHitPiezoelectricCrystalTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilLootingTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.AnvilOnGroundTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.ConvertBeaconTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DevourerDevourTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.DispenserRepairIronGolem;
import dev.dubhe.anvilcraft.advancements.criterion.FireReforgeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.HeatCollectorTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.InWorldRecipeTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MagnetLiftingAnvilTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MilkTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.MineralFountainCreateTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerPlaceTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlacerShuttleTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerKilledEntityByAnvilHammerTrigger;
import dev.dubhe.anvilcraft.advancements.criterion.PlayerWearAnvilHammerTrigger;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.criterion.ConsumeItemTrigger;
import net.minecraft.advancements.criterion.DamagePredicate;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.PlayerHurtEntityTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.advancements.criterion.RecipeCraftedTrigger;
import net.minecraft.advancements.criterion.SlotsPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AdvancementLineHelper {
    private final String namespace;
    private @Nullable AdvancementHolder parent;

    public AdvancementLineHelper(String namespace) {
        this.namespace = namespace;
    }

    public AdvancementHelper next() {
        return new AdvancementHelper(this);
    }

    public AdvancementLineHelper createBranch() {
        AdvancementLineHelper branch = new AdvancementLineHelper(this.namespace);
        branch.parent = this.parent;
        return branch;
    }
    
    public static class AdvancementHelper {
        private final AdvancementLineHelper lineHelper;
        private final Advancement.Builder current;

        public AdvancementHelper(AdvancementLineHelper lineHelper) {
            this.lineHelper = lineHelper;
            this.current = Advancement.Builder.advancement();
            if (lineHelper.parent != null) this.current.parent(lineHelper.parent);
        }

        public AdvancementHelper display(
            ItemStackTemplate icon,
            Component title,
            Component description,
            @Nullable Identifier background,
            AdvancementType type,
            boolean showToast,
            boolean announceChat,
            boolean hidden
        ) {
            this.current.display(icon, title, description, background, type, showToast, announceChat, hidden);
            return this;
        }

        public AdvancementHelper display(
            ItemLike icon,
            Component title,
            Component description,
            @Nullable Identifier background,
            AdvancementType type,
            boolean showToast,
            boolean announceChat,
            boolean hidden
        ) {
            this.current.display(icon, title, description, background, type, showToast, announceChat, hidden);
            return this;
        }

        public AdvancementHelper display(DisplayInfo display) {
            this.current.display(display);
            return this;
        }

        public AdvancementHelper rewards(AdvancementRewards rewards) {
            this.current.rewards(rewards);
            return this;
        }

        public AdvancementHelper rewards(AdvancementRewards.Builder rewards) {
            this.current.rewards(rewards);
            return this;
        }

        public AdvancementHelper rewardLoot(ResourceKey<LootTable> tableKey) {
            return this.rewards(AdvancementRewards.Builder.loot(tableKey));
        }

        // Requirements

        public AdvancementHelper requirements(AdvancementRequirements.Strategy strategy) {
            this.current.requirements(strategy);
            return this;
        }

        public AdvancementHelper requirements(AdvancementRequirements requirements) {
            this.current.requirements(requirements);
            return this;
        }

        public AdvancementHelper requireAll() {
            return this.requirements(AdvancementRequirements.Strategy.AND);
        }

        public AdvancementHelper requireAny() {
            return this.requirements(AdvancementRequirements.Strategy.OR);
        }

        @SafeVarargs
        public final AdvancementHelper requireAdvs(List<String>... requirements) {
            return this.requirements(new AdvancementRequirements(List.of(requirements)));
        }

        // Criterion Wrappers

        public AdvancementHelper addCriterion(String key, Criterion<?> criterion) {
            this.current.addCriterion(key, criterion);
            return this;
        }

        public AdvancementHelper playerFirstDetected(String key) {
            return this.addCriterion(key, PlayerTrigger.TriggerInstance.tick());
        }

        public AdvancementHelper hasItems(HolderGetter<Item> items, TagKey<Item> tag) {
            return this.addCriterion(
                "has_" + tag.location().toShortString().replace(':', '_'),
                InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, tag))
            );
        }

        public AdvancementHelper hasItems(String key, HolderGetter<Item> items, TagKey<Item> tag) {
            return this.addCriterion(key, InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items, tag)));
        }

        public AdvancementHelper hasItems(String key, ItemLike... items) {
            return this.addCriterion(key, InventoryChangeTrigger.TriggerInstance.hasItems(items));
        }

        public AdvancementHelper hasItemAny(String keyPrefix, ItemLike... items) {
            for (ItemLike item : items) {
                this.addCriterion(
                    keyPrefix + BuiltInRegistries.ITEM.getKey(item.asItem()).getPath(),
                    InventoryChangeTrigger.TriggerInstance.hasItems(item)
                );
            }
            return this;
        }

        public AdvancementHelper consumeItem(String key, HolderGetter<Item> items, ItemLike item) {
            return this.addCriterion(key, ConsumeItemTrigger.TriggerInstance.usedItem(items, item.asItem()));
        }

        public AdvancementHelper recipe(String key, Identifier recipeId) {
            return this.addCriterion(key, RecipeCraftedTrigger.TriggerInstance.craftedItem(
                ResourceKey.create(Registries.RECIPE, recipeId)
            ));
        }

        public AdvancementHelper recipeMod(String key, String namespace, String recipeId) {
            return this.recipe(key, Identifier.fromNamespaceAndPath(namespace, recipeId));
        }

        public AdvancementHelper recipeAnc(String key, String recipeId) {
            return this.recipe(key, AnvilCraft.of(recipeId));
        }

        public AdvancementHelper inWorldRecipe(String key) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipe());
        }

        public AdvancementHelper inWorldRecipe(String key, Identifier recipeId) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipe(recipeId));
        }

        public AdvancementHelper inWorldRecipeMod(String key, String namespace, String recipeId) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipe(
                Identifier.fromNamespaceAndPath(namespace, recipeId)
            ));
        }

        public AdvancementHelper inWorldRecipeAnc(String key, String recipeId) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipe(AnvilCraft.of(recipeId)));
        }

        public AdvancementHelper inWorldRecipeType(String key, Identifier typeId) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipeType(typeId));
        }

        public AdvancementHelper inWorldRecipeTypeMod(String key, String namespace, String typeId) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipeType(
                Identifier.fromNamespaceAndPath(namespace, typeId)
            ));
        }

        public AdvancementHelper inWorldRecipeTypeAnc(String key, String typeId) {
            return this.addCriterion(key, InWorldRecipeTrigger.TriggerInstance.inWorldRecipeType(AnvilCraft.of(typeId)));
        }

        public AdvancementHelper milk(String key) {
            return this.addCriterion(key, MilkTrigger.TriggerInstance.milk());
        }

        public AdvancementHelper anvilLooting(String key) {
            return this.addCriterion(key, AnvilLootingTrigger.TriggerInstance.looting());
        }

        public AdvancementHelper anvilLooting(String key, HolderGetter<EntityType<?>> lookup, EntityType<?> type) {
            return this.addCriterion(key, AnvilLootingTrigger.TriggerInstance.looting(lookup, type));
        }

        public AdvancementHelper repairIronGolem(String key) {
            return this.addCriterion(key, DispenserRepairIronGolem.TriggerInstance.repair());
        }

        public AdvancementHelper playerPlace(String key, Block block) {
            return this.addCriterion(key, ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        public AdvancementHelper playerPlace(String key, Supplier<? extends Block> block) {
            return this.addCriterion(key, ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block.get()));
        }

        public AdvancementHelper placerPlace(String key, Block block) {
            return this.addCriterion(key, PlacerPlaceTrigger.TriggerInstance.placeBlock(block));
        }

        public AdvancementHelper placerPlace(String key, Supplier<? extends Block> block) {
            return this.addCriterion(key, PlacerPlaceTrigger.TriggerInstance.placeBlock(block.get()));
        }

        public AdvancementHelper devourerDevour(String key, Block block) {
            return this.addCriterion(key, DevourerDevourTrigger.TriggerInstance.devourBlock(block));
        }

        public AdvancementHelper devourerDevour(String key, Supplier<? extends Block> block) {
            return this.addCriterion(key, DevourerDevourTrigger.TriggerInstance.devourBlock(block.get()));
        }

        public AdvancementHelper liftingAnvil(String key) {
            return this.addCriterion(key, MagnetLiftingAnvilTrigger.TriggerInstance.liftingAnvil());
        }

        public AdvancementHelper anvilOnGround(String key) {
            return this.addCriterion(key, AnvilOnGroundTrigger.TriggerInstance.onGround());
        }

        public AdvancementHelper hammerLeftClick(String key) {
            return this.addCriterion(key, AnvilHammerClickBlockTrigger.TriggerInstance.leftClickBlock());
        }

        public AdvancementHelper hammerRightClick(String key) {
            return this.addCriterion(key, AnvilHammerClickBlockTrigger.TriggerInstance.rightClickBlock());
        }

        public AdvancementHelper hammerShiftRightClick(String key) {
            return this.addCriterion(key, AnvilHammerClickBlockTrigger.TriggerInstance.shiftRightClickBlock());
        }

        public AdvancementHelper hammerHurt(String key) {
            return this.addCriterion(key, AnvilHammerHurtEntityTrigger.TriggerInstance.hurtEntity());
        }

        public AdvancementHelper hammerHurt(String key, float damage) {
            return this.addCriterion(key, AnvilHammerHurtEntityTrigger.TriggerInstance.hurtEntity(damage));
        }

        public AdvancementHelper hammerKill(String key, HolderGetter<EntityType<?>> lookup, EntityType<?> type) {
            return this.addCriterion(key, PlayerKilledEntityByAnvilHammerTrigger.TriggerInstance.killedEntity(lookup, type));
        }

        public AdvancementHelper wearHammer(String key) {
            return this.addCriterion(key, PlayerWearAnvilHammerTrigger.TriggerInstance.wear());
        }

        public AdvancementHelper hitPiezoelectricCrystal(String key) {
            return this.addCriterion(key, AnvilHitPiezoelectricCrystalTrigger.TriggerInstance.hit());
        }

        public AdvancementHelper convertBeacon(String key) {
            return this.addCriterion(key, ConvertBeaconTrigger.TriggerInstance.convertBeacon());
        }

        public AdvancementHelper hurt(String key, ItemPredicate.Builder builder, float damage) {
            return this.addCriterion(key, PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntity(
                DamagePredicate.Builder.damageInstance().takenDamage(MinMaxBounds.Doubles.atLeast(damage)),
                Optional.of(
                    EntityPredicate.Builder.entity()
                        .slots(new SlotsPredicate(Map.of(Objects.requireNonNull(SlotRanges.nameToIds("weapon")), builder.build())))
                        .build()
                )
            ));
        }

        @SuppressWarnings("deprecation")
        public AdvancementHelper hurt(String key, float damage, ItemLike... items) {
            return this.addCriterion(key, PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntityWithDamage(
                DamagePredicate.Builder.damageInstance()
                    .type(
                        DamageSourcePredicate.Builder.damageType()
                            .source(
                                EntityPredicate.Builder.entity()
                                    .slots(new SlotsPredicate(Map.of(
                                        Objects.requireNonNull(SlotRanges.nameToIds("weapon")),
                                        new ItemPredicate(
                                            Optional.of(HolderSet.direct(Lists.transform(
                                                List.of(items),
                                                item -> item.asItem().builtInRegistryHolder()
                                            ))),
                                            MinMaxBounds.Ints.ANY,
                                            DataComponentMatchers.ANY
                                        )
                                    )))
                            )
                    )
                    .takenDamage(MinMaxBounds.Doubles.atLeast(damage))
            ));
        }

        public AdvancementHelper fireReforge(String key) {
            return this.addCriterion(key, FireReforgeTrigger.TriggerInstance.fireReforge());
        }

        public AdvancementHelper heatCollectOn(String key, BlockStatePredicate.Builder collecting) {
            return this.addCriterion(key, HeatCollectorTrigger.TriggerInstance.collectOn(collecting));
        }

        public AdvancementHelper heatCollectorOutput(String key, MinMaxBounds.Ints output) {
            return this.addCriterion(key, HeatCollectorTrigger.TriggerInstance.output(output));
        }

        public AdvancementHelper mineralFountainCreate(String key) {
            return this.addCriterion(key, MineralFountainCreateTrigger.TriggerInstance.create());
        }

        public AdvancementHelper placerShuttle(String key) {
            return this.addCriterion(key, PlacerShuttleTrigger.TriggerInstance.create());
        }

        public AdvancementHolder build(String id) {
            AdvancementHolder holder = this.current.build(
                Identifier.fromNamespaceAndPath(this.lineHelper.namespace, this.lineHelper.namespace + "/" + id)
            );
            this.lineHelper.parent = holder;
            return holder;
        }

        public AdvancementHolder save(Consumer<AdvancementHolder> output, String id) {
            AdvancementHolder advancement = this.build(id);
            output.accept(advancement);
            return advancement;
        }
    }
}
