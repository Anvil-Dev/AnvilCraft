package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.util.IBlockStateInjector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;

import java.util.stream.Collectors;

@Mixin(BlockState.class)
abstract class BlockStateInjector extends BlockBehaviour.BlockStateBase implements IBlockStateInjector {
    protected BlockStateInjector(Block owner, ImmutableMap<Property<?>, Comparable<?>> values, MapCodec<BlockState> propertiesCodec) {
        super(owner, values, propertiesCodec);
    }

    @Override
    public JsonElement anvilcraft$toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.getBlock()).toString());
        if (!this.getValues().isEmpty()) {
            String stringBuilder = '[' +
                    this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")) +
                    ']';
            object.addProperty("state", stringBuilder);
        }
        return object;
    }
}
