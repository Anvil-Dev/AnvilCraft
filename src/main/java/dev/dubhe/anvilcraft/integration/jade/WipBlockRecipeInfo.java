package dev.dubhe.anvilcraft.integration.jade;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.workstation.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.workstation.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.workstation.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockProcessingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WipBlockRecipeInfo {
    private static final String PREFIX = "tooltip.anvilcraft.wip_block.jade.";
    private static final int MAX_ALTERNATIVES = 3;

    public static List<Component> getDetails(Recipe<?> recipe) {
        List<Component> details = new ArrayList<>();
        if (recipe instanceof BlockCompressRecipe compress) {
            List<Component> blocks = compress.getFirstInputBlock().getBlocks().stream()
                .map(holder -> (Component) holder.value().getName()).toList();
            addMaterial(details, "compress", blocks, 1);
        } else if (recipe instanceof ItemInjectRecipe inject) {
            for (ItemIngredientPredicate ingredient : inject.getInputItems()) {
                List<Component> items = Arrays.stream(ingredient.getItems()).map(ItemStackTemplate::create)
                    .map(ItemStack::getHoverName).distinct().toList();
                addMaterial(details, "inject", items, ingredient.count());
            }
        } else if (recipe instanceof BlockProcessingRecipe processing) {
            for (int i = 1; i < processing.getInputBlocks().size(); i++) {
                BlockStatePredicate predicate = processing.getInputBlocks().get(i);
                List<Component> blocks = predicate.constructStatesForRender().stream()
                    .filter(predicate::testWithoutEntity)
                    .map(WipBlockRecipeInfo::describeWorkBlock).distinct().toList();
                addAlternatives(details, "work_block", blocks);
            }
        }
        return details;
    }

    private static void addMaterial(List<Component> details, String action, List<Component> options, int count) {
        addAlternatives(details, action, options.stream()
            .<Component>map(option -> Component.translatable(PREFIX + "amount", count, option)).toList());
    }

    private static void addAlternatives(List<Component> details, String action, List<Component> options) {
        if (options.isEmpty()) return;
        details.add(Component.translatable(PREFIX + action, options.getFirst()));
        for (int i = 1; i < Math.min(options.size(), MAX_ALTERNATIVES); i++) {
            details.add(Component.translatable(PREFIX + "alternative", options.get(i)));
        }
        if (options.size() > MAX_ALTERNATIVES) {
            details.add(Component.translatable(PREFIX + "alternative",
                Component.translatable(PREFIX + "more_options", options.size() - MAX_ALTERNATIVES)));
        }
    }

    private static Component describeWorkBlock(BlockState state) {
        Component name = state.getBlock().getName();
        if (state.is(ModBlocks.NEUTRON_IRRADIATOR.get())) {
            return Component.translatable(PREFIX + "block_state", name,
                Component.translatable(PREFIX + "irradiation_type." + state.getValue(NeutronIrradiatorBlock.TYPE).getSerializedName()));
        }
        if (state.is(ModBlocks.CORRUPTED_BEACON.get()) && state.getValue(CorruptedBeaconBlock.LIT)) {
            return Component.translatable(PREFIX + "block_state", name, Component.translatable(PREFIX + "activated"));
        }
        if (state.is(ModBlocks.HEATER.get()) && !state.getValue(HeaterBlock.POWERED) && !state.getValue(HeaterBlock.OVERLOAD)) {
            return Component.translatable(PREFIX + "block_state", name, Component.translatable(PREFIX + "heater_ready"));
        }
        if (state.is(ModBlocks.BURNING_HEATER.get())) {
            String heat = switch (state.getValue(BurningHeaterBlock.LEVEL)) {
                case 1 -> "smoldering";
                case 2 -> "lit";
                default -> "off";
            };
            return Component.translatable(PREFIX + "block_state", name,
                Component.translatable("tooltip.anvilcraft.burning_heater.jade.state." + heat));
        }
        return name;
    }
}
