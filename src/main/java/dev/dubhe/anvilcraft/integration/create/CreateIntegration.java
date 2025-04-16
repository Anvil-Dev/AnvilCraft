package dev.dubhe.anvilcraft.integration.create;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.integration.Integration;
import dev.dubhe.anvilcraft.block.GlowingMetalBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.IncandescentMetalBlock;
import dev.dubhe.anvilcraft.block.RedhotMetalBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemGroups;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.amulet.CogwheelAmuletItem;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.util.AmuletUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@Integration("create")
public class CreateIntegration {
    private static final BoilerHeater HEATER = CreateIntegration::heater;
    private static final BoilerHeater REDHOT_METAL = new ConstantValueHeater(1);
    private static final BoilerHeater GLOWING_METAL = new ConstantValueHeater(2);
    private static final BoilerHeater INCANDESCENT_METAL = new ConstantValueHeater(3);

    public void apply() {
        BoilerHeater.REGISTRY.registerProvider(new MyProvider());
        AnvilCraft.MOD_BUS.addListener(this::registerToTab);
    }

    private static float heater(Level level, BlockPos blockPos, BlockState blockState) {
        if (blockState.is(ModBlocks.HEATER) && !blockState.getValue(HeaterBlock.OVERLOAD)) {
            return 1;
        }
        return -1;
    }

    private static class MyProvider implements SimpleRegistry.Provider<Block, BoilerHeater> {

        @Override
        public @Nullable BoilerHeater get(Block block) {
            if (block == ModBlocks.HEATER.get()) {
                return HEATER;
            }
            if (block instanceof IncandescentMetalBlock) {
                return INCANDESCENT_METAL;
            }
            if (block instanceof GlowingMetalBlock) {
                return GLOWING_METAL;
            }
            if (block instanceof RedhotMetalBlock) {
                return REDHOT_METAL;
            }
            return null;
        }
    }

    private record ConstantValueHeater(float level) implements BoilerHeater {

        @Override
        public float getHeat(Level level, BlockPos blockPos, BlockState blockState) {
            return this.level;
        }
    }

    private void registerToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ModItemGroups.ANVILCRAFT_TOOL.getKey())) {
            event.insertAfter(
                ModItems.ANVIL_AMULET.asStack(), COGWHEEL_AMULET.asStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }

    public static final ItemEntry<CogwheelAmuletItem> COGWHEEL_AMULET = REGISTRATE
        .item("cogwheel_amulet", CogwheelAmuletItem::new)
        .properties(properties -> properties.stacksTo(1))
        .recipe((ctx, provider) -> JewelCraftingRecipe.builder()
            .requires(ModItems.SILVER_INGOT, 1)
            .requires(AllItems.PRECISION_MECHANISM, 16)
            .result(new ItemStack(ctx.get()))
            .save(provider))
        .register();

    static {
        AmuletUtil.registerCustomType(new AmuletUtil.Type(
            "cogwheel", (sources, source) ->
            ModList.get().isLoaded("create")
            && Objects.requireNonNull(sources.damageTypes.getKey(source.type())).getNamespace().contains("create"),
            COGWHEEL_AMULET
        ));
    }
}
