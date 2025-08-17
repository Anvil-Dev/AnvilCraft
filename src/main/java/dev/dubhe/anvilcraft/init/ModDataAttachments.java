package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.amulet.AmuletRaffleProbability;
import net.minecraft.nbt.CompoundTag;
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

    public static final Supplier<AttachmentType<AmuletRaffleProbability>> AMULET_RAFFLE_PROBABILITY = ATTACHMENT_TYPES.register(
        "amulet_raffle_probability", () -> AttachmentType.builder(() -> AmuletRaffleProbability.EMPTY)
            .serialize(AmuletRaffleProbability.CODEC).copyOnDeath().build());

    public static final Supplier<AttachmentType<CompoundTag>> SCARE_ENTITIES = ATTACHMENT_TYPES.register(
        "scare_entities", () -> AttachmentType.builder(CompoundTag::new).serialize(CompoundTag.CODEC).build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
