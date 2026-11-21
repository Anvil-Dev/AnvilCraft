package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.List;

public final class JeiCauldronUtil {
    private JeiCauldronUtil() {
    }

    public static int requiredAmount(HasCauldronSimple cauldron) {
        if (cauldron.fluid().isNegate()) return cauldron.consume();
        return Math.max(
            cauldron.consume(),
            cauldron.fluid().amount().flatMap(MinMaxBounds.Ints::min).orElse(0)
        );
    }

    public static List<FluidStack> containerFluids(HasCauldronSimple cauldron) {
        List<FluidStack> fluids = new ArrayList<>();
        if (cauldron.hasFluid()) {
            fluids.addAll(JeiFluidUtil.getDisplayFluids(cauldron.fluid(), JeiCauldronUtil.requiredAmount(cauldron)));
        }
        for (FluidStackTemplate template : cauldron.transforms()) {
            fluids.add(template.create());
        }
        return fluids;
    }

    public static boolean needsLargeCauldron(List<FluidStack> fluids) {
        return fluids.stream().anyMatch(fluid -> fluid.getFluidType() == ModFluids.LIQUID_ENCHANTMENT_TYPE.get());
    }

    public static boolean exceedsBucket(List<FluidStack> fluids) {
        return fluids.stream().anyMatch(fluid -> fluid.getAmount() > FluidType.BUCKET_VOLUME);
    }

    public static boolean fitsCauldron(List<FluidStack> fluids) {
        return fluids.stream().allMatch(fluid -> CauldronFluidContent.getForFluid(fluid.getFluid()) != null);
    }

    public static BlockState smallContainer(List<FluidStack> fluids, boolean fishTankPhase) {
        if (!fluids.isEmpty() && !JeiCauldronUtil.fitsCauldron(fluids)) {
            return ModBlocks.FISH_TANK.getDefaultState();
        }
        return fishTankPhase ? ModBlocks.FISH_TANK.getDefaultState() : Blocks.CAULDRON.defaultBlockState();
    }
}
