package dev.dubhe.anvilcraft.data.advancement;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootTables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;

public class AnvilCraftAdvancement {
    public static void init(RegistrateAdvancementProvider provider) {
        AdvancementHolder root = Advancement.Builder.advancement()
            .display(
                ModBlocks.ROYAL_ANVIL.asItem(),
                Component.translatable("advancements.anvilcraft.root.title"),
                Component.translatable("advancements.anvilcraft.root.description"),
                AnvilCraft.of("textures/gui/advancements/background.png"),
                AdvancementType.TASK,
                false,
                true,
                false)
            .addCriterion("join", PlayerTrigger.TriggerInstance.tick())
            .rewards(AdvancementRewards.Builder.loot(ModLootTables.ADVANCEMENT_ROOT))
            .build(AnvilCraft.of("anvilcraft/root"));

        AdvancementHolder crabClaw = Advancement.Builder.advancement()
            .parent(root)
            .display(
                ModItems.CRAB_CLAW,
                Component.translatable("advancements.anvilcraft.crab_claw.title"),
                Component.translatable("advancements.anvilcraft.crab_claw.description"),
                null,
                AdvancementType.TASK,
                true,
                true,
                false)
            .addCriterion("crab_claw", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.CRAB_CLAW))
            .build(AnvilCraft.of("anvilcraft/crab_claw"));

        AdvancementHolder dragonRod = Advancement.Builder.advancement()
            .parent(root)
            .display(
                ModItems.DRAGON_ROD,
                Component.translatable("advancements.anvilcraft.dragon_rod.title"),
                Component.translatable("advancements.anvilcraft.dragon_rod.description"),
                null,
                AdvancementType.CHALLENGE,
                true,
                true,
                false)
            .addCriterion("dragon_rod", InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(ModItemTags.DRAGON_ROD)))
            .rewards(AdvancementRewards.Builder.experience(50))
            .build(AnvilCraft.of("anvilcraft/dragon_rod"));

        AdvancementHolder amulet = Advancement.Builder.recipeAdvancement()
            .parent(root)
            .addCriterion("get_any_amulet", InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(ModItemTags.AMULET)))
            .build(AnvilCraft.of("anvilcraft/advanced_amulet"));

        provider.accept(root);
        provider.accept(crabClaw);
        provider.accept(dragonRod);
        provider.accept(amulet);
    }
}
