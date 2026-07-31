package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletRaffleProbability;
import dev.dubhe.anvilcraft.inventory.SmithingTemplateFavorites;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModDataAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AnvilCraft.MOD_ID);

    public static final Supplier<AttachmentType<AmuletRaffleProbability>> AMULET_RAFFLE_PROBABILITY =
        ModDataAttachments.ATTACHMENT_TYPES.register(
        "amulet_raffle_probability",
        () -> AttachmentType.builder(() -> AmuletRaffleProbability.EMPTY)
            .serialize(AmuletRaffleProbability.CODEC)
            .copyOnDeath()
            .build()
    );

    public static final Supplier<AttachmentType<Boolean>> ZOMBIFICATED_BY_CURSE = ModDataAttachments.ATTACHMENT_TYPES.register(
        "zombificated_by_curse",
        () -> AttachmentType.builder(() -> false)
            .serialize(Codec.BOOL.fieldOf("zombificated_by_curse"))
            .build()
    );

    public static final Supplier<AttachmentType<SmithingTemplateFavorites>> SMITHING_TEMPLATE_FAVORITES =
        ModDataAttachments.ATTACHMENT_TYPES.register(
            "smithing_template_favorites",
            () -> AttachmentType.builder(() -> SmithingTemplateFavorites.EMPTY)
                .serialize(SmithingTemplateFavorites.CODEC)
                .copyOnDeath()
                .build()
        );

    public static void register(IEventBus eventBus) {
        ModDataAttachments.ATTACHMENT_TYPES.register(eventBus);
    }
}
