package dev.dubhe.anvilcraft.data.advancement;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootTables;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
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

        provider.accept(root);
        provider.accept(crabClaw);
    }
}
