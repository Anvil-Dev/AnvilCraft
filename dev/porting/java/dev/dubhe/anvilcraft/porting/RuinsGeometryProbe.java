package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsRenderContext;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class RuinsGeometryProbe {
    private static BlockPos outlinePos = BlockPos.ZERO;
    private static int outlineRenderers;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void outline(ExtractBlockOutlineRenderStateEvent event) {
        if (!Boolean.getBoolean("anvilcraft.portRuinsGeometryScene")) return;
        outlinePos = event.getBlockPos().immutable();
        outlineRenderers = event.getCustomRenderers().size();
    }

    public static void verify(Minecraft client) {
        for (var pos : List.of(RuinsGeometryClientScene.CRATE, RuinsGeometryClientScene.CRATE.above().east(),
            RuinsGeometryClientScene.ARM)) {
            var ruins = (RuinsBlockEntity) client.level.getBlockEntity(pos);
            var renderer = renderer(client, ruins);
            var bounds = renderer.getRenderBoundingBox(ruins);
            CubeSelection.Target expected;
            try (var ignored = RuinsRenderContext.enter(client.level)) {
                renderer.prepareDisplay(ruins);
                expected = CubeSelection.target(client.level, pos, ruins.getDisplayState(), 0);
            }
            CubeSelection.Target actual = CubeSelection.target(client.level, pos, ruins.getBlockState(), 0);
            require(actual != null && expected != null && actual.state() == ruins.getDisplayState(), "Disguised picking state");
            require(actual.parts().size() == expected.parts().size() && actual.bounds().equals(expected.bounds()),
                "Disguised picking uses original geometry and dynamic pose");
            for (var part : actual.parts()) {
                var box = part.bounds().move(pos);
                require(bounds.inflate(0.001).contains(box.minX, box.minY, box.minZ)
                    && bounds.inflate(0.001).contains(box.maxX, box.maxY, box.maxZ), "Render bounds contain selected geometry");
            }
            if (!pos.equals(RuinsGeometryClientScene.ARM)) {
                require(bounds.getXsize() >= 3 && bounds.getYsize() >= 3 && bounds.getZsize() >= 3, "Whole multipart bounds");
                require(client.level.getGloballyRenderedBlockEntities().contains(ruins), "Multipart globally collected");
                require(!inSection(client, ruins), "Global multipart is not also submitted through its section");
            } else {
                var parts = ModelBlockSelection.rendererParts(ruins, 0);
                require(!parts.isEmpty(), "Ruins renderer preserves dynamic arm selection parts");
                require(!client.level.getGloballyRenderedBlockEntities().contains(ruins), "Ordinary arm remains section collected");
                require(inSection(client, ruins), "Ordinary arm included in section");
            }
            if (Boolean.getBoolean("anvilcraft.portSodiumScene")) {
                RuinsSodiumGeometryProbe.verify(ruins, !pos.equals(RuinsGeometryClientScene.ARM));
            }
            var camera = new Vec3(bounds.maxX + renderer.getViewDistance() - 0.25, bounds.getCenter().y, bounds.getCenter().z);
            require(renderer.shouldRender(ruins, camera), "Range is measured from model extent rather than block center");
        }
        verifyBeacon(client, true);
        verifyOutline(client);
        AnvilCraft.LOGGER.info("PORT_RUINS_GEOMETRY_PROBE_PASSED: original targets, child poses, whole bounds and section/global routing");
    }

    public static void verifyOutline(Minecraft client) {
        require(client.hitResult instanceof BlockHitResult hit && outlinePos.equals(hit.getBlockPos()) && outlineRenderers > 0,
            "Actual outline extraction uses custom disguise geometry for the current target");
    }

    public static void verifyBeacon(Minecraft client, boolean global) {
        var ruins = (RuinsBlockEntity) client.level.getBlockEntity(RuinsGeometryClientScene.BEACON);
        require(client.level.getGloballyRenderedBlockEntities().contains(ruins) == global, "Updated disguise changes global collection");
        require(inSection(client, ruins) != global, "Section and global collection cannot duplicate a disguise");
        if (Boolean.getBoolean("anvilcraft.portSodiumScene")) RuinsSodiumGeometryProbe.verify(ruins, global);
    }

    private static RuinsBlockEntityRenderer renderer(Minecraft client, RuinsBlockEntity ruins) {
        Object renderer = client.getBlockEntityRenderDispatcher().getRenderer(ruins);
        return (RuinsBlockEntityRenderer) renderer;
    }

    private static boolean inSection(Minecraft client, RuinsBlockEntity ruins) {
        var models = client.getModelManager();
        var compiler = new SectionCompiler(true, false, models.getBlockStateModelSet(), models.getFluidStateModelSet(),
            client.getBlockColors(), client.getBlockEntityRenderDispatcher());
        var results = new SectionCompiler.Results();
        try {
            var method = SectionCompiler.class.getDeclaredMethod("handleBlockEntity", SectionCompiler.Results.class, BlockEntity.class);
            method.setAccessible(true);
            method.invoke(compiler, results, ruins);
            return results.blockEntities.contains(ruins);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
