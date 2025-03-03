package dev.dubhe.anvilcraft.event;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.loot.functions.CurseLootItemFunction;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LootTableEventListener {
    /**
     * 战利品表加载事件侦听器
     *
     * @param event 战利品表加载事件
     */
    @SubscribeEvent
    public static void lootTable(@NotNull LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        LootTable table = event.getTable();
        if (Blocks.BUDDING_AMETHYST.getLootTable().location().equals(id)) {
            table.addPool(new LootPool.Builder()
                .add(LootItem.lootTableItem(ModItems.GEODE))
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                .build());
        }
        if (BuiltInLootTables.SPAWN_BONUS_CHEST.location().equals(id)) {
            table.addPool(new LootPool.Builder()
                .add(LootItem.lootTableItem(ModItems.GEODE))
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6)))
                .build());
        }
        if (BuiltInLootTables.VILLAGE_WEAPONSMITH.location().equals(id)) {
            table.addPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
                    .setWeight(2)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(ModItems.ROYAL_STEEL_INGOT)
                    .setWeight(3)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(ModItems.ROYAL_STEEL_NUGGET)
                    .setWeight(10)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 5))))
                .build());
        }
        if (EntityType.ZOMBIFIED_PIGLIN.getDefaultLootTable().location().equals(id)) {
            CompoundTag cursedTag = new CompoundTag();
            cursedTag.putBoolean("anvilcraft:zombificated_by_curse", true);
            CompoundTag attachmentTag = new CompoundTag();
            attachmentTag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, cursedTag);
            LootItemFunction convertToCursed = new CurseLootItemFunction(List.of(
                LootItemEntityPropertyCondition.hasProperties(
                    LootContext.EntityTarget.THIS,
                    EntityPredicate.Builder.entity()
                        .of(EntityType.ZOMBIFIED_PIGLIN)
                        .nbt(new NbtPredicate(attachmentTag))
                        .build()
                ).build()
            ));
            table.functions = ImmutableList.<LootItemFunction>builder()
                .addAll(table.functions)
                .add(convertToCursed)
                .build();
            table.compositeFunction = LootItemFunctions.compose(table.functions);
        }
        event.setTable(table);
    }
}
