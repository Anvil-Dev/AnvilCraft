package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 消耗燃烧加热器燃料的配方结果。
 *
 * <p>当铁砧合成成功时，检查下方是否存在点燃状态的燃烧加热器，
 * 如果存在则消耗燃烧时间。</p>
 */
public record ConsumeBurningHeaterFuel(int fuelCostTicks) implements IRecipeOutcome<ConsumeBurningHeaterFuel> {
    public static final InWorldRecipeData<Boolean> FUEL_CONSUMED =
        InWorldRecipeData.of(AnvilCraft.of("burning_heater_fuel_consumed"), false);

    @Override
    public IRecipeOutcome.Type<ConsumeBurningHeaterFuel> getType() {
        return ModRecipeOutcomeTypes.CONSUME_BURNING_HEATER_FUEL.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        if (context.get(FUEL_CONSUMED)) return;
        context.put(FUEL_CONSUMED, true);

        ServerLevel level = context.getLevel();
        Vec3 pos = context.getPos();
        BlockPos heaterPos = BlockPos.containing(pos.x(), pos.y() + 0.5, pos.z()).below(2);
        BlockState state = level.getBlockState(heaterPos);

        if (!(state.getBlock() instanceof BurningHeaterBlock)) return;
        if (state.getValue(BurningHeaterBlock.LEVEL) != 2) return;
        if (!(level.getBlockEntity(heaterPos) instanceof BurningHeaterBlockEntity be)) return;

        be.consumeBurnTime(this.fuelCostTicks);
    }

    public static class Type implements IRecipeOutcome.Type<ConsumeBurningHeaterFuel> {
        @Override
        public MapCodec<ConsumeBurningHeaterFuel> codec() {
            return MapCodec.unit(new ConsumeBurningHeaterFuel(240 * 20));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConsumeBurningHeaterFuel> streamCodec() {
            return StreamCodec.of(
                (buf, value) -> ByteBufCodecs.VAR_INT.encode(buf, value.fuelCostTicks),
                buf -> new ConsumeBurningHeaterFuel(ByteBufCodecs.VAR_INT.decode(buf))
            );
        }
    }
}
