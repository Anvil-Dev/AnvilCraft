package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelRenderer;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.Blocks;

public final class StellarEvolutionRendererChecks {
    public static void verify() {
        var renderer = BlockStateModelRenderer.INSTANCE.getTessellatorNoLighting();
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(Blocks.WHITE_CONCRETE.defaultBlockState());
        renderer.tesselateBlock((x, y, z, quad, instance) -> instance.setColor(0x03010203),
            0, 0, 0, BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), model, 42);
        int[] count = {0};
        renderer.tesselateBlock((x, y, z, quad, instance) -> {
            for (int vertex = 0; vertex < 4; vertex++) {
                if (instance.getColor(vertex) != -1) throw new IllegalStateException("Previous stellar tint leaked into the next model");
            }
            count[0]++;
        }, 0, 0, 0, BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), model, 42);
        if (count[0] != 6) throw new IllegalStateException("Missing geometry in stellar tint isolation check");
        AnvilCraft.LOGGER.info("PORT_STELLAR_TINT_ISOLATION_PASSED: all six faces reset after an alpha-tinted draw");
        verifyReplacement();
    }

    private static void verifyReplacement() {
        var be = new CelestialForgingAnvilBlockEntity(ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(),
            BlockPos.ZERO, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState());
        be.setLevel(Minecraft.getInstance().level);
        var original = new StarData(CelestialBodyClass.G_MAIN, 32, 255, 220, 160, 0, 0, 0, 32, null);
        var remnant = new StarData(CelestialBodyClass.WHITE_DWARF, 8, 255, 255, 255, 0, 0, 0, 47, null);
        var replacement = new StarData(CelestialBodyClass.F_MAIN, 32, 255, 255, 255, 0, 0, 0, 40, null);
        try {
            var method = CelestialForgingAnvilBlockEntity.class.getDeclaredMethod("detectAnimationTransition",
                CelestialBodyData.class, CelestialBodyData.class);
            method.setAccessible(true);
            method.invoke(be, original, remnant);
            if (be.getAnimationTicks() != 0) throw new IllegalStateException("Remnant replayed the discovery animation");
            method.invoke(be, original, replacement);
            if (be.getAnimationTicks() <= 0) throw new IllegalStateException("Ordinary replacement lost its discovery animation");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        AnvilCraft.LOGGER.info("PORT_STELLAR_REPLACEMENT_PASSED: continuous remnant and ordinary discovery transitions");
    }

    public static void verifyBounds(CelestialForgingAnvilBlockEntity be) {
        var client = Minecraft.getInstance();
        var renderer = client.getBlockEntityRenderDispatcher()
            .<CelestialForgingAnvilBlockEntity, CFARenderState>getRenderer(be);
        if (renderer == null) throw new IllegalStateException("Missing CFA renderer");
        var bounds = renderer.getRenderBoundingBox(be);
        var visual = be.getStellarVisualState(0);
        float shell = visual == null ? 1 : Math.clamp(visual.ejectaRadius() / Math.max(visual.radius(), 0.01F), 1, 12);
        float radius = be.getSmoothBodyScale() * Math.max(2.4F, shell) / 2;
        double centerX = be.getBlockPos().getX() + 0.5;
        double centerY = be.getBlockPos().getY() + be.getSmoothCenterY();
        double centerZ = be.getBlockPos().getZ() + 0.5;
        if (!bounds.contains(centerX - radius, centerY - radius, centerZ - radius)
            || !bounds.contains(centerX + radius, centerY + radius, centerZ + radius)) {
            throw new IllegalStateException("Evolution surface or ejecta escaped its culling bounds");
        }
        var camera = client.gameRenderer.getMainCamera();
        AnvilCraft.LOGGER.info("PORT_STELLAR_ENV_FOG: start={}, end={}",
            camera.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, 0),
            camera.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, 0));
    }
}
