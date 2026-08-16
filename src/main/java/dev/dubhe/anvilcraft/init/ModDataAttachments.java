package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.amulet.AmuletRaffleProbability;
import dev.dubhe.anvilcraft.inventory.SmithingTemplateFavorites;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.AnvilCraft.MOD_ID;

public class ModDataAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public static final Supplier<AttachmentType<Float>> DISCOUNT_RATE = ATTACHMENT_TYPES.register(
        "discount_rate", () -> AttachmentType.builder(() -> 0f).build());

    public static final Supplier<AttachmentType<Boolean>> ZOMBIFICATED_BY_CURSE = ATTACHMENT_TYPES.register(
        "zombificated_by_curse", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    public static final Supplier<AttachmentType<Boolean>> ENCHANTED_GOLD_BARTER = ATTACHMENT_TYPES.register(
        "enchanted_gold_barter", () -> AttachmentType.builder(() -> false).build());

    public static final Supplier<AttachmentType<AmuletRaffleProbability>> AMULET_RAFFLE_PROBABILITY = ATTACHMENT_TYPES.register(
        "amulet_raffle_probability", () -> AttachmentType.builder(() -> AmuletRaffleProbability.EMPTY)
            .serialize(AmuletRaffleProbability.CODEC.codec()).copyOnDeath().build());

    public static final Supplier<AttachmentType<SmithingTemplateFavorites>> SMITHING_TEMPLATE_FAVORITES =
        ATTACHMENT_TYPES.register(
            "smithing_template_favorites",
            () -> AttachmentType.builder(() -> SmithingTemplateFavorites.EMPTY)
                .serialize(SmithingTemplateFavorites.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
