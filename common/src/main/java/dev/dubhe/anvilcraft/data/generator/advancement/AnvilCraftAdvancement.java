package dev.dubhe.anvilcraft.data.generator.advancement;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class AnvilCraftAdvancement {
    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateAdvancementProvider provider) {
        Advancement root = Advancement.Builder.advancement()
            .display(
                ModBlocks.ROYAL_ANVIL,
                Component.translatable("advancements.anvilcraft.root.title"),
                Component.translatable("advancements.anvilcraft.root.description"),
                AnvilCraft.of("textures/gui/advancements/background.png"),
                FrameType.TASK,
                false,
                true,
                false
            )
            .addCriterion("join", PlayerTrigger.TriggerInstance.tick())
            .rewards(AdvancementRewards.Builder.loot(AnvilCraft.of("advancement/root")))
            .build(AnvilCraft.of("anvilcraft/root"));
        Advancement crabClaw = Advancement.Builder.advancement()
            .parent(root)
            .display(
                ModItems.CRAB_CLAW,
                Component.translatable("advancements.anvilcraft.crab_claw.title"),
                Component.translatable("advancements.anvilcraft.crab_claw.description"),
                null,
                FrameType.TASK,
                true,
                true,
                false
            )
            .addCriterion("crab_claw", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.CRAB_CLAW))
            .build(AnvilCraft.of("anvilcraft/crab_claw"));

        provider.accept(root);
        provider.accept(crabClaw);
    }
}
