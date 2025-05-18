package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ModifySpawnerAction(BlockPos fromPos, BlockPos toPos) {

    public static final MapCodec<ModifySpawnerAction> CODEC =
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            BlockPos.CODEC.fieldOf("fromPos").forGetter(ModifySpawnerAction::fromPos),
            BlockPos.CODEC.fieldOf("toPos").forGetter(ModifySpawnerAction::toPos)
        ).apply(inst, ModifySpawnerAction::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModifySpawnerAction> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, ModifySpawnerAction::fromPos,
            BlockPos.STREAM_CODEC, ModifySpawnerAction::toPos,
            ModifySpawnerAction::new
        );
}
