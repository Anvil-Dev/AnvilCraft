package dev.dubhe.anvilcraft.init.entity;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.predicate.FallingBlockPredicate;
import dev.dubhe.anvilcraft.predicate.InWaterOrBreathingPredicate;
import dev.dubhe.anvilcraft.predicate.NotInLavaPredicate;
import dev.dubhe.anvilcraft.predicate.NotInWaterPredicate;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntitySubPredicates {
    private static final DeferredRegister<MapCodec<? extends EntitySubPredicate>> REGISTER = DeferredRegister.create(
        Registries.ENTITY_SUB_PREDICATE_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<FallingBlockPredicate>> FALLING_BLOCK = REGISTER
        .register("falling_block", () -> FallingBlockPredicate.CODEC);
    public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<InWaterOrBreathingPredicate>>
        IN_WATER_OR_BREATHING = REGISTER.register("in_water_or_breathing", () -> InWaterOrBreathingPredicate.CODEC);
    public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<NotInWaterPredicate>> NOT_IN_WATER = REGISTER
        .register("not_in_water", () -> NotInWaterPredicate.CODEC);
    public static final DeferredHolder<MapCodec<? extends EntitySubPredicate>, MapCodec<NotInLavaPredicate>> NOT_IN_LAVA = REGISTER
        .register("not_in_lava", () -> NotInLavaPredicate.CODEC);

    public static void register(IEventBus modEventBus) {
        ModEntitySubPredicates.REGISTER.register(modEventBus);
    }
}
