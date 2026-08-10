package dev.dubhe.anvilcraft.init.entity;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.predicate.FallingBlockPredicate;
import net.minecraft.advancements.criterion.EntitySubPredicate;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntitySubPredicates {
    private static final DeferredRegister<MapCodec<? extends EntitySubPredicate>> REGISTER = DeferredRegister.create(
        Registries.ENTITY_SUB_PREDICATE_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<
        MapCodec<? extends EntitySubPredicate>, MapCodec<FallingBlockPredicate>
    > FALLING_BLOCK = ModEntitySubPredicates.REGISTER
        .register("falling_block", () -> FallingBlockPredicate.CODEC);

    public static void register(IEventBus modEventBus) {
        ModEntitySubPredicates.REGISTER.register(modEventBus);
    }
}
