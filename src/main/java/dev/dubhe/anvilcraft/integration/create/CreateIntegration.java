package dev.dubhe.anvilcraft.integration.create;

import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.api.integration.Integration;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.heatable.GlowingBlock;
import dev.dubhe.anvilcraft.block.heatable.IncandescentBlock;
import dev.dubhe.anvilcraft.block.heatable.RedhotBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemGroups;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.item.amulet.AmuletItem;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@Integration("create")
public class CreateIntegration {
    private static final BoilerHeater HEATER = CreateIntegration::heater;
    private static final BoilerHeater REDHOT = new ConstantValueHeater(1);
    private static final BoilerHeater GLOWING = new ConstantValueHeater(2);
    private static final BoilerHeater INCANDESCENT = new ConstantValueHeater(3);
    private static final DeferredRegister<AmuletType> REGISTER = DeferredRegister.create(ModRegistries.AMULET_TYPE_KEY, AnvilCraft.MOD_ID);

    private static float heater(Level level, BlockPos blockPos, @NotNull BlockState blockState) {
        if (blockState.is(ModBlocks.HEATER) && !blockState.getValue(HeaterBlock.OVERLOAD)) {
            return 1;
        }
        return -1;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void apply() {
        BoilerHeater.REGISTRY.registerProvider(new MyProvider());
        AnvilCraft.MOD_BUS.addListener(this::registerToTab);
        UnpackingHandler.REGISTRY.registerProvider(new BatchCrafterUnpackingHandler.Provider());
        REGISTER.register(AnvilCraft.MOD_BUS);
        AmuletManager.INSTANCE.registerAmulets(COGWHEEL_AMULET::get);
    }

    private void registerToTab(@NotNull BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ModItemGroups.ANVILCRAFT_TOOL.getKey())) {
            event.insertAfter(
                ModItems.ANVIL_AMULET.asStack(),
                COGWHEEL_AMULET.asStack(),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }

    private static class MyProvider implements SimpleRegistry.Provider<Block, BoilerHeater> {

        @Override
        public @Nullable BoilerHeater get(Block block) {
            if (block == ModBlocks.HEATER.get()) {
                return HEATER;
            }
            if (block instanceof IncandescentBlock) {
                return INCANDESCENT;
            }
            if (block instanceof GlowingBlock) {
                return GLOWING;
            }
            if (block instanceof RedhotBlock) {
                return REDHOT;
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

    public static final ItemEntry<? extends AmuletItem> COGWHEEL_AMULET = REGISTRATE
        .item(
            "cogwheel_amulet",
            properties -> new AmuletItem(properties) {
                @Override
                public Holder<AmuletType> getType() {
                    return COGWHEEL;
                }
            }
        )
        .properties(properties -> properties.stacksTo(1))
        .removeTab(ModItemGroups.ANVILCRAFT_INGREDIENTS.getKey())
        .recipe((ctx, provider) -> JewelCraftingRecipe.builder()
            .withCondition(new ModLoadedCondition("create"))
            .requires(ModItems.SILVER_INGOT, 1)
            .requires(AllItems.PRECISION_MECHANISM, 16)
            .result(new ItemStack(ctx.get()))
            .save(provider)
        )
        .register();


    private static final DeferredHolder<AmuletType, ? extends AmuletType> COGWHEEL = REGISTER.register(
        "cogwheel", AmuletType.builder()
            .obtainByDamage(
                AllDamageTypes.CRUSH,
                AllDamageTypes.CUCKOO_SURPRISE,
                AllDamageTypes.DRILL,
                AllDamageTypes.POTATO_CANNON,
                AllDamageTypes.ROLLER,
                AllDamageTypes.RUN_OVER,
                AllDamageTypes.SAW)
            .obtainOr((player, source) -> source.typeHolder().is(DamageTypes.PLAYER_ATTACK) && Optional.ofNullable(source.getEntity())
                .map(entity -> Util.instanceOfAny(entity, DeployerFakePlayer.class))
                .orElse(false))
            .immuneDamageFromObtain()
            .amulet(COGWHEEL_AMULET)
            ::build
    );
}
