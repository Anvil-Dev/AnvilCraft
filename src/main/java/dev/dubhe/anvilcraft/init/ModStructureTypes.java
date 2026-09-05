package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.TheMonolithPiece;
import dev.dubhe.anvilcraft.worldgen.TheMonolithStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registers structure types used by AnvilCraft's built-in dimensions. */
public final class ModStructureTypes {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(
        Registries.STRUCTURE_TYPE, AnvilCraft.MOD_ID
    );
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(
        Registries.STRUCTURE_PIECE, AnvilCraft.MOD_ID
    );

    public static final Supplier<StructureType<TheMonolithStructure>> THE_MONOLITH = STRUCTURE_TYPES.register(
        "the_monolith", () -> () -> TheMonolithStructure.CODEC
    );
    public static final Supplier<StructurePieceType> THE_MONOLITH_PIECE = STRUCTURE_PIECES.register(
        "the_monolith", () -> (StructurePieceType.ContextlessType) TheMonolithPiece::new
    );

    private ModStructureTypes() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
